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
package net.server.guild;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import tools.DatabaseConnection;

/**
 *
 * @author XoticStory.
 */
public class MapleAlliance {
	private final int[] guilds = new int[5];
	private int allianceId = -1;
	private int capacity;
	private String name;
	private String notice = "";
	private String rankTitles[] = new String[5];

	public MapleAlliance(String name, int id, int guild1, int guild2) {
		this.name = name;
		this.allianceId = id;
		final int[] guild = { guild1, guild2, -1, -1, -1 };
		final String[] ranks = { "Master", "Jr.Master", "Member", "Member",
				"Member" };
		for (int i = 0; i < 5; i++) {
			this.guilds[i] = guild[i];
			this.rankTitles[i] = ranks[i];
		}
	}

	public static MapleAlliance loadAlliance(int id) {
		if (id <= 0) {
			return null;
		}
		final MapleAlliance alliance = new MapleAlliance(null, -1, -1, -1);
		try {
			final PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement("SELECT * FROM alliance WHERE id = ?");
			ps.setInt(1, id);
			final ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return null;
			}
			alliance.allianceId = id;
			alliance.capacity = rs.getInt("capacity");
			alliance.name = rs.getString("name");
			alliance.notice = rs.getString("notice");
			for (int i = 1; i <= 5; i++) {
				alliance.rankTitles[i - 1] = rs.getString("rank_title" + i);
			}
			for (int i = 1; i <= 5; i++) {
				alliance.guilds[i - 1] = rs.getInt("guild" + i);
			}
			ps.close();
			rs.close();
		} catch (final SQLException e) {
		}
		return alliance;
	}

	public void saveToDB() {
		final StringBuilder sb = new StringBuilder();
		sb.append("capacity = ?, ");
		sb.append("notice = ?, ");
		for (int i = 1; i <= 5; i++) {
			sb.append("rank_title").append(i).append(" = ?, ");
		}
		for (int i = 1; i <= 5; i++) {
			sb.append("guild").append(i).append(" = ?, ");
		}
		try {
			final PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"UPDATE `alliance` SET " + sb.toString()
									+ " WHERE id = ?");
			ps.setInt(1, this.capacity);
			ps.setString(2, this.notice);
			for (int i = 0; i < this.rankTitles.length; i++) {
				ps.setString(i + 3, this.rankTitles[i]);
			}
			for (int i = 0; i < this.guilds.length; i++) {
				ps.setInt(i + 8, this.guilds[i]);
			}
			ps.setInt(13, this.allianceId);
			ps.executeQuery();
			ps.close();
		} catch (final SQLException e) {
		}
	}

	public boolean addRemGuildFromDB(int gid, boolean add) {
		final Connection con = DatabaseConnection.getConnection();
		boolean ret = false;
		try {
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM alliance WHERE id = ?");
			ps.setInt(1, this.allianceId);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int avail = -1;
				for (int i = 1; i <= 5; i++) {
					final int guildId = rs.getInt("guild" + i);
					if (add) {
						if (guildId == -1) {
							avail = i;
							break;
						}
					} else if (guildId == gid) {
						avail = i;
						break;
					}
				}
				rs.close();
				if (avail != -1) { // empty slot
					ps = con.prepareStatement("UPDATE alliance SET guild"
							+ avail + " = ? WHERE id = ?");
					if (add) {
						ps.setInt(1, gid);
					} else {
						ps.setInt(1, -1);
					}
					ps.setInt(2, this.allianceId);
					ps.executeUpdate();
					ret = true;
				}
				ps.close();
			}
		} catch (final SQLException e) {
		}
		return ret;
	}

	public boolean removeGuild(int gid) {
		synchronized (this.guilds) {
			final int gIndex = this.getGuildIndex(gid);
			if (gIndex != -1) {
				this.guilds[gIndex] = -1;
			}
			return this.addRemGuildFromDB(gid, false);
		}
	}

	public boolean addGuild(int gid) {
		synchronized (this.guilds) {
			if (this.getGuildIndex(gid) == -1) {
				final int emptyIndex = this.getGuildIndex(-1);
				if (emptyIndex != -1) {
					this.guilds[emptyIndex] = gid;
					return this.addRemGuildFromDB(gid, true);
				}
			}
		}
		return false;
	}

	private int getGuildIndex(int gid) {
		for (int i = 0; i < this.guilds.length; i++) {
			if (this.guilds[i] == gid) {
				return i;
			}
		}
		return -1;
	}

	public void setRankTitle(String[] ranks) {
		this.rankTitles = ranks;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}

	public int getId() {
		return this.allianceId;
	}

	public String getName() {
		return this.name;
	}

	public String getRankTitle(int rank) {
		return this.rankTitles[rank - 1];
	}

	public String getAllianceNotice() {
		return this.notice;
	}

	public List<Integer> getGuilds() {
		final List<Integer> guilds_ = new LinkedList<Integer>();
		for (final int guild : this.guilds) {
			if (guild != -1) {
				guilds_.add(guild);
			}
		}
		return guilds_;
	}

	public String getNotice() {
		return this.notice;
	}

	public void increaseCapacity(int inc) {
		this.capacity += inc;
	}

	public int getCapacity() {
		return this.capacity;
	}
}
