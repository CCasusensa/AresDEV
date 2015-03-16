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
package net.server;

import gm.GMPacketCreator;
import gm.server.GMServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.MapleServerHandler;
import net.mina.MapleCodecFactory;
import net.server.channel.Channel;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.World;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import server.CashShop.CashItemFactory;
import server.MapleItemInformationProvider;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;
import client.MapleCharacter;
import client.SkillFactory;
import constants.ServerConstants;

public class Server implements Runnable {

	private IoAcceptor acceptor;
	private List<Map<Integer, String>> channels = new LinkedList<>();
	private List<World> worlds = new ArrayList<>();
	private final Properties subnetInfo = new Properties();
	private static Server instance = null;
	private List<Pair<Integer, String>> worldRecommendedList = new LinkedList<>();
	private final Map<Integer, MapleGuild> guilds = new LinkedHashMap<>();
	private final PlayerBuffStorage buffStorage = new PlayerBuffStorage();
	private final Map<Integer, MapleAlliance> alliances = new LinkedHashMap<>();
	private boolean online = false;

	public static Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}

	public boolean isOnline() {
		return this.online;
	}

	public List<Pair<Integer, String>> worldRecommendedList() {
		return this.worldRecommendedList;
	}

	public void removeChannel(int worldid, int channel) {
		this.channels.remove(channel);

		final World world = this.worlds.get(worldid);
		if (world != null) {
			world.removeChannel(channel);
		}
	}

	public Channel getChannel(int world, int channel) {
		return this.worlds.get(world).getChannel(channel);
	}

	public List<Channel> getChannelsFromWorld(int world) {
		return this.worlds.get(world).getChannels();
	}

	public List<Channel> getAllChannels() {
		final List<Channel> channelz = new ArrayList<>();
		for (final World world : this.worlds) {
			for (final Channel ch : world.getChannels()) {
				channelz.add(ch);
			}
		}

		return channelz;
	}

	public String getIP(int world, int channel) {
		return this.channels.get(world).get(channel);
	}

	@Override
	public void run() {

		System.out.println("AresDEV - V" + ServerConstants.VERSION
				+ " started...\r\n");

		Runtime.getRuntime().addShutdownHook(new Thread(this.shutdown(false)));
		DatabaseConnection.getConnection();
		final Connection c = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = c
					.prepareStatement("UPDATE accounts SET loggedin = 0");
			ps.executeUpdate();
			ps.close();
			ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException sqle) {
		}
		IoBuffer.setUseDirectBuffer(false);
		IoBuffer.setAllocator(new SimpleBufferAllocator());
		this.acceptor = new NioSocketAcceptor();
		this.acceptor.getFilterChain().addLast("codec",
				new ProtocolCodecFilter(new MapleCodecFactory()));
		final TimerManager tMan = TimerManager.getInstance();
		tMan.start();
		tMan.register(tMan.purge(), 300000);
		tMan.register(new RankingWorker(), ServerConstants.RANKING_INTERVAL);

		long timeToTake = System.currentTimeMillis();
		System.out.println("Loading skills...");
		SkillFactory.loadAllSkills();
		System.out.println("Skills loaded in "
				+ ((System.currentTimeMillis() - timeToTake) / 1000.0)
				+ " seconds...");

		timeToTake = System.currentTimeMillis();
		System.out.println("Loading items...");
		MapleItemInformationProvider.getInstance().getAllItems();

		CashItemFactory.getSpecialCashItems();
		System.out.println("Items loaded in "
				+ ((System.currentTimeMillis() - timeToTake) / 1000.0)
				+ " seconds...");
		try {
			final World scaniaWorld = new World(
					ServerConstants.WorldConstants.Scania.getWorld(),
					ServerConstants.WorldConstants.Scania.getFlag(),
					ServerConstants.WorldConstants.Scania.getEventMsg(),
					ServerConstants.WorldConstants.Scania.getExpRate(),
					ServerConstants.WorldConstants.Scania.getDropRate(),
					ServerConstants.WorldConstants.Scania.getMesoRate(),
					ServerConstants.WorldConstants.Scania.getBossDropRate());
			this.worldRecommendedList.add(new Pair<>(
					ServerConstants.WorldConstants.Scania.getWorld(), ""));
			this.worlds.add(scaniaWorld);
			System.out.println("World "
					+ ServerConstants.WorldConstants.Scania.getWorld()
					+ " is online...");
			this.channels.add(new LinkedHashMap<Integer, String>());
			for (int j = 0; j < ServerConstants.CHANNEL_COUNT; j++) {
				final int channelId = j + 1;
				final Channel channel = new Channel(
						ServerConstants.WorldConstants.Scania.getWorld(),
						channelId);
				scaniaWorld.addChannel(channel);
				this.channels.get(
						ServerConstants.WorldConstants.Scania.getWorld()).put(
						channelId, channel.getIP());
			}
			scaniaWorld.setServerMessage(ServerConstants.WorldConstants.Scania
					.getServerMsg());
			/*
			 * for (int i = 0; i < Integer.parseInt(p.getProperty("worlds"));
			 * i++) { System.out.println("Starting world " + i); final World
			 * world = new World(i, Integer.parseInt(p .getProperty("flag" +
			 * i)), p.getProperty("eventmessage" + i),
			 * Integer.parseInt(p.getProperty("exprate" + i)),
			 * Integer.parseInt(p.getProperty("droprate" + i)),
			 * Integer.parseInt(p.getProperty("mesorate" + i)),
			 * Integer.parseInt(p.getProperty("bossdroprate" + i)));
			 *
			 * this.worldRecommendedList.add(new Pair<>(i, p
			 * .getProperty("whyamirecommended" + i))); this.worlds.add(world);
			 * this.channels.add(new LinkedHashMap<Integer, String>()); for (int
			 * j = 0; j < Integer.parseInt(p.getProperty("channels" + i)); j++)
			 * { final int channelId = j + 1; final Channel channel = new
			 * Channel(i, channelId); world.addChannel(channel);
			 * this.channels.get(i).put(channelId, channel.getIP()); }
			 * world.setServerMessage(p.getProperty("servermessage" + i));
			 * System.out.println("Finished loading world " + i + "\r\n"); }
			 */
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		this.acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
		this.acceptor.setHandler(new MapleServerHandler());
		try {
			this.acceptor.bind(new InetSocketAddress(8484));
		} catch (final IOException ex) {
		}

		System.out.println("Listening on port 8484...\r\n\r\n");

		if (ServerConstants.ENABLE_GM_SERVER) {
			GMServer.startGMServer();
		}

		System.out.println("AresDEV - V" + ServerConstants.VERSION
				+ " is online...\r\n");
		this.online = true;
	}

	public void shutdown() {
		TimerManager.getInstance().stop();
		this.acceptor.unbind();
		System.out.println("AresDEV - V" + ServerConstants.VERSION
				+ " is offline...");
		System.exit(0);// BOEIEND :D
	}

	public static void main(String args[]) {
		Server.getInstance().run();
	}

	public Properties getSubnetInfo() {
		return this.subnetInfo;
	}

	public MapleAlliance getAlliance(int id) {
		synchronized (this.alliances) {
			if (this.alliances.containsKey(id)) {
				return this.alliances.get(id);
			}
			return null;
		}
	}

	public void addAlliance(int id, MapleAlliance alliance) {
		synchronized (this.alliances) {
			if (!this.alliances.containsKey(id)) {
				this.alliances.put(id, alliance);
			}
		}
	}

	public void disbandAlliance(int id) {
		synchronized (this.alliances) {
			final MapleAlliance alliance = this.alliances.get(id);
			if (alliance != null) {
				for (final Integer gid : alliance.getGuilds()) {
					this.guilds.get(gid).setAllianceId(0);
				}
				this.alliances.remove(id);
			}
		}
	}

	public void allianceMessage(int id, final byte[] packet, int exception,
			int guildex) {
		final MapleAlliance alliance = this.alliances.get(id);
		if (alliance != null) {
			for (final Integer gid : alliance.getGuilds()) {
				if (guildex == gid) {
					continue;
				}
				final MapleGuild guild = this.guilds.get(gid);
				if (guild != null) {
					guild.broadcast(packet, exception);
				}
			}
		}
	}

	public boolean addGuildtoAlliance(int aId, int guildId) {
		final MapleAlliance alliance = this.alliances.get(aId);
		if (alliance != null) {
			alliance.addGuild(guildId);
			return true;
		}
		return false;
	}

	public boolean removeGuildFromAlliance(int aId, int guildId) {
		final MapleAlliance alliance = this.alliances.get(aId);
		if (alliance != null) {
			alliance.removeGuild(guildId);
			return true;
		}
		return false;
	}

	public boolean setAllianceRanks(int aId, String[] ranks) {
		final MapleAlliance alliance = this.alliances.get(aId);
		if (alliance != null) {
			alliance.setRankTitle(ranks);
			return true;
		}
		return false;
	}

	public boolean setAllianceNotice(int aId, String notice) {
		final MapleAlliance alliance = this.alliances.get(aId);
		if (alliance != null) {
			alliance.setNotice(notice);
			return true;
		}
		return false;
	}

	public boolean increaseAllianceCapacity(int aId, int inc) {
		final MapleAlliance alliance = this.alliances.get(aId);
		if (alliance != null) {
			alliance.increaseCapacity(inc);
			return true;
		}
		return false;
	}

	public Set<Integer> getChannelServer(int world) {
		return new HashSet<>(this.channels.get(world).keySet());
	}

	public byte getHighestChannelId() {
		byte highest = 0;
		for (final Integer channel : this.channels.get(0).keySet()) {
			if ((channel != null) && (channel.intValue() > highest)) {
				highest = channel.byteValue();
			}
		}
		return highest;
	}

	public int createGuild(int leaderId, String name) {
		return MapleGuild.createGuild(leaderId, name);
	}

	public MapleGuild getGuild(int id, MapleGuildCharacter mgc) {
		synchronized (this.guilds) {
			if (this.guilds.get(id) != null) {
				return this.guilds.get(id);
			}
			if (mgc == null) {
				return null;
			}
			final MapleGuild g = new MapleGuild(mgc);
			if (g.getId() == -1) {
				return null;
			}
			this.guilds.put(id, g);
			return g;
		}
	}

	public void clearGuilds() {// remake
		synchronized (this.guilds) {
			this.guilds.clear();
		}
		// for (List<Channel> world : worlds.values()) {
		// reloadGuildCharacters();

	}

	public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline,
			int channel) {
		final MapleGuild g = this.getGuild(mgc.getGuildId(), mgc);
		g.setOnline(mgc.getId(), bOnline, channel);
	}

	public int addGuildMember(MapleGuildCharacter mgc) {
		final MapleGuild g = this.guilds.get(mgc.getGuildId());
		if (g != null) {
			return g.addGuildMember(mgc);
		}
		return 0;
	}

	public boolean setGuildAllianceId(int gId, int aId) {
		final MapleGuild guild = this.guilds.get(gId);
		if (guild != null) {
			guild.setAllianceId(aId);
			return true;
		}
		return false;
	}

	public void leaveGuild(MapleGuildCharacter mgc) {
		final MapleGuild g = this.guilds.get(mgc.getGuildId());
		if (g != null) {
			g.leaveGuild(mgc);
		}
	}

	public void guildChat(int gid, String name, int cid, String msg) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			g.guildChat(name, cid, msg);
		}
	}

	public void changeRank(int gid, int cid, int newRank) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			g.changeRank(cid, newRank);
		}
	}

	public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
		final MapleGuild g = this.guilds.get(initiator.getGuildId());
		if (g != null) {
			g.expelMember(initiator, name, cid);
		}
	}

	public void setGuildNotice(int gid, String notice) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			g.setGuildNotice(notice);
		}
	}

	public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
		final MapleGuild g = this.guilds.get(mgc.getGuildId());
		if (g != null) {
			g.memberLevelJobUpdate(mgc);
		}
	}

	public void changeRankTitle(int gid, String[] ranks) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			g.changeRankTitle(ranks);
		}
	}

	public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo,
			byte logocolor) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			g.setGuildEmblem(bg, bgcolor, logo, logocolor);
		}
	}

	public void disbandGuild(int gid) {
		synchronized (this.guilds) {
			final MapleGuild g = this.guilds.get(gid);
			g.disbandGuild();
			this.guilds.remove(gid);
		}
	}

	public boolean increaseGuildCapacity(int gid) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			return g.increaseCapacity();
		}
		return false;
	}

	public void gainGP(int gid, int amount) {
		final MapleGuild g = this.guilds.get(gid);
		if (g != null) {
			g.gainGP(amount);
		}
	}

	public PlayerBuffStorage getPlayerBuffStorage() {
		return this.buffStorage;
	}

	public void deleteGuildCharacter(MapleGuildCharacter mgc) {
		this.setGuildMemberOnline(mgc, false, (byte) -1);
		if (mgc.getGuildRank() > 1) {
			this.leaveGuild(mgc);
		} else {
			this.disbandGuild(mgc.getGuildId());
		}
	}

	public void reloadGuildCharacters(int world) {
		final World worlda = this.getWorld(world);
		for (final MapleCharacter mc : worlda.getPlayerStorage()
				.getAllCharacters()) {
			if (mc.getGuildId() > 0) {
				this.setGuildMemberOnline(mc.getMGC(), true, worlda.getId());
				this.memberLevelJobUpdate(mc.getMGC());
			}
		}
		worlda.reloadGuildSummary();
	}

	public void broadcastMessage(int world, final byte[] packet) {
		for (final Channel ch : this.getChannelsFromWorld(world)) {
			ch.broadcastPacket(packet);
		}
	}

	public World getWorld(int id) {
		return this.worlds.get(id);
	}

	public List<World> getWorlds() {
		return this.worlds;
	}

	public void gmChat(String message, String exclude) {
		GMServer.broadcastInGame(MaplePacketCreator.serverNotice(6, message));
		GMServer.broadcastOutGame(GMPacketCreator.chat(message), exclude);
	}

	public final Runnable shutdown(final boolean restart) {
		return new Runnable() {
			@Override
			public void run() {
				System.out.println((restart ? "Restarting" : "Shutting down")
						+ " the server...\r\n");
				if (Server.this.getWorlds() == null) {
					return;
				}
				for (final World w : Server.this.getWorlds()) {
					w.shutdown();
				}
				for (final World w : Server.this.getWorlds()) {
					while (w.getPlayerStorage().getAllCharacters().size() > 0) {
						try {
							Thread.sleep(1000);
						} catch (final InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
				for (final Channel ch : Server.this.getAllChannels()) {
					while (ch.getConnectedClients() > 0) {
						try {
							Thread.sleep(1000);
						} catch (final InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}

				TimerManager.getInstance().purge();
				TimerManager.getInstance().stop();

				for (final Channel ch : Server.this.getAllChannels()) {
					while (!ch.finishedShutdown()) {
						try {
							Thread.sleep(1000);
						} catch (final InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
				Server.this.worlds.clear();
				Server.this.worlds = null;
				Server.this.channels.clear();
				Server.this.channels = null;
				Server.this.worldRecommendedList.clear();
				Server.this.worldRecommendedList = null;

				System.out.println("Worlds and Channels are offline....");
				Server.this.acceptor.unbind();
				Server.this.acceptor = null;
				if (!restart) {
					System.exit(0);
				} else {
					System.out.println("\r\nRestarting the server...\r\n");
					try {
						instance.finalize();// FUU I CAN AND IT'S FREE
					} catch (final Throwable ex) {
					}
					instance = null;
					System.gc();
					getInstance().run();// DID I DO EVERYTHING?! D:
				}
			}
		};
	}
}