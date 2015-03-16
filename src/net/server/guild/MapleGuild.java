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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.server.Server;
import net.server.channel.Channel;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;

public class MapleGuild {
	public final static int CREATE_GUILD_COST = 1500000;
	public final static int CHANGE_EMBLEM_COST = 5000000;

	private enum BCOp {
		NONE, DISBAND, EMBELMCHANGE
	}

	private List<MapleGuildCharacter> members;
	private final String rankTitles[] = new String[5]; // 1 = master, 2 = jr, 5
														// =
														// lowest member
	private String name, notice;
	private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor,
			signature, allianceId;
	private int world;
	private final Map<Integer, List<Integer>> notifications = new LinkedHashMap<>();
	private boolean bDirty = true;

	public MapleGuild(MapleGuildCharacter initiator) {
		final int guildid = initiator.getGuildId();
		this.world = initiator.getWorld();
		this.members = new ArrayList<>();
		final Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM guilds WHERE guildid = "
							+ guildid);
			ResultSet rs = ps.executeQuery();
			if (!rs.first()) {
				this.id = -1;
				ps.close();
				rs.close();
				return;
			}
			this.id = guildid;
			this.name = rs.getString("name");
			this.gp = rs.getInt("GP");
			this.logo = rs.getInt("logo");
			this.logoColor = rs.getInt("logoColor");
			this.logoBG = rs.getInt("logoBG");
			this.logoBGColor = rs.getInt("logoBGColor");
			this.capacity = rs.getInt("capacity");
			for (int i = 1; i <= 5; i++) {
				this.rankTitles[i - 1] = rs.getString("rank" + i + "title");
			}
			this.leader = rs.getInt("leader");
			this.notice = rs.getString("notice");
			this.signature = rs.getInt("signature");
			this.allianceId = rs.getInt("allianceId");
			ps.close();
			rs.close();
			ps = con.prepareStatement("SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
			ps.setInt(1, guildid);
			rs = ps.executeQuery();
			if (!rs.first()) {
				rs.close();
				ps.close();
				return;
			}
			do {
				this.members.add(new MapleGuildCharacter(rs.getInt("id"), rs
						.getInt("level"), rs.getString("name"), (byte) -1,
						this.world, rs.getInt("job"), rs.getInt("guildrank"),
						guildid, false, rs.getInt("allianceRank")));
			} while (rs.next());
			this.setOnline(initiator.getId(), true, initiator.getChannel());
			ps.close();
			rs.close();
		} catch (final SQLException se) {
			System.out
					.println("unable to read guild information from sql" + se);
		}
	}

	public void buildNotifications() {
		if (!this.bDirty) {
			return;
		}
		final Set<Integer> chs = Server.getInstance().getChannelServer(
				this.world);
		if (this.notifications.keySet().size() != chs.size()) {
			this.notifications.clear();
			for (final Integer ch : chs) {
				this.notifications.put(ch, new LinkedList<Integer>());
			}
		} else {
			for (final List<Integer> l : this.notifications.values()) {
				l.clear();
			}
		}
		synchronized (this.members) {
			for (final MapleGuildCharacter mgc : this.members) {
				if (!mgc.isOnline()) {
					continue;
				}
				final List<Integer> ch = this.notifications.get(mgc
						.getChannel());
				if (ch != null) {
					ch.add(mgc.getId());
					// Unable to connect to Channel... error was here
				}
			}
		}
		this.bDirty = false;
	}

	public void writeToDB(boolean bDisband) {
		try {
			final Connection con = DatabaseConnection.getConnection();
			if (!bDisband) {
				final StringBuilder builder = new StringBuilder();
				builder.append("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
				for (int i = 0; i < 5; i++) {
					builder.append("rank").append(i + 1).append("title = ?, ");
				}
				builder.append("capacity = ?, notice = ? WHERE guildid = ?");
				try (PreparedStatement ps = con.prepareStatement(builder
						.toString())) {
					ps.setInt(1, this.gp);
					ps.setInt(2, this.logo);
					ps.setInt(3, this.logoColor);
					ps.setInt(4, this.logoBG);
					ps.setInt(5, this.logoBGColor);
					for (int i = 6; i < 11; i++) {
						ps.setString(i, this.rankTitles[i - 6]);
					}
					ps.setInt(11, this.capacity);
					ps.setString(12, this.notice);
					ps.setInt(13, this.id);
					ps.execute();
				}
			} else {
				PreparedStatement ps = con
						.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				this.broadcast(MaplePacketCreator.guildDisband(this.id));
			}
		} catch (final SQLException se) {
		}
	}

	public int getId() {
		return this.id;
	}

	public int getLeaderId() {
		return this.leader;
	}

	public int getGP() {
		return this.gp;
	}

	public int getLogo() {
		return this.logo;
	}

	public void setLogo(int l) {
		this.logo = l;
	}

	public int getLogoColor() {
		return this.logoColor;
	}

	public void setLogoColor(int c) {
		this.logoColor = c;
	}

	public int getLogoBG() {
		return this.logoBG;
	}

	public void setLogoBG(int bg) {
		this.logoBG = bg;
	}

	public int getLogoBGColor() {
		return this.logoBGColor;
	}

	public void setLogoBGColor(int c) {
		this.logoBGColor = c;
	}

	public String getNotice() {
		if (this.notice == null) {
			return "";
		}
		return this.notice;
	}

	public String getName() {
		return this.name;
	}

	public java.util.Collection<MapleGuildCharacter> getMembers() {
		return java.util.Collections.unmodifiableCollection(this.members);
	}

	public int getCapacity() {
		return this.capacity;
	}

	public int getSignature() {
		return this.signature;
	}

	public void broadcast(final byte[] packet) {
		this.broadcast(packet, -1, BCOp.NONE);
	}

	public void broadcast(final byte[] packet, int exception) {
		this.broadcast(packet, exception, BCOp.NONE);
	}

	public void broadcast(final byte[] packet, int exceptionId, BCOp bcop) {
		synchronized (this.notifications) {
			if (this.bDirty) {
				this.buildNotifications();
			}
			try {
				for (final Integer b : Server.getInstance().getChannelServer(
						this.world)) {
					if (this.notifications.get(b).size() > 0) {
						if (bcop == BCOp.DISBAND) {
							Server.getInstance()
									.getWorld(this.world)
									.setGuildAndRank(this.notifications.get(b),
											0, 5, exceptionId);
						} else if (bcop == BCOp.EMBELMCHANGE) {
							Server.getInstance()
									.getWorld(this.world)
									.changeEmblem(this.id,
											this.notifications.get(b),
											new MapleGuildSummary(this));
						} else {
							Server.getInstance()
									.getWorld(this.world)
									.sendPacket(this.notifications.get(b),
											packet, exceptionId);
						}
					}
				}
			} catch (final Exception re) {
				System.out
						.println("Failed to contact channel(s) for broadcast.");// fu?
			}
		}
	}

	public void guildMessage(final byte[] serverNotice) {
		for (final MapleGuildCharacter mgc : this.members) {
			for (final Channel cs : Server.getInstance().getChannelsFromWorld(
					this.world)) {
				if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
					cs.getPlayerStorage().getCharacterById(mgc.getId())
							.getClient().announce(serverNotice);
					break;
				}
			}
		}
	}

	public final void setOnline(int cid, boolean online, int channel) {
		boolean bBroadcast = true;
		for (final MapleGuildCharacter mgc : this.members) {
			if (mgc.getId() == cid) {
				if (mgc.isOnline() && online) {
					bBroadcast = false;
				}
				mgc.setOnline(online);
				mgc.setChannel(channel);
				break;
			}
		}
		if (bBroadcast) {
			this.broadcast(
					MaplePacketCreator.guildMemberOnline(this.id, cid, online),
					cid);
		}
		this.bDirty = true;
	}

	public void guildChat(String name, int cid, String msg) {
		this.broadcast(MaplePacketCreator.multiChat(name, msg, 2), cid);
	}

	public String getRankTitle(int rank) {
		return this.rankTitles[rank - 1];
	}

	public static int createGuild(int leaderId, String name) {
		try {
			final Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.first()) {
				ps.close();
				rs.close();
				return 0;
			}
			ps.close();
			rs.close();
			ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
			ps.setInt(1, leaderId);
			ps.setString(2, name);
			ps.setInt(3, (int) System.currentTimeMillis());
			ps.execute();
			ps.close();
			ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
			ps.setInt(1, leaderId);
			rs = ps.executeQuery();
			rs.first();
			final int guildid = rs.getInt("guildid");
			rs.close();
			ps.close();
			return guildid;
		} catch (final Exception e) {
			return 0;
		}
	}

	public int addGuildMember(MapleGuildCharacter mgc) {
		synchronized (this.members) {
			if (this.members.size() >= this.capacity) {
				return 0;
			}
			for (int i = this.members.size() - 1; i >= 0; i--) {
				if ((this.members.get(i).getGuildRank() < 5)
						|| (this.members.get(i).getName()
								.compareTo(mgc.getName()) < 0)) {
					this.members.add(i + 1, mgc);
					this.bDirty = true;
					break;
				}
			}
		}
		this.broadcast(MaplePacketCreator.newGuildMember(mgc));
		return 1;
	}

	public void leaveGuild(MapleGuildCharacter mgc) {
		this.broadcast(MaplePacketCreator.memberLeft(mgc, false));
		synchronized (this.members) {
			this.members.remove(mgc);
			this.bDirty = true;
		}
	}

	public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
		synchronized (this.members) {
			final java.util.Iterator<MapleGuildCharacter> itr = this.members
					.iterator();
			MapleGuildCharacter mgc;
			while (itr.hasNext()) {
				mgc = itr.next();
				if ((mgc.getId() == cid)
						&& (initiator.getGuildRank() < mgc.getGuildRank())) {
					this.broadcast(MaplePacketCreator.memberLeft(mgc, true));
					itr.remove();
					this.bDirty = true;
					try {
						if (mgc.isOnline()) {
							Server.getInstance().getWorld(mgc.getWorld())
									.setGuildAndRank(cid, 0, 5);
						} else {
							try {
								try (PreparedStatement ps = DatabaseConnection
										.getConnection()
										.prepareStatement(
												"INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)")) {
									ps.setString(1, mgc.getName());
									ps.setString(2, initiator.getName());
									ps.setString(3,
											"You have been expelled from the guild.");
									ps.setLong(4, System.currentTimeMillis());
									ps.executeUpdate();
								}
							} catch (final SQLException e) {
								System.out.println("expelMember - MapleGuild "
										+ e);
							}
							Server.getInstance()
									.getWorld(mgc.getWorld())
									.setOfflineGuildStatus((short) 0, (byte) 5,
											cid);
						}
					} catch (final Exception re) {
						re.printStackTrace();
						return;
					}
					return;
				}
			}
			System.out.println("Unable to find member with name " + name
					+ " and id " + cid);
		}
	}

	public void changeRank(int cid, int newRank) {
		for (final MapleGuildCharacter mgc : this.members) {
			if (cid == mgc.getId()) {
				try {
					if (mgc.isOnline()) {
						Server.getInstance().getWorld(mgc.getWorld())
								.setGuildAndRank(cid, this.id, newRank);
					} else {
						Server.getInstance()
								.getWorld(mgc.getWorld())
								.setOfflineGuildStatus((short) this.id,
										(byte) newRank, cid);
					}
				} catch (final Exception re) {
					re.printStackTrace();
					return;
				}
				mgc.setGuildRank(newRank);
				this.broadcast(MaplePacketCreator.changeRank(mgc));
				return;
			}
		}
	}

	public void setGuildNotice(String notice) {
		this.notice = notice;
		this.writeToDB(false);
		this.broadcast(MaplePacketCreator.guildNotice(this.id, notice));
	}

	public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
		for (final MapleGuildCharacter member : this.members) {
			if (mgc.equals(member)) {
				member.setJobId(mgc.getJobId());
				member.setLevel(mgc.getLevel());
				this.broadcast(MaplePacketCreator
						.guildMemberLevelJobUpdate(mgc));
				break;
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MapleGuildCharacter)) {
			return false;
		}
		final MapleGuildCharacter o = (MapleGuildCharacter) other;
		return ((o.getId() == this.id) && o.getName().equals(this.name));
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = (89 * hash) + (this.name != null ? this.name.hashCode() : 0);
		hash = (89 * hash) + this.id;
		return hash;
	}

	public void changeRankTitle(String[] ranks) {
		System.arraycopy(ranks, 0, this.rankTitles, 0, 5);
		this.broadcast(MaplePacketCreator.rankTitleChange(this.id, ranks));
		this.writeToDB(false);
	}

	public void disbandGuild() {
		this.writeToDB(true);
		this.broadcast(null, -1, BCOp.DISBAND);
	}

	public void setGuildEmblem(short bg, byte bgcolor, short logo,
			byte logocolor) {
		this.logoBG = bg;
		this.logoBGColor = bgcolor;
		this.logo = logo;
		this.logoColor = logocolor;
		this.writeToDB(false);
		this.broadcast(null, -1, BCOp.EMBELMCHANGE);
	}

	public MapleGuildCharacter getMGC(int cid) {
		for (final MapleGuildCharacter mgc : this.members) {
			if (mgc.getId() == cid) {
				return mgc;
			}
		}
		return null;
	}

	public boolean increaseCapacity() {
		if (this.capacity > 99) {
			return false;
		}
		this.capacity += 5;
		this.writeToDB(false);
		this.broadcast(MaplePacketCreator.guildCapacityChange(this.id,
				this.capacity));
		return true;
	}

	public void gainGP(int amount) {
		this.gp += amount;
		this.writeToDB(false);
		this.guildMessage(MaplePacketCreator.updateGP(this.id, this.gp));
	}

	public static MapleGuildResponse sendInvite(MapleClient c, String targetName) {
		final MapleCharacter mc = c.getChannelServer().getPlayerStorage()
				.getCharacterByName(targetName);
		if (mc == null) {
			return MapleGuildResponse.NOT_IN_CHANNEL;
		}
		if (mc.getGuildId() > 0) {
			return MapleGuildResponse.ALREADY_IN_GUILD;
		}
		mc.getClient().announce(
				MaplePacketCreator.guildInvite(c.getPlayer().getGuildId(), c
						.getPlayer().getName()));
		return null;
	}

	public static void displayGuildRanks(MapleClient c, int npcid) {
		try {
			ResultSet rs;
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT `name`, `GP`, `logoBG`, `logoBGColor`, "
									+ "`logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50")) {
				rs = ps.executeQuery();
				c.announce(MaplePacketCreator.showGuildRanks(npcid, rs));
			}
			rs.close();
		} catch (final SQLException e) {
			System.out.println("failed to display guild ranks. " + e);
		}
	}

	public int getAllianceId() {
		return this.allianceId;
	}

	public void setAllianceId(int aid) {
		this.allianceId = aid;
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE guilds SET allianceId = ? WHERE guildid = ?")) {
				ps.setInt(1, aid);
				ps.setInt(2, this.id);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public int getIncreaseGuildCost(int size) {
		return (500000 * (size - 6)) / 6;
	}
}
