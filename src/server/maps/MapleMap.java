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
package server.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import net.server.Server;
import net.server.channel.Channel;
import scripting.map.MapScriptManager;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.TimerManager;
import server.events.gm.MapleCoconut;
import server.events.gm.MapleFitness;
import server.events.gm.MapleOla;
import server.events.gm.MapleOxQuiz;
import server.events.gm.MapleSnowball;
import server.life.MapleLifeFactory;
import server.life.MapleLifeFactory.selfDestruction;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.life.SpawnPoint;
import server.partyquest.MonsterCarnival;
import server.partyquest.MonsterCarnivalParty;
import server.partyquest.Pyramid;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ItemConstants;

public class MapleMap {

	private static final List<MapleMapObjectType> rangedMapobjectTypes = Arrays
			.asList(MapleMapObjectType.SHOP, MapleMapObjectType.ITEM,
					MapleMapObjectType.NPC, MapleMapObjectType.MONSTER,
					MapleMapObjectType.DOOR, MapleMapObjectType.SUMMON,
					MapleMapObjectType.REACTOR);
	private final Map<Integer, MapleMapObject> mapobjects = new LinkedHashMap<>();
	private final Collection<SpawnPoint> monsterSpawn = Collections
			.synchronizedList(new LinkedList<SpawnPoint>());
	private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
	private final Collection<MapleCharacter> characters = new LinkedHashSet<>();
	private final Map<Integer, MaplePortal> portals = new HashMap<>();
	private final List<Rectangle> areas = new ArrayList<>();
	private MapleFootholdTree footholds = null;
	private final int mapid;
	private int runningOid = 100;
	private final int returnMapId;
	private final int channel, world;
	private byte monsterRate;
	private boolean clock;
	private boolean boat;
	private boolean docked;
	private String mapName;
	private String streetName;
	private MapleMapEffect mapEffect = null;
	private boolean everlast = false;
	private int forcedReturnMap = 999999999;
	private long timeLimit;
	private int decHP = 0;
	private int protectItem = 0;
	private boolean town;
	private MapleOxQuiz ox;
	private boolean isOxQuiz = false;
	private boolean dropsOn = true;
	private String onFirstUserEnter;
	private String onUserEnter;
	private int fieldType;
	private int fieldLimit = 0;
	private int mobCapacity = -1;
	private ScheduledFuture<?> mapMonitor = null;
	private Pair<Integer, String> timeMob = null;
	private short mobInterval = 5000;
	// HPQ
	private int riceCakeNum = 0; // bad place to put this (why is it in here
									// then)
	private boolean allowHPQSummon = false; // bad place to put this
	// events
	private boolean eventstarted = false;
	private MapleSnowball snowball0 = null;
	private MapleSnowball snowball1 = null;
	private MapleCoconut coconut;
	// locks
	private final ReadLock chrRLock;
	private final WriteLock chrWLock;
	private final ReadLock objectRLock;
	private final WriteLock objectWLock;

	public MapleMap(int mapid, int world, int channel, int returnMapId,
			float monsterRate) {
		this.mapid = mapid;
		this.channel = channel;
		this.world = world;
		this.returnMapId = returnMapId;
		this.monsterRate = (byte) Math.round(monsterRate);
		if (this.monsterRate == 0) {
			this.monsterRate = 1;
		}
		final ReentrantReadWriteLock chrLock = new ReentrantReadWriteLock(true);
		this.chrRLock = chrLock.readLock();
		this.chrWLock = chrLock.writeLock();

		final ReentrantReadWriteLock objectLock = new ReentrantReadWriteLock(
				true);
		this.objectRLock = objectLock.readLock();
		this.objectWLock = objectLock.writeLock();
	}

	public void broadcastMessage(MapleCharacter source, final byte[] packet) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter chr : this.characters) {
				if (chr != source) {
					chr.getClient().announce(packet);
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
	}

	public void broadcastGMMessage(MapleCharacter source, final byte[] packet) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter chr : this.characters) {
				if ((chr != source) && (chr.gmLevel() > source.gmLevel())) {
					chr.getClient().announce(packet);
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
	}

	public void toggleDrops() {
		this.dropsOn = !this.dropsOn;
	}

	public List<MapleMapObject> getMapObjectsInRect(Rectangle box,
			List<MapleMapObjectType> types) {
		this.objectRLock.lock();
		final List<MapleMapObject> ret = new LinkedList<>();
		try {
			for (final MapleMapObject l : this.mapobjects.values()) {
				if (types.contains(l.getType())) {
					if (box.contains(l.getPosition())) {
						ret.add(l);
					}
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
		return ret;
	}

	public int getId() {
		return this.mapid;
	}

	public MapleMap getReturnMap() {
		return Server.getInstance().getWorld(this.world)
				.getChannel(this.channel).getMapFactory()
				.getMap(this.returnMapId);
	}

	public int getReturnMapId() {
		return this.returnMapId;
	}

	public void setReactorState() {
		this.objectRLock.lock();
		try {
			for (final MapleMapObject o : this.mapobjects.values()) {
				if (o.getType() == MapleMapObjectType.REACTOR) {
					if (((MapleReactor) o).getState() < 1) {
						((MapleReactor) o).setState((byte) 1);
						this.broadcastMessage(MaplePacketCreator
								.triggerReactor((MapleReactor) o, 1));
					}
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
	}

	public int getForcedReturnId() {
		return this.forcedReturnMap;
	}

	public MapleMap getForcedReturnMap() {
		return Server.getInstance().getWorld(this.world)
				.getChannel(this.channel).getMapFactory()
				.getMap(this.forcedReturnMap);
	}

	public void setForcedReturnMap(int map) {
		this.forcedReturnMap = map;
	}

	public long getTimeLimit() {
		return this.timeLimit;
	}

	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	public int getTimeLeft() {
		return (int) ((this.timeLimit - System.currentTimeMillis()) / 1000);
	}

	public int getCurrentPartyId() {
		for (final MapleCharacter chr : this.getCharacters()) {
			if (chr.getPartyId() != -1) {
				return chr.getPartyId();
			}
		}
		return -1;
	}

	public void addMapObject(MapleMapObject mapobject) {
		this.objectWLock.lock();
		try {
			mapobject.setObjectId(this.runningOid);
			this.mapobjects.put(Integer.valueOf(this.runningOid), mapobject);
			this.incrementRunningOid();
		} finally {
			this.objectWLock.unlock();
		}
	}

	private void spawnAndAddRangedMapObject(MapleMapObject mapobject,
			DelayedPacketCreation packetbakery) {
		this.spawnAndAddRangedMapObject(mapobject, packetbakery, null);
	}

	private void spawnAndAddRangedMapObject(MapleMapObject mapobject,
			DelayedPacketCreation packetbakery, SpawnCondition condition) {
		this.chrRLock.lock();
		try {
			mapobject.setObjectId(this.runningOid);
			for (final MapleCharacter chr : this.characters) {
				if ((condition == null) || condition.canSpawn(chr)) {
					if (chr.getPosition().distanceSq(mapobject.getPosition()) <= 722500) {
						packetbakery.sendPackets(chr.getClient());
						chr.addVisibleMapObject(mapobject);
					}
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
		this.objectWLock.lock();
		try {
			this.mapobjects.put(Integer.valueOf(this.runningOid), mapobject);
		} finally {
			this.objectWLock.unlock();
		}
		this.incrementRunningOid();
	}

	private void incrementRunningOid() {
		this.runningOid++;
		if (this.runningOid >= 30000) {
			this.runningOid = 1000;// Lol, like there are monsters with the same
									// oid
									// NO
		}
		this.objectRLock.lock();
		try {
			if (!this.mapobjects.containsKey(Integer.valueOf(this.runningOid))) {
				return;
			}
		} finally {
			this.objectRLock.unlock();
		}
		throw new RuntimeException("Out of OIDs on map " + this.mapid
				+ " (channel: " + this.channel + ")");
	}

	public void removeMapObject(int num) {
		this.objectWLock.lock();
		try {
			this.mapobjects.remove(Integer.valueOf(num));
		} finally {
			this.objectWLock.unlock();
		}
	}

	public void removeMapObject(final MapleMapObject obj) {
		this.removeMapObject(obj.getObjectId());
	}

	private Point calcPointBelow(Point initial) {
		final MapleFoothold fh = this.footholds.findBelow(initial);
		if (fh == null) {
			return null;
		}
		int dropY = fh.getY1();
		if (!fh.isWall() && (fh.getY1() != fh.getY2())) {
			final double s1 = Math.abs(fh.getY2() - fh.getY1());
			final double s2 = Math.abs(fh.getX2() - fh.getX1());
			final double s5 = Math.cos(Math.atan(s2 / s1))
					* (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1
							/ s2)));
			if (fh.getY2() < fh.getY1()) {
				dropY = fh.getY1() - (int) s5;
			} else {
				dropY = fh.getY1() + (int) s5;
			}
		}
		return new Point(initial.x, dropY);
	}

	public Point calcDropPos(Point initial, Point fallback) {
		final Point ret = this.calcPointBelow(new Point(initial.x,
				initial.y - 50));
		if (ret == null) {
			return fallback;
		}
		return ret;
	}

	private void dropFromMonster(final MapleCharacter chr,
			final MapleMonster mob) {
		if (mob.dropsDisabled() || !this.dropsOn) {
			return;
		}
		final MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3
				: mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1
						: 0);
		final int mobpos = mob.getPosition().x;
		int chServerrate = chr.getDropRate();
		Item idrop;
		byte d = 1;
		final Point pos = new Point(0, mob.getPosition().y);

		final Map<MonsterStatus, MonsterStatusEffect> stati = mob.getStati();
		if (stati.containsKey(MonsterStatus.SHOWDOWN)) {
			chServerrate *= ((stati.get(MonsterStatus.SHOWDOWN).getStati()
					.get(MonsterStatus.SHOWDOWN).doubleValue() / 100.0) + 1.0);
		}

		final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider
				.getInstance();
		final List<MonsterDropEntry> dropEntry = new ArrayList<>(
				mi.retrieveDrop(mob.getId()));

		Collections.shuffle(dropEntry);
		for (final MonsterDropEntry de : dropEntry) {
			if (Randomizer.nextInt(999999) < (de.chance * chServerrate)) {
				if (droptype == 3) {
					pos.x = mobpos
							+ ((d % 2) == 0 ? ((40 * (d + 1)) / 2)
									: -(40 * (d / 2)));
				} else {
					pos.x = mobpos
							+ (((d % 2) == 0) ? ((25 * (d + 1)) / 2)
									: -(25 * (d / 2)));
				}
				if (de.itemId == 0) { // meso
					int mesos = Randomizer.nextInt(de.Maximum - de.Minimum)
							+ de.Minimum;

					if (mesos > 0) {
						if (chr.getBuffedValue(MapleBuffStat.MESOUP) != null) {
							mesos = (int) ((mesos * chr.getBuffedValue(
									MapleBuffStat.MESOUP).doubleValue()) / 100.0);
						}
						this.spawnMesoDrop(mesos * chr.getMesoRate(),
								this.calcDropPos(pos, mob.getPosition()), mob,
								chr, false, droptype);
					}
				} else {
					if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii
								.getEquipById(de.itemId));
					} else {
						idrop = new Item(de.itemId, (byte) 0,
								(short) (de.Maximum != 1 ? Randomizer
										.nextInt(de.Maximum - de.Minimum)
										+ de.Minimum : 1));
					}
					this.spawnDrop(idrop,
							this.calcDropPos(pos, mob.getPosition()), mob, chr,
							droptype, de.questid);
				}
				d++;
			}
		}
		final List<MonsterGlobalDropEntry> globalEntry = mi.getGlobalDrop();
		// Global Drops
		for (final MonsterGlobalDropEntry de : globalEntry) {
			if (Randomizer.nextInt(999999) < de.chance) {
				if (droptype == 3) {
					pos.x = mobpos
							+ ((d % 2) == 0 ? ((40 * (d + 1)) / 2)
									: -(40 * (d / 2)));
				} else {
					pos.x = mobpos
							+ (((d % 2) == 0) ? ((25 * (d + 1)) / 2)
									: -(25 * (d / 2)));
				}
				if (de.itemId == 0) {
					// chr.getCashShop().gainCash(1, 80);
				} else {
					if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii
								.getEquipById(de.itemId));
					} else {
						idrop = new Item(de.itemId, (byte) 0,
								(short) (de.Maximum != 1 ? Randomizer
										.nextInt(de.Maximum - de.Minimum)
										+ de.Minimum : 1));
					}
					this.spawnDrop(idrop,
							this.calcDropPos(pos, mob.getPosition()), mob, chr,
							droptype, de.questid);
					d++;
				}
			}
		}
	}

	private void spawnDrop(final Item idrop, final Point dropPos,
			final MapleMonster mob, final MapleCharacter chr,
			final byte droptype, final short questid) {
		final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr,
				droptype, false, questid);
		this.spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				if ((questid <= 0)
						|| ((c.getPlayer().getQuestStatus(questid) == 1) && c
								.getPlayer().needQuestItem(questid,
										idrop.getItemId()))) {
					c.announce(MaplePacketCreator.dropItemFromMapObject(mdrop,
							mob.getPosition(), dropPos, (byte) 1));
				}
			}
		}, null);

		TimerManager.getInstance()
				.schedule(new ExpireMapItemJob(mdrop), 180000);
		this.activateItemReactors(mdrop, chr.getClient());
	}

	public final void spawnMesoDrop(final int meso, final Point position,
			final MapleMapObject dropper, final MapleCharacter owner,
			final boolean playerDrop, final byte droptype) {
		final Point droppos = this.calcDropPos(position, position);
		final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper,
				owner, droptype, playerDrop);

		this.spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.dropItemFromMapObject(mdrop,
						dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);

		TimerManager.getInstance()
				.schedule(new ExpireMapItemJob(mdrop), 180000);
	}

	public final void disappearingItemDrop(final MapleMapObject dropper,
			final MapleCharacter owner, final Item item, final Point pos) {
		final Point droppos = this.calcDropPos(pos, pos);
		final MapleMapItem drop = new MapleMapItem(item, droppos, dropper,
				owner, (byte) 1, false);
		this.broadcastMessage(
				MaplePacketCreator.dropItemFromMapObject(drop,
						dropper.getPosition(), droppos, (byte) 3),
				drop.getPosition());
	}

	public MapleMonster getMonsterById(int id) {
		this.objectRLock.lock();
		try {
			for (final MapleMapObject obj : this.mapobjects.values()) {
				if (obj.getType() == MapleMapObjectType.MONSTER) {
					if (((MapleMonster) obj).getId() == id) {
						return (MapleMonster) obj;
					}
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
		return null;
	}

	public int countMonster(int id) {
		int count = 0;
		for (final MapleMapObject m : this.getMapObjectsInRange(
				new Point(0, 0), Double.POSITIVE_INFINITY,
				Arrays.asList(MapleMapObjectType.MONSTER))) {
			final MapleMonster mob = (MapleMonster) m;
			if (mob.getId() == id) {
				count++;
			}
		}
		return count;
	}

	public boolean damageMonster(final MapleCharacter chr,
			final MapleMonster monster, final int damage) {
		if (monster.getId() == 8800000) {
			for (final MapleMapObject object : chr.getMap().getMapObjects()) {
				final MapleMonster mons = chr.getMap().getMonsterByOid(
						object.getObjectId());
				if (mons != null) {
					if ((mons.getId() >= 8800003) && (mons.getId() <= 8800010)) {
						return true;
					}
				}
			}
		}
		if (monster.isAlive()) {
			boolean killed = false;
			monster.monsterLock.lock();
			try {
				if (!monster.isAlive()) {
					return false;
				}
				final Pair<Integer, Integer> cool = monster.getStats()
						.getCool();
				if (cool != null) {
					final Pyramid pq = (Pyramid) chr.getPartyQuest();
					if (pq != null) {
						if (damage > 0) {
							if (damage >= cool.getLeft()) {
								if ((Math.random() * 100) < cool.getRight()) {
									pq.cool();
								} else {
									pq.kill();
								}
							} else {
								pq.kill();
							}
						} else {
							pq.miss();
						}
						killed = true;
					}
				}
				if (damage > 0) {
					monster.damage(chr, damage, true);
					if (!monster.isAlive()) { // monster just died
						// killMonster(monster, chr, true);
						killed = true;
					}
				} else if ((monster.getId() >= 8810002)
						&& (monster.getId() <= 8810009)) {
					for (final MapleMapObject object : chr.getMap()
							.getMapObjects()) {
						final MapleMonster mons = chr.getMap().getMonsterByOid(
								object.getObjectId());
						if (mons != null) {
							if (mons.getId() == 8810018) {
								this.damageMonster(chr, mons, damage);
							}
						}
					}
				}
			} finally {
				monster.monsterLock.unlock();
			}
			if ((monster.getStats().selfDestruction() != null)
					&& (monster.getStats().selfDestruction().getHp() > -1)) {// should
																				// work
																				// ;p
				if (monster.getHp() <= monster.getStats().selfDestruction()
						.getHp()) {
					this.killMonster(monster, chr, true, false, monster
							.getStats().selfDestruction().getAction());
					return true;
				}
			}
			if (killed && (monster != null)) {
				this.killMonster(monster, chr, true);
			}
			return true;
		}
		return false;
	}

	public void killMonster(final MapleMonster monster,
			final MapleCharacter chr, final boolean withDrops) {
		this.killMonster(monster, chr, withDrops, false, 1);
	}

	public void killMonster(final MapleMonster monster,
			final MapleCharacter chr, final boolean withDrops,
			final boolean secondTime, int animation) {
		if ((monster.getId() == 8810018) && !secondTime) {
			TimerManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					MapleMap.this.killMonster(monster, chr, withDrops, true, 1);
					MapleMap.this.killAllMonsters();
				}
			}, 3000);
			return;
		}
		if (chr == null) {
			this.spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			this.broadcastMessage(MaplePacketCreator.killMonster(
					monster.getObjectId(), animation), monster.getPosition());
			this.removeMapObject(monster);
			return;
		}
		/*
		 * if (chr.getQuest(MapleQuest.getInstance(29400)).getStatus().equals(
		 * MapleQuestStatus.Status.STARTED)) { if (chr.getLevel() >= 120 &&
		 * monster.getStats().getLevel() >= 120) { //FIX MEDAL SHET } else if
		 * (monster.getStats().getLevel() >= chr.getLevel()) { } }
		 */
		final int buff = monster.getBuffToGive();
		if (buff > -1) {
			final MapleItemInformationProvider mii = MapleItemInformationProvider
					.getInstance();
			for (final MapleMapObject mmo : this.getAllPlayer()) {
				final MapleCharacter character = (MapleCharacter) mmo;
				if (character.isAlive()) {
					final MapleStatEffect statEffect = mii.getItemEffect(buff);
					character.getClient().announce(
							MaplePacketCreator.showOwnBuffEffect(buff, 1));
					this.broadcastMessage(character, MaplePacketCreator
							.showBuffeffect(character.getId(), buff, 1), false);
					statEffect.applyTo(character);
				}
			}
		}
		if (monster.getId() == 8810018) {
			for (final Channel cserv : Server.getInstance()
					.getWorld(this.world).getChannels()) {
				for (final MapleCharacter player : cserv.getPlayerStorage()
						.getAllCharacters()) {
					if (player.getMapId() == 240000000) {
						player.addMessage("Mysterious power arose as I heard the powerful cry of the Nine Spirit Baby Dragon.");
					} else {
						player.dropMessage("To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!");
						if (player.isGM()) {
							player.addMessage("[GM-Message] Horntail was killed by : "
									+ chr.getName());
						}
					}
				}
			}
		}
		this.spawnedMonstersOnMap.decrementAndGet();
		monster.setHp(0);
		this.broadcastMessage(MaplePacketCreator.killMonster(
				monster.getObjectId(), animation), monster.getPosition());
		if (monster.getStats().selfDestruction() == null) {// FUU BOMBS D:
			this.removeMapObject(monster);
		}
		if ((monster.getCP() > 0) && (chr.getCarnival() != null)) {
			chr.getCarnivalParty().addCP(chr, monster.getCP());
			chr.announce(MaplePacketCreator.updateCP(chr.getCP(),
					chr.getObtainedCP()));
			this.broadcastMessage(MaplePacketCreator.updatePartyCP(chr
					.getCarnivalParty()));
			// they drop items too ):
		}
		if ((monster.getId() >= 8800003) && (monster.getId() <= 8800010)) {
			boolean makeZakReal = true;
			final Collection<MapleMapObject> objects = this.getMapObjects();
			for (final MapleMapObject object : objects) {
				final MapleMonster mons = this.getMonsterByOid(object
						.getObjectId());
				if (mons != null) {
					if ((mons.getId() >= 8800003) && (mons.getId() <= 8800010)) {
						makeZakReal = false;
						break;
					}
				}
			}
			if (makeZakReal) {
				for (final MapleMapObject object : objects) {
					final MapleMonster mons = chr.getMap().getMonsterByOid(
							object.getObjectId());
					if (mons != null) {
						if (mons.getId() == 8800000) {
							this.makeMonsterReal(mons);
							this.updateMonsterController(mons);
							break;
						}
					}
				}
			}
		}
		MapleCharacter dropOwner = monster.killBy(chr);
		if (withDrops && !monster.dropsDisabled()) {
			if (dropOwner == null) {
				dropOwner = chr;
			}
			this.dropFromMonster(dropOwner, monster);
		}
	}

	public void killMonster(int monsId) {
		for (final MapleMapObject mmo : this.getMapObjects()) {
			if (mmo instanceof MapleMonster) {
				if (((MapleMonster) mmo).getId() == monsId) {
					this.killMonster((MapleMonster) mmo, (MapleCharacter) this
							.getAllPlayer().get(0), false);
				}
			}
		}
	}

	public void killAllMonsters() {
		for (final MapleMapObject monstermo : this.getMapObjectsInRange(
				new Point(0, 0), Double.POSITIVE_INFINITY,
				Arrays.asList(MapleMapObjectType.MONSTER))) {
			final MapleMonster monster = (MapleMonster) monstermo;
			this.spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			this.broadcastMessage(
					MaplePacketCreator.killMonster(monster.getObjectId(), true),
					monster.getPosition());
			this.removeMapObject(monster);
		}
	}

	public List<MapleMapObject> getAllPlayer() {
		return this.getMapObjectsInRange(new Point(0, 0),
				Double.POSITIVE_INFINITY,
				Arrays.asList(MapleMapObjectType.PLAYER));
	}

	public void destroyReactor(int oid) {
		final MapleReactor reactor = this.getReactorByOid(oid);
		final TimerManager tMan = TimerManager.getInstance();
		this.broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
		reactor.setAlive(false);
		this.removeMapObject(reactor);
		reactor.setTimerActive(false);
		if (reactor.getDelay() > 0) {
			tMan.schedule(new Runnable() {
				@Override
				public void run() {
					MapleMap.this.respawnReactor(reactor);
				}
			}, reactor.getDelay());
		}
	}

	public void resetReactors() {
		this.objectRLock.lock();
		try {
			for (final MapleMapObject o : this.mapobjects.values()) {
				if (o.getType() == MapleMapObjectType.REACTOR) {
					final MapleReactor r = ((MapleReactor) o);
					r.setState((byte) 0);
					r.setTimerActive(false);
					this.broadcastMessage(MaplePacketCreator.triggerReactor(r,
							0));
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
	}

	public void shuffleReactors() {
		final List<Point> points = new ArrayList<>();
		this.objectRLock.lock();
		try {
			for (final MapleMapObject o : this.mapobjects.values()) {
				if (o.getType() == MapleMapObjectType.REACTOR) {
					points.add(((MapleReactor) o).getPosition());
				}
			}
			Collections.shuffle(points);
			for (final MapleMapObject o : this.mapobjects.values()) {
				if (o.getType() == MapleMapObjectType.REACTOR) {
					((MapleReactor) o)
							.setPosition(points.remove(points.size() - 1));
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
	}

	public MapleReactor getReactorById(int Id) {
		this.objectRLock.lock();
		try {
			for (final MapleMapObject obj : this.mapobjects.values()) {
				if (obj.getType() == MapleMapObjectType.REACTOR) {
					if (((MapleReactor) obj).getId() == Id) {
						return (MapleReactor) obj;
					}
				}
			}
			return null;
		} finally {
			this.objectRLock.unlock();
		}
	}

	/**
	 * Automagically finds a new controller for the given monster from the chars
	 * on the map...
	 *
	 * @param monster
	 */
	public void updateMonsterController(MapleMonster monster) {
		monster.monsterLock.lock();
		try {
			if (!monster.isAlive()) {
				return;
			}
			if (monster.getController() != null) {
				if (monster.getController().getMap() != this) {
					monster.getController().stopControllingMonster(monster);
				} else {
					return;
				}
			}
			int mincontrolled = -1;
			MapleCharacter newController = null;
			this.chrRLock.lock();
			try {
				for (final MapleCharacter chr : this.characters) {
					if (!chr.isHidden()
							&& ((chr.getControlledMonsters().size() < mincontrolled) || (mincontrolled == -1))) {
						mincontrolled = chr.getControlledMonsters().size();
						newController = chr;
					}
				}
			} finally {
				this.chrRLock.unlock();
			}
			if (newController != null) {// was a new controller found? (if not
										// no one is on the map)
				if (monster.isFirstAttack()) {
					newController.controlMonster(monster, true);
					monster.setControllerHasAggro(true);
					monster.setControllerKnowsAboutAggro(true);
				} else {
					newController.controlMonster(monster, false);
				}
			}
		} finally {
			monster.monsterLock.unlock();
		}
	}

	public Collection<MapleMapObject> getMapObjects() {
		return Collections.unmodifiableCollection(this.mapobjects.values());
	}

	public boolean containsNPC(int npcid) {
		if (npcid == 9000066) {
			return true;
		}
		this.objectRLock.lock();
		try {
			for (final MapleMapObject obj : this.mapobjects.values()) {
				if (obj.getType() == MapleMapObjectType.NPC) {
					if (((MapleNPC) obj).getId() == npcid) {
						return true;
					}
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
		return false;
	}

	public MapleMapObject getMapObject(int oid) {
		return this.mapobjects.get(oid);
	}

	/**
	 * returns a monster with the given oid, if no such monster exists returns
	 * null
	 *
	 * @param oid
	 * @return
	 */
	public MapleMonster getMonsterByOid(int oid) {
		final MapleMapObject mmo = this.getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		if (mmo.getType() == MapleMapObjectType.MONSTER) {
			return (MapleMonster) mmo;
		}
		return null;
	}

	public MapleReactor getReactorByOid(int oid) {
		final MapleMapObject mmo = this.getMapObject(oid);
		if (mmo == null) {
			return null;
		}
		return mmo.getType() == MapleMapObjectType.REACTOR ? (MapleReactor) mmo
				: null;
	}

	public MapleReactor getReactorByName(String name) {
		this.objectRLock.lock();
		try {
			for (final MapleMapObject obj : this.mapobjects.values()) {
				if (obj.getType() == MapleMapObjectType.REACTOR) {
					if (((MapleReactor) obj).getName().equals(name)) {
						return (MapleReactor) obj;
					}
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
		return null;
	}

	public void spawnMonsterOnGroudBelow(MapleMonster mob, Point pos) {
		this.spawnMonsterOnGroundBelow(mob, pos);
	}

	public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = this.calcPointBelow(spos);
		spos.y--;
		mob.setPosition(spos);
		this.spawnMonster(mob);
	}

	public void spawnCPQMonster(MapleMonster mob, Point pos, int team) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = this.calcPointBelow(spos);
		spos.y--;
		mob.setPosition(spos);
		mob.setTeam(team);
		this.spawnMonster(mob);
	}

	private void monsterItemDrop(final MapleMonster m, final Item item,
			long delay) {
		final ScheduledFuture<?> monsterItemDrop = TimerManager.getInstance()
				.register(new Runnable() {
					@Override
					public void run() {
						if (MapleMap.this.getMonsterById(m.getId()) != null) {
							if (item.getItemId() == 4001101) {
								MapleMap.this
										.broadcastMessage(MaplePacketCreator
												.serverNotice(
														6,
														"The Moon Bunny made rice cake number "
																+ (MapleMap.this.riceCakeNum + 1)));
							}
							MapleMap.this.spawnItemDrop(m, null, item,
									m.getPosition(), true, true);
						}
					}
				}, delay, delay);
		if (this.getMonsterById(m.getId()) == null) {
			monsterItemDrop.cancel(true);
		}
	}

	public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
		final Point spos = this.getGroundBelow(pos);
		mob.setPosition(spos);
		this.spawnFakeMonster(mob);
	}

	public Point getGroundBelow(Point pos) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = this.calcPointBelow(spos);
		spos.y--;// shouldn't be null!
		return spos;
	}

	public void spawnRevives(final MapleMonster monster) {
		monster.setMap(this);

		this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.spawnMonster(monster, false));
			}
		});
		this.updateMonsterController(monster);
		this.spawnedMonstersOnMap.incrementAndGet();
	}

	public void spawnMonster(final MapleMonster monster) {
		if ((this.mobCapacity != -1)
				&& (this.mobCapacity == this.spawnedMonstersOnMap.get())) {
			return;// PyPQ
		}
		monster.setMap(this);
		this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.spawnMonster(monster, true));
			}
		}, null);
		this.updateMonsterController(monster);

		if (monster.getDropPeriodTime() > 0) { // 9300102 - Watchhog, 9300061 -
												// Moon Bunny (HPQ)
			if (monster.getId() == 9300102) {
				this.monsterItemDrop(monster, new Item(4031507, (byte) 0,
						(short) 1), monster.getDropPeriodTime());
			} else if (monster.getId() == 9300061) {
				this.monsterItemDrop(monster, new Item(4001101, (byte) 0,
						(short) 1), monster.getDropPeriodTime() / 3);
			} else {
				System.out.println("UNCODED TIMED MOB DETECTED: "
						+ monster.getId());
			}
		}
		this.spawnedMonstersOnMap.incrementAndGet();
		final selfDestruction selfDestruction = monster.getStats()
				.selfDestruction();
		if ((monster.getStats().removeAfter() > 0)
				|| ((selfDestruction != null) && (selfDestruction.getHp() < 0))) {
			if (selfDestruction == null) {
				TimerManager.getInstance().schedule(new Runnable() {
					@Override
					public void run() {
						MapleMap.this.killMonster(monster,
								(MapleCharacter) MapleMap.this.getAllPlayer()
										.get(0), false);
					}
				}, monster.getStats().removeAfter() * 1000);
			} else {
				TimerManager.getInstance().schedule(new Runnable() {
					@Override
					public void run() {
						MapleMap.this.killMonster(monster,
								(MapleCharacter) MapleMap.this.getAllPlayer()
										.get(0), false, false, selfDestruction
										.getAction());
					}
				}, selfDestruction.removeAfter() * 1000);
			}
		}
		if ((this.mapid == 910110000) && !this.allowHPQSummon) { // HPQ make
																	// monsters
			// invisible
			this.broadcastMessage(MaplePacketCreator
					.makeMonsterInvisible(monster));
		}
	}

	public void spawnDojoMonster(final MapleMonster monster) {
		final Point[] pts = { new Point(140, 0), new Point(190, 7),
				new Point(187, 7) };
		this.spawnMonsterWithEffect(monster, 15, pts[Randomizer.nextInt(3)]);
	}

	public void spawnMonsterWithEffect(final MapleMonster monster,
			final int effect, Point pos) {
		monster.setMap(this);
		Point spos = new Point(pos.x, pos.y - 1);
		spos = this.calcPointBelow(spos);
		spos.y--;
		monster.setPosition(spos);
		if ((this.mapid < 925020000) || (this.mapid > 925030000)) {
			monster.disableDrops();
		}
		this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.spawnMonster(monster, true,
						effect));
			}
		});
		if (monster.hasBossHPBar()) {
			this.broadcastMessage(monster.makeBossHPBarPacket(),
					monster.getPosition());
		}
		this.updateMonsterController(monster);

		this.spawnedMonstersOnMap.incrementAndGet();
	}

	public void spawnFakeMonster(final MapleMonster monster) {
		monster.setMap(this);
		monster.setFake(true);
		this.spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.spawnFakeMonster(monster, 0));
			}
		});

		this.spawnedMonstersOnMap.incrementAndGet();
	}

	public void makeMonsterReal(final MapleMonster monster) {
		monster.setFake(false);
		this.broadcastMessage(MaplePacketCreator.makeMonsterReal(monster));
		this.updateMonsterController(monster);
	}

	public void spawnReactor(final MapleReactor reactor) {
		reactor.setMap(this);
		this.spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(reactor.makeSpawnData());
			}
		});

	}

	private void respawnReactor(final MapleReactor reactor) {
		reactor.setState((byte) 0);
		reactor.setAlive(true);
		this.spawnReactor(reactor);
	}

	public void spawnDoor(final MapleDoor door) {
		this.spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.spawnDoor(
						door.getOwner().getId(), door.getTargetPosition(),
						false));
				if ((door.getOwner().getParty() != null)
						&& ((door.getOwner() == c.getPlayer()) || door
								.getOwner().getParty()
								.containsMembers(c.getPlayer().getMPC()))) {
					c.announce(MaplePacketCreator.partyPortal(door.getTown()
							.getId(), door.getTarget().getId(), door
							.getTargetPosition()));
				}
				c.announce(MaplePacketCreator.spawnPortal(door.getTown()
						.getId(), door.getTarget().getId(), door
						.getTargetPosition()));
				c.announce(MaplePacketCreator.enableActions());
			}
		}, new SpawnCondition() {
			@Override
			public boolean canSpawn(MapleCharacter chr) {
				return (chr.getMapId() == door.getTarget().getId())
						|| ((chr == door.getOwner()) && (chr.getParty() == null));
			}
		});

	}

	public List<MapleCharacter> getPlayersInRange(Rectangle box,
			List<MapleCharacter> chr) {
		final List<MapleCharacter> character = new LinkedList<>();
		this.chrRLock.lock();
		try {
			for (final MapleCharacter a : this.characters) {
				if (chr.contains(a.getClient().getPlayer())) {
					if (box.contains(a.getPosition())) {
						character.add(a);
					}
				}
			}
			return character;
		} finally {
			this.chrRLock.unlock();
		}
	}

	public void spawnSummon(final MapleSummon summon) {
		this.spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				if (summon != null) {
					c.announce(MaplePacketCreator.spawnSummon(summon, true));
				}
			}
		}, null);
	}

	public void spawnMist(final MapleMist mist, final int duration,
			boolean poison, boolean fake) {
		this.addMapObject(mist);
		this.broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist
				.makeSpawnData());
		final TimerManager tMan = TimerManager.getInstance();
		final ScheduledFuture<?> poisonSchedule;
		if (poison) {
			final Runnable poisonTask = new Runnable() {
				@Override
				public void run() {
					final List<MapleMapObject> affectedMonsters = MapleMap.this
							.getMapObjectsInBox(mist.getBox(), Collections
									.singletonList(MapleMapObjectType.MONSTER));
					for (final MapleMapObject mo : affectedMonsters) {
						if (mist.makeChanceResult()) {
							final MonsterStatusEffect poisonEffect = new MonsterStatusEffect(
									Collections.singletonMap(
											MonsterStatus.POISON, 1),
									mist.getSourceSkill(), null, false);
							((MapleMonster) mo).applyStatus(mist.getOwner(),
									poisonEffect, true, duration);
						}
					}
				}
			};
			poisonSchedule = tMan.register(poisonTask, 2000, 2500);
		} else {
			poisonSchedule = null;
		}
		tMan.schedule(new Runnable() {
			@Override
			public void run() {
				MapleMap.this.removeMapObject(mist);
				if (poisonSchedule != null) {
					poisonSchedule.cancel(false);
				}
				MapleMap.this.broadcastMessage(mist.makeDestroyData());
			}
		}, duration);
	}

	public final void spawnItemDrop(final MapleMapObject dropper,
			final MapleCharacter owner, final Item item, Point pos,
			final boolean ffaDrop, final boolean playerDrop) {
		final Point droppos = this.calcDropPos(pos, pos);
		final MapleMapItem drop = new MapleMapItem(item, droppos, dropper,
				owner, (byte) (ffaDrop ? 2 : 0), playerDrop);

		this.spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {
			@Override
			public void sendPackets(MapleClient c) {
				c.announce(MaplePacketCreator.dropItemFromMapObject(drop,
						dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);
		this.broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop,
				dropper.getPosition(), droppos, (byte) 0));

		if (!this.everlast) {
			TimerManager.getInstance().schedule(new ExpireMapItemJob(drop),
					180000);
			this.activateItemReactors(drop, owner.getClient());
		}
	}

	private void activateItemReactors(final MapleMapItem drop,
			final MapleClient c) {
		final Item item = drop.getItem();

		for (final MapleMapObject o : this.getAllReactor()) {
			final MapleReactor react = (MapleReactor) o;

			if (react.getReactorType() == 100) {
				if ((react.getReactItem((byte) 0).getLeft() == item.getItemId())
						&& (react.getReactItem((byte) 0).getRight() == item
								.getQuantity())) {

					if (react.getArea().contains(drop.getPosition())) {
						if (!react.isTimerActive()) {
							TimerManager.getInstance().schedule(
									new ActivateItemReactor(drop, react, c),
									5000);
							react.setTimerActive(true);
							break;
						}
					}
				}
			}
		}
	}

	public final List<MapleMapObject> getAllReactor() {
		return this.getMapObjectsInRange(new Point(0, 0),
				Double.POSITIVE_INFINITY,
				Arrays.asList(MapleMapObjectType.REACTOR));
	}

	public void startMapEffect(String msg, int itemId) {
		this.startMapEffect(msg, itemId, 30000);
	}

	public void startMapEffect(String msg, int itemId, long time) {
		if (this.mapEffect != null) {
			return;
		}
		this.mapEffect = new MapleMapEffect(msg, itemId);
		this.broadcastMessage(this.mapEffect.makeStartData());
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleMap.this.broadcastMessage(MapleMap.this.mapEffect
						.makeDestroyData());
				MapleMap.this.mapEffect = null;
			}
		}, time);
	}

	public void addPlayer(final MapleCharacter chr) {
		this.chrWLock.lock();
		try {
			this.characters.add(chr);
		} finally {
			this.chrWLock.unlock();
		}
		chr.setMapId(this.mapid);
		if ((this.onFirstUserEnter.length() != 0)
				&& !chr.hasEntered(this.onFirstUserEnter, this.mapid)
				&& MapScriptManager.getInstance().scriptExists(
						this.onFirstUserEnter, true)) {
			if (this.getAllPlayer().size() <= 1) {
				chr.enteredScript(this.onFirstUserEnter, this.mapid);
				MapScriptManager.getInstance().getMapScript(chr.getClient(),
						this.onFirstUserEnter, true);
			}
		}
		if (this.onUserEnter.length() != 0) {
			if (this.onUserEnter.equals("cygnusTest")
					&& ((this.mapid < 913040000) || (this.mapid > 913040006))) {
				chr.saveLocation("INTRO");
			}
			MapScriptManager.getInstance().getMapScript(chr.getClient(),
					this.onUserEnter, false);
		}
		if (FieldLimit.CANNOTUSEMOUNTS.check(this.fieldLimit)
				&& (chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null)) {
			chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
			chr.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
		}
		if ((this.mapid == 923010000) && (this.getMonsterById(9300102) == null)) { // Kenta's
			// Mount
			// Quest
			this.spawnMonsterOnGroundBelow(
					MapleLifeFactory.getMonster(9300102), new Point(77, 426));
		} else if (this.mapid == 910110000) { // Henesys Party Quest
			chr.getClient().announce(MaplePacketCreator.getClock(15 * 60));
			TimerManager.getInstance().register(new Runnable() {
				@Override
				public void run() {
					if (MapleMap.this.mapid == 910110000) {
						chr.getClient()
								.getPlayer()
								.changeMap(
										chr.getClient().getChannelServer()
												.getMapFactory()
												.getMap(925020000));
					}
				}
			}, (15 * 60 * 1000) + 3000);
		}
		final MaplePet[] pets = chr.getPets();
		for (int i = 0; i < chr.getPets().length; i++) {
			if (pets[i] != null) {
				pets[i].setPos(this.getGroundBelow(chr.getPosition()));
				chr.announce(MaplePacketCreator.showPet(chr, pets[i], false,
						false));
			} else {
				break;
			}
		}

		if (chr.isHidden()) {
			this.broadcastGMMessage(chr,
					MaplePacketCreator.spawnPlayerMapobject(chr), false);
			chr.announce(MaplePacketCreator.getGMEffect(0x10, (byte) 1));
		} else {
			this.broadcastMessage(chr,
					MaplePacketCreator.spawnPlayerMapobject(chr), false);
		}

		this.sendObjectPlacement(chr.getClient());
		if (this.isStartingEventMap() && !this.eventStarted()) {
			chr.getMap().getPortal("join00").setPortalStatus(false);
		}
		if (this.hasForcedEquip()) {
			chr.getClient().announce(MaplePacketCreator.showForcedEquip(-1));
		}
		if (this.specialEquip()) {
			chr.getClient().announce(MaplePacketCreator.coconutScore(0, 0));
			chr.getClient().announce(
					MaplePacketCreator.showForcedEquip(chr.getTeam()));
		}
		this.objectWLock.lock();
		try {
			this.mapobjects.put(Integer.valueOf(chr.getObjectId()), chr);
		} finally {
			this.objectWLock.unlock();
		}
		if (chr.getPlayerShop() != null) {
			this.addMapObject(chr.getPlayerShop());
		}
		final MapleStatEffect summonStat = chr
				.getStatForBuff(MapleBuffStat.SUMMON);
		if (summonStat != null) {
			final MapleSummon summon = chr.getSummons().get(
					summonStat.getSourceId());
			summon.setPosition(chr.getPosition());
			chr.getMap().spawnSummon(summon);
			this.updateMapObjectVisibility(chr, summon);
		}
		if (this.mapEffect != null) {
			this.mapEffect.sendStartData(chr.getClient());
		}
		chr.getClient().announce(MaplePacketCreator.resetForcedStats());
		if ((this.mapid == 914000200) || (this.mapid == 914000210)
				|| (this.mapid == 914000220)) {
			chr.getClient().announce(MaplePacketCreator.aranGodlyStats());
		}
		if ((chr.getEventInstance() != null)
				&& chr.getEventInstance().isTimerStarted()) {
			chr.getClient().announce(
					MaplePacketCreator.getClock((int) (chr.getEventInstance()
							.getTimeLeft() / 1000)));
		}
		if ((chr.getFitness() != null) && chr.getFitness().isTimerStarted()) {
			chr.getClient().announce(
					MaplePacketCreator.getClock((int) (chr.getFitness()
							.getTimeLeft() / 1000)));
		}

		if ((chr.getOla() != null) && chr.getOla().isTimerStarted()) {
			chr.getClient().announce(
					MaplePacketCreator.getClock((int) (chr.getOla()
							.getTimeLeft() / 1000)));
		}

		if (this.mapid == 109060000) {
			chr.announce(MaplePacketCreator.rollSnowBall(true, 0, null, null));
		}

		final MonsterCarnival carnival = chr.getCarnival();
		final MonsterCarnivalParty cparty = chr.getCarnivalParty();
		if ((carnival != null)
				&& (cparty != null)
				&& ((this.mapid == 980000101) || (this.mapid == 980000201)
						|| (this.mapid == 980000301)
						|| (this.mapid == 980000401)
						|| (this.mapid == 980000501) || (this.mapid == 980000601))) {
			chr.getClient()
					.announce(
							MaplePacketCreator.getClock((int) (carnival
									.getTimeLeft() / 1000)));
			chr.getClient().announce(
					MaplePacketCreator.startCPQ(chr,
							carnival.oppositeTeam(cparty)));
		}
		if (this.hasClock()) {
			final Calendar cal = Calendar.getInstance();
			chr.getClient()
					.announce(
							(MaplePacketCreator.getClockTime(
									cal.get(Calendar.HOUR_OF_DAY),
									cal.get(Calendar.MINUTE),
									cal.get(Calendar.SECOND))));
		}
		if (this.hasBoat() == 2) {
			chr.getClient().announce((MaplePacketCreator.boatPacket(true)));
		} else if ((this.hasBoat() == 1)
				&& ((chr.getMapId() != 200090000) || (chr.getMapId() != 200090010))) {
			chr.getClient().announce(MaplePacketCreator.boatPacket(false));
		}
		chr.receivePartyMemberHP();
	}

	public MaplePortal findClosestPortal(Point from) {
		MaplePortal closest = null;
		double shortestDistance = Double.POSITIVE_INFINITY;
		for (final MaplePortal portal : this.portals.values()) {
			final double distance = portal.getPosition().distanceSq(from);
			if (distance < shortestDistance) {
				closest = portal;
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public MaplePortal getRandomSpawnpoint() {
		final List<MaplePortal> spawnPoints = new ArrayList<>();
		for (final MaplePortal portal : this.portals.values()) {
			if ((portal.getType() >= 0) && (portal.getType() <= 2)) {
				spawnPoints.add(portal);
			}
		}
		final MaplePortal portal = spawnPoints.get(new Random()
				.nextInt(spawnPoints.size()));
		return portal != null ? portal : this.getPortal(0);
	}

	public void removePlayer(MapleCharacter chr) {
		this.chrWLock.lock();
		try {
			this.characters.remove(chr);
		} finally {
			this.chrWLock.unlock();
		}
		this.removeMapObject(Integer.valueOf(chr.getObjectId()));
		if (!chr.isHidden()) {
			this.broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr
					.getId()));
		} else {
			this.broadcastGMMessage(MaplePacketCreator.removePlayerFromMap(chr
					.getId()));
		}

		for (final MapleMonster monster : chr.getControlledMonsters()) {
			monster.setController(null);
			monster.setControllerHasAggro(false);
			monster.setControllerKnowsAboutAggro(false);
			this.updateMonsterController(monster);
		}
		chr.leaveMap();
		chr.cancelMapTimeLimitTask();
		for (final MapleSummon summon : chr.getSummons().values()) {
			if (summon.isStationary()) {
				chr.cancelBuffStats(MapleBuffStat.PUPPET);
			} else {
				this.removeMapObject(summon);
			}
		}
	}

	public void broadcastMessage(final byte[] packet) {
		this.broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
	}

	public void broadcastGMMessage(final byte[] packet) {
		this.broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
	}

	/**
	 * Nonranged. Repeat to source according to parameter.
	 *
	 * @param source
	 * @param packet
	 * @param repeatToSource
	 */
	public void broadcastMessage(MapleCharacter source, final byte[] packet,
			boolean repeatToSource) {
		this.broadcastMessage(repeatToSource ? null : source, packet,
				Double.POSITIVE_INFINITY, source.getPosition());
	}

	/**
	 * Ranged and repeat according to parameters.
	 *
	 * @param source
	 * @param packet
	 * @param repeatToSource
	 * @param ranged
	 */
	public void broadcastMessage(MapleCharacter source, final byte[] packet,
			boolean repeatToSource, boolean ranged) {
		this.broadcastMessage(repeatToSource ? null : source, packet,
				ranged ? 722500 : Double.POSITIVE_INFINITY,
				source.getPosition());
	}

	/**
	 * Always ranged from Point.
	 *
	 * @param packet
	 * @param rangedFrom
	 */
	public void broadcastMessage(final byte[] packet, Point rangedFrom) {
		this.broadcastMessage(null, packet, 722500, rangedFrom);
	}

	/**
	 * Always ranged from point. Does not repeat to source.
	 *
	 * @param source
	 * @param packet
	 * @param rangedFrom
	 */
	public void broadcastMessage(MapleCharacter source, final byte[] packet,
			Point rangedFrom) {
		this.broadcastMessage(source, packet, 722500, rangedFrom);
	}

	private void broadcastMessage(MapleCharacter source, final byte[] packet,
			double rangeSq, Point rangedFrom) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter chr : this.characters) {
				if (chr != source) {
					if (rangeSq < Double.POSITIVE_INFINITY) {
						if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
							chr.getClient().announce(packet);
						}
					} else {
						chr.getClient().announce(packet);
					}
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
	}

	private boolean isNonRangedType(MapleMapObjectType type) {
		switch (type) {
		case NPC:
		case PLAYER:
		case HIRED_MERCHANT:
		case PLAYER_NPC:
		case MIST:
			return true;
		}
		return false;
	}

	private void sendObjectPlacement(MapleClient mapleClient) {
		final MapleCharacter chr = mapleClient.getPlayer();
		this.objectRLock.lock();
		try {
			for (final MapleMapObject o : this.mapobjects.values()) {
				if (o.getType() == MapleMapObjectType.SUMMON) {
					final MapleSummon summon = (MapleSummon) o;
					if (summon.getOwner() == chr) {
						if (chr.getSummons().isEmpty()
								|| !chr.getSummons().containsValue(summon)) {
							this.objectWLock.lock();
							try {
								this.mapobjects.remove(o);
							} finally {
								this.objectWLock.unlock();
							}
							continue;
						}
					}
				}
				if (this.isNonRangedType(o.getType())) {
					o.sendSpawnData(mapleClient);
				} else if (o.getType() == MapleMapObjectType.MONSTER) {
					this.updateMonsterController((MapleMonster) o);
				}
			}
		} finally {
			this.objectRLock.unlock();
		}
		if (chr != null) {
			for (final MapleMapObject o : this.getMapObjectsInRange(
					chr.getPosition(), 722500, rangedMapobjectTypes)) {
				if (o.getType() == MapleMapObjectType.REACTOR) {
					if (((MapleReactor) o).isAlive()) {
						o.sendSpawnData(chr.getClient());
						chr.addVisibleMapObject(o);
					}
				} else {
					o.sendSpawnData(chr.getClient());
					chr.addVisibleMapObject(o);
				}
			}
		}
	}

	public List<MapleMapObject> getMapObjectsInRange(Point from,
			double rangeSq, List<MapleMapObjectType> types) {
		final List<MapleMapObject> ret = new LinkedList<>();
		this.objectRLock.lock();
		try {
			for (final MapleMapObject l : this.mapobjects.values()) {
				if (types.contains(l.getType())) {
					if (from.distanceSq(l.getPosition()) <= rangeSq) {
						ret.add(l);
					}
				}
			}
			return ret;
		} finally {
			this.objectRLock.unlock();
		}
	}

	public List<MapleMapObject> getMapObjectsInBox(Rectangle box,
			List<MapleMapObjectType> types) {
		final List<MapleMapObject> ret = new LinkedList<>();
		this.objectRLock.lock();
		try {
			for (final MapleMapObject l : this.mapobjects.values()) {
				if (types.contains(l.getType())) {
					if (box.contains(l.getPosition())) {
						ret.add(l);
					}
				}
			}
			return ret;
		} finally {
			this.objectRLock.unlock();
		}
	}

	public void addPortal(MaplePortal myPortal) {
		this.portals.put(myPortal.getId(), myPortal);
	}

	public MaplePortal getPortal(String portalname) {
		for (final MaplePortal port : this.portals.values()) {
			if (port.getName().equals(portalname)) {
				return port;
			}
		}
		return null;
	}

	public MaplePortal getPortal(int portalid) {
		return this.portals.get(portalid);
	}

	public void addMapleArea(Rectangle rec) {
		this.areas.add(rec);
	}

	public List<Rectangle> getAreas() {
		return new ArrayList<>(this.areas);
	}

	public Rectangle getArea(int index) {
		return this.areas.get(index);
	}

	public void setFootholds(MapleFootholdTree footholds) {
		this.footholds = footholds;
	}

	public MapleFootholdTree getFootholds() {
		return this.footholds;
	}

	/**
	 * it's threadsafe, gtfo :D
	 *
	 * @param monster
	 * @param mobTime
	 */
	public void addMonsterSpawn(MapleMonster monster, int mobTime, int team) {
		final Point newpos = this.calcPointBelow(monster.getPosition());
		newpos.y -= 1;
		final SpawnPoint sp = new SpawnPoint(monster, newpos,
				!monster.isMobile(), mobTime, this.mobInterval, team);
		this.monsterSpawn.add(sp);
		if (sp.shouldSpawn() || (mobTime == -1)) {// -1 does not respawn and
													// should not either but
													// force
													// ONE spawn
			this.spawnMonster(sp.getMonster());
		}

	}

	public float getMonsterRate() {
		return this.monsterRate;
	}

	public Collection<MapleCharacter> getCharacters() {
		return Collections.unmodifiableCollection(this.characters);
	}

	public MapleCharacter getCharacterById(int id) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter c : this.characters) {
				if (c.getId() == id) {
					return c;
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
		return null;
	}

	private void updateMapObjectVisibility(MapleCharacter chr, MapleMapObject mo) {
		if (!chr.isMapObjectVisible(mo)) { // monster entered view range
			if ((mo.getType() == MapleMapObjectType.SUMMON)
					|| (mo.getPosition().distanceSq(chr.getPosition()) <= 722500)) {
				chr.addVisibleMapObject(mo);
				mo.sendSpawnData(chr.getClient());
			}
		} else if ((mo.getType() != MapleMapObjectType.SUMMON)
				&& (mo.getPosition().distanceSq(chr.getPosition()) > 722500)) {
			chr.removeVisibleMapObject(mo);
			mo.sendDestroyData(chr.getClient());
		}
	}

	public void moveMonster(MapleMonster monster, Point reportedPos) {
		monster.setPosition(reportedPos);
		this.chrRLock.lock();
		try {
			for (final MapleCharacter chr : this.characters) {
				this.updateMapObjectVisibility(chr, monster);
			}
		} finally {
			this.chrRLock.unlock();
		}
	}

	public void movePlayer(MapleCharacter player, Point newPosition) {
		player.setPosition(newPosition);
		final Collection<MapleMapObject> visibleObjects = player
				.getVisibleMapObjects();
		final MapleMapObject[] visibleObjectsNow = visibleObjects
				.toArray(new MapleMapObject[visibleObjects.size()]);
		try {
			for (final MapleMapObject mo : visibleObjectsNow) {
				if (mo != null) {
					if (this.mapobjects.get(mo.getObjectId()) == mo) {
						this.updateMapObjectVisibility(player, mo);
					} else {
						player.removeVisibleMapObject(mo);
					}
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		for (final MapleMapObject mo : this.getMapObjectsInRange(
				player.getPosition(), 722500, rangedMapobjectTypes)) {
			if (!player.isMapObjectVisible(mo)) {
				mo.sendSpawnData(player.getClient());
				player.addVisibleMapObject(mo);
			}
		}
	}

	public MaplePortal findClosestSpawnpoint(Point from) {
		MaplePortal closest = null;
		double shortestDistance = Double.POSITIVE_INFINITY;
		for (final MaplePortal portal : this.portals.values()) {
			final double distance = portal.getPosition().distanceSq(from);
			if ((portal.getType() >= 0) && (portal.getType() <= 2)
					&& (distance < shortestDistance)
					&& (portal.getTargetMapId() == 999999999)) {
				closest = portal;
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public Collection<MaplePortal> getPortals() {
		return Collections.unmodifiableCollection(this.portals.values());
	}

	public String getMapName() {
		return this.mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public String getStreetName() {
		return this.streetName;
	}

	public void setClock(boolean hasClock) {
		this.clock = hasClock;
	}

	public boolean hasClock() {
		return this.clock;
	}

	public void setTown(boolean isTown) {
		this.town = isTown;
	}

	public boolean isTown() {
		return this.town;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	public void setEverlast(boolean everlast) {
		this.everlast = everlast;
	}

	public boolean getEverlast() {
		return this.everlast;
	}

	public int getSpawnedMonstersOnMap() {
		return this.spawnedMonstersOnMap.get();
	}

	public void setMobCapacity(int capacity) {
		this.mobCapacity = capacity;

	}

	public MapleCharacter getCharacterByName(String name) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter c : this.characters) {
				if (c.getName().toLowerCase().equals(name.toLowerCase())) {
					return c;
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
		return null;
	}

	private class ExpireMapItemJob implements Runnable {

		private final MapleMapItem mapitem;

		public ExpireMapItemJob(MapleMapItem mapitem) {
			this.mapitem = mapitem;
		}

		@Override
		public void run() {
			if ((this.mapitem != null)
					&& (this.mapitem == MapleMap.this.getMapObject(this.mapitem
							.getObjectId()))) {
				this.mapitem.itemLock.lock();
				try {
					if (this.mapitem.isPickedUp()) {
						return;
					}
					MapleMap.this.broadcastMessage(
							MaplePacketCreator.removeItemFromMap(
									this.mapitem.getObjectId(), 0, 0),
							this.mapitem.getPosition());
					this.mapitem.setPickedUp(true);
				} finally {
					this.mapitem.itemLock.unlock();
					MapleMap.this.removeMapObject(this.mapitem);
				}
			}
		}
	}

	private class ActivateItemReactor implements Runnable {

		private final MapleMapItem mapitem;
		private final MapleReactor reactor;
		private final MapleClient c;

		public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor,
				MapleClient c) {
			this.mapitem = mapitem;
			this.reactor = reactor;
			this.c = c;
		}

		@Override
		public void run() {
			if ((this.mapitem != null)
					&& (this.mapitem == MapleMap.this.getMapObject(this.mapitem
							.getObjectId()))) {
				this.mapitem.itemLock.lock();
				try {
					final TimerManager tMan = TimerManager.getInstance();
					if (this.mapitem.isPickedUp()) {
						return;
					}
					MapleMap.this.broadcastMessage(
							MaplePacketCreator.removeItemFromMap(
									this.mapitem.getObjectId(), 0, 0),
							this.mapitem.getPosition());
					MapleMap.this.removeMapObject(this.mapitem);
					this.reactor.hitReactor(this.c);
					this.reactor.setTimerActive(false);
					if (this.reactor.getDelay() > 0) {
						tMan.schedule(new Runnable() {
							@Override
							public void run() {
								ActivateItemReactor.this.reactor
										.setState((byte) 0);
								MapleMap.this.broadcastMessage(MaplePacketCreator
										.triggerReactor(
												ActivateItemReactor.this.reactor,
												0));
							}
						}, this.reactor.getDelay());
					}
				} finally {
					this.mapitem.itemLock.unlock();
				}
			}
		}
	}

	public void respawn() {
		if (this.characters.isEmpty()) {
			return;
		}
		final short numShouldSpawn = (short) ((this.monsterSpawn.size() - this.spawnedMonstersOnMap
				.get()) * this.monsterRate);// Fking lol'd
		if (numShouldSpawn > 0) {
			final List<SpawnPoint> randomSpawn = new ArrayList<>(
					this.monsterSpawn);
			Collections.shuffle(randomSpawn);
			short spawned = 0;
			for (final SpawnPoint spawnPoint : randomSpawn) {
				if (spawnPoint.shouldSpawn()) {
					this.spawnMonster(spawnPoint.getMonster());
					spawned++;
				}
				if (spawned >= numShouldSpawn) {
					break;

				}
			}
		}
	}

	private static interface DelayedPacketCreation {

		void sendPackets(MapleClient c);
	}

	private static interface SpawnCondition {

		boolean canSpawn(MapleCharacter chr);
	}

	public int getHPDec() {
		return this.decHP;
	}

	public void setHPDec(int delta) {
		this.decHP = delta;
	}

	public int getHPDecProtect() {
		return this.protectItem;
	}

	public void setHPDecProtect(int delta) {
		this.protectItem = delta;
	}

	private int hasBoat() {
		return this.docked ? 2 : (this.boat ? 1 : 0);
	}

	public void setBoat(boolean hasBoat) {
		this.boat = hasBoat;
	}

	public void setDocked(boolean isDocked) {
		this.docked = isDocked;
	}

	public void broadcastGMMessage(MapleCharacter source, final byte[] packet,
			boolean repeatToSource) {
		this.broadcastGMMessage(repeatToSource ? null : source, packet,
				Double.POSITIVE_INFINITY, source.getPosition());
	}

	private void broadcastGMMessage(MapleCharacter source, final byte[] packet,
			double rangeSq, Point rangedFrom) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter chr : this.characters) {
				if ((chr != source) && chr.isGM()) {
					if (rangeSq < Double.POSITIVE_INFINITY) {
						if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
							chr.getClient().announce(packet);
						}
					} else {
						chr.getClient().announce(packet);
					}
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
	}

	public void broadcastNONGMMessage(MapleCharacter source,
			final byte[] packet, boolean repeatToSource) {
		this.chrRLock.lock();
		try {
			for (final MapleCharacter chr : this.characters) {
				if ((chr != source) && !chr.isGM()) {
					chr.getClient().announce(packet);
				}
			}
		} finally {
			this.chrRLock.unlock();
		}
	}

	public MapleOxQuiz getOx() {
		return this.ox;
	}

	public void setOx(MapleOxQuiz set) {
		this.ox = set;
	}

	public void setOxQuiz(boolean b) {
		this.isOxQuiz = b;
	}

	public boolean isOxQuiz() {
		return this.isOxQuiz;
	}

	public void setOnUserEnter(String onUserEnter) {
		this.onUserEnter = onUserEnter;
	}

	public String getOnUserEnter() {
		return this.onUserEnter;
	}

	public void setOnFirstUserEnter(String onFirstUserEnter) {
		this.onFirstUserEnter = onFirstUserEnter;
	}

	public String getOnFirstUserEnter() {
		return this.onFirstUserEnter;
	}

	private boolean hasForcedEquip() {
		return (this.fieldType == 81) || (this.fieldType == 82);
	}

	public void setFieldType(int fieldType) {
		this.fieldType = fieldType;
	}

	public void clearDrops(MapleCharacter player) {
		final List<MapleMapObject> items = player.getMap()
				.getMapObjectsInRange(player.getPosition(),
						Double.POSITIVE_INFINITY,
						Arrays.asList(MapleMapObjectType.ITEM));
		for (final MapleMapObject i : items) {
			player.getMap().removeMapObject(i);
			player.getMap().broadcastMessage(
					MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0,
							player.getId()));
		}
	}

	public void clearDrops() {
		for (final MapleMapObject i : this.getMapObjectsInRange(
				new Point(0, 0), Double.POSITIVE_INFINITY,
				Arrays.asList(MapleMapObjectType.ITEM))) {
			this.removeMapObject(i);
		}
	}

	public void addMapTimer(int time) {
		this.timeLimit = System.currentTimeMillis() + (time * 1000);
		this.broadcastMessage(MaplePacketCreator.getClock(time));
		this.mapMonitor = TimerManager.getInstance().register(new Runnable() {
			@Override
			public void run() {
				if ((MapleMap.this.timeLimit != 0)
						&& (MapleMap.this.timeLimit < System.currentTimeMillis())) {
					MapleMap.this.warpEveryone(MapleMap.this
							.getForcedReturnId());
				}
				if (MapleMap.this.getCharacters().isEmpty()) {
					MapleMap.this.resetReactors();
					MapleMap.this.killAllMonsters();
					MapleMap.this.clearDrops();
					MapleMap.this.timeLimit = 0;
					if ((MapleMap.this.mapid >= 922240100)
							&& (MapleMap.this.mapid <= 922240119)) {
						MapleMap.this.toggleHiddenNPC(9001108);
					}
					MapleMap.this.mapMonitor.cancel(true);
					MapleMap.this.mapMonitor = null;
				}
			}
		}, 1000);
	}

	public void setFieldLimit(int fieldLimit) {
		this.fieldLimit = fieldLimit;
	}

	public int getFieldLimit() {
		return this.fieldLimit;
	}

	public void resetRiceCakes() {
		this.riceCakeNum = 0;
	}

	public void setAllowHPQSummon(boolean b) {
		this.allowHPQSummon = b;
	}

	public void warpEveryone(int to) {
		for (final MapleCharacter chr : this.getCharacters()) {
			chr.changeMap(to);
		}
	}

	// BEGIN EVENTS
	public void setSnowball(int team, MapleSnowball ball) {
		switch (team) {
		case 0:
			this.snowball0 = ball;
			break;
		case 1:
			this.snowball1 = ball;
			break;
		default:
			break;
		}
	}

	public MapleSnowball getSnowball(int team) {
		switch (team) {
		case 0:
			return this.snowball0;
		case 1:
			return this.snowball1;
		default:
			return null;
		}
	}

	private boolean specialEquip() {// Maybe I shouldn't use fieldType :\
		return (this.fieldType == 4) || (this.fieldType == 19);
	}

	public void setCoconut(MapleCoconut nut) {
		this.coconut = nut;
	}

	public MapleCoconut getCoconut() {
		return this.coconut;
	}

	public void warpOutByTeam(int team, int mapid) {
		for (final MapleCharacter chr : this.getCharacters()) {
			if (chr != null) {
				if (chr.getTeam() == team) {
					chr.changeMap(mapid);
				}
			}
		}
	}

	public void startEvent(final MapleCharacter chr) {
		if (this.mapid == 109080000) {
			this.setCoconut(new MapleCoconut(this));
			this.coconut.startEvent();

		} else if (this.mapid == 109040000) {
			chr.setFitness(new MapleFitness(chr));
			chr.getFitness().startFitness();

		} else if ((this.mapid == 109030001) || (this.mapid == 109030101)) {
			chr.setOla(new MapleOla(chr));
			chr.getOla().startOla();

		} else if ((this.mapid == 109020001) && (this.getOx() == null)) {
			this.setOx(new MapleOxQuiz(this));
			this.getOx().sendQuestion();
			this.setOxQuiz(true);

		} else if ((this.mapid == 109060000)
				&& (this.getSnowball(chr.getTeam()) == null)) {
			this.setSnowball(0, new MapleSnowball(0, this));
			this.setSnowball(1, new MapleSnowball(1, this));
			this.getSnowball(chr.getTeam()).startEvent();
		}
	}

	public boolean eventStarted() {
		return this.eventstarted;
	}

	public void startEvent() {
		this.eventstarted = true;
	}

	public void setEventStarted(boolean event) {
		this.eventstarted = event;
	}

	public String getEventNPC() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Talk to ");
		if (this.mapid == 60000) {
			sb.append("Paul!");
		} else if (this.mapid == 104000000) {
			sb.append("Jean!");
		} else if (this.mapid == 200000000) {
			sb.append("Martin!");
		} else if (this.mapid == 220000000) {
			sb.append("Tony!");
		} else {
			return null;
		}
		return sb.toString();
	}

	public boolean hasEventNPC() {
		return (this.mapid == 60000) || (this.mapid == 104000000)
				|| (this.mapid == 200000000) || (this.mapid == 220000000);
	}

	public boolean isStartingEventMap() {
		return (this.mapid == 109040000) || (this.mapid == 109020001)
				|| (this.mapid == 109010000) || (this.mapid == 109030001)
				|| (this.mapid == 109030101);
	}

	public boolean isEventMap() {
		return ((this.mapid >= 109010000) && (this.mapid < 109050000))
				|| ((this.mapid > 109050001) && (this.mapid <= 109090000));
	}

	public void timeMob(int id, String msg) {
		this.timeMob = new Pair<>(id, msg);
	}

	public Pair<Integer, String> getTimeMob() {
		return this.timeMob;
	}

	public void toggleHiddenNPC(int id) {
		for (final MapleMapObject obj : this.mapobjects.values()) {
			if (obj.getType() == MapleMapObjectType.NPC) {
				final MapleNPC npc = (MapleNPC) obj;
				if (npc.getId() == id) {
					npc.setHide(!npc.isHidden());
					if (!npc.isHidden()) // Should only be hidden upon changing
											// maps
					{
						this.broadcastMessage(MaplePacketCreator.spawnNPC(npc));
					}
				}
			}
		}
	}

	public void setMobInterval(short interval) {
		this.mobInterval = interval;
	}

	public short getMobInterval() {
		return this.mobInterval;
	}
}
