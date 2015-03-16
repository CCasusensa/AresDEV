/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.life;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tools.DatabaseConnection;

public class MapleMonsterInformationProvider {
	// Author : LightPepsi

	private static final MapleMonsterInformationProvider instance = new MapleMonsterInformationProvider();
	private final Map<Integer, List<MonsterDropEntry>> drops = new HashMap<>();
	private final List<MonsterGlobalDropEntry> globaldrops = new ArrayList<>();

	protected MapleMonsterInformationProvider() {
		this.retrieveGlobal();
	}

	public static MapleMonsterInformationProvider getInstance() {
		return instance;
	}

	public final List<MonsterGlobalDropEntry> getGlobalDrop() {
		return this.globaldrops;
	}

	private void retrieveGlobal() {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			final Connection con = DatabaseConnection.getConnection();
			ps = con.prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0");
			rs = ps.executeQuery();

			while (rs.next()) {
				this.globaldrops.add(new MonsterGlobalDropEntry(rs
						.getInt("itemid"), rs.getInt("chance"), rs
						.getInt("continent"), rs.getByte("dropType"), rs
						.getInt("minimum_quantity"), rs
						.getInt("maximum_quantity"), rs.getShort("questid")));
			}
			rs.close();
			ps.close();
		} catch (final SQLException e) {
			System.err.println("Error retrieving drop" + e);
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				if (rs != null) {
					rs.close();
				}
			} catch (final SQLException ignore) {
			}
		}
	}

	public final List<MonsterDropEntry> retrieveDrop(final int monsterId) {
		if (this.drops.containsKey(monsterId)) {
			return this.drops.get(monsterId);
		}
		final List<MonsterDropEntry> ret = new LinkedList<>();

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"SELECT * FROM drop_data WHERE dropperid = ?");
			ps.setInt(1, monsterId);
			rs = ps.executeQuery();

			while (rs.next()) {
				ret.add(new MonsterDropEntry(rs.getInt("itemid"), rs
						.getInt("chance"), rs.getInt("minimum_quantity"), rs
						.getInt("maximum_quantity"), rs.getShort("questid")));
			}
		} catch (final SQLException e) {
			return ret;
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				if (rs != null) {
					rs.close();
				}
			} catch (final SQLException ignore) {
				return ret;
			}
		}
		this.drops.put(monsterId, ret);
		return ret;
	}

	public final void clearDrops() {
		this.drops.clear();
		this.globaldrops.clear();
		this.retrieveGlobal();
	}
}