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
package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.SendOpcode;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.data.output.MaplePacketLittleEndianWriter;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;

/**
 *
 * @author Matze
 */
public class MaplePlayerShop extends AbstractMapleMapObject {
	private final MapleCharacter owner;
	private final MapleCharacter[] visitors = new MapleCharacter[3];
	private final List<MaplePlayerShopItem> items = new ArrayList<>();
	private final MapleCharacter[] slot = { null, null, null };
	private String description;
	private int boughtnumber = 0;
	private final List<String> bannedList = new ArrayList<>();

	public MaplePlayerShop(MapleCharacter owner, String description) {
		this.setPosition(owner.getPosition());
		this.owner = owner;
		this.description = description;
	}

	public boolean hasFreeSlot() {
		return (this.visitors[0] == null) || (this.visitors[1] == null)
				|| (this.visitors[2] == null);
	}

	public boolean isOwner(MapleCharacter c) {
		return this.owner.equals(c);
	}

	public void addVisitor(MapleCharacter visitor) {
		for (int i = 0; i < 3; i++) {
			if (this.visitors[i] == null) {
				this.visitors[i] = visitor;
				if (this.getSlot(0) == null) {
					this.setSlot(visitor, 0);
					this.broadcast(MaplePacketCreator.getPlayerShopNewVisitor(
							visitor, 1));
				} else if (this.getSlot(1) == null) {
					this.setSlot(visitor, 1);
					this.broadcast(MaplePacketCreator.getPlayerShopNewVisitor(
							visitor, 2));
				} else if (this.getSlot(2) == null) {
					this.setSlot(visitor, 2);
					this.broadcast(MaplePacketCreator.getPlayerShopNewVisitor(
							visitor, 3));
					visitor.getMap().broadcastMessage(
							MaplePacketCreator.addCharBox(this.getOwner(), 1));
				}
				break;
			}
		}
	}

	public void removeVisitor(MapleCharacter visitor) {
		if (visitor == this.owner) {
			this.owner.getMap().removeMapObject(this);
			this.owner.setPlayerShop(null);
		}
		for (int i = 0; i < 3; i++) {
			if ((this.visitors[i] != null)
					&& (this.visitors[i].getId() == visitor.getId())) {
				final int slot_ = visitor.getSlot();
				this.visitors[i] = null;
				this.setSlot(null, i);
				visitor.setSlot(-1);
				this.broadcast(MaplePacketCreator
						.getPlayerShopRemoveVisitor(slot_ + 1));
				return;
			}
		}
	}

	public boolean isVisitor(MapleCharacter visitor) {
		return (this.visitors[0] == visitor) || (this.visitors[1] == visitor)
				|| (this.visitors[2] == visitor);
	}

	public void addItem(MaplePlayerShopItem item) {
		this.items.add(item);
	}

	public void removeItem(int item) {
		this.items.remove(item);
	}

	/**
	 * no warnings for now o.op
	 *
	 * @param c
	 * @param item
	 * @param quantity
	 */
	public void buy(MapleClient c, int item, short quantity) {
		if (this.isVisitor(c.getPlayer())) {
			final MaplePlayerShopItem pItem = this.items.get(item);
			final Item newItem = pItem.getItem().copy();
			newItem.setQuantity(newItem.getQuantity());
			if ((quantity < 1) || (pItem.getBundles() < 1)
					|| (newItem.getQuantity() > pItem.getBundles())
					|| !pItem.isExist()) {
				return;
			} else if ((newItem.getType() == 1) && (newItem.getQuantity() > 1)) {
				return;
			}
			synchronized (c.getPlayer()) {
				if (c.getPlayer().getMeso() >= ((long) pItem.getPrice() * quantity)) {
					if (MapleInventoryManipulator
							.addFromDrop(c, newItem, false)) {
						c.getPlayer().gainMeso(-pItem.getPrice() * quantity,
								true);
						this.owner.gainMeso(pItem.getPrice() * quantity, true);
						pItem.setBundles((short) (pItem.getBundles() - quantity));
						if (pItem.getBundles() < 1) {
							pItem.setDoesExist(false);
							if (++this.boughtnumber == this.items.size()) {
								this.owner.setPlayerShop(null);
								this.owner.getMap().broadcastMessage(
										MaplePacketCreator
												.removeCharBox(this.owner));
								this.removeVisitors();
								this.owner
										.dropMessage(1,
												"Your items are sold out, and therefore your shop is closed.");
							}
						}
					} else {
						c.getPlayer()
								.dropMessage(1,
										"Your inventory is full. Please clean a slot before buying this item.");
					}
				}
			}
		}
	}

	public void broadcastToVisitors(final byte[] packet) {
		for (int i = 0; i < 3; i++) {
			if (this.visitors[i] != null) {
				this.visitors[i].getClient().announce(packet);
			}
		}
	}

	public void removeVisitors() {
		try {
			for (int i = 0; i < 3; i++) {
				if (this.visitors[i] != null) {
					this.visitors[i].getClient().announce(
							MaplePacketCreator.shopErrorMessage(10, 1));
					this.removeVisitor(this.visitors[i]);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		if (this.owner != null) {
			this.removeVisitor(this.getOwner());
		}
	}

	public static byte[] shopErrorMessage(int error, int type) {
		final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0x0A);
		mplew.write(type);
		mplew.write(error);
		return mplew.getPacket();
	}

	public void broadcast(final byte[] packet) {
		if ((this.owner.getClient() != null)
				&& (this.owner.getClient().getSession() != null)) {
			this.owner.getClient().announce(packet);
		}
		this.broadcastToVisitors(packet);
	}

	public void chat(MapleClient c, String chat) {
		byte s = 0;
		for (final MapleCharacter mc : this.getVisitors()) {
			s++;
			if (mc != null) {
				if (mc.getName().equalsIgnoreCase(c.getPlayer().getName())) {
					break;
				}
			} else if (s == 3) {
				s = 0;
			}
		}
		this.broadcast(MaplePacketCreator.getPlayerShopChat(c.getPlayer(),
				chat, s));
	}

	public void sendShop(MapleClient c) {
		c.announce(MaplePacketCreator.getPlayerShop(c, this,
				this.isOwner(c.getPlayer())));
	}

	public MapleCharacter getOwner() {
		return this.owner;
	}

	public MapleCharacter[] getVisitors() {
		return this.visitors;
	}

	public MapleCharacter getSlot(int s) {
		return this.slot[s];
	}

	private void setSlot(MapleCharacter person, int s) {
		this.slot[s] = person;
		if (person != null) {
			person.setSlot(s);
		}
	}

	public List<MaplePlayerShopItem> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void banPlayer(String name) {
		if (!this.bannedList.contains(name)) {
			this.bannedList.add(name);
		}
		for (int i = 0; i < 3; i++) {
			if ((this.visitors[i] != null)
					&& this.visitors[i].getName().equals(name)) {
				this.visitors[i].getClient().announce(
						MaplePacketCreator.shopErrorMessage(5, 1));
				this.removeVisitor(this.visitors[i]);
			}
			return;
		}
	}

	public boolean isBanned(String name) {
		return this.bannedList.contains(name);
	}

	@Override
	public void sendDestroyData(MapleClient client) {
		client.announce(MaplePacketCreator.removeCharBox(this.getOwner()));
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		client.announce(MaplePacketCreator.addCharBox(this.getOwner(), 4));
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.SHOP;
	}
}