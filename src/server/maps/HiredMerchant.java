/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.maps;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.server.Server;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePlayerShopItem;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;

/**
 *
 * @author XoticStory
 */
public class HiredMerchant extends AbstractMapleMapObject {

	private final int ownerId, itemId, mesos = 0;
	private final int channel, world;
	private final long start;
	private String ownerName = "";
	private String description = "";
	private final MapleCharacter[] visitors = new MapleCharacter[3];
	private final List<MaplePlayerShopItem> items = new LinkedList<>();
	private final List<Pair<String, Byte>> messages = new LinkedList<>();
	private final List<SoldItem> sold = new LinkedList<>();
	private boolean open;
	public ScheduledFuture<?> schedule = null;
	private MapleMap map;

	public HiredMerchant(final MapleCharacter owner, int itemId, String desc) {
		this.setPosition(owner.getPosition());
		this.start = System.currentTimeMillis();
		this.ownerId = owner.getId();
		this.channel = owner.getClient().getChannel();
		this.world = owner.getWorld();
		this.itemId = itemId;
		this.ownerName = owner.getName();
		this.description = desc;
		this.map = owner.getMap();
		this.schedule = TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				HiredMerchant.this.closeShop(owner.getClient(), true);
			}
		}, 1000 * 60 * 60 * 24);
	}

	public void broadcastToVisitors(final byte[] packet) {
		for (final MapleCharacter visitor : this.visitors) {
			if (visitor != null) {
				visitor.getClient().announce(packet);
			}
		}
	}

	public void addVisitor(MapleCharacter visitor) {
		final int i = this.getFreeSlot();
		if (i > -1) {
			this.visitors[i] = visitor;
			this.broadcastToVisitors(MaplePacketCreator
					.hiredMerchantVisitorAdd(visitor, i + 1));
		}
	}

	public void removeVisitor(MapleCharacter visitor) {
		final int slot = this.getVisitorSlot(visitor);
		if (this.visitors[slot] == visitor) {
			this.visitors[slot] = null;
			if (slot != -1) {
				this.broadcastToVisitors(MaplePacketCreator
						.hiredMerchantVisitorLeave(slot + 1));
			}
		}
	}

	public int getVisitorSlot(MapleCharacter visitor) {
		for (int i = 0; i < 3; i++) {
			if (this.visitors[i] == visitor) {
				return i;
			}
		}
		return -1; // Actually 0 because of the +1's.
	}

	public void removeAllVisitors(String message) {
		for (int i = 0; i < 3; i++) {
			if (this.visitors[i] != null) {
				this.visitors[i].setHiredMerchant(null);
				this.visitors[i].getClient().announce(
						MaplePacketCreator.leaveHiredMerchant(i + 1, 0x11));
				if (message.length() > 0) {
					this.visitors[i].dropMessage(1, message);
				}
				this.visitors[i] = null;
			}
		}
	}

	public void buy(MapleClient c, int item, short quantity) {
		final MaplePlayerShopItem pItem = this.items.get(item);
		synchronized (this.items) {
			final Item newItem = pItem.getItem().copy();
			newItem.setQuantity((short) ((pItem.getItem().getQuantity() * quantity)));
			if ((newItem.getFlag() & ItemConstants.KARMA) == ItemConstants.KARMA) {
				newItem.setFlag((byte) (newItem.getFlag() ^ ItemConstants.KARMA));
			}
			if ((newItem.getType() == 2)
					&& ((newItem.getFlag() & ItemConstants.SPIKES) == ItemConstants.SPIKES)) {
				newItem.setFlag((byte) (newItem.getFlag() ^ ItemConstants.SPIKES));
			}
			if ((quantity < 1) || (pItem.getBundles() < 1) || !pItem.isExist()
					|| (pItem.getBundles() < quantity)) {
				c.announce(MaplePacketCreator.enableActions());
				return;
			} else if ((newItem.getType() == 1) && (newItem.getQuantity() > 1)) {
				c.announce(MaplePacketCreator.enableActions());
				return;
			} else if (!pItem.isExist()) {
				c.announce(MaplePacketCreator.enableActions());
				return;
			}
			final int price = pItem.getPrice() * quantity;
			if (c.getPlayer().getMeso() >= price) {
				if (MapleInventoryManipulator.addFromDrop(c, newItem, true)) {
					c.getPlayer().gainMeso(-price, false);
					this.sold.add(new SoldItem(c.getPlayer().getName(), pItem
							.getItem().getItemId(), quantity, price));
					pItem.setBundles((short) (pItem.getBundles() - quantity));
					if (pItem.getBundles() < 1) {
						pItem.setDoesExist(false);
					}
					final MapleCharacter owner = Server.getInstance()
							.getWorld(this.world).getPlayerStorage()
							.getCharacterByName(this.ownerName);
					if (owner != null) {
						owner.addMerchantMesos(price);
					} else {
						try {
							try (PreparedStatement ps = DatabaseConnection
									.getConnection()
									.prepareStatement(
											"UPDATE characters SET MerchantMesos = MerchantMesos + "
													+ price + " WHERE id = ?",
											java.sql.Statement.RETURN_GENERATED_KEYS)) {
								ps.setInt(1, this.ownerId);
								ps.executeUpdate();
							}
						} catch (final Exception e) {
						}
					}
				} else {
					c.getPlayer()
							.dropMessage(1,
									"Your inventory is full. Please clean a slot before buying this item.");
				}
			} else {
				c.getPlayer().dropMessage(1, "You do not have enough mesos.");
			}
			try {
				this.saveItems(false);
			} catch (final Exception e) {
			}
		}
	}

	public void forceClose() {
		if (this.schedule != null) {
			this.schedule.cancel(false);
		}
		try {
			this.saveItems(true);
		} catch (final SQLException ex) {
		}
		Server.getInstance().getChannel(this.world, this.channel)
				.removeHiredMerchant(this.ownerId);
		this.map.broadcastMessage(MaplePacketCreator.destroyHiredMerchant(this
				.getOwnerId()));

		this.map.removeMapObject(this);

		this.map = null;
		this.schedule = null;
	}

	public void closeShop(MapleClient c, boolean timeout) {
		this.map.removeMapObject(this);
		this.map.broadcastMessage(MaplePacketCreator
				.destroyHiredMerchant(this.ownerId));
		c.getChannelServer().removeHiredMerchant(this.ownerId);
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE characters SET HasMerchant = 0 WHERE id = ?",
							java.sql.Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, this.ownerId);
				ps.executeUpdate();
			}
			if (check(c.getPlayer(), this.getItems()) && !timeout) {
				for (final MaplePlayerShopItem mpsi : this.getItems()) {
					if (mpsi.isExist() && (mpsi.getItem().getType() == 1)) {
						MapleInventoryManipulator.addFromDrop(c,
								mpsi.getItem(), false);
					} else if (mpsi.isExist()) {
						MapleInventoryManipulator.addById(c, mpsi.getItem()
								.getItemId(), (short) (mpsi.getBundles() * mpsi
								.getItem().getQuantity()), null, -1, mpsi
								.getItem().getExpiration());
					}
				}
				this.items.clear();
			}
			try {
				this.saveItems(false);
			} catch (final Exception e) {
			}
			this.items.clear();

		} catch (final Exception e) {
		}
		this.schedule.cancel(false);
	}

	public String getOwner() {
		return this.ownerName;
	}

	public int getOwnerId() {
		return this.ownerId;
	}

	public String getDescription() {
		return this.description;
	}

	public MapleCharacter[] getVisitors() {
		return this.visitors;
	}

	public List<MaplePlayerShopItem> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	public void addItem(MaplePlayerShopItem item) {
		this.items.add(item);
		try {
			this.saveItems(false);
		} catch (final SQLException ex) {
		}
	}

	public void removeFromSlot(int slot) {
		this.items.remove(slot);
		try {
			this.saveItems(false);
		} catch (final SQLException ex) {
		}
	}

	public int getFreeSlot() {
		for (int i = 0; i < 3; i++) {
			if (this.visitors[i] == null) {
				return i;
			}
		}
		return -1;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isOpen() {
		return this.open;
	}

	public void setOpen(boolean set) {
		this.open = set;
	}

	public int getItemId() {
		return this.itemId;
	}

	public boolean isOwner(MapleCharacter chr) {
		return chr.getId() == this.ownerId;
	}

	public void saveItems(boolean shutdown) throws SQLException {
		final List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

		for (final MaplePlayerShopItem pItems : this.items) {
			final Item newItem = pItems.getItem();
			if (shutdown) {
				newItem.setQuantity((short) (pItems.getItem().getQuantity() * pItems
						.getBundles()));
			} else {
				newItem.setQuantity(pItems.getItem().getQuantity());
			}
			if (pItems.getBundles() > 0) {
				itemsWithType.add(new Pair<>(newItem, MapleInventoryType
						.getByType(newItem.getType())));
			}
		}
		ItemFactory.MERCHANT.saveItems(itemsWithType, this.ownerId);
	}

	private static boolean check(MapleCharacter chr,
			List<MaplePlayerShopItem> items) {
		byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
		final List<MapleInventoryType> li = new LinkedList<>();
		for (final MaplePlayerShopItem item : items) {
			final MapleInventoryType invtype = MapleItemInformationProvider
					.getInstance().getInventoryType(item.getItem().getItemId());
			if (!li.contains(invtype)) {
				li.add(invtype);
			}
			if (invtype == MapleInventoryType.EQUIP) {
				eq++;
			} else if (invtype == MapleInventoryType.USE) {
				use++;
			} else if (invtype == MapleInventoryType.SETUP) {
				setup++;
			} else if (invtype == MapleInventoryType.ETC) {
				etc++;
			} else if (invtype == MapleInventoryType.CASH) {
				cash++;
			}
		}
		for (final MapleInventoryType mit : li) {
			if (mit == MapleInventoryType.EQUIP) {
				if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() <= eq) {
					return false;
				}
			} else if (mit == MapleInventoryType.USE) {
				if (chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() <= use) {
					return false;
				}
			} else if (mit == MapleInventoryType.SETUP) {
				if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() <= setup) {
					return false;
				}
			} else if (mit == MapleInventoryType.ETC) {
				if (chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() <= etc) {
					return false;
				}
			} else if (mit == MapleInventoryType.CASH) {
				if (chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() <= cash) {
					return false;
				}
			}
		}
		return true;
	}

	public int getChannel() {
		return this.channel;
	}

	public int getTimeLeft() {
		return (int) ((System.currentTimeMillis() - this.start) / 1000);
	}

	public List<Pair<String, Byte>> getMessages() {
		return this.messages;
	}

	public int getMapId() {
		return this.map.getId();
	}

	public List<SoldItem> getSold() {
		return this.sold;
	}

	public int getMesos() {
		return this.mesos;
	}

	@Override
	public void sendDestroyData(MapleClient client) {
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.HIRED_MERCHANT;
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		client.announce(MaplePacketCreator.spawnHiredMerchant(this));
	}

	public class SoldItem {

		int itemid, mesos;
		short quantity;
		String buyer;

		public SoldItem(String buyer, int itemid, short quantity, int mesos) {
			this.buyer = buyer;
			this.itemid = itemid;
			this.quantity = quantity;
			this.mesos = mesos;
		}

		public String getBuyer() {
			return this.buyer;
		}

		public int getItemId() {
			return this.itemid;
		}

		public short getQuantity() {
			return this.quantity;
		}

		public int getMesos() {
			return this.mesos;
		}
	}
}
