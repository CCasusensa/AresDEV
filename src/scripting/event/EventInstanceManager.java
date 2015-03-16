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
package scripting.event;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptException;

import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import provider.MapleDataProviderFactory;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;
import client.MapleCharacter;

/**
 *
 * @author Matze
 */
public class EventInstanceManager {
	private final List<MapleCharacter> chars = new ArrayList<>();
	private final List<MapleMonster> mobs = new LinkedList<>();
	private final Map<MapleCharacter, Integer> killCount = new HashMap<>();
	private EventManager em;
	private MapleMapFactory mapFactory;
	private final String name;
	private final Properties props = new Properties();
	private long timeStarted = 0;
	private long eventTime = 0;

	public EventInstanceManager(EventManager em, String name) {
		this.em = em;
		this.name = name;
		this.mapFactory = new MapleMapFactory(
				MapleDataProviderFactory.getDataProvider(new File(System
						.getProperty("wzpath") + "/Map.wz")),
				MapleDataProviderFactory.getDataProvider(new File(System
						.getProperty("wzpath") + "/String.wz")), (byte) 0,
				(byte) 1);// Fk this
		this.mapFactory.setChannel(em.getChannelServer().getId());
	}

	public EventManager getEm() {
		return this.em;
	}

	public void registerPlayer(MapleCharacter chr) {
		try {
			this.chars.add(chr);
			chr.setEventInstance(this);
			this.em.getIv().invokeFunction("playerEntry", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void startEventTimer(long time) {
		this.timeStarted = System.currentTimeMillis();
		this.eventTime = time;
	}

	public boolean isTimerStarted() {
		return (this.eventTime > 0) && (this.timeStarted > 0);
	}

	public long getTimeLeft() {
		return this.eventTime - (System.currentTimeMillis() - this.timeStarted);
	}

	public void registerParty(MapleParty party, MapleMap map) {
		for (final MaplePartyCharacter pc : party.getMembers()) {
			final MapleCharacter c = map.getCharacterById(pc.getId());
			this.registerPlayer(c);
		}
	}

	public void unregisterPlayer(MapleCharacter chr) {
		this.chars.remove(chr);
		chr.setEventInstance(null);
	}

	public int getPlayerCount() {
		return this.chars.size();
	}

	public List<MapleCharacter> getPlayers() {
		return new ArrayList<>(this.chars);
	}

	public void registerMonster(MapleMonster mob) {
		this.mobs.add(mob);
		mob.setEventInstance(this);
	}

	public void unregisterMonster(MapleMonster mob) {
		this.mobs.remove(mob);
		mob.setEventInstance(null);
		if (this.mobs.isEmpty()) {
			try {
				this.em.getIv().invokeFunction("allMonstersDead", this);
			} catch (ScriptException | NoSuchMethodException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void playerKilled(MapleCharacter chr) {
		try {
			this.em.getIv().invokeFunction("playerDead", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public boolean revivePlayer(MapleCharacter chr) {
		try {
			final Object b = this.em.getIv().invokeFunction("playerRevive",
					this, chr);
			if (b instanceof Boolean) {
				return (Boolean) b;
			}
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
		return true;
	}

	public void playerDisconnected(MapleCharacter chr) {
		try {
			this.em.getIv().invokeFunction("playerDisconnected", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 *
	 * @param chr
	 * @param mob
	 */
	public void monsterKilled(MapleCharacter chr, MapleMonster mob) {
		try {
			Integer kc = this.killCount.get(chr);
			final int inc = ((Double) this.em.getIv().invokeFunction(
					"monsterValue", this, mob.getId())).intValue();
			if (kc == null) {
				kc = inc;
			} else {
				kc += inc;
			}
			this.killCount.put(chr, kc);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public int getKillCount(MapleCharacter chr) {
		final Integer kc = this.killCount.get(chr);
		if (kc == null) {
			return 0;
		} else {
			return kc;
		}
	}

	public void dispose() {
		this.chars.clear();
		this.mobs.clear();
		this.killCount.clear();
		this.mapFactory = null;
		this.em.disposeInstance(this.name);
		this.em = null;
	}

	public MapleMapFactory getMapFactory() {
		return this.mapFactory;
	}

	public void schedule(final String methodName, long delay) {
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				try {
					EventInstanceManager.this.em.getIv().invokeFunction(
							methodName, EventInstanceManager.this);
				} catch (ScriptException | NoSuchMethodException ex) {
					ex.printStackTrace();
				}
			}
		}, delay);
	}

	public String getName() {
		return this.name;
	}

	public void saveWinner(MapleCharacter chr) {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)")) {
				ps.setString(1, this.em.getName());
				ps.setString(2, this.getName());
				ps.setInt(3, chr.getId());
				ps.setInt(4, chr.getClient().getChannel());
				ps.executeUpdate();
			}
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	public MapleMap getMapInstance(int mapId) {
		final MapleMap map = this.mapFactory.getMap(mapId);
		if (!this.mapFactory.isMapLoaded(mapId)) {
			if ((this.em.getProperty("shuffleReactors") != null)
					&& this.em.getProperty("shuffleReactors").equals("true")) {
				map.shuffleReactors();
			}
		}
		return map;
	}

	public void setProperty(String key, String value) {
		this.props.setProperty(key, value);
	}

	public Object setProperty(String key, String value, boolean prev) {
		return this.props.setProperty(key, value);
	}

	public String getProperty(String key) {
		return this.props.getProperty(key);
	}

	public void leftParty(MapleCharacter chr) {
		try {
			this.em.getIv().invokeFunction("leftParty", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void disbandParty() {
		try {
			this.em.getIv().invokeFunction("disbandParty", this);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void finishPQ() {
		try {
			this.em.getIv().invokeFunction("clearPQ", this);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void removePlayer(MapleCharacter chr) {
		try {
			this.em.getIv().invokeFunction("playerExit", this, chr);
		} catch (ScriptException | NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public boolean isLeader(MapleCharacter chr) {
		return (chr.getParty().getLeader().getId() == chr.getId());
	}
}
