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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;

/**
 *
 * @author Matze
 */
public class MapleStorage {

	private final int id;
	private final List<Item> items;
	private int meso;
	private byte slots;
	private final Map<MapleInventoryType, List<Item>> typeItems = new HashMap<>();

	private MapleStorage(int id, byte slots, int meso) {
		this.id = id;
		this.slots = slots;
		this.items = new LinkedList<>();
		this.meso = meso;
	}

	private static MapleStorage create(int id, int world) {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"INSERT INTO storages (accountid, world, slots, meso) VALUES (?, ?, 4, 0)")) {
				ps.setInt(1, id);
				ps.setInt(2, world);
				ps.executeUpdate();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return loadOrCreateFromDB(id, world);
	}

	public static MapleStorage loadOrCreateFromDB(int id, int world) {
		MapleStorage ret = null;
		int storeId;
		try {
			final Connection con = DatabaseConnection.getConnection();
			final PreparedStatement ps = con
					.prepareStatement("SELECT storageid, slots, meso FROM storages WHERE accountid = ? AND world = ?");
			ps.setInt(1, id);
			ps.setInt(2, world);
			final ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return create(id, world);
			} else {
				storeId = rs.getInt("storageid");
				ret = new MapleStorage(storeId, (byte) rs.getInt("slots"),
						rs.getInt("meso"));
				rs.close();
				ps.close();
				for (final Pair<Item, MapleInventoryType> item : ItemFactory.STORAGE
						.loadItems(ret.id, false)) {
					ret.items.add(item.getLeft());
				}
			}
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return ret;
	}

	public byte getSlots() {
		return this.slots;
	}

	public boolean gainSlots(int slots) {
		slots += this.slots;

		if (slots <= 48) {
			this.slots = (byte) slots;
			return true;
		}

		return false;
	}

	public void setSlots(byte set) {
		this.slots = set;
	}

	public void saveToDB() {
		try {
			final Connection con = DatabaseConnection.getConnection();
			try (PreparedStatement ps = con
					.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?")) {
				ps.setInt(1, this.slots);
				ps.setInt(2, this.meso);
				ps.setInt(3, this.id);
				ps.executeUpdate();
			}
			final List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

			for (final Item item : this.items) {
				itemsWithType.add(new Pair<>(item, MapleItemInformationProvider
						.getInstance().getInventoryType(item.getItemId())));
			}

			ItemFactory.STORAGE.saveItems(itemsWithType, this.id);
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	public Item getItem(byte slot) {
		return this.items.get(slot);
	}

	public Item takeOut(byte slot) {
		final Item ret = this.items.remove(slot);
		final MapleInventoryType type = MapleItemInformationProvider
				.getInstance().getInventoryType(ret.getItemId());
		this.typeItems.put(type, new ArrayList<>(this.filterItems(type)));
		return ret;
	}

	public void store(Item item) {
		this.items.add(item);
		final MapleInventoryType type = MapleItemInformationProvider
				.getInstance().getInventoryType(item.getItemId());
		this.typeItems.put(type, new ArrayList<>(this.filterItems(type)));
	}

	public List<Item> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	private List<Item> filterItems(MapleInventoryType type) {
		final List<Item> ret = new LinkedList<>();
		final MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		for (final Item item : this.items) {
			if (ii.getInventoryType(item.getItemId()) == type) {
				ret.add(item);
			}
		}
		return ret;
	}

	public byte getSlot(MapleInventoryType type, byte slot) {
		byte ret = 0;
		for (final Item item : this.items) {
			if (item == this.typeItems.get(type).get(slot)) {
				return ret;
			}
			ret++;
		}
		return -1;
	}

	public void sendStorage(MapleClient c, int npcId) {
		final MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		Collections.sort(this.items, new Comparator<Item>() {
			@Override
			public int compare(Item o1, Item o2) {
				if (ii.getInventoryType(o1.getItemId()).getType() < ii
						.getInventoryType(o2.getItemId()).getType()) {
					return -1;
				} else if (ii.getInventoryType(o1.getItemId()) == ii
						.getInventoryType(o2.getItemId())) {
					return 0;
				}
				return 1;
			}
		});
		for (final MapleInventoryType type : MapleInventoryType.values()) {
			this.typeItems.put(type, new ArrayList<>(this.items));
		}
		c.announce(MaplePacketCreator.getStorage(npcId, this.slots, this.items,
				this.meso));
	}

	public void sendStored(MapleClient c, MapleInventoryType type) {
		c.announce(MaplePacketCreator.storeStorage(this.slots, type,
				this.typeItems.get(type)));
	}

	public void sendTakenOut(MapleClient c, MapleInventoryType type) {
		c.announce(MaplePacketCreator.takeOutStorage(this.slots, type,
				this.typeItems.get(type)));
	}

	public int getMeso() {
		return this.meso;
	}

	public void setMeso(int meso) {
		if (meso < 0) {
			throw new RuntimeException();
		}
		this.meso = meso;
	}

	public void sendMeso(MapleClient c) {
		c.announce(MaplePacketCreator.mesoStorage(this.slots, this.meso));
	}

	public boolean isFull() {
		return this.items.size() >= this.slots;
	}

	public void close() {
		this.typeItems.clear();
	}
}
