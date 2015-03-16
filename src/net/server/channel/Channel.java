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
package net.server.channel;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import net.MapleServerHandler;
import net.mina.MapleCodecFactory;
import net.server.PlayerStorage;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import provider.MapleDataProviderFactory;
import scripting.event.EventScriptManager;
import server.TimerManager;
import server.events.gm.MapleEvent;
import server.expeditions.MapleExpedition;
import server.expeditions.MapleExpeditionType;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import constants.ServerConstants;

public final class Channel {

	private int port = 7575;
	private final PlayerStorage players = new PlayerStorage();
	private final int world, channel;
	private IoAcceptor acceptor;
	private String ip, serverMessage;
	private final MapleMapFactory mapFactory;
	private EventScriptManager eventSM;
	private final Map<Integer, HiredMerchant> hiredMerchants = new HashMap<>();
	private final ReentrantReadWriteLock merchant_lock = new ReentrantReadWriteLock(
			true);
	private final EnumMap<MapleExpeditionType, MapleExpedition> expeditions = new EnumMap<>(
			MapleExpeditionType.class);
	private MapleEvent event;
	private boolean finishedShutdown = false;

	public Channel(final int world, final int channel) {
		this.world = world;
		this.channel = channel;
		this.mapFactory = new MapleMapFactory(
				MapleDataProviderFactory.getDataProvider(new File(System
						.getProperty("wzpath") + "/Map.wz")),
				MapleDataProviderFactory.getDataProvider(new File(System
						.getProperty("wzpath") + "/String.wz")), world, channel);

		try {
			this.eventSM = new EventScriptManager(this,
					ServerConstants.EVENTS.split(" "));
			this.port = (7575 + this.channel) - 1;
			this.port += (world * 100);
			this.ip = ServerConstants.HOST + ":" + this.port;
			IoBuffer.setUseDirectBuffer(false);
			IoBuffer.setAllocator(new SimpleBufferAllocator());
			this.acceptor = new NioSocketAcceptor();
			TimerManager.getInstance().register(new respawnMaps(), 10000);
			this.acceptor.setHandler(new MapleServerHandler(world, channel));
			this.acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE,
					30);
			this.acceptor.getFilterChain().addLast("codec",
					new ProtocolCodecFilter(new MapleCodecFactory()));
			this.acceptor.bind(new InetSocketAddress(this.port));
			((SocketSessionConfig) this.acceptor.getSessionConfig())
					.setTcpNoDelay(true);

			this.eventSM.init();
			System.out.println("    Channel " + this.getId()
					+ ": Listening on port " + this.port + "...");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public final void shutdown() {
		try {
			System.out.println("Shutting down Channel " + this.channel
					+ " on World " + this.world);

			this.closeAllMerchants();
			this.players.disconnectAll();
			this.acceptor.unbind();

			this.finishedShutdown = true;
			System.out.println("Successfully shut down Channel " + this.channel
					+ " on World " + this.world + "\r\n");
		} catch (final Exception e) {
			System.err.println("Error while shutting down Channel "
					+ this.channel + " on World " + this.world + "\r\n" + e);
		}
	}

	public void closeAllMerchants() {
		final WriteLock wlock = this.merchant_lock.writeLock();
		wlock.lock();
		try {
			final Iterator<HiredMerchant> hmit = this.hiredMerchants.values()
					.iterator();
			while (hmit.hasNext()) {
				hmit.next().forceClose();
				hmit.remove();
			}
		} catch (final Exception e) {
		} finally {
			wlock.unlock();
		}
	}

	public MapleMapFactory getMapFactory() {
		return this.mapFactory;
	}

	public int getWorld() {
		return this.world;
	}

	public void addPlayer(MapleCharacter chr) {
		this.players.addPlayer(chr);
		chr.announce(MaplePacketCreator.serverMessage(this.serverMessage));
	}

	public PlayerStorage getPlayerStorage() {
		return this.players;
	}

	public void removePlayer(MapleCharacter chr) {
		this.players.removePlayer(chr.getId());
	}

	public int getConnectedClients() {
		return this.players.getAllCharacters().size();
	}

	public void broadcastPacket(final byte[] data) {
		for (final MapleCharacter chr : this.players.getAllCharacters()) {
			chr.announce(data);
		}
	}

	public final int getId() {
		return this.channel;
	}

	public String getIP() {
		return this.ip;
	}

	public MapleEvent getEvent() {
		return this.event;
	}

	public void setEvent(MapleEvent event) {
		this.event = event;
	}

	public EventScriptManager getEventSM() {
		return this.eventSM;
	}

	public void broadcastGMPacket(final byte[] data) {
		for (final MapleCharacter chr : this.players.getAllCharacters()) {
			if (chr.isGM()) {
				chr.announce(data);
			}
		}
	}

	public void broadcastGMPacket(final byte[] data, String exclude) {
		for (final MapleCharacter chr : this.players.getAllCharacters()) {
			if (chr.isGM() && !chr.getName().equals(exclude)) {
				chr.announce(data);
			}
		}
	}

	public void yellowWorldMessage(String msg) {
		for (final MapleCharacter mc : this.getPlayerStorage()
				.getAllCharacters()) {
			mc.announce(MaplePacketCreator.sendYellowTip(msg));
		}
	}

	public void worldMessage(String msg) {
		for (final MapleCharacter mc : this.getPlayerStorage()
				.getAllCharacters()) {
			mc.dropMessage(msg);
		}
	}

	public List<MapleCharacter> getPartyMembers(MapleParty party) {
		final List<MapleCharacter> partym = new ArrayList<>(8);
		for (final MaplePartyCharacter partychar : party.getMembers()) {
			if (partychar.getChannel() == this.getId()) {
				final MapleCharacter chr = this.getPlayerStorage()
						.getCharacterByName(partychar.getName());
				if (chr != null) {
					partym.add(chr);
				}
			}
		}
		return partym;

	}

	public class respawnMaps implements Runnable {

		@Override
		public void run() {
			for (final Entry<Integer, MapleMap> map : Channel.this.mapFactory
					.getMaps().entrySet()) {
				map.getValue().respawn();
			}
		}
	}

	public Map<Integer, HiredMerchant> getHiredMerchants() {
		return this.hiredMerchants;
	}

	public void addHiredMerchant(int chrid, HiredMerchant hm) {
		final WriteLock wlock = this.merchant_lock.writeLock();
		wlock.lock();
		try {
			this.hiredMerchants.put(chrid, hm);
		} finally {
			wlock.unlock();
		}
	}

	public void removeHiredMerchant(int chrid) {
		final WriteLock wlock = this.merchant_lock.writeLock();
		wlock.lock();
		try {
			this.hiredMerchants.remove(chrid);
		} finally {
			wlock.unlock();
		}
	}

	public int[] multiBuddyFind(int charIdFrom, int[] characterIds) {
		final List<Integer> ret = new ArrayList<>(characterIds.length);
		final PlayerStorage playerStorage = this.getPlayerStorage();
		for (final int characterId : characterIds) {
			final MapleCharacter chr = playerStorage
					.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddylist().containsVisible(charIdFrom)) {
					ret.add(characterId);
				}
			}
		}
		final int[] retArr = new int[ret.size()];
		int pos = 0;
		for (final Integer i : ret) {
			retArr[pos++] = i.intValue();
		}
		return retArr;
	}

	public boolean hasExpedition(MapleExpeditionType type) {
		return this.expeditions.containsKey(type);
	}

	public void addExpedition(MapleExpeditionType type, MapleExpedition exped) {
		this.expeditions.put(type, exped);
	}

	public MapleExpedition getExpedition(MapleExpeditionType type) {
		return this.expeditions.get(type);
	}

	public boolean isConnected(String name) {
		return this.getPlayerStorage().getCharacterByName(name) != null;
	}

	public boolean finishedShutdown() {
		return this.finishedShutdown;
	}

	public void setServerMessage(String message) {
		this.serverMessage = message;
		this.broadcastPacket(MaplePacketCreator.serverMessage(message));
	}
}