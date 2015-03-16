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
package client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import tools.DatabaseConnection;
import tools.MaplePacketCreator;

public class BuddyList {
	public enum BuddyOperation {
		ADDED, DELETED
	}

	public enum BuddyAddResult {
		BUDDYLIST_FULL, ALREADY_ON_LIST, OK
	}

	private final Map<Integer, BuddyListEntry> buddies = new LinkedHashMap<>();
	private int capacity;
	private final Deque<CharacterNameAndId> pendingRequests = new LinkedList<>();

	public BuddyList(int capacity) {
		this.capacity = capacity;
	}

	public boolean contains(int characterId) {
		return this.buddies.containsKey(Integer.valueOf(characterId));
	}

	public boolean containsVisible(int characterId) {
		final BuddyListEntry ble = this.buddies.get(characterId);
		if (ble == null) {
			return false;
		}
		return ble.isVisible();
	}

	public int getCapacity() {
		return this.capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public BuddyListEntry get(int characterId) {
		return this.buddies.get(Integer.valueOf(characterId));
	}

	public BuddyListEntry get(String characterName) {
		final String lowerCaseName = characterName.toLowerCase();
		for (final BuddyListEntry ble : this.buddies.values()) {
			if (ble.getName().toLowerCase().equals(lowerCaseName)) {
				return ble;
			}
		}
		return null;
	}

	public void put(BuddyListEntry entry) {
		this.buddies.put(Integer.valueOf(entry.getCharacterId()), entry);
	}

	public void remove(int characterId) {
		this.buddies.remove(Integer.valueOf(characterId));
	}

	public Collection<BuddyListEntry> getBuddies() {
		return this.buddies.values();
	}

	public boolean isFull() {
		return this.buddies.size() >= this.capacity;
	}

	public int[] getBuddyIds() {
		final int buddyIds[] = new int[this.buddies.size()];
		int i = 0;
		for (final BuddyListEntry ble : this.buddies.values()) {
			buddyIds[i++] = ble.getCharacterId();
		}
		return buddyIds;
	}

	public void loadFromDb(int characterId) {
		try {
			PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT b.buddyid, b.pending, b.group, c.name as buddyname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?");
			ps.setInt(1, characterId);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getInt("pending") == 1) {
					this.pendingRequests.push(new CharacterNameAndId(rs
							.getInt("buddyid"), rs.getString("buddyname")));
				} else {
					this.put(new BuddyListEntry(rs.getString("buddyname"), rs
							.getString("group"), rs.getInt("buddyid"),
							(byte) -1, true));
				}
			}
			rs.close();
			ps.close();
			ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"DELETE FROM buddies WHERE pending = 1 AND characterid = ?");
			ps.setInt(1, characterId);
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	public CharacterNameAndId pollPendingRequest() {
		return this.pendingRequests.pollLast();
	}

	public void addBuddyRequest(MapleClient c, int cidFrom, String nameFrom,
			int channelFrom) {
		this.put(new BuddyListEntry(nameFrom, "Default Group", cidFrom,
				channelFrom, false));
		if (this.pendingRequests.isEmpty()) {
			c.announce(MaplePacketCreator.requestBuddylistAdd(cidFrom, c
					.getPlayer().getId(), nameFrom));
		} else {
			this.pendingRequests
					.push(new CharacterNameAndId(cidFrom, nameFrom));
		}
	}
}
