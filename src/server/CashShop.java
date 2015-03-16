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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.DatabaseConnection;
import tools.Pair;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.ItemConstants;

/*
 * @author Flav
 */
public class CashShop {
	public static class CashItem {

		private final int sn, itemId, price;
		private final long period;
		private final short count;
		private final boolean onSale;

		private CashItem(int sn, int itemId, int price, long period,
				short count, boolean onSale) {
			this.sn = sn;
			this.itemId = itemId;
			this.price = price;
			this.period = (period == 0 ? 90 : period);
			this.count = count;
			this.onSale = onSale;
		}

		public int getSN() {
			return this.sn;
		}

		public int getItemId() {
			return this.itemId;
		}

		public int getPrice() {
			return this.price;
		}

		public short getCount() {
			return this.count;
		}

		public boolean isOnSale() {
			return this.onSale;
		}

		public Item toItem() {
			final MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			Item item;

			int petid = -1;

			if (ItemConstants.isPet(this.itemId)) {
				petid = MaplePet.createPet(this.itemId);
			}

			if (ii.getInventoryType(this.itemId).equals(
					MapleInventoryType.EQUIP)) {
				item = ii.getEquipById(this.itemId);
			} else {
				item = new Item(this.itemId, (byte) 0, this.count, petid);
			}

			if (ItemConstants.EXPIRING_ITEMS) {
				item.setExpiration(this.period == 1 ? System
						.currentTimeMillis()
						+ (1000 * 60 * 60 * 4 * this.period) : System
						.currentTimeMillis()
						+ (1000 * 60 * 60 * 24 * this.period));
			}

			item.setSN(this.sn);
			return item;
		}
	}

	public static class SpecialCashItem {
		private final int sn, modifier;
		private final byte info; // ?

		public SpecialCashItem(int sn, int modifier, byte info) {
			this.sn = sn;
			this.modifier = modifier;
			this.info = info;
		}

		public int getSN() {
			return this.sn;
		}

		public int getModifier() {
			return this.modifier;
		}

		public byte getInfo() {
			return this.info;
		}
	}

	public static class CashItemFactory {

		private static final Map<Integer, CashItem> items = new HashMap<>();
		private static final Map<Integer, List<Integer>> packages = new HashMap<>();
		private static final List<SpecialCashItem> specialcashitems = new ArrayList<>();

		static {
			final MapleDataProvider etc = MapleDataProviderFactory
					.getDataProvider(new File("wz/Etc.wz"));

			for (final MapleData item : etc.getData("Commodity.img")
					.getChildren()) {
				final int sn = MapleDataTool.getIntConvert("SN", item);
				final int itemId = MapleDataTool.getIntConvert("ItemId", item);
				final int price = MapleDataTool.getIntConvert("Price", item, 0);
				final long period = MapleDataTool.getIntConvert("Period", item,
						1);
				final short count = (short) MapleDataTool.getIntConvert(
						"Count", item, 1);
				final boolean onSale = MapleDataTool.getIntConvert("OnSale",
						item, 0) == 1;
				items.put(sn, new CashItem(sn, itemId, price, period, count,
						onSale));
			}

			for (final MapleData cashPackage : etc.getData("CashPackage.img")
					.getChildren()) {
				final List<Integer> cPackage = new ArrayList<>();

				for (final MapleData item : cashPackage.getChildByPath("SN")
						.getChildren()) {
					cPackage.add(Integer.parseInt(item.getData().toString()));
				}

				packages.put(Integer.parseInt(cashPackage.getName()), cPackage);
			}
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				ps = DatabaseConnection.getConnection().prepareStatement(
						"SELECT * FROM specialcashitems");
				rs = ps.executeQuery();
				while (rs.next()) {
					specialcashitems.add(new SpecialCashItem(rs.getInt("sn"),
							rs.getInt("modifier"), rs.getByte("info")));
				}
			} catch (final SQLException ex) {
				ex.printStackTrace();
			} finally {
				try {
					if (rs != null) {
						rs.close();
					}
					if (ps != null) {
						ps.close();
					}
				} catch (final SQLException ex) {
				}
			}
		}

		public static CashItem getItem(int sn) {
			return items.get(sn);
		}

		public static List<Item> getPackage(int itemId) {
			final List<Item> cashPackage = new ArrayList<>();

			for (final int sn : packages.get(itemId)) {
				cashPackage.add(getItem(sn).toItem());
			}

			return cashPackage;
		}

		public static boolean isPackage(int itemId) {
			return packages.containsKey(itemId);
		}

		public static List<SpecialCashItem> getSpecialCashItems() {
			return specialcashitems;
		}

		public static void reloadSpecialCashItems() {// Yay?
			specialcashitems.clear();
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				ps = DatabaseConnection.getConnection().prepareStatement(
						"SELECT * FROM specialcashitems");
				rs = ps.executeQuery();
				while (rs.next()) {
					specialcashitems.add(new SpecialCashItem(rs.getInt("sn"),
							rs.getInt("modifier"), rs.getByte("info")));
				}
			} catch (final SQLException ex) {
				ex.printStackTrace();
			} finally {
				try {
					if (rs != null) {
						rs.close();
					}
					if (ps != null) {
						ps.close();
					}
				} catch (final SQLException ex) {
				}
			}
		}
	}

	private final int accountId, characterId;
	private int nxCredit;
	private int maplePoint;
	private int nxPrepaid;
	private boolean opened;
	private ItemFactory factory;
	private final List<Item> inventory = new ArrayList<>();
	private final List<Integer> wishList = new ArrayList<>();
	private int notes = 0;

	public CashShop(int accountId, int characterId, int jobType)
			throws SQLException {
		this.accountId = accountId;
		this.characterId = characterId;

		if (jobType == 0) {
			this.factory = ItemFactory.CASH_EXPLORER;
		} else if (jobType == 1) {
			this.factory = ItemFactory.CASH_CYGNUS;
		} else if (jobType == 2) {
			this.factory = ItemFactory.CASH_ARAN;
		}

		final Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `nxCredit`, `maplePoint`, `nxPrepaid` FROM `accounts` WHERE `id` = ?");
			ps.setInt(1, accountId);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.nxCredit = rs.getInt("nxCredit");
				this.maplePoint = rs.getInt("maplePoint");
				this.nxPrepaid = rs.getInt("nxPrepaid");
			}

			rs.close();
			ps.close();

			for (final Pair<Item, MapleInventoryType> item : this.factory
					.loadItems(accountId, false)) {
				this.inventory.add(item.getLeft());
			}

			ps = con.prepareStatement("SELECT `sn` FROM `wishlists` WHERE `charid` = ?");
			ps.setInt(1, characterId);
			rs = ps.executeQuery();

			while (rs.next()) {
				this.wishList.add(rs.getInt("sn"));
			}

			rs.close();
			ps.close();
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (rs != null) {
				rs.close();
			}
		}
	}

	public int getCash(int type) {
		switch (type) {
		case 1:
			return this.nxCredit;
		case 2:
			return this.maplePoint;
		case 4:
			return this.nxPrepaid;
		}

		return 0;
	}

	public void gainCash(int type, int cash) {
		switch (type) {
		case 1:
			this.nxCredit += cash;
			break;
		case 2:
			this.maplePoint += cash;
			break;
		case 4:
			this.nxPrepaid += cash;
			break;
		}
	}

	public boolean isOpened() {
		return this.opened;
	}

	public void open(boolean b) {
		this.opened = b;
	}

	public List<Item> getInventory() {
		return this.inventory;
	}

	public Item findByCashId(int cashId) {
		boolean isRing = false;
		Equip equip = null;
		for (final Item item : this.inventory) {
			if (item.getType() == 1) {
				equip = (Equip) item;
				isRing = equip.getRingId() > -1;
			}
			if ((item.getPetId() > -1 ? item.getPetId() : isRing ? equip
					.getRingId() : item.getCashId()) == cashId) {
				return item;
			}
		}

		return null;
	}

	public void addToInventory(Item item) {
		this.inventory.add(item);
	}

	public void removeFromInventory(Item item) {
		this.inventory.remove(item);
	}

	public List<Integer> getWishList() {
		return this.wishList;
	}

	public void clearWishList() {
		this.wishList.clear();
	}

	public void addToWishList(int sn) {
		this.wishList.add(sn);
	}

	public void gift(int recipient, String from, String message, int sn) {
		this.gift(recipient, from, message, sn, -1);
	}

	public void gift(int recipient, String from, String message, int sn,
			int ringid) {
		PreparedStatement ps = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"INSERT INTO `gifts` VALUES (DEFAULT, ?, ?, ?, ?, ?)");
			ps.setInt(1, recipient);
			ps.setString(2, from);
			ps.setString(3, message);
			ps.setInt(4, sn);
			ps.setInt(5, ringid);
			ps.executeUpdate();
		} catch (final SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
			} catch (final SQLException ex) {
			}
		}
	}

	public List<Pair<Item, String>> loadGifts() {
		final List<Pair<Item, String>> gifts = new ArrayList<>();
		final Connection con = DatabaseConnection.getConnection();

		try {
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM `gifts` WHERE `to` = ?");
			ps.setInt(1, this.characterId);
			final ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				this.notes++;
				final CashItem cItem = CashItemFactory.getItem(rs.getInt("sn"));
				final Item item = cItem.toItem();
				Equip equip = null;
				item.setGiftFrom(rs.getString("from"));
				if (item.getType() == MapleInventoryType.EQUIP.getType()) {
					equip = (Equip) item;
					equip.setRingId(rs.getInt("ringid"));
					gifts.add(new Pair<Item, String>(equip, rs
							.getString("message")));
				} else {
					gifts.add(new Pair<>(item, rs.getString("message")));
				}

				if (CashItemFactory.isPackage(cItem.getItemId())) { // Packages
																	// never
																	// contains
																	// a ring
					for (final Item packageItem : CashItemFactory
							.getPackage(cItem.getItemId())) {
						packageItem.setGiftFrom(rs.getString("from"));
						this.addToInventory(packageItem);
					}
				} else {
					this.addToInventory(equip == null ? item : equip);
				}
			}

			rs.close();
			ps.close();
			ps = con.prepareStatement("DELETE FROM `gifts` WHERE `to` = ?");
			ps.setInt(1, this.characterId);
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException sqle) {
			sqle.printStackTrace();
		}

		return gifts;
	}

	public int getAvailableNotes() {
		return this.notes;
	}

	public void decreaseNotes() {
		this.notes--;
	}

	public void save() throws SQLException {
		final Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = con
				.prepareStatement("UPDATE `accounts` SET `nxCredit` = ?, `maplePoint` = ?, `nxPrepaid` = ? WHERE `id` = ?");
		ps.setInt(1, this.nxCredit);
		ps.setInt(2, this.maplePoint);
		ps.setInt(3, this.nxPrepaid);
		ps.setInt(4, this.accountId);
		ps.executeUpdate();
		ps.close();
		final List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

		for (final Item item : this.inventory) {
			itemsWithType.add(new Pair<>(item, MapleItemInformationProvider
					.getInstance().getInventoryType(item.getItemId())));
		}

		this.factory.saveItems(itemsWithType, this.accountId);
		ps = con.prepareStatement("DELETE FROM `wishlists` WHERE `charid` = ?");
		ps.setInt(1, this.characterId);
		ps.executeUpdate();
		ps = con.prepareStatement("INSERT INTO `wishlists` VALUES (DEFAULT, ?, ?)");
		ps.setInt(1, this.characterId);

		for (final int sn : this.wishList) {
			ps.setInt(2, sn);
			ps.executeUpdate();
		}

		ps.close();
	}
}
