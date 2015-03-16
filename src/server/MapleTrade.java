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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;

/**
 *
 * @author Matze
 */
public class MapleTrade {
	private MapleTrade partner = null;
	private final List<Item> items = new ArrayList<>();
	private List<Item> exchangeItems;
	private int meso = 0;
	private int exchangeMeso;
	boolean locked = false;
	private final MapleCharacter chr;
	private final byte number;

	public MapleTrade(byte number, MapleCharacter c) {
		this.chr = c;
		this.number = number;
	}

	private static int getFee(int meso) {
		int fee = 0;
		if (meso >= 100000000) {
			fee = (int) Math.round(0.06 * meso);
		} else if (meso >= 25000000) {
			fee = meso / 20;
		} else if (meso >= 10000000) {
			fee = meso / 25;
		} else if (meso >= 5000000) {
			fee = (int) Math.round(.03 * meso);
		} else if (meso >= 1000000) {
			fee = (int) Math.round(.018 * meso);
		} else if (meso >= 100000) {
			fee = meso / 125;
		}
		return fee;
	}

	private void lock() {
		this.locked = true;
		this.partner.getChr().getClient()
				.announce(MaplePacketCreator.getTradeConfirmation());
	}

	private void complete1() {
		this.exchangeItems = this.partner.getItems();
		this.exchangeMeso = this.partner.getMeso();
	}

	private void complete2() {
		this.items.clear();
		this.meso = 0;
		for (final Item item : this.exchangeItems) {
			if ((item.getFlag() & ItemConstants.KARMA) == ItemConstants.KARMA) {
				item.setFlag((byte) (item.getFlag() ^ ItemConstants.KARMA)); // items
			} else if ((item.getType() == 2)
					&& ((item.getFlag() & ItemConstants.SPIKES) == ItemConstants.SPIKES)) {
				item.setFlag((byte) (item.getFlag() ^ ItemConstants.SPIKES));
			}

			MapleInventoryManipulator.addFromDrop(this.chr.getClient(), item,
					true);
		}
		if (this.exchangeMeso > 0) {
			this.chr.gainMeso(this.exchangeMeso - getFee(this.exchangeMeso),
					true, true, true);
		}
		this.exchangeMeso = 0;
		if (this.exchangeItems != null) {
			this.exchangeItems.clear();
		}
		this.chr.getClient().announce(
				MaplePacketCreator.getTradeCompletion(this.number));
	}

	private void cancel() {
		for (final Item item : this.items) {
			MapleInventoryManipulator.addFromDrop(this.chr.getClient(), item,
					true);
		}
		if (this.meso > 0) {
			this.chr.gainMeso(this.meso, true, true, true);
		}
		this.meso = 0;
		if (this.items != null) {
			this.items.clear();
		}
		this.exchangeMeso = 0;
		if (this.exchangeItems != null) {
			this.exchangeItems.clear();
		}
		this.chr.getClient().announce(
				MaplePacketCreator.getTradeCancel(this.number));
	}

	private boolean isLocked() {
		return this.locked;
	}

	private int getMeso() {
		return this.meso;
	}

	public void setMeso(int meso) {
		if (this.locked) {
			throw new RuntimeException("Trade is locked.");
		}
		if (meso < 0) {
			System.out.println("[h4x] " + this.chr.getName()
					+ " Trying to trade < 0 mesos");
			return;
		}
		if (this.chr.getMeso() >= meso) {
			this.chr.gainMeso(-meso, false, true, false);
			this.meso += meso;
			this.chr.getClient().announce(
					MaplePacketCreator.getTradeMesoSet((byte) 0, this.meso));
			if (this.partner != null) {
				this.partner
						.getChr()
						.getClient()
						.announce(
								MaplePacketCreator.getTradeMesoSet((byte) 1,
										this.meso));
			}
		} else {
		}
	}

	public void addItem(Item item) {
		this.items.add(item);
		this.chr.getClient().announce(
				MaplePacketCreator.getTradeItemAdd((byte) 0, item));
		if (this.partner != null) {
			this.partner
					.getChr()
					.getClient()
					.announce(
							MaplePacketCreator.getTradeItemAdd((byte) 1, item));
		}
	}

	public void chat(String message) {
		this.chr.getClient().announce(
				MaplePacketCreator.getTradeChat(this.chr, message, true));
		if (this.partner != null) {
			this.partner
					.getChr()
					.getClient()
					.announce(
							MaplePacketCreator.getTradeChat(this.chr, message,
									false));
		}
	}

	public MapleTrade getPartner() {
		return this.partner;
	}

	public void setPartner(MapleTrade partner) {
		if (this.locked) {
			return;
		}
		this.partner = partner;
	}

	public MapleCharacter getChr() {
		return this.chr;
	}

	public List<Item> getItems() {
		return new LinkedList<>(this.items);
	}

	private boolean fitsInInventory() {
		final MapleItemInformationProvider mii = MapleItemInformationProvider
				.getInstance();
		final Map<MapleInventoryType, Integer> neededSlots = new LinkedHashMap<>();
		for (final Item item : this.exchangeItems) {
			final MapleInventoryType type = mii.getInventoryType(item
					.getItemId());
			if (neededSlots.get(type) == null) {
				neededSlots.put(type, 1);
			} else {
				neededSlots.put(type, neededSlots.get(type) + 1);
			}
		}
		for (final Map.Entry<MapleInventoryType, Integer> entry : neededSlots
				.entrySet()) {
			if (this.chr.getInventory(entry.getKey()).isFull(
					entry.getValue() - 1)) {
				return false;
			}
		}
		return true;
	}

	public static void completeTrade(MapleCharacter c) {
		c.getTrade().lock();
		final MapleTrade local = c.getTrade();
		final MapleTrade partner = local.getPartner();
		if (partner.isLocked()) {
			local.complete1();
			partner.complete1();
			if (!local.fitsInInventory() || !partner.fitsInInventory()) {
				cancelTrade(c);
				c.addMessage("There is not enough inventory space to complete the trade.");
				partner.getChr()
						.addMessage(
								"There is not enough inventory space to complete the trade.");
				return;
			}
			if (local.getChr().getLevel() < 15) {
				if ((local.getChr().getMesosTraded() + local.exchangeMeso) > 1000000) {
					cancelTrade(c);
					local.getChr().getClient()
							.announce(MaplePacketCreator.sendMesoLimit());
					return;
				} else {
					local.getChr().addMesosTraded(local.exchangeMeso);
				}
			} else if (c.getTrade().getChr().getLevel() < 15) {
				if ((c.getMesosTraded() + c.getTrade().exchangeMeso) > 1000000) {
					cancelTrade(c);
					c.getClient().announce(MaplePacketCreator.sendMesoLimit());
					return;
				} else {
					c.addMesosTraded(local.exchangeMeso);
				}
			}
			local.complete2();
			partner.complete2();
			partner.getChr().setTrade(null);
			c.setTrade(null);
		}
	}

	public static void cancelTrade(MapleCharacter c) {
		c.getTrade().cancel();
		if (c.getTrade().getPartner() != null) {
			c.getTrade().getPartner().cancel();
			c.getTrade().getPartner().getChr().setTrade(null);
		}
		c.setTrade(null);
	}

	public static void startTrade(MapleCharacter c) {
		if (c.getTrade() == null) {
			c.setTrade(new MapleTrade((byte) 0, c));
			c.getClient().announce(
					MaplePacketCreator.getTradeStart(c.getClient(),
							c.getTrade(), (byte) 0));
		} else {
			c.addMessage("You are already in a trade.");
		}
	}

	public static void inviteTrade(MapleCharacter c1, MapleCharacter c2) {
		if (c2.getTrade() == null) {
			c2.setTrade(new MapleTrade((byte) 1, c2));
			c2.getTrade().setPartner(c1.getTrade());
			c1.getTrade().setPartner(c2.getTrade());
			c2.getClient().announce(MaplePacketCreator.getTradeInvite(c1));
		} else {
			c1.addMessage("The other player is already trading with someone else.");
			cancelTrade(c1);
		}
	}

	public static void visitTrade(MapleCharacter c1, MapleCharacter c2) {
		if ((c1.getTrade() != null)
				&& (c1.getTrade().getPartner() == c2.getTrade())
				&& (c2.getTrade() != null)
				&& (c2.getTrade().getPartner() == c1.getTrade())) {
			c2.getClient().announce(MaplePacketCreator.getTradePartnerAdd(c1));
			c1.getClient().announce(
					MaplePacketCreator.getTradeStart(c1.getClient(),
							c1.getTrade(), (byte) 1));
		} else {
			c1.addMessage("The other player has already closed the trade.");
		}
	}

	public static void declineTrade(MapleCharacter c) {
		final MapleTrade trade = c.getTrade();
		if (trade != null) {
			if (trade.getPartner() != null) {
				final MapleCharacter other = trade.getPartner().getChr();
				other.getTrade().cancel();
				other.setTrade(null);
				other.addMessage(c.getName()
						+ " has declined your trade request.");
			}
			trade.cancel();
			c.setTrade(null);
		}
	}
}