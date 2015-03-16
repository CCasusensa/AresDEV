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

import gm.server.GMServer;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptEngine;

import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.MapleGuildCharacter;
import net.server.world.MapleMessengerCharacter;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;

import org.apache.mina.core.session.IoSession;

import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.MapleMiniGame;
import server.MaplePlayerShop;
import server.MapleTrade;
import server.TimerManager;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.MaplePacketCreator;

public class MapleClient {

	public static final int LOGIN_NOTLOGGEDIN = 0;
	public static final int LOGIN_SERVER_TRANSITION = 1;
	public static final int LOGIN_LOGGEDIN = 2;
	public static final String CLIENT_KEY = "CLIENT";
	private final MapleAESOFB send;
	private final MapleAESOFB receive;
	private final IoSession session;
	private MapleCharacter player;
	private int channel = 1;
	private int accId = 1;
	private boolean loggedIn = false;
	private boolean serverTransition = false;
	private Calendar birthday = null;
	private String accountName = null;
	private int world;
	private long lastPong;
	private int gmlevel;
	private final Set<String> macs = new HashSet<>();
	private final Map<String, ScriptEngine> engines = new HashMap<>();
	private ScheduledFuture<?> idleTask = null;
	private byte characterSlots = 3;
	private byte loginattempt = 0;
	private String pin = null;
	private int pinattempt = 0;
	private String pic = null;
	private int picattempt = 0;
	private byte gender = -1;
	private boolean disconnecting = false;
	private final Lock mutex = new ReentrantLock(true);

	public MapleClient(MapleAESOFB send, MapleAESOFB receive, IoSession session) {
		this.send = send;
		this.receive = receive;
		this.session = session;
	}

	public synchronized MapleAESOFB getReceiveCrypto() {
		return this.receive;
	}

	public synchronized MapleAESOFB getSendCrypto() {
		return this.send;
	}

	public synchronized IoSession getSession() {
		return this.session;
	}

	public MapleCharacter getPlayer() {
		return this.player;
	}

	public void setPlayer(MapleCharacter player) {
		this.player = player;
	}

	public void sendCharList(int server) {
		this.session.write(MaplePacketCreator.getCharList(this, server));
	}

	public List<MapleCharacter> loadCharacters(int serverId) {
		final List<MapleCharacter> chars = new ArrayList<>(15);
		try {
			for (final CharNameAndId cni : this
					.loadCharactersInternal(serverId)) {
				chars.add(MapleCharacter.loadCharFromDB(cni.id, this, false));
			}
		} catch (final Exception e) {
		}
		return chars;
	}

	public List<String> loadCharacterNames(int serverId) {
		final List<String> chars = new ArrayList<>(15);
		for (final CharNameAndId cni : this.loadCharactersInternal(serverId)) {
			chars.add(cni.name);
		}
		return chars;
	}

	private List<CharNameAndId> loadCharactersInternal(int serverId) {
		PreparedStatement ps;
		final List<CharNameAndId> chars = new ArrayList<>(15);
		try {
			ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
			ps.setInt(1, this.getAccID());
			ps.setInt(2, serverId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					chars.add(new CharNameAndId(rs.getString("name"), rs
							.getInt("id")));
				}
			}
			ps.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		return chars;
	}

	public boolean isLoggedIn() {
		return this.loggedIn;
	}

	public boolean hasBannedIP() {
		boolean ret = false;
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
				ps.setString(1, this.session.getRemoteAddress().toString());
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					if (rs.getInt(1) > 0) {
						ret = true;
					}
				}
			}
		} catch (final SQLException e) {
		}
		return ret;
	}

	public boolean hasBannedMac() {
		if (this.macs.isEmpty()) {
			return false;
		}
		boolean ret = false;
		int i;
		try {
			final StringBuilder sql = new StringBuilder(
					"SELECT COUNT(*) FROM macbans WHERE mac IN (");
			for (i = 0; i < this.macs.size(); i++) {
				sql.append("?");
				if (i != (this.macs.size() - 1)) {
					sql.append(", ");
				}
			}
			sql.append(")");
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(sql.toString())) {
				i = 0;
				for (final String mac : this.macs) {
					i++;
					ps.setString(i, mac);
				}
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					if (rs.getInt(1) > 0) {
						ret = true;
					}
				}
			}
		} catch (final Exception e) {
		}
		return ret;
	}

	private void loadMacsIfNescessary() throws SQLException {
		if (this.macs.isEmpty()) {
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement("SELECT macs FROM accounts WHERE id = ?")) {
				ps.setInt(1, this.accId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						for (final String mac : rs.getString("macs")
								.split(", ")) {
							if (!mac.equals("")) {
								this.macs.add(mac);
							}
						}
					}
				}
			}
		}
	}

	public void banMacs() {
		final Connection con = DatabaseConnection.getConnection();
		try {
			this.loadMacsIfNescessary();
			final List<String> filtered = new LinkedList<>();
			try (PreparedStatement ps = con
					.prepareStatement("SELECT filter FROM macfilters");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					filtered.add(rs.getString("filter"));
				}
			}
			try (PreparedStatement ps = con
					.prepareStatement("INSERT INTO macbans (mac) VALUES (?)")) {
				for (final String mac : this.macs) {
					boolean matched = false;
					for (final String filter : filtered) {
						if (mac.matches(filter)) {
							matched = true;
							break;
						}
					}
					if (!matched) {
						ps.setString(1, mac);
						ps.executeUpdate();
					}
				}
			}
		} catch (final SQLException e) {
		}
	}

	public int finishLogin() {
		synchronized (MapleClient.class) {
			if (this.getLoginState() > LOGIN_NOTLOGGEDIN) {
				this.loggedIn = false;
				return 7;
			}
			this.updateLoginState(LOGIN_LOGGEDIN);
		}
		return 0;
	}

	public void setPin(String pin) {
		this.pin = pin;
		try {
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"UPDATE accounts SET pin = ? WHERE id = ?")) {
				ps.setString(1, pin);
				ps.setInt(2, this.accId);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public String getPin() {
		return this.pin;
	}

	public boolean checkPin(String other) {
		this.pinattempt++;
		if (this.pinattempt > 5) {
			this.getSession().close(true);
		}
		if (this.pin.equals(other)) {
			this.pinattempt = 0;
			return true;
		}
		return false;
	}

	public void setPic(String pic) {
		this.pic = pic;
		try {
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"UPDATE accounts SET pic = ? WHERE id = ?")) {
				ps.setString(1, pic);
				ps.setInt(2, this.accId);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public String getPic() {
		return this.pic;
	}

	public boolean checkPic(String other) {
		this.picattempt++;
		if (this.picattempt > 5) {
			this.getSession().close(true);
		}
		if (this.pic.equals(other)) {
			this.picattempt = 0;
			return true;
		}
		return false;
	}

	public int login(String login, String pwd) {
		this.loginattempt++;
		if (this.loginattempt > 4) {
			this.getSession().close(true);
		}
		int loginok = 5;
		final Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT id, password, salt, gender, banned, gm, pin, pic, characterslots, tos FROM accounts WHERE name = ?");
			ps.setString(1, login);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (rs.getByte("banned") == 1) {
					return 3;
				}
				this.accId = rs.getInt("id");
				this.gmlevel = rs.getInt("gm");
				this.pin = rs.getString("pin");
				this.pic = rs.getString("pic");
				this.gender = rs.getByte("gender");
				this.characterSlots = rs.getByte("characterslots");
				final String passhash = rs.getString("password");
				final String salt = rs.getString("salt");

				// we do not unban
				final byte tos = rs.getByte("tos");
				ps.close();
				rs.close();
				if (this.getLoginState() > LOGIN_NOTLOGGEDIN) { // already
																// loggedin
					this.loggedIn = false;
					loginok = 7;
				} else if (pwd.equals(passhash)
						|| checkHash(passhash, "SHA-1", pwd)
						|| checkHash(passhash, "SHA-512", pwd + salt)) {
					if (tos == 0) {
						loginok = 23;
					} else {
						loginok = 0;
					}
				} else {
					this.loggedIn = false;
					loginok = 4;
				}

				ps = con.prepareStatement("INSERT INTO iplog (accountid, ip) VALUES (?, ?)");
				ps.setInt(1, this.accId);
				ps.setString(2, this.session.getRemoteAddress().toString());
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if ((ps != null) && !ps.isClosed()) {
					ps.close();
				}
				if ((rs != null) && !rs.isClosed()) {
					rs.close();
				}
			} catch (final SQLException e) {
			}
		}

		if (loginok == 0) {
			this.loginattempt = 0;
		}
		return loginok;
	}

	public Calendar getTempBanCalendar() {
		final Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		final Calendar lTempban = Calendar.getInstance();
		try {
			ps = con.prepareStatement("SELECT `tempban` FROM accounts WHERE id = ?");
			ps.setInt(1, this.getAccID());
			rs = ps.executeQuery();
			if (!rs.next()) {
				return null;
			}
			final long blubb = rs.getLong("tempban");
			if (blubb == 0) { // basically if timestamp in db is 0000-00-00
				return null;
			}
			lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
			return lTempban;
		} catch (final SQLException e) {
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				if (rs != null) {
					rs.close();
				}
			} catch (final SQLException e) {
			}
		}
		return null;// why oh why!?!
	}

	public static long dottedQuadToLong(String dottedQuad)
			throws RuntimeException {
		final String[] quads = dottedQuad.split("\\.");
		if (quads.length != 4) {
			throw new RuntimeException("Invalid IP Address format.");
		}
		long ipAddress = 0;
		for (int i = 0; i < 4; i++) {
			final int quad = Integer.parseInt(quads[i]);
			ipAddress += (quad % 256) * (long) Math.pow(256, 4 - i);
		}
		return ipAddress;
	}

	public void updateMacs(String macData) {
		this.macs.addAll(Arrays.asList(macData.split(", ")));
		final StringBuilder newMacData = new StringBuilder();
		final Iterator<String> iter = this.macs.iterator();
		PreparedStatement ps = null;
		while (iter.hasNext()) {
			final String cur = iter.next();
			newMacData.append(cur);
			if (iter.hasNext()) {
				newMacData.append(", ");
			}
		}
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"UPDATE accounts SET macs = ? WHERE id = ?");
			ps.setString(1, newMacData.toString());
			ps.setInt(2, this.accId);
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if ((ps != null) && !ps.isClosed()) {
					ps.close();
				}
			} catch (final SQLException ex) {
			}
		}
	}

	public void setAccID(int id) {
		this.accId = id;
	}

	public int getAccID() {
		return this.accId;
	}

	public void updateLoginState(int newstate) {
		try {
			final Connection con = DatabaseConnection.getConnection();
			try (PreparedStatement ps = con
					.prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
				ps.setInt(1, newstate);
				ps.setInt(2, this.getAccID());
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		if (newstate == LOGIN_NOTLOGGEDIN) {
			this.loggedIn = false;
			this.serverTransition = false;
		} else {
			this.serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
			this.loggedIn = !this.serverTransition;
		}
	}

	public int getLoginState() {
		try {
			final Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?");
			ps.setInt(1, this.getAccID());
			final ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				throw new RuntimeException("getLoginState - MapleClient");
			}
			this.birthday = Calendar.getInstance();
			final long blubb = rs.getLong("birthday");
			if (blubb > 0) {
				this.birthday.setTimeInMillis(blubb * 1000);
			}
			int state = rs.getInt("loggedin");
			if (state == LOGIN_SERVER_TRANSITION) {
				if ((rs.getTimestamp("lastlogin").getTime() + 30000) < System
						.currentTimeMillis()) {
					state = LOGIN_NOTLOGGEDIN;
					this.updateLoginState(LOGIN_NOTLOGGEDIN);
				}
			}
			rs.close();
			ps.close();
			if (state == LOGIN_LOGGEDIN) {
				this.loggedIn = true;
			} else if (state == LOGIN_SERVER_TRANSITION) {
				ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
				ps.setInt(1, this.getAccID());
				ps.executeUpdate();
				ps.close();
			} else {
				this.loggedIn = false;
			}
			return state;
		} catch (final SQLException e) {
			this.loggedIn = false;
			e.printStackTrace();
			throw new RuntimeException("login state");
		}
	}

	public boolean checkBirthDate(Calendar date) {
		return (date.get(Calendar.YEAR) == this.birthday.get(Calendar.YEAR))
				&& (date.get(Calendar.MONTH) == this.birthday
						.get(Calendar.MONTH))
				&& (date.get(Calendar.DAY_OF_MONTH) == this.birthday
						.get(Calendar.DAY_OF_MONTH));
	}

	private void removePlayer() {
		try {
			this.player.cancelAllBuffs(true);
			this.player.cancelAllDebuffs();
			final MaplePlayerShop mps = this.player.getPlayerShop();
			if (mps != null) {
				mps.removeVisitors();
				this.player.setPlayerShop(null);
			}
			final HiredMerchant merchant = this.player.getHiredMerchant();
			if (merchant != null) {
				if (merchant.isOwner(this.player)) {
					merchant.setOpen(true);
				} else {
					merchant.removeVisitor(this.player);
				}
				try {
					merchant.saveItems(false);
				} catch (final SQLException ex) {
					System.out
							.println("Error while saving Hired Merchant items.");
				}
			}
			this.player.setMessenger(null);
			final MapleMiniGame game = this.player.getMiniGame();
			if (game != null) {
				this.player.setMiniGame(null);
				if (game.isOwner(this.player)) {
					this.player.getMap().broadcastMessage(
							MaplePacketCreator.removeCharBox(this.player));
					game.broadcastToVisitor(MaplePacketCreator
							.getMiniGameClose());
				} else {
					game.removeVisitor(this.player);
				}
			}
			NPCScriptManager.getInstance().dispose(this);
			QuestScriptManager.getInstance().dispose(this);
			if (this.player.getTrade() != null) {
				MapleTrade.cancelTrade(this.player);
			}
			if (this.gmlevel > 0) {
				GMServer.removeInGame(this.player.getName());
			}
			if (this.player.getEventInstance() != null) {
				this.player.getEventInstance().playerDisconnected(this.player);
			}
			if (this.player.getMap() != null) {
				this.player.getMap().removePlayer(this.player);
			}
		} catch (final Throwable t) {
			FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, t);
		}
	}

	public final void disconnect(boolean shutdown, boolean cashshop) {// once
																		// per
																		// MapleClient
																		// instance
		if (this.disconnecting) {
			return;
		}
		this.disconnecting = true;
		if ((this.player != null) && this.player.isLoggedin()
				&& (this.player.getClient() != null)) {
			final MapleMap map = this.player.getMap();
			final MapleParty party = this.player.getParty();
			final int idz = this.player.getId(), messengerid = this.player
					.getMessenger() == null ? 0 : this.player.getMessenger()
					.getId();
			this.player.getFamilyId();
			this.player.getName();
			final BuddyList bl = this.player.getBuddylist();
			final MaplePartyCharacter chrp = new MaplePartyCharacter(
					this.player);
			final MapleMessengerCharacter chrm = new MapleMessengerCharacter(
					this.player);
			// final MapleGuildCharacter chrg = player.getMGC();

			this.removePlayer();
			this.player.saveToDB();
			if ((this.channel == -1) || shutdown) {
				this.player = null;
				return;
			}
			final World worlda = this.getWorldServer();
			try {
				if (!cashshop) {
					if (messengerid > 0) {
						worlda.leaveMessenger(messengerid, chrm);
					}

					/*
					 * for (MapleQuestStatus status : player.getStartedQuests())
					 * { MapleQuest quest = status.getQuest(); if
					 * (quest.getTimeLimit() > 0) { MapleQuestStatus newStatus =
					 * new MapleQuestStatus(quest,
					 * MapleQuestStatus.Status.NOT_STARTED);
					 * newStatus.setForfeited
					 * (player.getQuest(quest).getForfeited() + 1);
					 * player.updateQuest(newStatus); } }
					 */
					if (party != null) {
						chrp.setOnline(false);
						worlda.updateParty(party.getId(),
								PartyOperation.LOG_ONOFF, chrp);
						if ((map != null) && (party.getLeader().getId() == idz)) {
							MaplePartyCharacter lchr = null;
							for (final MaplePartyCharacter pchr : party
									.getMembers()) {
								if ((pchr != null)
										&& (map.getCharacterById(pchr.getId()) != null)
										&& ((lchr == null) || (lchr.getLevel() < pchr
												.getLevel()))) {
									lchr = pchr;
								}
							}
							if (lchr != null) {
								worlda.updateParty(party.getId(),
										PartyOperation.CHANGE_LEADER, lchr);
							}
						}
					}
					if (bl != null) {
						if (!this.serverTransition) {
							worlda.loggedOff(this.player.getName(),
									this.player.getId(), this.channel,
									this.player.getBuddylist().getBuddyIds());
						} else {
							worlda.loggedOn(this.player.getName(),
									this.player.getId(), this.channel,
									this.player.getBuddylist().getBuddyIds());
						}
					}
				} else {
					if (party != null) {
						chrp.setOnline(false);
						worlda.updateParty(party.getId(),
								PartyOperation.LOG_ONOFF, chrp);
					}
					if (!this.serverTransition) {
						worlda.loggedOff(this.player.getName(), this.player
								.getId(), this.channel, this.player
								.getBuddylist().getBuddyIds());
					} else {
						worlda.loggedOn(this.player.getName(), this.player
								.getId(), this.channel, this.player
								.getBuddylist().getBuddyIds());
					}
				}
			} catch (final Exception e) {
				FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, e);
			} finally {
				this.getChannelServer().removePlayer(this.player);
				if (!this.serverTransition) {
					worlda.removePlayer(this.player);
					if (this.player != null) {// no idea, occur :(
						this.player.empty(false);
					}
					this.player.logOff();
				}
				this.player = null;
			}
		}
		if (!this.serverTransition && this.isLoggedIn()) {
			this.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
			this.session.removeAttribute(MapleClient.CLIENT_KEY); // prevents
																	// double
																	// dcing
																	// during
																	// login
			this.session.close();
		}
		this.engines.clear();
	}

	public int getChannel() {
		return this.channel;
	}

	public Channel getChannelServer() {
		return Server.getInstance().getChannel(this.world, this.channel);
	}

	public World getWorldServer() {
		return Server.getInstance().getWorld(this.world);
	}

	public Channel getChannelServer(byte channel) {
		return Server.getInstance().getChannel(this.world, channel);
	}

	public boolean deleteCharacter(int cid) {
		final Connection con = DatabaseConnection.getConnection();
		try {
			try (PreparedStatement ps = con
					.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?")) {
				ps.setInt(1, cid);
				ps.setInt(2, this.accId);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return false;
					}
					if (rs.getInt("guildid") > 0) {
						try {
							Server.getInstance().deleteGuildCharacter(
									new MapleGuildCharacter(cid, 0, rs
											.getString("name"), (byte) -1,
											(byte) -1, 0, rs
													.getInt("guildrank"), rs
													.getInt("guildid"), false,
											rs.getInt("allianceRank")));
						} catch (final Exception re) {
							return false;
						}
					}
				}
			}
			try (PreparedStatement ps = con
					.prepareStatement("DELETE FROM wishlists WHERE charid = ?")) {
				ps.setInt(1, cid);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = con
					.prepareStatement("DELETE FROM characters WHERE id = ?")) {
				ps.setInt(1, cid);
				ps.executeUpdate();
			}
			final String[] toDel = { "famelog", "inventoryitems", "keymap",
					"queststatus", "savedlocations", "skillmacros", "skills",
					"eventstats" };
			for (final String s : toDel) {
				MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM `" + s
						+ "` WHERE characterid = ?", cid);
			}
			return true;
		} catch (final SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String getAccountName() {
		return this.accountName;
	}

	public void setAccountName(String a) {
		this.accountName = a;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public int getWorld() {
		return this.world;
	}

	public void setWorld(int world) {
		this.world = world;
	}

	public void pongReceived() {
		this.lastPong = System.currentTimeMillis();
	}

	public void sendPing() {
		final long then = System.currentTimeMillis();
		this.announce(MaplePacketCreator.getPing());
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					if (MapleClient.this.lastPong < then) {
						if ((MapleClient.this.getSession() != null)
								&& MapleClient.this.getSession().isConnected()) {
							MapleClient.this.getSession().close(true);
						}
					}
				} catch (final NullPointerException e) {
				}
			}
		}, 15000);
	}

	public Set<String> getMacs() {
		return Collections.unmodifiableSet(this.macs);
	}

	public int gmLevel() {
		return this.gmlevel;
	}

	public void setScriptEngine(String name, ScriptEngine e) {
		this.engines.put(name, e);
	}

	public ScriptEngine getScriptEngine(String name) {
		return this.engines.get(name);
	}

	public void removeScriptEngine(String name) {
		this.engines.remove(name);
	}

	public ScheduledFuture<?> getIdleTask() {
		return this.idleTask;
	}

	public void setIdleTask(ScheduledFuture<?> idleTask) {
		this.idleTask = idleTask;
	}

	public NPCConversationManager getCM() {
		return NPCScriptManager.getInstance().getCM(this);
	}

	public QuestActionManager getQM() {
		return QuestScriptManager.getInstance().getQM(this);
	}

	public boolean acceptToS() {
		boolean disconnectForBeingAFaggot = false;
		if (this.accountName == null) {
			return true;
		}
		try {
			PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement("SELECT `tos` FROM accounts WHERE id = ?");
			ps.setInt(1, this.accId);
			final ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				if (rs.getByte("tos") == 1) {
					disconnectForBeingAFaggot = true;
				}
			}
			ps.close();
			rs.close();
			ps = DatabaseConnection.getConnection().prepareStatement(
					"UPDATE accounts SET tos = 1 WHERE id = ?");
			ps.setInt(1, this.accId);
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException e) {
		}
		return disconnectForBeingAFaggot;
	}

	public final Lock getLock() {
		return this.mutex;
	}

	private static class CharNameAndId {

		public String name;
		public int id;

		public CharNameAndId(String name, int id) {
			super();
			this.name = name;
			this.id = id;
		}
	}

	public static boolean checkHash(String hash, String type, String password) {
		try {
			final MessageDigest digester = MessageDigest.getInstance(type);
			digester.update(password.getBytes("UTF-8"), 0, password.length());
			return HexTool.toString(digester.digest()).replace(" ", "")
					.toLowerCase().equals(hash);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			throw new RuntimeException("Encoding the string failed", e);
		}
	}

	public short getCharacterSlots() {
		return this.characterSlots;
	}

	public boolean gainCharacterSlot() {
		if (this.characterSlots < 15) {
			final Connection con = DatabaseConnection.getConnection();
			try {
				try (PreparedStatement ps = con
						.prepareStatement("UPDATE accounts SET characterslots = ? WHERE id = ?")) {
					ps.setInt(1, this.characterSlots += 1);
					ps.setInt(2, this.accId);
					ps.executeUpdate();
				}
			} catch (final SQLException e) {
			}
			return true;
		}
		return false;
	}

	public final byte getGReason() {
		final Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `greason` FROM `accounts` WHERE id = ?");
			ps.setInt(1, this.accId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getByte("greason");
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				if (rs != null) {
					rs.close();
				}
			} catch (final SQLException e) {
			}
		}
		return 0;
	}

	public byte getGender() {
		return this.gender;
	}

	public void setGender(byte m) {
		this.gender = m;
		try {
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"UPDATE accounts SET gender = ? WHERE id = ?")) {
				ps.setByte(1, this.gender);
				ps.setInt(2, this.accId);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public synchronized void announce(final byte[] packet) {// MINA CORE IS A
															// FUCKING BITCH AND
															// I HATE IT <3
		this.session.write(packet);
	}
}
