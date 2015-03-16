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
package net.server.world;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.server.PlayerStorage;
import net.server.Server;
import net.server.channel.Channel;
import net.server.channel.CharacterIdChannelPair;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.guild.MapleGuildSummary;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddyListEntry;
import client.MapleCharacter;
import client.MapleFamily;

/**
 *
 * @author kevintjuh93
 */
public class World {

	private final int id;
	private int flag;
	private int expRate;
	private int dropRate;
	private int mesoRate;
	private final int bossDropRate;
	private final String eventMsg;
	private final List<Channel> channels = new ArrayList<>();
	private final Map<Integer, MapleParty> parties = new HashMap<>();
	private final AtomicInteger runningPartyId = new AtomicInteger();
	private final Map<Integer, MapleMessenger> messengers = new HashMap<>();
	private final AtomicInteger runningMessengerId = new AtomicInteger();
	private final Map<Integer, MapleFamily> families = new LinkedHashMap<>();
	private final Map<Integer, MapleGuildSummary> gsStore = new HashMap<>();
	private final PlayerStorage players = new PlayerStorage();

	/**
	 *
	 * @param worldId
	 * @param flag
	 *            - 0 = nothing, 1 = event, 2 = new, 3 = hot
	 * @param eventMsg
	 * @param expRate
	 * @param dropRate
	 * @param mesoRate
	 * @param bossDropRate
	 */
	public World(int worldId, int flag, String eventMsg, int expRate,
			int dropRate, int mesoRate, int bossDropRate) {
		this.id = worldId;
		this.flag = flag;
		this.eventMsg = eventMsg;
		this.expRate = expRate;
		this.dropRate = dropRate;
		this.mesoRate = mesoRate;
		this.bossDropRate = bossDropRate;
		this.runningPartyId.set(1);
		this.runningMessengerId.set(1);
	}

	public List<Channel> getChannels() {
		return this.channels;
	}

	public Channel getChannel(int channel) {
		return this.channels.get(channel - 1);
	}

	public void addChannel(Channel channel) {
		this.channels.add(channel);
	}

	public void removeChannel(int channel) {
		this.channels.remove(channel);
	}

	public void setFlag(byte b) {
		this.flag = b;
	}

	public int getFlag() {
		return this.flag;
	}

	public String getEventMessage() {
		return this.eventMsg;
	}

	public int getExpRate() {
		return this.expRate;
	}

	public void setExpRate(int expRate) {
		this.expRate = expRate;
	}

	public int getDropRate() {
		return this.dropRate;
	}

	public void setDropRate(int dropRate) {
		this.dropRate = dropRate;
	}

	public int getMesoRate() {
		return this.mesoRate;
	}

	public void setMesoRate(int mesoRate) {
		this.mesoRate = mesoRate;
	}

	public int getBossDropRate() {
		return this.bossDropRate;
	}

	public PlayerStorage getPlayerStorage() {
		return this.players;
	}

	public void removePlayer(MapleCharacter chr) {
		this.channels.get(chr.getClient().getChannel() - 1).removePlayer(chr);
		this.players.removePlayer(chr.getId());
	}

	public int getId() {
		return this.id;
	}

	public void addFamily(int id, MapleFamily f) {
		synchronized (this.families) {
			if (!this.families.containsKey(id)) {
				this.families.put(id, f);
			}
		}
	}

	public MapleFamily getFamily(int id) {
		synchronized (this.families) {
			if (this.families.containsKey(id)) {
				return this.families.get(id);
			}
			return null;
		}
	}

	public MapleGuild getGuild(MapleGuildCharacter mgc) {
		final int gid = mgc.getGuildId();
		MapleGuild g;
		g = Server.getInstance().getGuild(gid, mgc);
		if (this.gsStore.get(gid) == null) {
			this.gsStore.put(gid, new MapleGuildSummary(g));
		}
		return g;
	}

	public MapleGuildSummary getGuildSummary(int gid) {
		if (this.gsStore.containsKey(gid)) {
			return this.gsStore.get(gid);
		} else {
			final MapleGuild g = Server.getInstance().getGuild(gid, null);
			if (g != null) {
				this.gsStore.put(gid, new MapleGuildSummary(g));
			}
			return this.gsStore.get(gid);
		}
	}

	public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
		this.gsStore.put(gid, mgs);
	}

	public void reloadGuildSummary() {
		MapleGuild g;
		final Server server = Server.getInstance();
		for (final int i : this.gsStore.keySet()) {
			g = server.getGuild(i, null);
			if (g != null) {
				this.gsStore.put(i, new MapleGuildSummary(g));
			} else {
				this.gsStore.remove(i);
			}
		}
	}

	public void setGuildAndRank(List<Integer> cids, int guildid, int rank,
			int exception) {
		for (final int cid : cids) {
			if (cid != exception) {
				this.setGuildAndRank(cid, guildid, rank);
			}
		}
	}

	public void setOfflineGuildStatus(int guildid, int guildrank, int cid) {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
				ps.setInt(1, guildid);
				ps.setInt(2, guildrank);
				ps.setInt(3, cid);
				ps.execute();
			}
		} catch (final SQLException se) {
			se.printStackTrace();
		}
	}

	public void setGuildAndRank(int cid, int guildid, int rank) {
		final MapleCharacter mc = this.getPlayerStorage().getCharacterById(cid);
		if (mc == null) {
			return;
		}
		boolean bDifferentGuild;
		if ((guildid == -1) && (rank == -1)) {
			bDifferentGuild = true;
		} else {
			bDifferentGuild = guildid != mc.getGuildId();
			mc.setGuildId(guildid);
			mc.setGuildRank(rank);
			mc.saveGuildStatus();
		}
		if (bDifferentGuild) {
			mc.getMap().broadcastMessage(mc,
					MaplePacketCreator.removePlayerFromMap(cid), false);
			mc.getMap().broadcastMessage(mc,
					MaplePacketCreator.spawnPlayerMapobject(mc), false);
		}
	}

	public void changeEmblem(int gid, List<Integer> affectedPlayers,
			MapleGuildSummary mgs) {
		this.updateGuildSummary(gid, mgs);
		this.sendPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(
				gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(),
				mgs.getLogoColor()), -1);
		this.setGuildAndRank(affectedPlayers, -1, -1, -1); // respawn player
	}

	public void sendPacket(List<Integer> targetIds, final byte[] packet,
			int exception) {
		MapleCharacter c;
		for (final int i : targetIds) {
			if (i == exception) {
				continue;
			}
			c = this.getPlayerStorage().getCharacterById(i);
			if (c != null) {
				c.getClient().announce(packet);
			}
		}
	}

	public MapleParty createParty(MaplePartyCharacter chrfor) {
		final int partyid = this.runningPartyId.getAndIncrement();
		final MapleParty party = new MapleParty(partyid, chrfor);
		this.parties.put(party.getId(), party);
		return party;
	}

	public MapleParty getParty(int partyid) {
		return this.parties.get(partyid);
	}

	public MapleParty disbandParty(int partyid) {
		return this.parties.remove(partyid);
	}

	public void updateParty(MapleParty party, PartyOperation operation,
			MaplePartyCharacter target) {
		for (final MaplePartyCharacter partychar : party.getMembers()) {
			final MapleCharacter chr = this.getPlayerStorage()
					.getCharacterByName(partychar.getName());
			if (chr != null) {
				if (operation == PartyOperation.DISBAND) {
					chr.setParty(null);
					chr.setMPC(null);
				} else {
					chr.setParty(party);
					chr.setMPC(partychar);
				}
				chr.getClient().announce(
						MaplePacketCreator.updateParty(chr.getClient()
								.getChannel(), party, operation, target));
			}
		}
		switch (operation) {
		case LEAVE:
		case EXPEL:
			final MapleCharacter chr = this.getPlayerStorage()
					.getCharacterByName(target.getName());
			if (chr != null) {
				chr.getClient().announce(
						MaplePacketCreator.updateParty(chr.getClient()
								.getChannel(), party, operation, target));
				chr.setParty(null);
				chr.setMPC(null);
			}
		}
	}

	public void updateParty(int partyid, PartyOperation operation,
			MaplePartyCharacter target) {
		final MapleParty party = this.getParty(partyid);
		if (party == null) {
			throw new IllegalArgumentException(
					"no party with the specified partyid exists");
		}
		switch (operation) {
		case JOIN:
			party.addMember(target);
			break;
		case EXPEL:
		case LEAVE:
			party.removeMember(target);
			break;
		case DISBAND:
			this.disbandParty(partyid);
			break;
		case SILENT_UPDATE:
		case LOG_ONOFF:
			party.updateMember(target);
			break;
		case CHANGE_LEADER:
			party.setLeader(target);
			break;
		default:
			System.out.println("Unhandeled updateParty operation "
					+ operation.name());
		}
		this.updateParty(party, operation, target);
	}

	public int find(String name) {
		int channel = -1;
		final MapleCharacter chr = this.getPlayerStorage().getCharacterByName(
				name);
		if (chr != null) {
			channel = chr.getClient().getChannel();
		}
		return channel;
	}

	public int find(int id) {
		int channel = -1;
		final MapleCharacter chr = this.getPlayerStorage().getCharacterById(id);
		if (chr != null) {
			channel = chr.getClient().getChannel();
		}
		return channel;
	}

	public void partyChat(MapleParty party, String chattext, String namefrom) {
		for (final MaplePartyCharacter partychar : party.getMembers()) {
			if (!(partychar.getName().equals(namefrom))) {
				final MapleCharacter chr = this.getPlayerStorage()
						.getCharacterByName(partychar.getName());
				if (chr != null) {
					chr.getClient()
							.announce(
									MaplePacketCreator.multiChat(namefrom,
											chattext, 1));
				}
			}
		}
	}

	public void buddyChat(int[] recipientCharacterIds, int cidFrom,
			String nameFrom, String chattext) {
		final PlayerStorage playerStorage = this.getPlayerStorage();
		for (final int characterId : recipientCharacterIds) {
			final MapleCharacter chr = playerStorage
					.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddylist().containsVisible(cidFrom)) {
					chr.getClient()
							.announce(
									MaplePacketCreator.multiChat(nameFrom,
											chattext, 0));
				}
			}
		}
	}

	public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom,
			int[] characterIds) {
		final List<CharacterIdChannelPair> foundsChars = new ArrayList<>(
				characterIds.length);
		for (final Channel ch : this.getChannels()) {
			for (final int charid : ch.multiBuddyFind(charIdFrom, characterIds)) {
				foundsChars.add(new CharacterIdChannelPair(charid, ch.getId()));
			}
		}
		return foundsChars.toArray(new CharacterIdChannelPair[foundsChars
				.size()]);
	}

	public MapleMessenger getMessenger(int messengerid) {
		return this.messengers.get(messengerid);
	}

	public void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
		final MapleMessenger messenger = this.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException(
					"No messenger with the specified messengerid exists");
		}
		final int position = messenger.getPositionByName(target.getName());
		messenger.removeMember(target);
		this.removeMessengerPlayer(messenger, position);
	}

	public void messengerInvite(String sender, int messengerid, String target,
			int fromchannel) {
		if (this.isConnected(target)) {
			final MapleMessenger messenger = this.getPlayerStorage()
					.getCharacterByName(target).getMessenger();
			if (messenger == null) {
				this.getPlayerStorage()
						.getCharacterByName(target)
						.getClient()
						.announce(
								MaplePacketCreator.messengerInvite(sender,
										messengerid));
				final MapleCharacter from = this.getChannel(fromchannel)
						.getPlayerStorage().getCharacterByName(sender);
				from.getClient().announce(
						MaplePacketCreator.messengerNote(target, 4, 1));
			} else {
				final MapleCharacter from = this.getChannel(fromchannel)
						.getPlayerStorage().getCharacterByName(sender);
				from.getClient()
						.announce(
								MaplePacketCreator.messengerChat(sender + " : "
										+ target
										+ " is already using Maple Messenger"));
			}
		}
	}

	public void addMessengerPlayer(MapleMessenger messenger, String namefrom,
			int fromchannel, int position) {
		for (final MapleMessengerCharacter messengerchar : messenger
				.getMembers()) {
			if (!(messengerchar.getName().equals(namefrom))) {
				final MapleCharacter chr = this.getPlayerStorage()
						.getCharacterByName(messengerchar.getName());
				if (chr != null) {
					final MapleCharacter from = this.getChannel(fromchannel)
							.getPlayerStorage().getCharacterByName(namefrom);
					chr.getClient().announce(
							MaplePacketCreator.addMessengerPlayer(namefrom,
									from, position, (byte) (fromchannel - 1)));
					from.getClient().announce(
							MaplePacketCreator.addMessengerPlayer(
									chr.getName(), chr,
									messengerchar.getPosition(),
									(byte) (messengerchar.getChannel() - 1)));
				}
			} else if ((messengerchar.getName().equals(namefrom))) {
				final MapleCharacter chr = this.getPlayerStorage()
						.getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().announce(
							MaplePacketCreator.joinMessenger(messengerchar
									.getPosition()));
				}
			}
		}
	}

	public void removeMessengerPlayer(MapleMessenger messenger, int position) {
		for (final MapleMessengerCharacter messengerchar : messenger
				.getMembers()) {
			final MapleCharacter chr = this.getPlayerStorage()
					.getCharacterByName(messengerchar.getName());
			if (chr != null) {
				chr.getClient().announce(
						MaplePacketCreator.removeMessengerPlayer(position));
			}
		}
	}

	public void messengerChat(MapleMessenger messenger, String chattext,
			String namefrom) {
		for (final MapleMessengerCharacter messengerchar : messenger
				.getMembers()) {
			if (!(messengerchar.getName().equals(namefrom))) {
				final MapleCharacter chr = this.getPlayerStorage()
						.getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().announce(
							MaplePacketCreator.messengerChat(chattext));
				}
			}
		}
	}

	public void declineChat(String target, String namefrom) {
		if (this.isConnected(target)) {
			final MapleCharacter chr = this.getPlayerStorage()
					.getCharacterByName(target);
			if ((chr != null) && (chr.getMessenger() != null)) {
				chr.getClient().announce(
						MaplePacketCreator.messengerNote(namefrom, 5, 0));
			}
		}
	}

	public void updateMessenger(int messengerid, String namefrom,
			int fromchannel) {
		final MapleMessenger messenger = this.getMessenger(messengerid);
		final int position = messenger.getPositionByName(namefrom);
		this.updateMessenger(messenger, namefrom, position, fromchannel);
	}

	public void updateMessenger(MapleMessenger messenger, String namefrom,
			int position, int fromchannel) {
		for (final MapleMessengerCharacter messengerchar : messenger
				.getMembers()) {
			final Channel ch = this.getChannel(fromchannel);
			if (!(messengerchar.getName().equals(namefrom))) {
				final MapleCharacter chr = ch.getPlayerStorage()
						.getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().announce(
							MaplePacketCreator.updateMessengerPlayer(namefrom,
									this.getChannel(fromchannel)
											.getPlayerStorage()
											.getCharacterByName(namefrom),
									position, (byte) (fromchannel - 1)));
				}
			}
		}
	}

	public void silentLeaveMessenger(int messengerid,
			MapleMessengerCharacter target) {
		final MapleMessenger messenger = this.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException(
					"No messenger with the specified messengerid exists");
		}
		messenger.silentRemoveMember(target);
	}

	public void joinMessenger(int messengerid, MapleMessengerCharacter target,
			String from, int fromchannel) {
		final MapleMessenger messenger = this.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException(
					"No messenger with the specified messengerid exists");
		}
		messenger.addMember(target);
		this.addMessengerPlayer(messenger, from, fromchannel,
				target.getPosition());
	}

	public void silentJoinMessenger(int messengerid,
			MapleMessengerCharacter target, int position) {
		final MapleMessenger messenger = this.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException(
					"No messenger with the specified messengerid exists");
		}
		messenger.silentAddMember(target, position);
	}

	public MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
		final int messengerid = this.runningMessengerId.getAndIncrement();
		final MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
		this.messengers.put(messenger.getId(), messenger);
		return messenger;
	}

	public boolean isConnected(String charName) {
		return this.getPlayerStorage().getCharacterByName(charName) != null;
	}

	public void whisper(String sender, String target, int channel,
			String message) {
		if (this.isConnected(target)) {
			this.getPlayerStorage()
					.getCharacterByName(target)
					.getClient()
					.announce(
							MaplePacketCreator.getWhisper(sender, channel,
									message));
		}
	}

	public BuddyAddResult requestBuddyAdd(String addName, int channelFrom,
			int cidFrom, String nameFrom) {
		final MapleCharacter addChar = this.getPlayerStorage()
				.getCharacterByName(addName);
		if (addChar != null) {
			final BuddyList buddylist = addChar.getBuddylist();
			if (buddylist.isFull()) {
				return BuddyAddResult.BUDDYLIST_FULL;
			}
			if (!buddylist.contains(cidFrom)) {
				buddylist.addBuddyRequest(addChar.getClient(), cidFrom,
						nameFrom, channelFrom);
			} else if (buddylist.containsVisible(cidFrom)) {
				return BuddyAddResult.ALREADY_ON_LIST;
			}
		}
		return BuddyAddResult.OK;
	}

	public void buddyChanged(int cid, int cidFrom, String name, int channel,
			BuddyOperation operation) {
		final MapleCharacter addChar = this.getPlayerStorage()
				.getCharacterById(cid);
		if (addChar != null) {
			final BuddyList buddylist = addChar.getBuddylist();
			switch (operation) {
			case ADDED:
				if (buddylist.contains(cidFrom)) {
					buddylist.put(new BuddyListEntry(name, "Default Group",
							cidFrom, channel, true));
					addChar.getClient().announce(
							MaplePacketCreator.updateBuddyChannel(cidFrom,
									(byte) (channel - 1)));
				}
				break;
			case DELETED:
				if (buddylist.contains(cidFrom)) {
					buddylist.put(new BuddyListEntry(name, "Default Group",
							cidFrom, (byte) -1, buddylist.get(cidFrom)
									.isVisible()));
					addChar.getClient().announce(
							MaplePacketCreator.updateBuddyChannel(cidFrom,
									(byte) -1));
				}
				break;
			}
		}
	}

	public void loggedOff(String name, int characterId, int channel,
			int[] buddies) {
		this.updateBuddies(characterId, channel, buddies, true);
	}

	public void loggedOn(String name, int characterId, int channel,
			int buddies[]) {
		this.updateBuddies(characterId, channel, buddies, false);
	}

	private void updateBuddies(int characterId, int channel, int[] buddies,
			boolean offline) {
		final PlayerStorage playerStorage = this.getPlayerStorage();
		for (final int buddy : buddies) {
			final MapleCharacter chr = playerStorage.getCharacterById(buddy);
			if (chr != null) {
				final BuddyListEntry ble = chr.getBuddylist().get(characterId);
				if ((ble != null) && ble.isVisible()) {
					int mcChannel;
					if (offline) {
						ble.setChannel((byte) -1);
						mcChannel = -1;
					} else {
						ble.setChannel(channel);
						mcChannel = (byte) (channel - 1);
					}
					chr.getBuddylist().put(ble);
					chr.getClient().announce(
							MaplePacketCreator.updateBuddyChannel(
									ble.getCharacterId(), mcChannel));
				}
			}
		}
	}

	public void setServerMessage(String msg) {
		for (final Channel ch : this.channels) {
			ch.setServerMessage(msg);
		}
	}

	public void broadcastPacket(final byte[] data) {
		for (final MapleCharacter chr : this.players.getAllCharacters()) {
			chr.announce(data);
		}
	}

	public final void shutdown() {
		for (final Channel ch : this.getChannels()) {
			ch.shutdown();
		}
		this.players.disconnectAll();
	}
}
