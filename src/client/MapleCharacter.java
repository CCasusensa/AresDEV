/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program unader any cother version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import net.server.PlayerBuffValueHolder;
import net.server.PlayerCoolDownValueHolder;
import net.server.PlayerDiseaseValueHolder;
import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.MapleMessenger;
import net.server.world.MapleMessengerCharacter;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import scripting.event.EventInstanceManager;
import server.CashShop;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleMiniGame;
import server.MaplePlayerShop;
import server.MaplePortal;
import server.MapleShop;
import server.MapleStatEffect;
import server.MapleStorage;
import server.MapleTrade;
import server.TimerManager;
import server.events.MapleEvents;
import server.events.RescueGaga;
import server.events.gm.MapleFitness;
import server.events.gm.MapleOla;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.AbstractAnimatedMapleMapObject;
import server.maps.HiredMerchant;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapEffect;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.PlayerNPCs;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.partyquest.MonsterCarnival;
import server.partyquest.MonsterCarnivalParty;
import server.partyquest.PartyQuest;
import server.quest.MapleQuest;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import client.autoban.AutobanManager;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleWeaponType;
import client.inventory.ModifyInventory;
import constants.ExpTable;
import constants.ItemConstants;
import constants.ServerConstants;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.FPArchMage;
import constants.skills.GM;
import constants.skills.Hermit;
import constants.skills.ILArchMage;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.Swordsman;
import constants.skills.ThunderBreaker;

public class MapleCharacter extends AbstractAnimatedMapleMapObject {

	private static final String LEVEL_200 = "[Congrats] %s has reached Level 200! Congratulate %s on such an amazing achievement!";
	private static final int[] DEFAULT_KEY = { 18, 65, 2, 23, 3, 4, 5, 6, 16,
			17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56,
			59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41, 39 };
	private static final int[] DEFAULT_TYPE = { 4, 6, 4, 4, 4, 4, 4, 4, 4, 4,
			4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6,
			5, 4, 5, 4, 4, 4, 4, 4 };
	private static final int[] DEFAULT_ACTION = { 0, 106, 10, 1, 12, 13, 18,
			24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53,
			100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23, 27 };
	private int world;
	private int accountid, id;
	private int rank, rankMove, jobRank, jobRankMove;
	private int level, str, dex, luk, int_, hp, maxhp, mp, maxmp;
	private int hpMpApUsed;
	private int hair;
	private int face;
	private int remainingAp, remainingSp;
	private int fame;
	private int initialSpawnPoint;
	private int mapid;
	private int gender;
	private int currentPage, currentType = 0, currentTab = 1;
	private int chair;
	private int itemEffect;
	private int guildid, guildrank, allianceRank;
	private int messengerposition = 4;
	private int slots = 0;
	private int energybar;
	private int gmLevel;
	private int ci = 0;
	private MapleFamily family;
	private int familyId;
	private int bookCover;
	private int markedMonster = 0;
	private int battleshipHp = 0;
	private int mesosTraded = 0;
	private int possibleReports = 10;
	private int dojoPoints, vanquisherStage, dojoStage, dojoEnergy,
			vanquisherKills;
	private int warpToId;
	private int expRate = 1, mesoRate = 1, dropRate = 1;
	private int omokwins, omokties, omoklosses, matchcardwins, matchcardties,
			matchcardlosses;
	private int married;
	private long dojoFinish, lastfametime, lastUsedCashItem, lastHealed;
	private transient int localmaxhp, localmaxmp, localstr, localdex, localluk,
			localint_, magic, watk;
	private boolean hidden, canDoor = true, Berserk, hasMerchant;
	private int linkedLevel = 0;
	private String linkedName = null;
	private boolean finishedDojoTutorial, dojoParty;
	private String name;
	private String chalktext;
	private String search = null;
	private final AtomicInteger exp = new AtomicInteger();
	private final AtomicInteger gachaexp = new AtomicInteger();
	private final AtomicInteger meso = new AtomicInteger();
	private int merchantmeso;
	private BuddyList buddylist;
	private EventInstanceManager eventInstance = null;
	private HiredMerchant hiredMerchant = null;
	private MapleClient client;
	private MapleGuildCharacter mgc = null;
	private MaplePartyCharacter mpc = null;
	private final MapleInventory[] inventory;
	private MapleJob job = MapleJob.BEGINNER;
	private MapleMap map, dojoMap;// Make a Dojo pq instance
	private MapleMessenger messenger = null;
	private MapleMiniGame miniGame;
	private MapleMount maplemount;
	private MapleParty party;
	private final MaplePet[] pets = new MaplePet[3];
	private MaplePlayerShop playerShop = null;
	private MapleShop shop = null;
	private MapleSkinColor skinColor = MapleSkinColor.NORMAL;
	private MapleStorage storage = null;
	private MapleTrade trade = null;
	private final SavedLocation savedLocations[];
	private final SkillMacro[] skillMacros = new SkillMacro[5];
	private List<Integer> lastmonthfameids;
	private final Map<MapleQuest, MapleQuestStatus> quests;
	private final Set<MapleMonster> controlled = new LinkedHashSet<>();
	private final Map<Integer, String> entered = new LinkedHashMap<>();
	private final Set<MapleMapObject> visibleMapObjects = new LinkedHashSet<>();
	private final Map<Skill, SkillEntry> skills = new LinkedHashMap<>();
	private final EnumMap<MapleBuffStat, MapleBuffStatValueHolder> effects = new EnumMap<>(
			MapleBuffStat.class);
	private final Map<Integer, MapleKeyBinding> keymap = new LinkedHashMap<>();
	private final Map<Integer, MapleSummon> summons = new LinkedHashMap<>();
	private final Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<>(
			50);
	private final EnumMap<MapleDisease, DiseaseValueHolder> diseases = new EnumMap<>(
			MapleDisease.class);
	private final List<MapleDoor> doors = new ArrayList<>();
	private ScheduledFuture<?> dragonBloodSchedule;
	private final ScheduledFuture<?> mapTimeLimitTask = null;
	private final ScheduledFuture<?>[] fullnessSchedule = new ScheduledFuture<?>[3];
	private ScheduledFuture<?> hpDecreaseTask;
	private ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule,
			BerserkSchedule;
	private ScheduledFuture<?> expiretask;
	private ScheduledFuture<?> recoveryTask;
	private List<ScheduledFuture<?>> timers = new ArrayList<>();
	private final NumberFormat nf = new DecimalFormat("#,###,###,###");
	private final ArrayList<Integer> excluded = new ArrayList<>();
	private MonsterBook monsterbook;
	private final List<MapleRing> crushRings = new ArrayList<>();
	private final List<MapleRing> friendshipRings = new ArrayList<>();
	private MapleRing marriageRing;
	private static String[] ariantroomleader = new String[3];
	private static int[] ariantroomslot = new int[3];
	private CashShop cashshop;
	private long portaldelay = 0, lastcombo = 0;
	private short combocounter = 0;
	private final List<String> blockedPortals = new ArrayList<>();
	private final Map<Short, String> area_info = new LinkedHashMap<>();
	private AutobanManager autoban;
	private boolean isbanned = false;
	private ScheduledFuture<?> pendantOfSpirit = null; // 1122017
	private byte pendantExp = 0, lastmobcount = 0;
	private final int[] trockmaps = new int[5];
	private final int[] viptrockmaps = new int[10];
	private Map<String, MapleEvents> events = new LinkedHashMap<>();
	private PartyQuest partyQuest = null;
	private boolean loggedIn = false;

	private MapleCharacter() {
		this.setStance(0);
		this.inventory = new MapleInventory[MapleInventoryType.values().length];
		this.savedLocations = new SavedLocation[SavedLocationType.values().length];

		for (final MapleInventoryType type : MapleInventoryType.values()) {
			byte b = 24;
			if (type == MapleInventoryType.CASH) {
				b = 96;
			}
			this.inventory[type.ordinal()] = new MapleInventory(type, b);
		}
		for (int i = 0; i < SavedLocationType.values().length; i++) {
			this.savedLocations[i] = null;
		}
		this.quests = new LinkedHashMap<>();
		this.setPosition(new Point(0, 0));
	}

	public static MapleCharacter getDefault(MapleClient c) {
		final MapleCharacter ret = new MapleCharacter();
		ret.client = c;
		ret.gmLevel = c.gmLevel();
		ret.hp = 50;
		ret.maxhp = 50;
		ret.mp = 5;
		ret.maxmp = 5;
		ret.str = 12;
		ret.dex = 5;
		ret.int_ = 4;
		ret.luk = 4;
		ret.map = null;
		ret.job = MapleJob.BEGINNER;
		ret.level = 1;
		ret.accountid = c.getAccID();
		ret.buddylist = new BuddyList(20);
		ret.maplemount = null;
		ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(24);
		ret.getInventory(MapleInventoryType.USE).setSlotLimit(24);
		ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(24);
		ret.getInventory(MapleInventoryType.ETC).setSlotLimit(24);
		for (int i = 0; i < DEFAULT_KEY.length; i++) {
			ret.keymap.put(DEFAULT_KEY[i], new MapleKeyBinding(DEFAULT_TYPE[i],
					DEFAULT_ACTION[i]));
		}
		// to fix the map 0 lol
		for (int i = 0; i < 5; i++) {
			ret.trockmaps[i] = 999999999;
		}
		for (int i = 0; i < 10; i++) {
			ret.viptrockmaps[i] = 999999999;
		}

		if (ret.isGM()) {
			ret.job = MapleJob.SUPERGM;
			ret.level = 200;
			// int[] gmskills = {9001000, 9001001, 9001000, 9101000, 9101001,
			// 9101002, 9101003, 9101004, 9101005, 9101006, 9101007, 9101008};
		}
		return ret;
	}

	public void addCooldown(int skillId, long startTime, long length,
			ScheduledFuture<?> timer) {
		if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
			this.coolDowns.remove(skillId);
		}
		this.coolDowns
				.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(
						skillId, startTime, length, timer));
	}

	public void addCrushRing(MapleRing r) {
		this.crushRings.add(r);
	}

	public MapleRing getRingById(int id) {
		for (final MapleRing ring : this.getCrushRings()) {
			if (ring.getRingId() == id) {
				return ring;
			}
		}
		for (final MapleRing ring : this.getFriendshipRings()) {
			if (ring.getRingId() == id) {
				return ring;
			}
		}
		if (this.getMarriageRing().getRingId() == id) {
			return this.getMarriageRing();
		}

		return null;
	}

	public int addDojoPointsByMap() {
		int pts = 0;
		if (this.dojoPoints < 17000) {
			pts = 1 + ((((this.getMap().getId() - 1) / 100) % 100) / 6);
			if (!this.dojoParty) {
				pts++;
			}
			this.dojoPoints += pts;
		}
		return pts;
	}

	public void addDoor(MapleDoor door) {
		this.doors.add(door);
	}

	public void addExcluded(int x) {
		this.excluded.add(x);
	}

	public void addFame(int famechange) {
		this.fame += famechange;
	}

	public void addFriendshipRing(MapleRing r) {
		this.friendshipRings.add(r);
	}

	public void addHP(int delta) {
		this.setHp(this.hp + delta);
		this.updateSingleStat(MapleStat.HP, this.hp);
	}

	public void addMesosTraded(int gain) {
		this.mesosTraded += gain;
	}

	public void addMP(int delta) {
		this.setMp(this.mp + delta);
		this.updateSingleStat(MapleStat.MP, this.mp);
	}

	public void addMPHP(int hpDiff, int mpDiff) {
		this.setHp(this.hp + hpDiff);
		this.setMp(this.mp + mpDiff);
		this.updateSingleStat(MapleStat.HP, this.getHp());
		this.updateSingleStat(MapleStat.MP, this.getMp());
	}

	public void addPet(MaplePet pet) {
		for (int i = 0; i < 3; i++) {
			if (this.pets[i] == null) {
				this.pets[i] = pet;
				return;
			}
		}
	}

	public void addStat(int type, int up) {
		if (type == 1) {
			this.str += up;
			this.updateSingleStat(MapleStat.STR, this.str);
		} else if (type == 2) {
			this.dex += up;
			this.updateSingleStat(MapleStat.DEX, this.dex);
		} else if (type == 3) {
			this.int_ += up;
			this.updateSingleStat(MapleStat.INT, this.int_);
		} else if (type == 4) {
			this.luk += up;
			this.updateSingleStat(MapleStat.LUK, this.luk);
		}
	}

	public int addHP(MapleClient c) {
		final MapleCharacter player = c.getPlayer();
		final MapleJob jobtype = player.getJob();
		int MaxHP = player.getMaxHp();
		if ((player.getHpMpApUsed() > 9999) || (MaxHP >= 30000)) {
			return MaxHP;
		}
		if (jobtype.isA(MapleJob.BEGINNER)) {
			MaxHP += 8;
		} else if (jobtype.isA(MapleJob.WARRIOR)
				|| jobtype.isA(MapleJob.DAWNWARRIOR1)) {
			if (player.getSkillLevel(player.isCygnus() ? SkillFactory
					.getSkill(10000000) : SkillFactory.getSkill(1000001)) > 0) {
				MaxHP += 20;
			} else {
				MaxHP += 8;
			}
		} else if (jobtype.isA(MapleJob.MAGICIAN)
				|| jobtype.isA(MapleJob.BLAZEWIZARD1)) {
			MaxHP += 6;
		} else if (jobtype.isA(MapleJob.BOWMAN)
				|| jobtype.isA(MapleJob.WINDARCHER1)) {
			MaxHP += 8;
		} else if (jobtype.isA(MapleJob.THIEF)
				|| jobtype.isA(MapleJob.NIGHTWALKER1)) {
			MaxHP += 8;
		} else if (jobtype.isA(MapleJob.PIRATE)
				|| jobtype.isA(MapleJob.THUNDERBREAKER1)) {
			if (player.getSkillLevel(player.isCygnus() ? SkillFactory
					.getSkill(15100000) : SkillFactory.getSkill(5100000)) > 0) {
				MaxHP += 18;
			} else {
				MaxHP += 8;
			}
		}
		return MaxHP;
	}

	public int addMP(MapleClient c) {
		final MapleCharacter player = c.getPlayer();
		int MaxMP = player.getMaxMp();
		if ((player.getHpMpApUsed() > 9999) || (player.getMaxMp() >= 30000)) {
			return MaxMP;
		}
		if (player.getJob().isA(MapleJob.BEGINNER)
				|| player.getJob().isA(MapleJob.NOBLESSE)
				|| player.getJob().isA(MapleJob.LEGEND)) {
			MaxMP += 6;
		} else if (player.getJob().isA(MapleJob.WARRIOR)
				|| player.getJob().isA(MapleJob.DAWNWARRIOR1)
				|| player.getJob().isA(MapleJob.ARAN1)) {
			MaxMP += 2;
		} else if (player.getJob().isA(MapleJob.MAGICIAN)
				|| player.getJob().isA(MapleJob.BLAZEWIZARD1)) {
			if (player.getSkillLevel(player.isCygnus() ? SkillFactory
					.getSkill(12000000) : SkillFactory.getSkill(2000001)) > 0) {
				MaxMP += 18;
			} else {
				MaxMP += 14;
			}

		} else if (player.getJob().isA(MapleJob.BOWMAN)
				|| player.getJob().isA(MapleJob.THIEF)) {
			MaxMP += 10;
		} else if (player.getJob().isA(MapleJob.PIRATE)) {
			MaxMP += 14;
		}

		return MaxMP;
	}

	public void addSummon(int id, MapleSummon summon) {
		this.summons.put(id, summon);
	}

	public void addVisibleMapObject(MapleMapObject mo) {
		this.visibleMapObjects.add(mo);
	}

	public void ban(String reason) {
		try {
			final Connection con = DatabaseConnection.getConnection();
			try (PreparedStatement ps = con
					.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
				ps.setString(1, reason);
				ps.setInt(2, this.accountid);
				ps.executeUpdate();
			}
		} catch (final Exception e) {
		}

	}

	public static boolean ban(String id, String reason, boolean accountId) {
		PreparedStatement ps = null;
		try {
			final Connection con = DatabaseConnection.getConnection();
			if (id.matches("/[0-9]{1,3}\\..*")) {
				ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
				ps.setString(1, id);
				ps.executeUpdate();
				ps.close();
				return true;
			}
			if (accountId) {
				ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
			} else {
				ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
			}

			boolean ret = false;
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					try (PreparedStatement psb = DatabaseConnection
							.getConnection()
							.prepareStatement(
									"UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
						psb.setString(1, reason);
						psb.setInt(2, rs.getInt(1));
						psb.executeUpdate();
					}
					ret = true;
				}
			}
			ps.close();
			return ret;
		} catch (final SQLException ex) {
		} finally {
			try {
				if ((ps != null) && !ps.isClosed()) {
					ps.close();
				}
			} catch (final SQLException e) {
			}
		}
		return false;
	}

	public int calculateMaxBaseDamage(int watk) {
		int maxbasedamage;
		if (watk == 0) {
			maxbasedamage = 1;
		} else {
			final Item weapon_item = this.getInventory(
					MapleInventoryType.EQUIPPED).getItem((byte) -11);
			if (weapon_item != null) {
				final MapleWeaponType weapon = MapleItemInformationProvider
						.getInstance().getWeaponType(weapon_item.getItemId());
				int mainstat;
				int secondarystat;
				if ((weapon == MapleWeaponType.BOW)
						|| (weapon == MapleWeaponType.CROSSBOW)) {
					mainstat = this.localdex;
					secondarystat = this.localstr;
				} else if ((this.getJob().isA(MapleJob.THIEF) || this.getJob()
						.isA(MapleJob.NIGHTWALKER1))
						&& ((weapon == MapleWeaponType.CLAW) || (weapon == MapleWeaponType.DAGGER))) {
					mainstat = this.localluk;
					secondarystat = this.localdex + this.localstr;
				} else {
					mainstat = this.localstr;
					secondarystat = this.localdex;
				}
				maxbasedamage = (int) ((((weapon.getMaxDamageMultiplier() * mainstat) + secondarystat) / 100.0) * watk) + 10;
			} else {
				maxbasedamage = 0;
			}
		}
		return maxbasedamage;
	}

	public void cancelAllBuffs(boolean disconnect) {
		if (disconnect) {
			this.effects.clear();
		} else {
			for (final MapleBuffStatValueHolder mbsvh : new ArrayList<>(
					this.effects.values())) {
				this.cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	public void cancelBuffStats(MapleBuffStat stat) {
		final List<MapleBuffStat> buffStatList = Arrays.asList(stat);
		this.deregisterBuffStats(buffStatList);
		this.cancelPlayerBuffs(buffStatList);
	}

	public void setCombo(short count) {
		if (count < this.combocounter) {
			this.cancelEffectFromBuffStat(MapleBuffStat.ARAN_COMBO);
		}
		this.combocounter = (short) Math.min(30000, count);
		if (count > 0) {
			this.announce(MaplePacketCreator.showCombo(this.combocounter));
		}
	}

	public void setLastCombo(long time) {
		;
		this.lastcombo = time;
	}

	public short getCombo() {
		return this.combocounter;
	}

	public long getLastCombo() {
		return this.lastcombo;
	}

	public int getLastMobCount() { // Used for skills that have mobCount at 1.
									// (a/b)
		return this.lastmobcount;
	}

	public void setLastMobCount(byte count) {
		this.lastmobcount = count;
	}

	public void newClient(MapleClient c) {
		this.loggedIn = true;
		c.setAccountName(this.client.getAccountName());// No null's for
														// accountName
		this.client = c;
		MaplePortal portal = this.map.findClosestSpawnpoint(this.getPosition());
		if (portal == null) {
			portal = this.map.getPortal(0);
		}
		this.setPosition(portal.getPosition());
		this.initialSpawnPoint = portal.getId();
		this.map = c.getChannelServer().getMapFactory().getMap(this.getMapId());
	}

	public void cancelBuffEffects() {
		for (final MapleBuffStatValueHolder mbsvh : this.effects.values()) {
			mbsvh.schedule.cancel(false);
		}
		this.effects.clear();
	}

	public String getMedalText() {
		String medal = "";
		final Item medalItem = this.getInventory(MapleInventoryType.EQUIPPED)
				.getItem((byte) -49);
		if (medalItem != null) {
			medal = "<"
					+ MapleItemInformationProvider.getInstance().getName(
							medalItem.getItemId()) + "> ";
		}
		return medal;
	}

	public static class CancelCooldownAction implements Runnable {

		private final int skillId;
		private final WeakReference<MapleCharacter> target;

		public CancelCooldownAction(MapleCharacter target, int skillId) {
			this.target = new WeakReference<>(target);
			this.skillId = skillId;
		}

		@Override
		public void run() {
			final MapleCharacter realTarget = this.target.get();
			if (realTarget != null) {
				realTarget.removeCooldown(this.skillId);
				realTarget.client.announce(MaplePacketCreator.skillCooldown(
						this.skillId, 0));
			}
		}
	}

	public void cancelEffect(int itemId) {
		this.cancelEffect(MapleItemInformationProvider.getInstance()
				.getItemEffect(itemId), false, -1);
	}

	public void cancelEffect(MapleStatEffect effect, boolean overwrite,
			long startTime) {
		List<MapleBuffStat> buffstats;
		if (!overwrite) {
			buffstats = this.getBuffStats(effect, startTime);
		} else {
			final List<Pair<MapleBuffStat, Integer>> statups = effect
					.getStatups();
			buffstats = new ArrayList<>(statups.size());
			for (final Pair<MapleBuffStat, Integer> statup : statups) {
				buffstats.add(statup.getLeft());
			}
		}
		this.deregisterBuffStats(buffstats);
		if (effect.isMagicDoor()) {
			if (!this.getDoors().isEmpty()) {
				final MapleDoor door = this.getDoors().iterator().next();
				for (final MapleCharacter chr : door.getTarget()
						.getCharacters()) {
					door.sendDestroyData(chr.client);
				}
				for (final MapleCharacter chr : door.getTown().getCharacters()) {
					door.sendDestroyData(chr.client);
				}
				for (final MapleDoor destroyDoor : this.getDoors()) {
					door.getTarget().removeMapObject(destroyDoor);
					door.getTown().removeMapObject(destroyDoor);
				}
				this.clearDoors();
				this.silentPartyUpdate();
			}
		}
		if ((effect.getSourceId() == Spearman.HYPER_BODY)
				|| (effect.getSourceId() == GM.HYPER_BODY)
				|| (effect.getSourceId() == SuperGM.HYPER_BODY)) {
			final List<Pair<MapleStat, Integer>> statup = new ArrayList<>(4);
			statup.add(new Pair<>(MapleStat.HP, Math.min(this.hp, this.maxhp)));
			statup.add(new Pair<>(MapleStat.MP, Math.min(this.mp, this.maxmp)));
			statup.add(new Pair<>(MapleStat.MAXHP, this.maxhp));
			statup.add(new Pair<>(MapleStat.MAXMP, this.maxmp));
			this.client.announce(MaplePacketCreator.updatePlayerStats(statup));
		}
		if (effect.isMonsterRiding()) {
			if (effect.getSourceId() != Corsair.BATTLE_SHIP) {
				this.getMount().cancelSchedule();
				this.getMount().setActive(false);
			}
		}
		if (!overwrite) {
			this.cancelPlayerBuffs(buffstats);
		}
	}

	public void cancelEffectFromBuffStat(MapleBuffStat stat) {
		final MapleBuffStatValueHolder effect = this.effects.get(stat);
		if (effect != null) {
			this.cancelEffect(effect.effect, false, -1);
		}
	}

	public void Hide(boolean hide, boolean login) {
		if (this.isGM() && (hide != this.hidden)) {
			if (!hide) {
				this.hidden = false;
				this.announce(MaplePacketCreator.getGMEffect(0x10, (byte) 0));
				this.getMap().broadcastMessage(this,
						MaplePacketCreator.spawnPlayerMapobject(this), false);
				this.updatePartyMemberHP();
			} else {
				this.hidden = true;
				this.announce(MaplePacketCreator.getGMEffect(0x10, (byte) 1));
				if (!login) {
					this.getMap()
							.broadcastMessage(
									this,
									MaplePacketCreator.removePlayerFromMap(this
											.getId()), false);
				}
			}
			this.announce(MaplePacketCreator.enableActions());
		}
	}

	public void Hide(boolean hide) {
		this.Hide(hide, false);
	}

	public void toggleHide(boolean login) {
		this.Hide(!this.isHidden());
	}

	private void cancelFullnessSchedule(int petSlot) {
		if (this.fullnessSchedule[petSlot] != null) {
			this.fullnessSchedule[petSlot].cancel(false);
		}
	}

	public void cancelMagicDoor() {
		for (final MapleBuffStatValueHolder mbsvh : new ArrayList<>(
				this.effects.values())) {
			if (mbsvh.effect.isMagicDoor()) {
				this.cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	public void cancelMapTimeLimitTask() {
		if (this.mapTimeLimitTask != null) {
			this.mapTimeLimitTask.cancel(false);
		}
	}

	private void cancelPlayerBuffs(List<MapleBuffStat> buffstats) {
		if (this.client.getChannelServer().getPlayerStorage()
				.getCharacterById(this.getId()) != null) {
			this.recalcLocalStats();
			this.enforceMaxHpMp();
			this.client.announce(MaplePacketCreator.cancelBuff(buffstats));
			if (buffstats.size() > 0) {
				this.getMap().broadcastMessage(
						this,
						MaplePacketCreator.cancelForeignBuff(this.getId(),
								buffstats), false);
			}
		}
	}

	public static boolean canCreateChar(String name) {
		if ((name.length() < 4) || (name.length() > 12)) {
			return false;
		}

		if (isInUse(name)) {
			return false;
		}

		return (getIdByName(name) < 0)
				&& !name.toLowerCase().contains("gm")
				&& Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name)
						.matches();
	}

	public boolean canDoor() {
		return this.canDoor;
	}

	public FameStatus canGiveFame(MapleCharacter from) {
		if (this.gmLevel > 0) {
			return FameStatus.OK;
		} else if (this.lastfametime >= (System.currentTimeMillis() - (3600000 * 24))) {
			return FameStatus.NOT_TODAY;
		} else if (this.lastmonthfameids
				.contains(Integer.valueOf(from.getId()))) {
			return FameStatus.NOT_THIS_MONTH;
		} else {
			return FameStatus.OK;
		}
	}

	public void changeCI(int type) {
		this.ci = type;
	}

	public void changeJob(MapleJob newJob) {
		if (newJob == null) {
			return;// the fuck you doing idiot!
		}
		this.job = newJob;
		this.remainingSp++;
		if ((newJob.getId() % 10) == 2) {
			this.remainingSp += 2;
		}
		if ((newJob.getId() % 10) > 1) {
			this.remainingAp += 5;
		}
		final int job_ = this.job.getId() % 1000; // lame temp "fix"
		if (job_ == 100) {
			this.maxhp += Randomizer.rand(200, 250);
		} else if (job_ == 200) {
			this.maxmp += Randomizer.rand(100, 150);
		} else if ((job_ % 100) == 0) {
			this.maxhp += Randomizer.rand(100, 150);
			this.maxhp += Randomizer.rand(25, 50);
		} else if ((job_ > 0) && (job_ < 200)) {
			this.maxhp += Randomizer.rand(300, 350);
		} else if (job_ < 300) {
			this.maxmp += Randomizer.rand(450, 500);
		} // handle KoC here (undone)
		else if ((job_ > 0) && (job_ != 1000)) {
			this.maxhp += Randomizer.rand(300, 350);
			this.maxmp += Randomizer.rand(150, 200);
		}
		if (this.maxhp >= 30000) {
			this.maxhp = 30000;
		}
		if (this.maxmp >= 30000) {
			this.maxmp = 30000;
		}
		if (!this.isGM()) {
			for (byte i = 1; i < 5; i++) {
				this.gainSlots(i, 4, true);
			}
		}
		final List<Pair<MapleStat, Integer>> statup = new ArrayList<>(5);
		statup.add(new Pair<>(MapleStat.MAXHP, Integer.valueOf(this.maxhp)));
		statup.add(new Pair<>(MapleStat.MAXMP, Integer.valueOf(this.maxmp)));
		statup.add(new Pair<>(MapleStat.AVAILABLEAP, this.remainingAp));
		statup.add(new Pair<>(MapleStat.AVAILABLESP, this.remainingSp));
		statup.add(new Pair<>(MapleStat.JOB, Integer.valueOf(this.job.getId())));
		this.recalcLocalStats();
		this.client.announce(MaplePacketCreator.updatePlayerStats(statup));
		this.silentPartyUpdate();
		if (this.guildid > 0) {
			this.getGuild().broadcast(
					MaplePacketCreator.jobMessage(0, this.job.getId(),
							this.name), this.getId());
		}
		this.guildUpdate();
		this.getMap().broadcastMessage(this,
				MaplePacketCreator.showForeignEffect(this.getId(), 8), false);
	}

	public void changeKeybinding(int key, MapleKeyBinding keybinding) {
		if (keybinding.getType() != 0) {
			this.keymap.put(Integer.valueOf(key), keybinding);
		} else {
			this.keymap.remove(Integer.valueOf(key));
		}
	}

	public void changeMap(int map) {
		this.changeMap(map, 0);
	}

	public void changeMap(int map, int portal) {
		final MapleMap warpMap = this.client.getChannelServer().getMapFactory()
				.getMap(map);
		this.changeMap(warpMap, warpMap.getPortal(portal));
	}

	public void changeMap(int map, String portal) {
		final MapleMap warpMap = this.client.getChannelServer().getMapFactory()
				.getMap(map);
		this.changeMap(warpMap, warpMap.getPortal(portal));
	}

	public void changeMap(int map, MaplePortal portal) {
		final MapleMap warpMap = this.client.getChannelServer().getMapFactory()
				.getMap(map);
		this.changeMap(warpMap, portal);
	}

	public void changeMap(MapleMap to) {
		this.changeMap(to, to.getPortal(0));
	}

	public void changeMap(final MapleMap to, final MaplePortal pto) {
		this.changeMapInternal(to, pto.getPosition(),
				MaplePacketCreator.getWarpToMap(to, pto.getId(), this));
	}

	public void changeMap(final MapleMap to, final Point pos) {
		this.changeMapInternal(to, pos,
				MaplePacketCreator.getWarpToMap(to, 0x80, this));// Position :O
																	// (LEFT)
	}

	public void changeMapBanish(int mapid, String portal, String msg) {
		this.dropMessage(5, msg);
		final MapleMap map_ = this.client.getChannelServer().getMapFactory()
				.getMap(mapid);
		this.changeMap(map_, map_.getPortal(portal));
	}

	private void changeMapInternal(final MapleMap to, final Point pos,
			final byte[] warpPacket) {
		this.client.announce(warpPacket);
		this.map.removePlayer(MapleCharacter.this);
		if (this.client.getChannelServer().getPlayerStorage()
				.getCharacterById(this.getId()) != null) {
			this.map = to;
			this.setPosition(pos);
			this.map.addPlayer(MapleCharacter.this);
			if (this.party != null) {
				this.mpc.setMapId(to.getId());
				this.silentPartyUpdate();
				this.client.announce(MaplePacketCreator.updateParty(
						this.client.getChannel(), this.party,
						PartyOperation.SILENT_UPDATE, null));
				this.updatePartyMemberHP();
			}
			if (this.getMap().getHPDec() > 0) {
				this.hpDecreaseTask = TimerManager.getInstance().schedule(
						new Runnable() {
							@Override
							public void run() {
								MapleCharacter.this.doHurtHp();
							}
						}, 10000);
			}
		}
	}

	public void changePage(int page) {
		this.currentPage = page;
	}

	public void changeSkillLevel(Skill skill, byte newLevel,
			int newMasterlevel, long expiration) {
		if (newLevel > -1) {
			this.skills.put(skill, new SkillEntry(newLevel, newMasterlevel,
					expiration));
			this.client.announce(MaplePacketCreator.updateSkill(skill.getId(),
					newLevel, newMasterlevel, expiration));
		} else {
			this.skills.remove(skill);
			this.client.announce(MaplePacketCreator.updateSkill(skill.getId(),
					newLevel, newMasterlevel, -1)); // Shouldn't use expiration
													// anymore :)
			try {
				final Connection con = DatabaseConnection.getConnection();
				try (PreparedStatement ps = con
						.prepareStatement("DELETE FROM skills WHERE skillid = ? AND characterid = ?")) {
					ps.setInt(1, skill.getId());
					ps.setInt(2, this.id);
					ps.execute();
				}
			} catch (final SQLException ex) {
				System.out.print("Error deleting skill: " + ex);
			}
		}
	}

	public void changeTab(int tab) {
		this.currentTab = tab;
	}

	public void changeType(int type) {
		this.currentType = type;
	}

	public void checkBerserk() {
		if (this.BerserkSchedule != null) {
			this.BerserkSchedule.cancel(false);
		}
		final MapleCharacter chr = this;
		if (this.job.equals(MapleJob.DARKKNIGHT)) {
			final Skill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
			final int skilllevel = this.getSkillLevel(BerserkX);
			if (skilllevel > 0) {
				this.Berserk = ((chr.getHp() * 100) / chr.getMaxHp()) < BerserkX
						.getEffect(skilllevel).getX();
				this.BerserkSchedule = TimerManager.getInstance().register(
						new Runnable() {
							@Override
							public void run() {
								MapleCharacter.this.client.announce(MaplePacketCreator
										.showOwnBerserk(skilllevel,
												MapleCharacter.this.Berserk));
								MapleCharacter.this.getMap().broadcastMessage(
										MapleCharacter.this,
										MaplePacketCreator.showBerserk(
												MapleCharacter.this.getId(),
												skilllevel,
												MapleCharacter.this.Berserk),
										false);
							}
						}, 5000, 3000);
			}
		}
	}

	public void checkMessenger() {
		if ((this.messenger != null) && (this.messengerposition < 4)
				&& (this.messengerposition > -1)) {
			final World worldz = Server.getInstance().getWorld(this.world);
			worldz.silentJoinMessenger(this.messenger.getId(),
					new MapleMessengerCharacter(this, this.messengerposition),
					this.messengerposition);
			worldz.updateMessenger(this.getMessenger().getId(), this.name,
					this.client.getChannel());
		}
	}

	public void checkMonsterAggro(MapleMonster monster) {
		if (!monster.isControllerHasAggro()) {
			if (monster.getController() == this) {
				monster.setControllerHasAggro(true);
			} else {
				monster.switchController(this, true);
			}
		}
	}

	public void clearDoors() {
		this.doors.clear();
	}

	public void clearSavedLocation(SavedLocationType type) {
		this.savedLocations[type.ordinal()] = null;
	}

	public void controlMonster(MapleMonster monster, boolean aggro) {
		monster.setController(this);
		this.controlled.add(monster);
		this.client.announce(MaplePacketCreator.controlMonster(monster, false,
				aggro));
	}

	public int countItem(int itemid) {
		return this.inventory[MapleItemInformationProvider.getInstance()
				.getInventoryType(itemid).ordinal()].countById(itemid);
	}

	public void decreaseBattleshipHp(int decrease) {
		this.battleshipHp -= decrease;
		if (this.battleshipHp <= 0) {
			this.battleshipHp = 0;
			final Skill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
			final int cooldown = battleship.getEffect(
					this.getSkillLevel(battleship)).getCooldown();
			this.announce(MaplePacketCreator.skillCooldown(Corsair.BATTLE_SHIP,
					cooldown));
			this.addCooldown(
					Corsair.BATTLE_SHIP,
					System.currentTimeMillis(),
					cooldown,
					TimerManager.getInstance()
							.schedule(
									new CancelCooldownAction(this,
											Corsair.BATTLE_SHIP),
									cooldown * 1000));
			this.removeCooldown(5221999);
			this.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
		} else {
			this.announce(MaplePacketCreator.skillCooldown(5221999,
					this.battleshipHp / 10)); // :D
			this.addCooldown(5221999, 0, this.battleshipHp, null);
		}
	}

	public void decreaseReports() {
		this.possibleReports--;
	}

	public void deleteGuild(int guildId) {
		try {
			final Connection con = DatabaseConnection.getConnection();
			try (PreparedStatement ps = con
					.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?")) {
				ps.setInt(1, guildId);
				ps.execute();
			}
			try (PreparedStatement ps = con
					.prepareStatement("DELETE FROM guilds WHERE guildid = ?")) {
				ps.setInt(1, this.id);
				ps.execute();
			}
		} catch (final SQLException ex) {
			System.out.print("Error deleting guild: " + ex);
		}
	}

	private void deleteWhereCharacterId(Connection con, String sql)
			throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, this.id);
			ps.executeUpdate();
		}
	}

	public static void deleteWhereCharacterId(Connection con, String sql,
			int cid) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, cid);
			ps.executeUpdate();
		}
	}

	private void deregisterBuffStats(List<MapleBuffStat> stats) {
		synchronized (stats) {
			final List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<>(
					stats.size());
			for (final MapleBuffStat stat : stats) {
				final MapleBuffStatValueHolder mbsvh = this.effects.get(stat);
				if (mbsvh != null) {
					this.effects.remove(stat);
					boolean addMbsvh = true;
					for (final MapleBuffStatValueHolder contained : effectsToCancel) {
						if ((mbsvh.startTime == contained.startTime)
								&& (contained.effect == mbsvh.effect)) {
							addMbsvh = false;
						}
					}
					if (addMbsvh) {
						effectsToCancel.add(mbsvh);
					}
					if (stat == MapleBuffStat.RECOVERY) {
						if (this.recoveryTask != null) {
							this.recoveryTask.cancel(false);
							this.recoveryTask = null;
						}
					} else if ((stat == MapleBuffStat.SUMMON)
							|| (stat == MapleBuffStat.PUPPET)) {
						final int summonId = mbsvh.effect.getSourceId();
						final MapleSummon summon = this.summons.get(summonId);
						if (summon != null) {
							this.getMap().broadcastMessage(
									MaplePacketCreator.removeSummon(summon,
											true), summon.getPosition());
							this.getMap().removeMapObject(summon);
							this.removeVisibleMapObject(summon);
							this.summons.remove(summonId);
						}
						if (summon.getSkill() == DarkKnight.BEHOLDER) {
							if (this.beholderHealingSchedule != null) {
								this.beholderHealingSchedule.cancel(false);
								this.beholderHealingSchedule = null;
							}
							if (this.beholderBuffSchedule != null) {
								this.beholderBuffSchedule.cancel(false);
								this.beholderBuffSchedule = null;
							}
						}
					} else if (stat == MapleBuffStat.DRAGONBLOOD) {
						this.dragonBloodSchedule.cancel(false);
						this.dragonBloodSchedule = null;
					}
				}
			}
			for (final MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
				if (cancelEffectCancelTasks.schedule != null) {
					cancelEffectCancelTasks.schedule.cancel(false);
				}
			}
		}
	}

	public void disableDoor() {
		this.canDoor = false;
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleCharacter.this.canDoor = true;
			}
		}, 5000);
	}

	public void disbandGuild() {
		if ((this.guildid < 1) || (this.guildrank != 1)) {
			return;
		}
		try {
			Server.getInstance().disbandGuild(this.guildid);
		} catch (final Exception e) {
		}
	}

	public void dispel() {
		for (final MapleBuffStatValueHolder mbsvh : new ArrayList<>(
				this.effects.values())) {
			if (mbsvh.effect.isSkill()) {
				this.cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	public final List<PlayerDiseaseValueHolder> getAllDiseases() {
		final List<PlayerDiseaseValueHolder> ret = new ArrayList<>(5);

		DiseaseValueHolder vh;
		for (final Entry<MapleDisease, DiseaseValueHolder> disease : this.diseases
				.entrySet()) {
			vh = disease.getValue();
			ret.add(new PlayerDiseaseValueHolder(disease.getKey(),
					vh.startTime, vh.length));
		}
		return ret;
	}

	public final boolean hasDisease(final MapleDisease dis) {
		for (final MapleDisease disease : this.diseases.keySet()) {
			if (disease == dis) {
				return true;
			}
		}
		return false;
	}

	public void giveDebuff(final MapleDisease disease, MobSkill skill) {
		final List<Pair<MapleDisease, Integer>> debuff = Collections
				.singletonList(new Pair<>(disease,
						Integer.valueOf(skill.getX())));

		if (!this.hasDisease(disease) && (this.diseases.size() < 2)) {
			if (!((disease == MapleDisease.SEDUCE) || (disease == MapleDisease.STUN))) {
				if (this.isActiveBuffedValue(2321005)) {
					return;
				}
			}
			TimerManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					MapleCharacter.this.dispelDebuff(disease);
				}
			}, skill.getDuration());

			this.diseases.put(
					disease,
					new DiseaseValueHolder(System.currentTimeMillis(), skill
							.getDuration()));
			this.client.announce(MaplePacketCreator.giveDebuff(debuff, skill));
			this.map.broadcastMessage(this, MaplePacketCreator
					.giveForeignDebuff(this.id, debuff, skill), false);
		}
	}

	public void dispelDebuff(MapleDisease debuff) {
		if (this.hasDisease(debuff)) {
			final long mask = debuff.getValue();
			this.announce(MaplePacketCreator.cancelDebuff(mask));
			this.map.broadcastMessage(this,
					MaplePacketCreator.cancelForeignDebuff(this.id, mask),
					false);

			this.diseases.remove(debuff);
		}
	}

	public void dispelDebuffs() {
		this.dispelDebuff(MapleDisease.CURSE);
		this.dispelDebuff(MapleDisease.DARKNESS);
		this.dispelDebuff(MapleDisease.POISON);
		this.dispelDebuff(MapleDisease.SEAL);
		this.dispelDebuff(MapleDisease.WEAKEN);
	}

	public void cancelAllDebuffs() {
		this.diseases.clear();
	}

	public void dispelSkill(int skillid) {
		final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(
				this.effects.values());
		for (final MapleBuffStatValueHolder mbsvh : allBuffs) {
			if (skillid == 0) {
				if (mbsvh.effect.isSkill()
						&& (((mbsvh.effect.getSourceId() % 10000000) == 1004) || this
								.dispelSkills(mbsvh.effect.getSourceId()))) {
					this.cancelEffect(mbsvh.effect, false, mbsvh.startTime);
				}
			} else if (mbsvh.effect.isSkill()
					&& (mbsvh.effect.getSourceId() == skillid)) {
				this.cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	private boolean dispelSkills(int skillid) {
		switch (skillid) {
		case DarkKnight.BEHOLDER:
		case FPArchMage.ELQUINES:
		case ILArchMage.IFRIT:
		case Priest.SUMMON_DRAGON:
		case Bishop.BAHAMUT:
		case Ranger.PUPPET:
		case Ranger.SILVER_HAWK:
		case Sniper.PUPPET:
		case Sniper.GOLDEN_EAGLE:
		case Hermit.SHADOW_PARTNER:
			return true;
		default:
			return false;
		}
	}

	public void doHurtHp() {
		if (this.getInventory(MapleInventoryType.EQUIPPED).findById(
				this.getMap().getHPDecProtect()) != null) {
			return;
		}
		this.addHP(-this.getMap().getHPDec());
		this.hpDecreaseTask = TimerManager.getInstance().schedule(
				new Runnable() {
					@Override
					public void run() {
						MapleCharacter.this.doHurtHp();
					}
				}, 10000);
	}

	public void dropMessage(String message) {
		this.dropMessage(0, message);
	}

	public void dropMessage(int type, String message) {
		this.client.announce(MaplePacketCreator.serverNotice(type, message));
	}

	public String emblemCost() {
		return this.nf.format(MapleGuild.CHANGE_EMBLEM_COST);
	}

	public List<ScheduledFuture<?>> getTimers() {
		return this.timers;
	}

	private void enforceMaxHpMp() {
		final List<Pair<MapleStat, Integer>> stats = new ArrayList<>(2);
		if (this.getMp() > this.getCurrentMaxMp()) {
			this.setMp(this.getMp());
			stats.add(new Pair<>(MapleStat.MP, Integer.valueOf(this.getMp())));
		}
		if (this.getHp() > this.getCurrentMaxHp()) {
			this.setHp(this.getHp());
			stats.add(new Pair<>(MapleStat.HP, Integer.valueOf(this.getHp())));
		}
		if (stats.size() > 0) {
			this.client.announce(MaplePacketCreator.updatePlayerStats(stats));
		}
	}

	public void enteredScript(String script, int mapid) {
		if (!this.entered.containsKey(mapid)) {
			this.entered.put(mapid, script);
		}
	}

	public void equipChanged() {
		this.getMap().broadcastMessage(this,
				MaplePacketCreator.updateCharLook(this), false);
		this.recalcLocalStats();
		this.enforceMaxHpMp();
		if (this.getMessenger() != null) {
			Server.getInstance()
					.getWorld(this.world)
					.updateMessenger(this.getMessenger(), this.getName(),
							this.getWorld(), this.client.getChannel());
		}
	}

	public void cancelExpirationTask() {
		if (this.expiretask != null) {
			this.expiretask.cancel(false);
			this.expiretask = null;
		}
	}

	public void expirationTask() {
		if (this.expiretask == null) {
			this.expiretask = TimerManager.getInstance().register(
					new Runnable() {
						@Override
						public void run() {
							long expiration;
							final long currenttime = System.currentTimeMillis();
							final Set<Skill> keys = MapleCharacter.this
									.getSkills().keySet();
							for (final Skill key : keys) {
								final SkillEntry skill = MapleCharacter.this
										.getSkills().get(key);
								if ((skill.expiration != -1)
										&& (skill.expiration < currenttime)) {
									MapleCharacter.this.changeSkillLevel(key,
											(byte) -1, 0, -1);
								}
							}

							final List<Item> toberemove = new ArrayList<>();
							for (final MapleInventory inv : MapleCharacter.this.inventory) {
								for (final Item item : inv.list()) {
									expiration = item.getExpiration();
									if ((expiration != -1)
											&& (expiration < currenttime)
											&& ((item.getFlag() & ItemConstants.LOCK) == ItemConstants.LOCK)) {
										byte aids = item.getFlag();
										aids &= ~(ItemConstants.LOCK);
										item.setFlag(aids); // Probably need a
															// check,
															// else people can
															// make
															// expiring items
															// into
															// permanent
															// items...
										item.setExpiration(-1);
										MapleCharacter.this
												.forceUpdateItem(item); // TEST
																		// :3
									} else if ((expiration != -1)
											&& (expiration < currenttime)) {
										MapleCharacter.this.client
												.announce(MaplePacketCreator
														.itemExpired(item
																.getItemId()));
										toberemove.add(item);
									}
								}
								for (final Item item : toberemove) {
									MapleInventoryManipulator.removeFromSlot(
											MapleCharacter.this.client,
											inv.getType(), item.getPosition(),
											item.getQuantity(), true);
								}
								toberemove.clear();
							}
						}
					}, 60000);
		}
	}

	public enum FameStatus {

		OK, NOT_TODAY, NOT_THIS_MONTH
	}

	public void forceUpdateItem(Item item) {
		final List<ModifyInventory> mods = new LinkedList<>();
		mods.add(new ModifyInventory(3, item));
		mods.add(new ModifyInventory(0, item));
		this.client.announce(MaplePacketCreator.modifyInventory(true, mods));
	}

	public void gainGachaExp() {
		int expgain = 0;
		final int currentgexp = this.gachaexp.get();
		if ((currentgexp + this.exp.get()) >= ExpTable
				.getExpNeededForLevel(this.level)) {
			expgain += ExpTable.getExpNeededForLevel(this.level)
					- this.exp.get();
			final int nextneed = ExpTable.getExpNeededForLevel(this.level + 1);
			if ((currentgexp - expgain) >= nextneed) {
				expgain += nextneed;
			}
			this.gachaexp.set(currentgexp - expgain);
		} else {
			expgain = this.gachaexp.getAndSet(0);
		}
		this.gainExp(expgain, false, false);
		this.updateSingleStat(MapleStat.GACHAEXP, this.gachaexp.get());
	}

	public void gainGachaExp(int gain) {
		this.updateSingleStat(MapleStat.GACHAEXP, this.gachaexp.addAndGet(gain));
	}

	public void gainExp(int gain, boolean show, boolean inChat) {
		this.gainExp(gain, show, inChat, true);
	}

	public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
		final int equip = (gain / 10) * this.pendantExp;
		int total = gain + equip;

		if (this.level < this.getMaxLevel()) {
			if (((long) this.exp.get() + (long) total) > Integer.MAX_VALUE) {
				final int gainFirst = ExpTable.getExpNeededForLevel(this.level)
						- this.exp.get();
				total -= gainFirst + 1;
				this.gainExp(gainFirst + 1, false, inChat, white);
			}
			this.updateSingleStat(MapleStat.EXP, this.exp.addAndGet(total));
			if (show && (gain != 0)) {
				this.client.announce(MaplePacketCreator.getShowExpGain(gain,
						equip, inChat, white));
			}
			if (this.exp.get() >= ExpTable.getExpNeededForLevel(this.level)) {
				this.levelUp(true);
				final int need = ExpTable.getExpNeededForLevel(this.level);
				if (this.exp.get() >= need) {
					this.setExp(need - 1);
					this.updateSingleStat(MapleStat.EXP, need);
				}
			}
		}
	}

	public void gainFame(int delta) {
		this.addFame(delta);
		this.updateSingleStat(MapleStat.FAME, this.fame);
	}

	public void gainMeso(int gain, boolean show) {
		this.gainMeso(gain, show, false, false);
	}

	public void gainMeso(int gain, boolean show, boolean enableActions,
			boolean inChat) {
		if ((this.meso.get() + gain) < 0) {
			this.client.announce(MaplePacketCreator.enableActions());
			return;
		}
		this.updateSingleStat(MapleStat.MESO, this.meso.addAndGet(gain),
				enableActions);
		if (show) {
			this.client.announce(MaplePacketCreator.getShowMesoGain(gain,
					inChat));
		}
	}

	public void genericGuildMessage(int code) {
		this.client.announce(MaplePacketCreator
				.genericGuildMessage((byte) code));
	}

	public int getAccountID() {
		return this.accountid;
	}

	public List<PlayerBuffValueHolder> getAllBuffs() {
		final List<PlayerBuffValueHolder> ret = new ArrayList<>();
		for (final MapleBuffStatValueHolder mbsvh : this.effects.values()) {
			ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
		}
		return ret;
	}

	public List<PlayerCoolDownValueHolder> getAllCooldowns() {
		final List<PlayerCoolDownValueHolder> ret = new ArrayList<>();
		for (final MapleCoolDownValueHolder mcdvh : this.coolDowns.values()) {
			ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId,
					mcdvh.startTime, mcdvh.length));
		}
		return ret;
	}

	public int getAllianceRank() {
		return this.allianceRank;
	}

	public int getAllowWarpToId() {
		return this.warpToId;
	}

	public static String getAriantRoomLeaderName(int room) {
		return ariantroomleader[room];
	}

	public static int getAriantSlotsRoom(int room) {
		return ariantroomslot[room];
	}

	public int getBattleshipHp() {
		return this.battleshipHp;
	}

	public BuddyList getBuddylist() {
		return this.buddylist;
	}

	public static Map<String, String> getCharacterFromDatabase(String name) {
		final Map<String, String> character = new LinkedHashMap<>();

		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT `id`, `accountid`, `name` FROM `characters` WHERE `name` = ?")) {
				ps.setString(1, name);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						rs.close();
						ps.close();
						return null;
					}

					for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
						character.put(rs.getMetaData().getColumnLabel(i),
								rs.getString(i));
					}
				}
			}
		} catch (final SQLException sqle) {
			sqle.printStackTrace();
		}

		return character;
	}

	public static boolean isInUse(String name) {
		return getCharacterFromDatabase(name) != null;
	}

	public Long getBuffedStarttime(MapleBuffStat effect) {
		final MapleBuffStatValueHolder mbsvh = this.effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return Long.valueOf(mbsvh.startTime);
	}

	public Integer getBuffedValue(MapleBuffStat effect) {
		final MapleBuffStatValueHolder mbsvh = this.effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return Integer.valueOf(mbsvh.value);
	}

	public int getBuffSource(MapleBuffStat stat) {
		final MapleBuffStatValueHolder mbsvh = this.effects.get(stat);
		if (mbsvh == null) {
			return -1;
		}
		return mbsvh.effect.getSourceId();
	}

	private List<MapleBuffStat> getBuffStats(MapleStatEffect effect,
			long startTime) {
		final List<MapleBuffStat> stats = new ArrayList<>();
		for (final Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : this.effects
				.entrySet()) {
			if (stateffect.getValue().effect.sameSource(effect)
					&& ((startTime == -1) || (startTime == stateffect
							.getValue().startTime))) {
				stats.add(stateffect.getKey());
			}
		}
		return stats;
	}

	public int getChair() {
		return this.chair;
	}

	public String getChalkboard() {
		return this.chalktext;
	}

	public MapleClient getClient() {
		return this.client;
	}

	public final List<MapleQuestStatus> getCompletedQuests() {
		final List<MapleQuestStatus> ret = new LinkedList<>();
		for (final MapleQuestStatus q : this.quests.values()) {
			if (q.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public Collection<MapleMonster> getControlledMonsters() {
		return Collections.unmodifiableCollection(this.controlled);
	}

	public List<MapleRing> getCrushRings() {
		Collections.sort(this.crushRings);
		return this.crushRings;
	}

	public int getCurrentCI() {
		return this.ci;
	}

	public int getCurrentPage() {
		return this.currentPage;
	}

	public int getCurrentMaxHp() {
		return this.localmaxhp;
	}

	public int getCurrentMaxMp() {
		return this.localmaxmp;
	}

	public int getCurrentTab() {
		return this.currentTab;
	}

	public int getCurrentType() {
		return this.currentType;
	}

	public int getDex() {
		return this.dex;
	}

	public int getDojoEnergy() {
		return this.dojoEnergy;
	}

	public boolean getDojoParty() {
		return this.dojoParty;
	}

	public int getDojoPoints() {
		return this.dojoPoints;
	}

	public int getDojoStage() {
		return this.dojoStage;
	}

	public List<MapleDoor> getDoors() {
		return new ArrayList<>(this.doors);
	}

	public int getDropRate() {
		return this.dropRate;
	}

	public int getEnergyBar() {
		return this.energybar;
	}

	public EventInstanceManager getEventInstance() {
		return this.eventInstance;
	}

	public ArrayList<Integer> getExcluded() {
		return this.excluded;
	}

	public int getExp() {
		return this.exp.get();
	}

	public int getGachaExp() {
		return this.gachaexp.get();
	}

	public int getExpRate() {
		return this.expRate;
	}

	public int getFace() {
		return this.face;
	}

	public int getFame() {
		return this.fame;
	}

	public MapleFamily getFamily() {
		return this.family;
	}

	public void setFamily(MapleFamily f) {
		this.family = f;
	}

	public int getFamilyId() {
		return this.familyId;
	}

	public boolean getFinishedDojoTutorial() {
		return this.finishedDojoTutorial;
	}

	public List<MapleRing> getFriendshipRings() {
		Collections.sort(this.friendshipRings);
		return this.friendshipRings;
	}

	public int getGender() {
		return this.gender;
	}

	public boolean isMale() {
		return this.getGender() == 0;
	}

	public MapleGuild getGuild() {
		try {
			return Server.getInstance().getGuild(this.getGuildId(), null);
		} catch (final Exception ex) {
			return null;
		}
	}

	public int getGuildId() {
		return this.guildid;
	}

	public int getGuildRank() {
		return this.guildrank;
	}

	public int getHair() {
		return this.hair;
	}

	public HiredMerchant getHiredMerchant() {
		return this.hiredMerchant;
	}

	public int getHp() {
		return this.hp;
	}

	public int getHpMpApUsed() {
		return this.hpMpApUsed;
	}

	public int getId() {
		return this.id;
	}

	public static int getIdByName(String name) {
		try {
			int id;
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"SELECT id FROM characters WHERE name = ?")) {
				ps.setString(1, name);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						rs.close();
						ps.close();
						return -1;
					}
					id = rs.getInt("id");
				}
			}
			return id;
		} catch (final Exception e) {
		}
		return -1;
	}

	public static String getNameById(int id) {
		try {
			String name;
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"SELECT name FROM characters WHERE id = ?")) {
				ps.setInt(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						rs.close();
						ps.close();
						return null;
					}
					name = rs.getString("name");
				}
			}
			return name;
		} catch (final Exception e) {
		}
		return null;
	}

	public int getInitialSpawnpoint() {
		return this.initialSpawnPoint;
	}

	public int getInt() {
		return this.int_;
	}

	public MapleInventory getInventory(MapleInventoryType type) {
		return this.inventory[type.ordinal()];
	}

	public int getItemEffect() {
		return this.itemEffect;
	}

	public int getItemQuantity(int itemid, boolean checkEquipped) {
		int possesed = this.inventory[MapleItemInformationProvider
				.getInstance().getInventoryType(itemid).ordinal()]
				.countById(itemid);
		if (checkEquipped) {
			possesed += this.inventory[MapleInventoryType.EQUIPPED.ordinal()]
					.countById(itemid);
		}
		return possesed;
	}

	public MapleJob getJob() {
		return this.job;
	}

	public int getJobRank() {
		return this.jobRank;
	}

	public int getJobRankMove() {
		return this.jobRankMove;
	}

	public int getJobType() {
		return this.job.getId() / 1000;
	}

	public Map<Integer, MapleKeyBinding> getKeymap() {
		return this.keymap;
	}

	public long getLastHealed() {
		return this.lastHealed;
	}

	public long getLastUsedCashItem() {
		return this.lastUsedCashItem;
	}

	public int getLevel() {
		return this.level;
	}

	public int getLuk() {
		return this.luk;
	}

	public int getFh() {
		if (this.getMap().getFootholds().findBelow(this.getPosition()) == null) {
			return 0;
		} else {
			return this.getMap().getFootholds().findBelow(this.getPosition())
					.getId();
		}
	}

	public MapleMap getMap() {
		return this.map;
	}

	public int getMapId() {
		if (this.map != null) {
			return this.map.getId();
		}
		return this.mapid;
	}

	public int getMarkedMonster() {
		return this.markedMonster;
	}

	public MapleRing getMarriageRing() {
		return this.marriageRing;
	}

	public int getMarried() {
		return this.married;
	}

	public int getMasterLevel(Skill skill) {
		if (this.skills.get(skill) == null) {
			return 0;
		}
		return this.skills.get(skill).masterlevel;
	}

	public int getMaxHp() {
		return this.maxhp;
	}

	public int getMaxLevel() {
		return this.isCygnus() ? 120 : 200;
	}

	public int getMaxMp() {
		return this.maxmp;
	}

	public int getMeso() {
		return this.meso.get();
	}

	public int getMerchantMeso() {
		return this.merchantmeso;
	}

	public int getMesoRate() {
		return this.mesoRate;
	}

	public int getMesosTraded() {
		return this.mesosTraded;
	}

	public int getMessengerPosition() {
		return this.messengerposition;
	}

	public MapleGuildCharacter getMGC() {
		return this.mgc;
	}

	public MaplePartyCharacter getMPC() {
		// if (mpc == null) mpc = new MaplePartyCharacter(this);
		return this.mpc;
	}

	public void setMPC(MaplePartyCharacter mpc) {
		this.mpc = mpc;
	}

	public MapleMiniGame getMiniGame() {
		return this.miniGame;
	}

	public int getMiniGamePoints(String type, boolean omok) {
		if (omok) {
			switch (type) {
			case "wins":
				return this.omokwins;
			case "losses":
				return this.omoklosses;
			default:
				return this.omokties;
			}
		} else {
			switch (type) {
			case "wins":
				return this.matchcardwins;
			case "losses":
				return this.matchcardlosses;
			default:
				return this.matchcardties;
			}
		}
	}

	public MonsterBook getMonsterBook() {
		return this.monsterbook;
	}

	public int getMonsterBookCover() {
		return this.bookCover;
	}

	public MapleMount getMount() {
		return this.maplemount;
	}

	public int getMp() {
		return this.mp;
	}

	public MapleMessenger getMessenger() {
		return this.messenger;
	}

	public String getName() {
		return this.name;
	}

	public int getNextEmptyPetIndex() {
		for (int i = 0; i < 3; i++) {
			if (this.pets[i] == null) {
				return i;
			}
		}
		return 3;
	}

	public int getNoPets() {
		int ret = 0;
		for (int i = 0; i < 3; i++) {
			if (this.pets[i] != null) {
				ret++;
			}
		}
		return ret;
	}

	public int getNumControlledMonsters() {
		return this.controlled.size();
	}

	public MapleParty getParty() {
		return this.party;
	}

	public int getPartyId() {
		return (this.party != null ? this.party.getId() : -1);
	}

	public MaplePlayerShop getPlayerShop() {
		return this.playerShop;
	}

	public MaplePet[] getPets() {
		return this.pets;
	}

	public MaplePet getPet(int index) {
		return this.pets[index];
	}

	public byte getPetIndex(int petId) {
		for (byte i = 0; i < 3; i++) {
			if (this.pets[i] != null) {
				if (this.pets[i].getUniqueId() == petId) {
					return i;
				}
			}
		}
		return -1;
	}

	public byte getPetIndex(MaplePet pet) {
		for (byte i = 0; i < 3; i++) {
			if (this.pets[i] != null) {
				if (this.pets[i].getUniqueId() == pet.getUniqueId()) {
					return i;
				}
			}
		}
		return -1;
	}

	public int getPossibleReports() {
		return this.possibleReports;
	}

	public final byte getQuestStatus(final int quest) {
		for (final MapleQuestStatus q : this.quests.values()) {
			if (q.getQuest().getId() == quest) {
				return (byte) q.getStatus().getId();
			}
		}
		return 0;
	}

	public MapleQuestStatus getQuest(MapleQuest quest) {
		if (!this.quests.containsKey(quest)) {
			return new MapleQuestStatus(quest,
					MapleQuestStatus.Status.NOT_STARTED);
		}
		return this.quests.get(quest);
	}

	public boolean needQuestItem(int questid, int itemid) {
		if (questid <= 0) {
			return true; // For non quest items :3
		}
		final MapleQuest quest = MapleQuest.getInstance(questid);
		return this.getInventory(ItemConstants.getInventoryType(itemid))
				.countById(itemid) < quest.getItemAmountNeeded(itemid);
	}

	public int getRank() {
		return this.rank;
	}

	public int getRankMove() {
		return this.rankMove;
	}

	public int getRemainingAp() {
		return this.remainingAp;
	}

	public int getRemainingSp() {
		return this.remainingSp;
	}

	public int getSavedLocation(String type) {
		final SavedLocation sl = this.savedLocations[SavedLocationType
				.fromString(type).ordinal()];
		if (sl == null) {
			return 102000000;
		}
		final int m = sl.getMapId();
		if (!SavedLocationType.fromString(type).equals(
				SavedLocationType.WORLDTOUR)) {
			this.clearSavedLocation(SavedLocationType.fromString(type));
		}
		return m;
	}

	public String getSearch() {
		return this.search;
	}

	public MapleShop getShop() {
		return this.shop;
	}

	public Map<Skill, SkillEntry> getSkills() {
		return Collections.unmodifiableMap(this.skills);
	}

	public int getSkillLevel(int skill) {
		final SkillEntry ret = this.skills.get(SkillFactory.getSkill(skill));
		if (ret == null) {
			return 0;
		}
		return ret.skillevel;
	}

	public byte getSkillLevel(Skill skill) {
		if (this.skills.get(skill) == null) {
			return 0;
		}
		return this.skills.get(skill).skillevel;
	}

	public long getSkillExpiration(int skill) {
		final SkillEntry ret = this.skills.get(SkillFactory.getSkill(skill));
		if (ret == null) {
			return -1;
		}
		return ret.expiration;
	}

	public long getSkillExpiration(Skill skill) {
		if (this.skills.get(skill) == null) {
			return -1;
		}
		return this.skills.get(skill).expiration;
	}

	public MapleSkinColor getSkinColor() {
		return this.skinColor;
	}

	public int getSlot() {
		return this.slots;
	}

	public final List<MapleQuestStatus> getStartedQuests() {
		final List<MapleQuestStatus> ret = new LinkedList<>();
		for (final MapleQuestStatus q : this.quests.values()) {
			if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public final int getStartedQuestsSize() {
		int i = 0;
		for (final MapleQuestStatus q : this.quests.values()) {
			if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
				if (q.getQuest().getInfoNumber() > 0) {
					i++;
				}
				i++;
			}
		}
		return i;
	}

	public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
		final MapleBuffStatValueHolder mbsvh = this.effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return mbsvh.effect;
	}

	public MapleStorage getStorage() {
		return this.storage;
	}

	public int getStr() {
		return this.str;
	}

	public Map<Integer, MapleSummon> getSummons() {
		return this.summons;
	}

	public int getTotalLuk() {
		return this.localluk;
	}

	public int getTotalMagic() {
		return this.magic;
	}

	public int getTotalWatk() {
		return this.watk;
	}

	public MapleTrade getTrade() {
		return this.trade;
	}

	public int getVanquisherKills() {
		return this.vanquisherKills;
	}

	public int getVanquisherStage() {
		return this.vanquisherStage;
	}

	public Collection<MapleMapObject> getVisibleMapObjects() {
		return Collections.unmodifiableCollection(this.visibleMapObjects);
	}

	public int getWorld() {
		return this.world;
	}

	public void giveCoolDowns(final int skillid, long starttime, long length) {
		if (skillid == 5221999) {
			this.battleshipHp = (int) length;
			this.addCooldown(skillid, 0, length, null);
		} else {
			final int time = (int) ((length + starttime) - System
					.currentTimeMillis());
			this.addCooldown(
					skillid,
					System.currentTimeMillis(),
					time,
					TimerManager.getInstance().schedule(
							new CancelCooldownAction(this, skillid), time));
		}
	}

	public int gmLevel() {
		return this.gmLevel;
	}

	public String guildCost() {
		return this.nf.format(MapleGuild.CREATE_GUILD_COST);
	}

	private void guildUpdate() {
		if (this.guildid < 1) {
			return;
		}
		this.mgc.setLevel(this.level);
		this.mgc.setJobId(this.job.getId());
		try {
			Server.getInstance().memberLevelJobUpdate(this.mgc);
			final int allianceId = this.getGuild().getAllianceId();
			if (allianceId > 0) {
				Server.getInstance().allianceMessage(allianceId,
						MaplePacketCreator.updateAllianceJobLevel(this),
						this.getId(), -1);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEnergyChargeGain() { // to get here energychargelevel has
											// to be > 0
		final Skill energycharge = this.isCygnus() ? SkillFactory
				.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory
				.getSkill(Marauder.ENERGY_CHARGE);
		MapleStatEffect ceffect;
		ceffect = energycharge.getEffect(this.getSkillLevel(energycharge));
		final TimerManager tMan = TimerManager.getInstance();
		if (this.energybar < 10000) {
			this.energybar += 102;
			if (this.energybar > 10000) {
				this.energybar = 10000;
			}
			final List<Pair<MapleBuffStat, Integer>> stat = Collections
					.singletonList(new Pair<>(MapleBuffStat.ENERGY_CHARGE,
							this.energybar));
			this.setBuffedValue(MapleBuffStat.ENERGY_CHARGE, this.energybar);
			this.client.announce(MaplePacketCreator.giveBuff(this.energybar, 0,
					stat));
			this.client.announce(MaplePacketCreator.showOwnBuffEffect(
					energycharge.getId(), 2));
			this.getMap().broadcastMessage(
					this,
					MaplePacketCreator.showBuffeffect(this.id,
							energycharge.getId(), 2));
			this.getMap().broadcastMessage(this,
					MaplePacketCreator.giveForeignBuff(this.energybar, stat));
		}
		if ((this.energybar >= 10000) && (this.energybar < 11000)) {
			this.energybar = 15000;
			final MapleCharacter chr = this;
			tMan.schedule(new Runnable() {
				@Override
				public void run() {
					MapleCharacter.this.energybar = 0;
					final List<Pair<MapleBuffStat, Integer>> stat = Collections
							.singletonList(new Pair<>(
									MapleBuffStat.ENERGY_CHARGE,
									MapleCharacter.this.energybar));
					MapleCharacter.this.setBuffedValue(
							MapleBuffStat.ENERGY_CHARGE,
							MapleCharacter.this.energybar);
					MapleCharacter.this.client.announce(MaplePacketCreator
							.giveBuff(MapleCharacter.this.energybar, 0, stat));
					MapleCharacter.this.getMap().broadcastMessage(
							chr,
							MaplePacketCreator.giveForeignBuff(
									MapleCharacter.this.energybar, stat));
				}
			}, ceffect.getDuration());
		}
	}

	public void handleOrbconsume() {
		final int skillid = this.isCygnus() ? DawnWarrior.COMBO
				: Crusader.COMBO;
		final Skill combo = SkillFactory.getSkill(skillid);
		final List<Pair<MapleBuffStat, Integer>> stat = Collections
				.singletonList(new Pair<>(MapleBuffStat.COMBO, 1));
		this.setBuffedValue(MapleBuffStat.COMBO, 1);
		this.client
				.announce(MaplePacketCreator.giveBuff(
						skillid,
						combo.getEffect(this.getSkillLevel(combo))
								.getDuration()
								+ (int) ((this
										.getBuffedStarttime(MapleBuffStat.COMBO) - System
										.currentTimeMillis())), stat));
		this.getMap().broadcastMessage(this,
				MaplePacketCreator.giveForeignBuff(this.getId(), stat), false);
	}

	public boolean hasEntered(String script) {
		for (final int mapId : this.entered.keySet()) {
			if (this.entered.get(mapId).equals(script)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasEntered(String script, int mapId) {
		if (this.entered.containsKey(mapId)) {
			if (this.entered.get(mapId).equals(script)) {
				return true;
			}
		}
		return false;
	}

	public void hasGivenFame(MapleCharacter to) {
		this.lastfametime = System.currentTimeMillis();
		this.lastmonthfameids.add(Integer.valueOf(to.getId()));
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
				ps.setInt(1, this.getId());
				ps.setInt(2, to.getId());
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public boolean hasMerchant() {
		return this.hasMerchant;
	}

	public boolean haveItem(int itemid) {
		return this.getItemQuantity(itemid, false) > 0;
	}

	public void increaseGuildCapacity() { // hopefully nothing is null
		if (this.getMeso() < this.getGuild().getIncreaseGuildCost(
				this.getGuild().getCapacity())) {
			this.dropMessage(1, "You don't have enough mesos.");
			return;
		}
		Server.getInstance().increaseGuildCapacity(this.guildid);
		this.gainMeso(
				-this.getGuild().getIncreaseGuildCost(
						this.getGuild().getCapacity()), true, false, false);
	}

	public boolean isActiveBuffedValue(int skillid) {
		final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(
				this.effects.values());
		for (final MapleBuffStatValueHolder mbsvh : allBuffs) {
			if (mbsvh.effect.isSkill()
					&& (mbsvh.effect.getSourceId() == skillid)) {
				return true;
			}
		}
		return false;
	}

	public boolean isAlive() {
		return this.hp > 0;
	}

	public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
		final MapleBuffStatValueHolder mbsvh = this.effects.get(stat);
		if (mbsvh == null) {
			return false;
		}
		return mbsvh.effect.isSkill()
				&& (mbsvh.effect.getSourceId() == skill.getId());
	}

	public boolean isCygnus() {
		return this.getJobType() == 1;
	}

	public boolean isAran() {
		return (this.getJob().getId() >= 2000)
				&& (this.getJob().getId() <= 2112);
	}

	public boolean isBeginnerJob() {
		return ((this.getJob().getId() == 0) || (this.getJob().getId() == 1000) || (this
				.getJob().getId() == 2000)) && (this.getLevel() < 11);
	}

	public boolean isGM() {
		return this.gmLevel > 0;
	}

	public boolean isHidden() {
		return this.hidden;
	}

	public boolean isMapObjectVisible(MapleMapObject mo) {
		return this.visibleMapObjects.contains(mo);
	}

	public boolean isPartyLeader() {
		return this.party.getLeader() == this.party.getMemberById(this.getId());
	}

	public void leaveMap() {
		this.controlled.clear();
		this.visibleMapObjects.clear();
		if (this.chair != 0) {
			this.chair = 0;
		}
		if (this.hpDecreaseTask != null) {
			this.hpDecreaseTask.cancel(false);
		}
	}

	public void levelUp(boolean takeexp) {
		Skill improvingMaxHP = null;
		Skill improvingMaxMP = null;
		int improvingMaxHPLevel = 0;
		int improvingMaxMPLevel = 0;

		if (this.isBeginnerJob()) {
			this.remainingAp = 0;
			if (this.getLevel() < 6) {
				this.str += 5;
			} else {
				this.str += 4;
				this.dex += 1;
			}
		} else {
			this.remainingAp += 5;
			if (this.isCygnus() && (this.level < 70)) {
				this.remainingAp++;
			}
		}
		if ((this.job == MapleJob.BEGINNER) || (this.job == MapleJob.NOBLESSE)
				|| (this.job == MapleJob.LEGEND)) {
			this.maxhp += Randomizer.rand(12, 16);
			this.maxmp += Randomizer.rand(10, 12);
		} else if (this.job.isA(MapleJob.WARRIOR)
				|| this.job.isA(MapleJob.DAWNWARRIOR1)) {
			improvingMaxHP = this.isCygnus() ? SkillFactory
					.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory
					.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE);
			if (this.job.isA(MapleJob.CRUSADER)) {
				improvingMaxMP = SkillFactory.getSkill(1210000);
			} else if (this.job.isA(MapleJob.DAWNWARRIOR2)) {
				improvingMaxMP = SkillFactory.getSkill(11110000);
			}
			improvingMaxHPLevel = this.getSkillLevel(improvingMaxHP);
			this.maxhp += Randomizer.rand(24, 28);
			this.maxmp += Randomizer.rand(4, 6);
		} else if (this.job.isA(MapleJob.MAGICIAN)
				|| this.job.isA(MapleJob.BLAZEWIZARD1)) {
			improvingMaxMP = this.isCygnus() ? SkillFactory
					.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory
					.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
			improvingMaxMPLevel = this.getSkillLevel(improvingMaxMP);
			this.maxhp += Randomizer.rand(10, 14);
			this.maxmp += Randomizer.rand(22, 24);
		} else if (this.job.isA(MapleJob.BOWMAN)
				|| this.job.isA(MapleJob.THIEF)
				|| ((this.job.getId() > 1299) && (this.job.getId() < 1500))) {
			this.maxhp += Randomizer.rand(20, 24);
			this.maxmp += Randomizer.rand(14, 16);
		} else if (this.job.isA(MapleJob.GM)) {
			this.maxhp = 30000;
			this.maxmp = 30000;
		} else if (this.job.isA(MapleJob.PIRATE)
				|| this.job.isA(MapleJob.THUNDERBREAKER1)) {
			improvingMaxHP = this.isCygnus() ? SkillFactory
					.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory
					.getSkill(5100000);
			improvingMaxHPLevel = this.getSkillLevel(improvingMaxHP);
			this.maxhp += Randomizer.rand(22, 28);
			this.maxmp += Randomizer.rand(18, 23);
		} else if (this.job.isA(MapleJob.ARAN1)) {
			this.maxhp += Randomizer.rand(44, 48);
			final int aids = Randomizer.rand(4, 8);
			this.maxmp += aids + Math.floor(aids * 0.1);
		}
		if ((improvingMaxHPLevel > 0)
				&& (this.job.isA(MapleJob.WARRIOR)
						|| this.job.isA(MapleJob.PIRATE) || this.job
							.isA(MapleJob.DAWNWARRIOR1))) {
			this.maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
		}
		if ((improvingMaxMPLevel > 0)
				&& (this.job.isA(MapleJob.MAGICIAN)
						|| this.job.isA(MapleJob.CRUSADER) || this.job
							.isA(MapleJob.BLAZEWIZARD1))) {
			this.maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
		}
		this.maxmp += this.localint_ / 10;
		if (takeexp) {
			this.exp.addAndGet(-ExpTable.getExpNeededForLevel(this.level));
			if (this.exp.get() < 0) {
				this.exp.set(0);
			}
		}
		this.level++;
		if (this.level >= this.getMaxLevel()) {
			this.exp.set(0);
		}
		this.maxhp = Math.min(30000, this.maxhp);
		this.maxmp = Math.min(30000, this.maxmp);
		if (this.level == 200) {
			this.exp.set(0);
		}
		this.hp = this.maxhp;
		this.mp = this.maxmp;
		this.recalcLocalStats();
		final List<Pair<MapleStat, Integer>> statup = new ArrayList<>(10);
		statup.add(new Pair<>(MapleStat.AVAILABLEAP, this.remainingAp));
		statup.add(new Pair<>(MapleStat.HP, this.localmaxhp));
		statup.add(new Pair<>(MapleStat.MP, this.localmaxmp));
		statup.add(new Pair<>(MapleStat.EXP, this.exp.get()));
		statup.add(new Pair<>(MapleStat.LEVEL, this.level));
		statup.add(new Pair<>(MapleStat.MAXHP, this.maxhp));
		statup.add(new Pair<>(MapleStat.MAXMP, this.maxmp));
		statup.add(new Pair<>(MapleStat.STR, this.str));
		statup.add(new Pair<>(MapleStat.DEX, this.dex));
		if ((this.job.getId() % 1000) > 0) {
			this.remainingSp += 3;
			statup.add(new Pair<>(MapleStat.AVAILABLESP, this.remainingSp));
		}
		this.client.announce(MaplePacketCreator.updatePlayerStats(statup));
		this.getMap().broadcastMessage(this,
				MaplePacketCreator.showForeignEffect(this.getId(), 0), false);
		this.recalcLocalStats();
		this.setMPC(new MaplePartyCharacter(this));
		this.silentPartyUpdate();
		if (this.guildid > 0) {
			this.getGuild()
					.broadcast(
							MaplePacketCreator.levelUpMessage(2, this.level,
									this.name), this.getId());
		}
		if (ServerConstants.PERFECT_PITCH) {
			// milestones?
			if (MapleInventoryManipulator.checkSpace(this.client, 4310000,
					(short) 1, "")) {
				MapleInventoryManipulator.addById(this.client, 4310000,
						(short) 1);
			}
		}
		if ((this.level == 200) && !this.isGM()) {
			final String names = (this.getMedalText() + this.name);
			this.client.getWorldServer().broadcastPacket(
					MaplePacketCreator.serverNotice(6,
							String.format(LEVEL_200, names, names)));
		}
		this.guildUpdate();
	}

	public static MapleCharacter loadCharFromDB(int charid, MapleClient client,
			boolean channelserver) throws SQLException {
		try {
			final MapleCharacter ret = new MapleCharacter();
			ret.client = client;
			ret.id = charid;
			final Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM characters WHERE id = ?");
			ps.setInt(1, charid);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				throw new RuntimeException("Loading char failed (not found)");
			}
			ret.name = rs.getString("name");
			ret.level = rs.getInt("level");
			ret.fame = rs.getInt("fame");
			ret.str = rs.getInt("str");
			ret.dex = rs.getInt("dex");
			ret.int_ = rs.getInt("int");
			ret.luk = rs.getInt("luk");
			ret.exp.set(rs.getInt("exp"));
			ret.gachaexp.set(rs.getInt("gachaexp"));
			ret.hp = rs.getInt("hp");
			ret.maxhp = rs.getInt("maxhp");
			ret.mp = rs.getInt("mp");
			ret.maxmp = rs.getInt("maxmp");
			ret.hpMpApUsed = rs.getInt("hpMpUsed");
			ret.hasMerchant = rs.getInt("HasMerchant") == 1;
			ret.remainingSp = rs.getInt("sp");
			ret.remainingAp = rs.getInt("ap");
			ret.meso.set(rs.getInt("meso"));
			ret.merchantmeso = rs.getInt("MerchantMesos");
			ret.gmLevel = rs.getInt("gm");
			ret.skinColor = MapleSkinColor.getById(rs.getInt("skincolor"));
			ret.gender = rs.getInt("gender");
			ret.job = MapleJob.getById(rs.getInt("job"));
			ret.finishedDojoTutorial = rs.getInt("finishedDojoTutorial") == 1;
			ret.vanquisherKills = rs.getInt("vanquisherKills");
			ret.omokwins = rs.getInt("omokwins");
			ret.omoklosses = rs.getInt("omoklosses");
			ret.omokties = rs.getInt("omokties");
			ret.matchcardwins = rs.getInt("matchcardwins");
			ret.matchcardlosses = rs.getInt("matchcardlosses");
			ret.matchcardties = rs.getInt("matchcardties");
			ret.hair = rs.getInt("hair");
			ret.face = rs.getInt("face");
			ret.accountid = rs.getInt("accountid");
			ret.mapid = rs.getInt("map");
			ret.initialSpawnPoint = rs.getInt("spawnpoint");
			ret.world = rs.getByte("world");
			ret.rank = rs.getInt("rank");
			ret.rankMove = rs.getInt("rankMove");
			ret.jobRank = rs.getInt("jobRank");
			ret.jobRankMove = rs.getInt("jobRankMove");
			final int mountexp = rs.getInt("mountexp");
			final int mountlevel = rs.getInt("mountlevel");
			final int mounttiredness = rs.getInt("mounttiredness");
			ret.guildid = rs.getInt("guildid");
			ret.guildrank = rs.getInt("guildrank");
			ret.allianceRank = rs.getInt("allianceRank");
			ret.familyId = rs.getInt("familyId");
			ret.bookCover = rs.getInt("monsterbookcover");
			ret.monsterbook = new MonsterBook();
			ret.monsterbook.loadCards(charid);
			ret.vanquisherStage = rs.getInt("vanquisherStage");
			ret.dojoPoints = rs.getInt("dojoPoints");
			ret.dojoStage = rs.getInt("lastDojoStage");
			if (ret.guildid > 0) {
				ret.mgc = new MapleGuildCharacter(ret);
			}
			final int buddyCapacity = rs.getInt("buddyCapacity");
			ret.buddylist = new BuddyList(buddyCapacity);
			ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(
					rs.getByte("equipslots"));
			ret.getInventory(MapleInventoryType.USE).setSlotLimit(
					rs.getByte("useslots"));
			ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(
					rs.getByte("setupslots"));
			ret.getInventory(MapleInventoryType.ETC).setSlotLimit(
					rs.getByte("etcslots"));
			for (final Pair<Item, MapleInventoryType> item : ItemFactory.INVENTORY
					.loadItems(ret.id, !channelserver)) {
				ret.getInventory(item.getRight()).addFromDB(item.getLeft());
				final Item itemz = item.getLeft();
				if (itemz.getPetId() > -1) {
					final MaplePet pet = itemz.getPet();
					if ((pet != null) && pet.isSummoned()) {
						ret.addPet(pet);
					}
					continue;
				}
				if (item.getRight().equals(MapleInventoryType.EQUIP)
						|| item.getRight().equals(MapleInventoryType.EQUIPPED)) {
					final Equip equip = (Equip) item.getLeft();
					if (equip.getRingId() > -1) {
						final MapleRing ring = MapleRing.loadFromDb(equip
								.getRingId());
						if (item.getRight().equals(MapleInventoryType.EQUIPPED)) {
							ring.equip();
						}
						if (ring.getItemId() > 1112012) {
							ret.addFriendshipRing(ring);
						} else {
							ret.addCrushRing(ring);
						}
					}
				}
			}
			if (channelserver) {
				final MapleMapFactory mapFactory = client.getChannelServer()
						.getMapFactory();
				ret.map = mapFactory.getMap(ret.mapid);
				if (ret.map == null) {
					ret.map = mapFactory.getMap(100000000);
				}
				MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
				if (portal == null) {
					portal = ret.map.getPortal(0);
					ret.initialSpawnPoint = 0;
				}
				ret.setPosition(portal.getPosition());
				final int partyid = rs.getInt("party");
				final MapleParty party = Server.getInstance()
						.getWorld(ret.world).getParty(partyid);
				if (party != null) {
					ret.mpc = party.getMemberById(ret.id);
					if (ret.mpc != null) {
						ret.party = party;
					}
				}
				final int messengerid = rs.getInt("messengerid");
				final int position = rs.getInt("messengerposition");
				if ((messengerid > 0) && (position < 4) && (position > -1)) {
					final MapleMessenger messenger = Server.getInstance()
							.getWorld(ret.world).getMessenger(messengerid);
					if (messenger != null) {
						ret.messenger = messenger;
						ret.messengerposition = position;
					}
				}
				ret.loggedIn = true;
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT mapid,vip FROM trocklocations WHERE characterid = ? LIMIT 15");
			ps.setInt(1, charid);
			rs = ps.executeQuery();
			byte v = 0;
			byte r = 0;
			while (rs.next()) {
				if (rs.getInt("vip") == 1) {
					ret.viptrockmaps[v] = rs.getInt("mapid");
					v++;
				} else {
					ret.trockmaps[r] = rs.getInt("mapid");
					r++;
				}
			}
			while (v < 10) {
				ret.viptrockmaps[v] = 999999999;
				v++;
			}
			while (r < 5) {
				ret.trockmaps[r] = 999999999;
				r++;
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT name FROM accounts WHERE id = ?",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, ret.accountid);
			rs = ps.executeQuery();
			if (rs.next()) {
				ret.getClient().setAccountName(rs.getString("name"));
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT `area`,`info` FROM area_info WHERE charid = ?");
			ps.setInt(1, ret.id);
			rs = ps.executeQuery();
			while (rs.next()) {
				ret.area_info.put(rs.getShort("area"), rs.getString("info"));
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT `name`,`info` FROM eventstats WHERE characterid = ?");
			ps.setInt(1, ret.id);
			rs = ps.executeQuery();
			while (rs.next()) {
				final String name = rs.getString("name");
				if (rs.getString("name").equals("rescueGaga")) {
					ret.events.put(name, new RescueGaga(rs.getInt("info")));
				}
				// ret.events = new MapleEvents(new
				// RescueGaga(rs.getInt("rescuegaga")), new
				// ArtifactHunt(rs.getInt("artifacthunt")));
			}
			rs.close();
			ps.close();
			ret.cashshop = new CashShop(ret.accountid, ret.id, ret.getJobType());
			ret.autoban = new AutobanManager(ret);
			ret.marriageRing = null; // for now
			ps = con.prepareStatement("SELECT name, level FROM characters WHERE accountid = ? AND id != ? ORDER BY level DESC limit 1");
			ps.setInt(1, ret.accountid);
			ps.setInt(2, charid);
			rs = ps.executeQuery();
			if (rs.next()) {
				ret.linkedName = rs.getString("name");
				ret.linkedLevel = rs.getInt("level");
			}
			rs.close();
			ps.close();
			if (channelserver) {
				ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
				ps.setInt(1, charid);
				rs = ps.executeQuery();
				PreparedStatement psf;
				try (PreparedStatement pse = con
						.prepareStatement("SELECT * FROM questprogress WHERE queststatusid = ?")) {
					psf = con
							.prepareStatement("SELECT mapid FROM medalmaps WHERE queststatusid = ?");
					while (rs.next()) {
						final MapleQuest q = MapleQuest.getInstance(rs
								.getShort("quest"));
						final MapleQuestStatus status = new MapleQuestStatus(q,
								MapleQuestStatus.Status.getById(rs
										.getInt("status")));
						final long cTime = rs.getLong("time");
						if (cTime > -1) {
							status.setCompletionTime(cTime * 1000);
						}
						status.setForfeited(rs.getInt("forfeited"));
						ret.quests.put(q, status);
						pse.setInt(1, rs.getInt("queststatusid"));
						try (ResultSet rsProgress = pse.executeQuery()) {
							while (rsProgress.next()) {
								status.setProgress(
										rsProgress.getInt("progressid"),
										rsProgress.getString("progress"));
							}
						}
						psf.setInt(1, rs.getInt("queststatusid"));
						try (ResultSet medalmaps = psf.executeQuery()) {
							while (medalmaps.next()) {
								status.addMedalMap(medalmaps.getInt("mapid"));
							}
						}
					}
					rs.close();
					ps.close();
				}
				psf.close();
				ps = con.prepareStatement("SELECT skillid,skilllevel,masterlevel,expiration FROM skills WHERE characterid = ?");
				ps.setInt(1, charid);
				rs = ps.executeQuery();
				while (rs.next()) {
					ret.skills.put(
							SkillFactory.getSkill(rs.getInt("skillid")),
							new SkillEntry(rs.getByte("skilllevel"), rs
									.getInt("masterlevel"), rs
									.getLong("expiration")));
				}
				rs.close();
				ps.close();
				ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM cooldowns WHERE charid = ?");
				ps.setInt(1, ret.getId());
				rs = ps.executeQuery();
				while (rs.next()) {
					final int skillid = rs.getInt("SkillID");
					final long length = rs.getLong("length"), startTime = rs
							.getLong("StartTime");
					if ((skillid != 5221999)
							&& ((length + startTime) < System
									.currentTimeMillis())) {
						continue;
					}
					ret.giveCoolDowns(skillid, startTime, length);
				}
				rs.close();
				ps.close();
				ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?");
				ps.setInt(1, ret.getId());
				ps.executeUpdate();
				ps.close();
				ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
				ps.setInt(1, charid);
				rs = ps.executeQuery();
				while (rs.next()) {
					final int position = rs.getInt("position");
					final SkillMacro macro = new SkillMacro(
							rs.getInt("skill1"), rs.getInt("skill2"),
							rs.getInt("skill3"), rs.getString("name"),
							rs.getInt("shout"), position);
					ret.skillMacros[position] = macro;
				}
				rs.close();
				ps.close();
				ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
				ps.setInt(1, charid);
				rs = ps.executeQuery();
				while (rs.next()) {
					final int key = rs.getInt("key");
					final int type = rs.getInt("type");
					final int action = rs.getInt("action");
					ret.keymap.put(Integer.valueOf(key), new MapleKeyBinding(
							type, action));
				}
				rs.close();
				ps.close();
				ps = con.prepareStatement("SELECT `locationtype`,`map`,`portal` FROM savedlocations WHERE characterid = ?");
				ps.setInt(1, charid);
				rs = ps.executeQuery();
				while (rs.next()) {
					ret.savedLocations[SavedLocationType.valueOf(
							rs.getString("locationtype")).ordinal()] = new SavedLocation(
							rs.getInt("map"), rs.getInt("portal"));
				}
				rs.close();
				ps.close();
				ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
				ps.setInt(1, charid);
				rs = ps.executeQuery();
				ret.lastfametime = 0;
				ret.lastmonthfameids = new ArrayList<>(31);
				while (rs.next()) {
					ret.lastfametime = Math.max(ret.lastfametime, rs
							.getTimestamp("when").getTime());
					ret.lastmonthfameids.add(Integer.valueOf(rs
							.getInt("characterid_to")));
				}
				rs.close();
				ps.close();
				ret.buddylist.loadFromDb(charid);
				ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid,
						ret.world);
				ret.recalcLocalStats();
				// ret.resetBattleshipHp();
				ret.silentEnforceMaxHpMp();
			}
			final int mountid = (ret.getJobType() * 10000000) + 1004;
			if (ret.getInventory(MapleInventoryType.EQUIPPED).getItem(
					(byte) -18) != null) {
				ret.maplemount = new MapleMount(ret, ret
						.getInventory(MapleInventoryType.EQUIPPED)
						.getItem((byte) -18).getItemId(), mountid);
			} else {
				ret.maplemount = new MapleMount(ret, 0, mountid);
			}
			ret.maplemount.setExp(mountexp);
			ret.maplemount.setLevel(mountlevel);
			ret.maplemount.setTiredness(mounttiredness);
			ret.maplemount.setActive(false);
			return ret;
		} catch (SQLException | RuntimeException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String makeMapleReadable(String in) {
		String i = in.replace('I', 'i');
		i = i.replace('l', 'L');
		i = i.replace("rn", "Rn");
		i = i.replace("vv", "Vv");
		i = i.replace("VV", "Vv");
		return i;

	}

	private static class MapleBuffStatValueHolder {

		public MapleStatEffect effect;
		public long startTime;
		public int value;
		public ScheduledFuture<?> schedule;

		public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime,
				ScheduledFuture<?> schedule, int value) {
			super();
			this.effect = effect;
			this.startTime = startTime;
			this.schedule = schedule;
			this.value = value;
		}
	}

	public static class MapleCoolDownValueHolder {

		public int skillId;
		public long startTime, length;
		public ScheduledFuture<?> timer;

		public MapleCoolDownValueHolder(int skillId, long startTime,
				long length, ScheduledFuture<?> timer) {
			super();
			this.skillId = skillId;
			this.startTime = startTime;
			this.length = length;
			this.timer = timer;
		}
	}

	public void addMessage(String m) {
		this.dropMessage(5, m);
	}

	public void addYellowMessage(String m) {
		this.announce(MaplePacketCreator.sendYellowTip(m));
	}

	public void mobKilled(int id) {
		for (final MapleQuestStatus q : this.quests.values()) {
			if ((q.getStatus() == MapleQuestStatus.Status.COMPLETED)
					|| q.getQuest().canComplete(this, null)) {
				continue;
			}
			final String progress = q.getProgress(id);
			if (!progress.isEmpty()
					&& (Integer.parseInt(progress) >= q.getQuest()
							.getMobAmountNeeded(id))) {
				continue;
			}
			if (q.progress(id)) {
				this.client.announce(MaplePacketCreator.updateQuest(q
						.getQuest().getId(), q.getQuestData()));
			}
		}
	}

	public void mount(int id, int skillid) {
		this.maplemount = new MapleMount(this, id, skillid);
	}

	public void playerNPC(MapleCharacter v, int scriptId) {
		int npcId;
		try {
			final Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT id FROM playernpcs WHERE ScriptId = ?");
			ps.setInt(1, scriptId);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps = con.prepareStatement(
						"INSERT INTO playernpcs (name, hair, face, skin, x, cy, map, ScriptId, Foothold, rx0, rx1) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, v.getName());
				ps.setInt(2, v.getHair());
				ps.setInt(3, v.getFace());
				ps.setInt(4, v.getSkinColor().getId());
				ps.setInt(5, this.getPosition().x);
				ps.setInt(6, this.getPosition().y);
				ps.setInt(7, this.getMapId());
				ps.setInt(8, scriptId);
				ps.setInt(
						9,
						this.getMap().getFootholds()
								.findBelow(this.getPosition()).getId());
				ps.setInt(10, this.getPosition().x + 50);
				ps.setInt(11, this.getPosition().x - 50);
				ps.executeUpdate();
				rs = ps.getGeneratedKeys();
				rs.next();
				npcId = rs.getInt(1);
				ps.close();
				ps = con.prepareStatement("INSERT INTO playernpcs_equip (NpcId, equipid, equippos) VALUES (?, ?, ?)");
				ps.setInt(1, npcId);
				for (final Item equip : this
						.getInventory(MapleInventoryType.EQUIPPED)) {
					final int position = Math.abs(equip.getPosition());
					if (((position < 12) && (position > 0))
							|| ((position > 100) && (position < 112))) {
						ps.setInt(2, equip.getItemId());
						ps.setInt(3, equip.getPosition());
						ps.addBatch();
					}
				}
				ps.executeBatch();
				ps.close();
				rs.close();
				ps = con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
				ps.setInt(1, scriptId);
				rs = ps.executeQuery();
				rs.next();
				final PlayerNPCs pn = new PlayerNPCs(rs);
				for (final Channel channel : Server.getInstance()
						.getChannelsFromWorld(this.world)) {
					final MapleMap m = channel.getMapFactory().getMap(
							this.getMapId());
					m.broadcastMessage(MaplePacketCreator.spawnPlayerNPC(pn));
					m.broadcastMessage(MaplePacketCreator.getPlayerNPC(pn));
					m.addMapObject(pn);
				}
			}
			ps.close();
			rs.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	private void playerDead() {
		this.cancelAllBuffs(false);
		this.dispelDebuffs();
		if (this.getEventInstance() != null) {
			this.getEventInstance().playerKilled(this);
		}
		final int[] charmID = { 5130000, 4031283, 4140903 };
		int possesed = 0;
		int i;
		for (i = 0; i < charmID.length; i++) {
			final int quantity = this.getItemQuantity(charmID[i], false);
			if ((possesed == 0) && (quantity > 0)) {
				possesed = quantity;
				break;
			}
		}
		if (possesed > 0) {
			this.addMessage("You have used a safety charm, so your EXP points have not been decreased.");
			MapleInventoryManipulator.removeById(this.client,
					MapleItemInformationProvider.getInstance()
							.getInventoryType(charmID[i]), charmID[i], 1, true,
					false);
		} else if ((this.mapid > 925020000) && (this.mapid < 925030000)) {
			this.dojoStage = 0;
		} else if ((this.mapid > 980000100) && (this.mapid < 980000700)) {
			this.getMap().broadcastMessage(this,
					MaplePacketCreator.CPQDied(this));
		} else if (this.getJob() != MapleJob.BEGINNER) { // Hmm...
			int XPdummy = ExpTable.getExpNeededForLevel(this.getLevel());
			if (this.getMap().isTown()) {
				XPdummy /= 100;
			}
			if (XPdummy == ExpTable.getExpNeededForLevel(this.getLevel())) {
				if ((this.getLuk() <= 100) && (this.getLuk() > 8)) {
					XPdummy *= (200 - this.getLuk()) / 2000;
				} else if (this.getLuk() < 8) {
					XPdummy /= 10;
				} else {
					XPdummy /= 20;
				}
			}
			if (this.getExp() > XPdummy) {
				this.gainExp(-XPdummy, false, false);
			} else {
				this.gainExp(-this.getExp(), false, false);
			}
		}
		if (this.getBuffedValue(MapleBuffStat.MORPH) != null) {
			this.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
		}

		if (this.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
			this.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
		}

		if (this.getChair() == -1) {
			this.setChair(0);
			this.client.announce(MaplePacketCreator.cancelChair(-1));
			this.getMap().broadcastMessage(this,
					MaplePacketCreator.showChair(this.getId(), 0), false);
		}
		this.client.announce(MaplePacketCreator.enableActions());
	}

	private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
		if (this.dragonBloodSchedule != null) {
			this.dragonBloodSchedule.cancel(false);
		}
		this.dragonBloodSchedule = TimerManager.getInstance().register(
				new Runnable() {
					@Override
					public void run() {
						MapleCharacter.this.addHP(-bloodEffect.getX());
						MapleCharacter.this.client.announce(MaplePacketCreator
								.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
						MapleCharacter.this.getMap().broadcastMessage(
								MapleCharacter.this,
								MaplePacketCreator.showBuffeffect(
										MapleCharacter.this.getId(),
										bloodEffect.getSourceId(), 5), false);
						MapleCharacter.this.checkBerserk();
					}
				}, 4000, 4000);
	}

	private void recalcLocalStats() {
		final int oldmaxhp = this.localmaxhp;
		this.localmaxhp = this.getMaxHp();
		this.localmaxmp = this.getMaxMp();
		this.localdex = this.getDex();
		this.localint_ = this.getInt();
		this.localstr = this.getStr();
		this.localluk = this.getLuk();
		this.magic = this.localint_;
		this.watk = 0;
		for (final Item item : this.getInventory(MapleInventoryType.EQUIPPED)) {
			final Equip equip = (Equip) item;
			this.localmaxhp += equip.getHp();
			this.localmaxmp += equip.getMp();
			this.localdex += equip.getDex();
			this.localint_ += equip.getInt();
			this.localstr += equip.getStr();
			this.localluk += equip.getLuk();
			this.magic += equip.getMatk() + equip.getInt();
			this.watk += equip.getWatk();
			equip.getSpeed();
			equip.getJump();
		}
		this.magic = Math.min(this.magic, 2000);
		final Integer hbhp = this.getBuffedValue(MapleBuffStat.HYPERBODYHP);
		if (hbhp != null) {
			this.localmaxhp += (hbhp.doubleValue() / 100) * this.localmaxhp;
		}
		final Integer hbmp = this.getBuffedValue(MapleBuffStat.HYPERBODYMP);
		if (hbmp != null) {
			this.localmaxmp += (hbmp.doubleValue() / 100) * this.localmaxmp;
		}
		this.localmaxhp = Math.min(30000, this.localmaxhp);
		this.localmaxmp = Math.min(30000, this.localmaxmp);
		final Integer watkbuff = this.getBuffedValue(MapleBuffStat.WATK);
		if (watkbuff != null) {
			this.watk += watkbuff.intValue();
		}
		if (this.job.isA(MapleJob.BOWMAN)) {
			Skill expert = null;
			if (this.job.isA(MapleJob.MARKSMAN)) {
				expert = SkillFactory.getSkill(3220004);
			} else if (this.job.isA(MapleJob.BOWMASTER)) {
				expert = SkillFactory.getSkill(3120005);
			}
			if (expert != null) {
				final int boostLevel = this.getSkillLevel(expert);
				if (boostLevel > 0) {
					this.watk += expert.getEffect(boostLevel).getX();
				}
			}
		}
		final Integer matkbuff = this.getBuffedValue(MapleBuffStat.MATK);
		if (matkbuff != null) {
			this.magic += matkbuff.intValue();
		}
		final Integer speedbuff = this.getBuffedValue(MapleBuffStat.SPEED);
		if (speedbuff != null) {
			speedbuff.intValue();
		}
		final Integer jumpbuff = this.getBuffedValue(MapleBuffStat.JUMP);
		if (jumpbuff != null) {
			jumpbuff.intValue();
		}
		// if (speed > 140) {
		// speed = 140;
		// }
		// if (jump > 123) {
		// jump = 123;
		// }
		if ((oldmaxhp != 0) && (oldmaxhp != this.localmaxhp)) {
			this.updatePartyMemberHP();
		}
	}

	public void receivePartyMemberHP() {
		if (this.party != null) {
			final int channel = this.client.getChannel();
			for (final MaplePartyCharacter partychar : this.party.getMembers()) {
				if ((partychar.getMapId() == this.getMapId())
						&& (partychar.getChannel() == channel)) {
					final MapleCharacter other = Server.getInstance()
							.getWorld(this.world).getChannel(channel)
							.getPlayerStorage()
							.getCharacterByName(partychar.getName());
					if (other != null) {
						this.client
								.announce(MaplePacketCreator
										.updatePartyMemberHP(other.getId(),
												other.getHp(),
												other.getCurrentMaxHp()));
					}
				}
			}
		}
	}

	public void registerEffect(MapleStatEffect effect, long starttime,
			ScheduledFuture<?> schedule) {
		if (effect.isDragonBlood()) {
			this.prepareDragonBlood(effect);
		} else if (effect.isBerserk()) {
			this.checkBerserk();
		} else if (effect.isBeholder()) {
			final int beholder = DarkKnight.BEHOLDER;
			if (this.beholderHealingSchedule != null) {
				this.beholderHealingSchedule.cancel(false);
			}
			if (this.beholderBuffSchedule != null) {
				this.beholderBuffSchedule.cancel(false);
			}
			final Skill bHealing = SkillFactory
					.getSkill(DarkKnight.AURA_OF_BEHOLDER);
			final int bHealingLvl = this.getSkillLevel(bHealing);
			if (bHealingLvl > 0) {
				final MapleStatEffect healEffect = bHealing
						.getEffect(bHealingLvl);
				final int healInterval = healEffect.getX() * 1000;
				this.beholderHealingSchedule = TimerManager.getInstance()
						.register(new Runnable() {
							@Override
							public void run() {
								MapleCharacter.this.addHP(healEffect.getHp());
								MapleCharacter.this.client.announce(MaplePacketCreator
										.showOwnBuffEffect(beholder, 2));
								MapleCharacter.this.getMap().broadcastMessage(
										MapleCharacter.this,
										MaplePacketCreator.summonSkill(
												MapleCharacter.this.getId(),
												beholder, 5), true);
								MapleCharacter.this.getMap().broadcastMessage(
										MapleCharacter.this,
										MaplePacketCreator.showOwnBuffEffect(
												beholder, 2), false);
							}
						}, healInterval, healInterval);
			}
			final Skill bBuff = SkillFactory
					.getSkill(DarkKnight.HEX_OF_BEHOLDER);
			if (this.getSkillLevel(bBuff) > 0) {
				final MapleStatEffect buffEffect = bBuff.getEffect(this
						.getSkillLevel(bBuff));
				final int buffInterval = buffEffect.getX() * 1000;
				this.beholderBuffSchedule = TimerManager.getInstance()
						.register(new Runnable() {
							@Override
							public void run() {
								buffEffect.applyTo(MapleCharacter.this);
								MapleCharacter.this.client.announce(MaplePacketCreator
										.showOwnBuffEffect(beholder, 2));
								MapleCharacter.this.getMap().broadcastMessage(
										MapleCharacter.this,
										MaplePacketCreator.summonSkill(
												MapleCharacter.this.getId(),
												beholder,
												(int) (Math.random() * 3) + 6),
										true);
								MapleCharacter.this.getMap().broadcastMessage(
										MapleCharacter.this,
										MaplePacketCreator.showBuffeffect(
												MapleCharacter.this.getId(),
												beholder, 2), false);
							}
						}, buffInterval, buffInterval);
			}
		} else if (effect.isRecovery()) {
			final byte heal = (byte) effect.getX();
			this.recoveryTask = TimerManager.getInstance().register(
					new Runnable() {
						@Override
						public void run() {
							MapleCharacter.this.addHP(heal);
							MapleCharacter.this.client.announce(MaplePacketCreator
									.showOwnRecovery(heal));
							MapleCharacter.this.getMap().broadcastMessage(
									MapleCharacter.this,
									MaplePacketCreator.showRecovery(
											MapleCharacter.this.id, heal),
									false);
						}
					}, 5000, 5000);
		}
		for (final Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
			this.effects.put(statup.getLeft(), new MapleBuffStatValueHolder(
					effect, starttime, schedule, statup.getRight().intValue()));
		}
		this.recalcLocalStats();
	}

	public void removeAllCooldownsExcept(int id) {
		for (final MapleCoolDownValueHolder mcvh : this.coolDowns.values()) {
			if (mcvh.skillId != id) {
				this.coolDowns.remove(mcvh.skillId);
			}
		}
	}

	public static void removeAriantRoom(int room) {
		ariantroomleader[room] = "";
		ariantroomslot[room] = 0;
	}

	public void removeCooldown(int skillId) {
		if (this.coolDowns.containsKey(skillId)) {
			this.coolDowns.remove(skillId);
		}
	}

	public void removePet(MaplePet pet, boolean shift_left) {
		int slot = -1;
		for (int i = 0; i < 3; i++) {
			if (this.pets[i] != null) {
				if (this.pets[i].getUniqueId() == pet.getUniqueId()) {
					this.pets[i] = null;
					slot = i;
					break;
				}
			}
		}
		if (shift_left) {
			if (slot > -1) {
				for (int i = slot; i < 3; i++) {
					if (i != 2) {
						this.pets[i] = this.pets[i + 1];
					} else {
						this.pets[i] = null;
					}
				}
			}
		}
	}

	public void removeVisibleMapObject(MapleMapObject mo) {
		this.visibleMapObjects.remove(mo);
	}

	public void resetStats() {
		final List<Pair<MapleStat, Integer>> statup = new ArrayList<>(5);
		int tap = 0, tsp = 1;
		int tstr = 4, tdex = 4, tint = 4;
		final int tluk = 4;
		final int levelap = (this.isCygnus() ? 6 : 5);
		switch (this.job.getId()) {
		case 100:
		case 1100:
		case 2100:// ?
			tstr = 35;
			tap = ((this.getLevel() - 10) * levelap) + 14;
			tsp += ((this.getLevel() - 10) * 3);
			break;
		case 200:
		case 1200:
			tint = 20;
			tap = ((this.getLevel() - 8) * levelap) + 29;
			tsp += ((this.getLevel() - 8) * 3);
			break;
		case 300:
		case 1300:
		case 400:
		case 1400:
			tdex = 25;
			tap = ((this.getLevel() - 10) * levelap) + 24;
			tsp += ((this.getLevel() - 10) * 3);
			break;
		case 500:
		case 1500:
			tdex = 20;
			tap = ((this.getLevel() - 10) * levelap) + 29;
			tsp += ((this.getLevel() - 10) * 3);
			break;
		}
		this.remainingAp = tap;
		this.remainingSp = tsp;
		this.dex = tdex;
		this.int_ = tint;
		this.str = tstr;
		this.luk = tluk;
		statup.add(new Pair<>(MapleStat.AVAILABLEAP, tap));
		statup.add(new Pair<>(MapleStat.AVAILABLESP, tsp));
		statup.add(new Pair<>(MapleStat.STR, tstr));
		statup.add(new Pair<>(MapleStat.DEX, tdex));
		statup.add(new Pair<>(MapleStat.INT, tint));
		statup.add(new Pair<>(MapleStat.LUK, tluk));
		this.announce(MaplePacketCreator.updatePlayerStats(statup));
	}

	public void resetBattleshipHp() {
		this.battleshipHp = (4000 * this.getSkillLevel(SkillFactory
				.getSkill(Corsair.BATTLE_SHIP)))
				+ ((this.getLevel() - 120) * 2000);
	}

	public void resetEnteredScript() {
		if (this.entered.containsKey(this.map.getId())) {
			this.entered.remove(this.map.getId());
		}
	}

	public void resetEnteredScript(int mapId) {
		if (this.entered.containsKey(mapId)) {
			this.entered.remove(mapId);
		}
	}

	public void resetEnteredScript(String script) {
		for (final int mapId : this.entered.keySet()) {
			if (this.entered.get(mapId).equals(script)) {
				this.entered.remove(mapId);
			}
		}
	}

	public void resetMGC() {
		this.mgc = null;
	}

	public void saveCooldowns() {
		if (this.getAllCooldowns().size() > 0) {
			try {
				final Connection con = DatabaseConnection.getConnection();
				this.deleteWhereCharacterId(con,
						"DELETE FROM cooldowns WHERE charid = ?");
				try (PreparedStatement ps = con
						.prepareStatement("INSERT INTO cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)")) {
					ps.setInt(1, this.getId());
					for (final PlayerCoolDownValueHolder cooling : this
							.getAllCooldowns()) {
						ps.setInt(2, cooling.skillId);
						ps.setLong(3, cooling.startTime);
						ps.setLong(4, cooling.length);
						ps.addBatch();
					}
					ps.executeBatch();
				}
			} catch (final SQLException se) {
			}
		}
	}

	public void saveGuildStatus() {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE characters SET guildid = ?, guildrank = ?, allianceRank = ? WHERE id = ?")) {
				ps.setInt(1, this.guildid);
				ps.setInt(2, this.guildrank);
				ps.setInt(3, this.allianceRank);
				ps.setInt(4, this.id);
				ps.execute();
			}
		} catch (final SQLException se) {
		}
	}

	public void saveLocation(String type) {
		final MaplePortal closest = this.map.findClosestPortal(this
				.getPosition());
		this.savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(
				this.getMapId(), closest != null ? closest.getId() : 0);
	}

	public final boolean insertNewChar() {
		final Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;

		try {
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
			ps = con.prepareStatement(
					"INSERT INTO characters (str, dex, luk, `int`, gm, skincolor, gender, job, hair, face, map, meso, spawnpoint, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					DatabaseConnection.RETURN_GENERATED_KEYS);
			ps.setInt(1, 12);
			ps.setInt(2, 5);
			ps.setInt(3, 4);
			ps.setInt(4, 4);
			ps.setInt(5, this.gmLevel);
			ps.setInt(6, this.skinColor.getId());
			ps.setInt(7, this.gender);
			ps.setInt(8, this.getJob().getId());
			ps.setInt(9, this.hair);
			ps.setInt(10, this.face);
			ps.setInt(11, this.mapid);
			ps.setInt(12, Math.abs(this.meso.get()));
			ps.setInt(13, 0);
			ps.setInt(14, this.accountid);
			ps.setString(15, this.name);
			ps.setInt(16, this.world);

			final int updateRows = ps.executeUpdate();
			if (updateRows < 1) {
				ps.close();
				FilePrinter.printError(FilePrinter.INSERT_CHAR,
						"Error trying to insert " + this.name);
				return false;
			}
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				this.id = rs.getInt(1);
				rs.close();
				ps.close();
			} else {
				rs.close();
				ps.close();
				FilePrinter.printError(FilePrinter.INSERT_CHAR,
						"Inserting char failed " + this.name);
				return false;
				// throw new RuntimeException("Inserting char failed.");
			}

			ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
			ps.setInt(1, this.id);
			for (int i = 0; i < DEFAULT_KEY.length; i++) {
				ps.setInt(2, DEFAULT_KEY[i]);
				ps.setInt(3, DEFAULT_TYPE[i]);
				ps.setInt(4, DEFAULT_ACTION[i]);
				ps.execute();
			}
			ps.close();

			final List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

			for (final MapleInventory iv : this.inventory) {
				for (final Item item : iv.list()) {
					itemsWithType.add(new Pair<>(item, iv.getType()));
				}
			}

			ItemFactory.INVENTORY.saveItems(itemsWithType, this.id);

			/*
			 * //jobs start with skills :| ps = con.prepareStatement(
			 * "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)"
			 * ); ps.setInt(1, id); for (final Entry<Skill, SkillEntry> skill :
			 * skills.entrySet()) { ps.setInt(2, skill.getKey().getId());
			 * ps.setInt(3, skill.getValue().skillevel); ps.setInt(4,
			 * skill.getValue().masterlevel); ps.setLong(5,
			 * skill.getValue().expiration); ps.execute(); } ps.close();
			 *
			 * //sometimes starts with quests too :| ps = con.prepareStatement(
			 * "INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)"
			 * , DatabaseConnection.RETURN_GENERATED_KEYS); try
			 * (PreparedStatement pse = con.prepareStatement(
			 * "INSERT INTO questprogress VALUES (DEFAULT, ?, ?, ?)")) {
			 * ps.setInt(1, id); for (MapleQuestStatus q : quests.values()) {
			 * ps.setInt(2, q.getQuest().getId()); ps.setInt(3,
			 * q.getStatus().getId()); ps.setLong(4, q.getCompletionTime());
			 * ps.setInt(5, q.getForfeited()); ps.executeUpdate(); try
			 * (ResultSet rse = ps.getGeneratedKeys()) { rse.next(); for (int
			 * mob : q.getProgress().keySet()) { pse.setInt(1, rse.getInt(1));
			 * pse.setInt(2, mob); pse.setString(3, q.getProgress(mob));
			 * pse.addBatch(); } pse.executeBatch(); } } } don't think this is
			 * needed for v83
			 */

			con.commit();
			return true;
		} catch (final Throwable t) {
			FilePrinter.printError(FilePrinter.INSERT_CHAR, t,
					"Error creating " + this.name + " Level: " + this.level
							+ " Job: " + this.job.getId());
			try {
				con.rollback();
			} catch (final SQLException se) {
				FilePrinter.printError(FilePrinter.INSERT_CHAR, se,
						"Error trying to rollback " + this.name);
			}
			return false;
		} finally {
			try {
				if ((ps != null) && !ps.isClosed()) {
					ps.close();
				}
				con.setAutoCommit(true);
				con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} catch (final SQLException e) {
			}
		}
	}

	public void saveToDB() {
		final Connection con = DatabaseConnection.getConnection();
		try {
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
			PreparedStatement ps;
			ps = con.prepareStatement(
					"UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, gachaexp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpMpUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, mountlevel = ?, mountexp = ?, mounttiredness= ?, equipslots = ?, useslots = ?, setupslots = ?, etcslots = ?,  monsterbookcover = ?, vanquisherStage = ?, dojoPoints = ?, lastDojoStage = ?, finishedDojoTutorial = ?, vanquisherKills = ?, matchcardwins = ?, matchcardlosses = ?, matchcardties = ?, omokwins = ?, omoklosses = ?, omokties = ? WHERE id = ?",
					Statement.RETURN_GENERATED_KEYS);
			if ((this.gmLevel < 1) && (this.level > 199)) {
				ps.setInt(1, this.isCygnus() ? 120 : 200);
			} else {
				ps.setInt(1, this.level);
			}
			ps.setInt(2, this.fame);
			ps.setInt(3, this.str);
			ps.setInt(4, this.dex);
			ps.setInt(5, this.luk);
			ps.setInt(6, this.int_);
			ps.setInt(7, Math.abs(this.exp.get()));
			ps.setInt(8, Math.abs(this.gachaexp.get()));
			ps.setInt(9, this.hp);
			ps.setInt(10, this.mp);
			ps.setInt(11, this.maxhp);
			ps.setInt(12, this.maxmp);
			ps.setInt(13, this.remainingSp);
			ps.setInt(14, this.remainingAp);
			ps.setInt(15, this.gmLevel);
			ps.setInt(16, this.skinColor.getId());
			ps.setInt(17, this.gender);
			ps.setInt(18, this.job.getId());
			ps.setInt(19, this.hair);
			ps.setInt(20, this.face);
			if ((this.map == null)
					|| ((this.cashshop != null) && this.cashshop.isOpened())) {
				ps.setInt(21, this.mapid);
			} else {
				if (this.map.getForcedReturnId() != 999999999) {
					ps.setInt(21, this.map.getForcedReturnId());
				} else {
					ps.setInt(21, this.getHp() < 1 ? this.map.getReturnMapId()
							: this.map.getId());
				}
			}
			ps.setInt(22, this.meso.get());
			ps.setInt(23, this.hpMpApUsed);
			if ((this.map == null) || (this.map.getId() == 610020000)
					|| (this.map.getId() == 610020001)) {
				ps.setInt(24, 0);
			} else {
				final MaplePortal closest = this.map.findClosestSpawnpoint(this
						.getPosition());
				if (closest != null) {
					ps.setInt(24, closest.getId());
				} else {
					ps.setInt(24, 0);
				}
			}
			if (this.party != null) {
				ps.setInt(25, this.party.getId());
			} else {
				ps.setInt(25, -1);
			}
			ps.setInt(26, this.buddylist.getCapacity());
			if (this.messenger != null) {
				ps.setInt(27, this.messenger.getId());
				ps.setInt(28, this.messengerposition);
			} else {
				ps.setInt(27, 0);
				ps.setInt(28, 4);
			}
			if (this.maplemount != null) {
				ps.setInt(29, this.maplemount.getLevel());
				ps.setInt(30, this.maplemount.getExp());
				ps.setInt(31, this.maplemount.getTiredness());
			} else {
				ps.setInt(29, 1);
				ps.setInt(30, 0);
				ps.setInt(31, 0);
			}
			for (int i = 1; i < 5; i++) {
				ps.setInt(i + 31, this.getSlots(i));
			}

			this.monsterbook.saveCards(this.getId());

			ps.setInt(36, this.bookCover);
			ps.setInt(37, this.vanquisherStage);
			ps.setInt(38, this.dojoPoints);
			ps.setInt(39, this.dojoStage);
			ps.setInt(40, this.finishedDojoTutorial ? 1 : 0);
			ps.setInt(41, this.vanquisherKills);
			ps.setInt(42, this.matchcardwins);
			ps.setInt(43, this.matchcardlosses);
			ps.setInt(44, this.matchcardties);
			ps.setInt(45, this.omokwins);
			ps.setInt(46, this.omoklosses);
			ps.setInt(47, this.omokties);
			ps.setInt(48, this.id);

			final int updateRows = ps.executeUpdate();
			if (updateRows < 1) {
				throw new RuntimeException("Character not in database ("
						+ this.id + ")");
			}
			for (int i = 0; i < 3; i++) {
				if (this.pets[i] != null) {
					this.pets[i].saveToDb();
				}
			}
			this.deleteWhereCharacterId(con,
					"DELETE FROM keymap WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
			ps.setInt(1, this.id);
			for (final Entry<Integer, MapleKeyBinding> keybinding : this.keymap
					.entrySet()) {
				ps.setInt(2, keybinding.getKey().intValue());
				ps.setInt(3, keybinding.getValue().getType());
				ps.setInt(4, keybinding.getValue().getAction());
				ps.addBatch();
			}
			ps.executeBatch();
			this.deleteWhereCharacterId(con,
					"DELETE FROM skillmacros WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, this.getId());
			for (int i = 0; i < 5; i++) {
				final SkillMacro macro = this.skillMacros[i];
				if (macro != null) {
					ps.setInt(2, macro.getSkill1());
					ps.setInt(3, macro.getSkill2());
					ps.setInt(4, macro.getSkill3());
					ps.setString(5, macro.getName());
					ps.setInt(6, macro.getShout());
					ps.setInt(7, i);
					ps.addBatch();
				}
			}
			ps.executeBatch();
			final List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

			for (final MapleInventory iv : this.inventory) {
				for (final Item item : iv.list()) {
					itemsWithType.add(new Pair<>(item, iv.getType()));
				}
			}

			ItemFactory.INVENTORY.saveItems(itemsWithType, this.id);
			this.deleteWhereCharacterId(con,
					"DELETE FROM skills WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, this.id);
			for (final Entry<Skill, SkillEntry> skill : this.skills.entrySet()) {
				ps.setInt(2, skill.getKey().getId());
				ps.setInt(3, skill.getValue().skillevel);
				ps.setInt(4, skill.getValue().masterlevel);
				ps.setLong(5, skill.getValue().expiration);
				ps.addBatch();
			}
			ps.executeBatch();
			this.deleteWhereCharacterId(con,
					"DELETE FROM savedlocations WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`, `portal`) VALUES (?, ?, ?, ?)");
			ps.setInt(1, this.id);
			for (final SavedLocationType savedLocationType : SavedLocationType
					.values()) {
				if (this.savedLocations[savedLocationType.ordinal()] != null) {
					ps.setString(2, savedLocationType.name());
					ps.setInt(3, this.savedLocations[savedLocationType
							.ordinal()].getMapId());
					ps.setInt(4, this.savedLocations[savedLocationType
							.ordinal()].getPortal());
					ps.addBatch();
				}
			}
			ps.executeBatch();
			this.deleteWhereCharacterId(con,
					"DELETE FROM trocklocations WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 0)");
			for (int i = 0; i < this.getTrockSize(); i++) {
				if (this.trockmaps[i] != 999999999) {
					ps.setInt(1, this.getId());
					ps.setInt(2, this.trockmaps[i]);
					ps.addBatch();
				}
			}
			ps.executeBatch();
			ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 1)");
			for (int i = 0; i < this.getVipTrockSize(); i++) {
				if (this.viptrockmaps[i] != 999999999) {
					ps.setInt(1, this.getId());
					ps.setInt(2, this.viptrockmaps[i]);
					ps.addBatch();
				}
			}
			ps.executeBatch();
			this.deleteWhereCharacterId(con,
					"DELETE FROM buddies WHERE characterid = ? AND pending = 0");
			ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `group`) VALUES (?, ?, 0, ?)");
			ps.setInt(1, this.id);
			for (final BuddyListEntry entry : this.buddylist.getBuddies()) {
				if (entry.isVisible()) {
					ps.setInt(2, entry.getCharacterId());
					ps.setString(3, entry.getGroup());
					ps.addBatch();
				}
			}
			ps.executeBatch();
			this.deleteWhereCharacterId(con,
					"DELETE FROM area_info WHERE charid = ?");
			ps = con.prepareStatement("INSERT INTO area_info (id, charid, area, info) VALUES (DEFAULT, ?, ?, ?)");
			ps.setInt(1, this.id);
			for (final Entry<Short, String> area : this.area_info.entrySet()) {
				ps.setInt(2, area.getKey());
				ps.setString(3, area.getValue());
				ps.addBatch();
			}
			ps.executeBatch();
			this.deleteWhereCharacterId(con,
					"DELETE FROM eventstats WHERE characterid = ?");
			this.deleteWhereCharacterId(con,
					"DELETE FROM queststatus WHERE characterid = ?");
			ps = con.prepareStatement(
					"INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			PreparedStatement psf;
			try (PreparedStatement pse = con
					.prepareStatement("INSERT INTO questprogress VALUES (DEFAULT, ?, ?, ?)")) {
				psf = con
						.prepareStatement("INSERT INTO medalmaps VALUES (DEFAULT, ?, ?)");
				ps.setInt(1, this.id);
				for (final MapleQuestStatus q : this.quests.values()) {
					ps.setInt(2, q.getQuest().getId());
					ps.setInt(3, q.getStatus().getId());
					ps.setInt(4, (int) (q.getCompletionTime() / 1000));
					ps.setInt(5, q.getForfeited());
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						rs.next();
						for (final int mob : q.getProgress().keySet()) {
							pse.setInt(1, rs.getInt(1));
							pse.setInt(2, mob);
							pse.setString(3, q.getProgress(mob));
							pse.addBatch();
						}
						for (int i = 0; i < q.getMedalMaps().size(); i++) {
							psf.setInt(1, rs.getInt(1));
							psf.setInt(2, q.getMedalMaps().get(i));
							psf.addBatch();
						}
						pse.executeBatch();
						psf.executeBatch();
					}
				}
			}
			psf.close();
			ps = con.prepareStatement("UPDATE accounts SET gm = ? WHERE id = ?");
			ps.setInt(1, this.gmLevel);
			ps.setInt(2, this.client.getAccID());
			ps.executeUpdate();
			if (this.cashshop != null) {
				this.cashshop.save();
			}
			if (this.storage != null) {
				this.storage.saveToDB();
			}
			ps.close();
			con.commit();
		} catch (SQLException | RuntimeException t) {
			FilePrinter.printError(FilePrinter.SAVE_CHAR, t,
					"Error saving " + this.name + " Level: " + this.level
							+ " Job: " + this.job.getId());
			try {
				con.rollback();
			} catch (final SQLException se) {
				FilePrinter.printError(FilePrinter.SAVE_CHAR, se,
						"Error trying to rollback " + this.name);
			}
		} finally {
			try {
				con.setAutoCommit(true);
				con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} catch (final Exception e) {
			}
		}
	}

	public void sendPolice(int greason, String reason, int duration) {
		this.announce(MaplePacketCreator.sendPolice(String.format(
				"You have been blocked by #bPolice %s for the %s reason.#k",
				"Moople", "HACK")));
		this.isbanned = true;
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleCharacter.this.client.disconnect(false, false); // FAGGOTS
			}
		}, duration);
	}

	public void sendPolice(String text) {
		this.announce(MaplePacketCreator.sendPolice(text));
		this.isbanned = true;
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleCharacter.this.client.disconnect(false, false); // FAGGOTS
			}
		}, 6000);
	}

	public void sendKeymap() {
		this.client.announce(MaplePacketCreator.getKeymap(this.keymap));
	}

	public void sendMacros() {
		boolean macros = false;
		for (int i = 0; i < 5; i++) {
			if (this.skillMacros[i] != null) {
				macros = true;
			}
		}
		if (macros) {
			this.client
					.announce(MaplePacketCreator.getMacros(this.skillMacros));
		}
	}

	public void sendNote(String to, String msg, byte fame) throws SQLException {
		try (PreparedStatement ps = DatabaseConnection
				.getConnection()
				.prepareStatement(
						"INSERT INTO notes (`to`, `from`, `message`, `timestamp`, `fame`) VALUES (?, ?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, to);
			ps.setString(2, this.getName());
			ps.setString(3, msg);
			ps.setLong(4, System.currentTimeMillis());
			ps.setByte(5, fame);
			ps.executeUpdate();
		}
	}

	public void setAllianceRank(int rank) {
		this.allianceRank = rank;
		if (this.mgc != null) {
			this.mgc.setAllianceRank(rank);
		}
	}

	public void setAllowWarpToId(int id) {
		this.warpToId = id;
	}

	public static void setAriantRoomLeader(int room, String charname) {
		ariantroomleader[room] = charname;
	}

	public static void setAriantSlotRoom(int room, int slot) {
		ariantroomslot[room] = slot;
	}

	public void setBattleshipHp(int battleshipHp) {
		this.battleshipHp = battleshipHp;
	}

	public void setBuddyCapacity(int capacity) {
		this.buddylist.setCapacity(capacity);
		this.client.announce(MaplePacketCreator.updateBuddyCapacity(capacity));
	}

	public void setBuffedValue(MapleBuffStat effect, int value) {
		final MapleBuffStatValueHolder mbsvh = this.effects.get(effect);
		if (mbsvh == null) {
			return;
		}
		mbsvh.value = value;
	}

	public void setChair(int chair) {
		this.chair = chair;
	}

	public void setChalkboard(String text) {
		this.chalktext = text;
	}

	public void setDex(int dex) {
		this.dex = dex;
		this.recalcLocalStats();
	}

	public void setDojoEnergy(int x) {
		this.dojoEnergy = x;
	}

	public void setDojoParty(boolean b) {
		this.dojoParty = b;
	}

	public void setDojoPoints(int x) {
		this.dojoPoints = x;
	}

	public void setDojoStage(int x) {
		this.dojoStage = x;
	}

	public void setDojoStart() {
		this.dojoMap = this.map;
		final int stage = (this.map.getId() / 100) % 100;
		this.dojoFinish = System.currentTimeMillis()
				+ ((stage > 36 ? 15 : (stage / 6) + 5) * 60000);
	}

	public void setRates() {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT-8"));
		final World worldz = Server.getInstance().getWorld(this.world);
		final int hr = cal.get(Calendar.HOUR_OF_DAY);
		if ((this.haveItem(5360001) && (hr > 6) && (hr < 12))
				|| (this.haveItem(5360002) && (hr > 9) && (hr < 15))
				|| (this.haveItem(536000) && (hr > 12) && (hr < 18))
				|| (this.haveItem(5360004) && (hr > 15) && (hr < 21))
				|| (this.haveItem(536000) && (hr > 18))
				|| (this.haveItem(5360006) && (hr < 5))
				|| (this.haveItem(5360007) && (hr > 2) && (hr < 6))
				|| (this.haveItem(5360008) && (hr >= 6) && (hr < 11))) {
			this.dropRate = 2 * worldz.getDropRate();
			this.mesoRate = 2 * worldz.getMesoRate();
		} else {
			this.dropRate = worldz.getDropRate();
			this.mesoRate = worldz.getMesoRate();
		}
		if ((this.haveItem(5211000) && (hr > 17) && (hr < 21))
				|| (this.haveItem(5211014) && (hr > 6) && (hr < 12))
				|| (this.haveItem(5211015) && (hr > 9) && (hr < 15))
				|| (this.haveItem(5211016) && (hr > 12) && (hr < 18))
				|| (this.haveItem(5211017) && (hr > 15) && (hr < 21))
				|| (this.haveItem(5211018) && (hr > 14))
				|| (this.haveItem(5211039) && (hr < 5))
				|| (this.haveItem(5211042) && (hr > 2) && (hr < 8))
				|| (this.haveItem(5211045) && (hr > 5) && (hr < 11))
				|| this.haveItem(5211048)) {
			if (this.isBeginnerJob()) {
				this.expRate = 2;
			} else {
				this.expRate = 2 * worldz.getExpRate();
				;
			}
		} else {
			if (this.isBeginnerJob()) {
				this.expRate = 1;
			} else {
				this.expRate = worldz.getExpRate();
				;
			}
		}
	}

	public void setEnergyBar(int set) {
		this.energybar = set;
	}

	public void setEventInstance(EventInstanceManager eventInstance) {
		this.eventInstance = eventInstance;
	}

	public void setExp(int amount) {
		this.exp.set(amount);
	}

	public void setGachaExp(int amount) {
		this.gachaexp.set(amount);
	}

	public void setFace(int face) {
		this.face = face;
	}

	public void setFame(int fame) {
		this.fame = fame;
	}

	public void setFamilyId(int familyId) {
		this.familyId = familyId;
	}

	public void setFinishedDojoTutorial() {
		this.finishedDojoTutorial = true;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public void setGM(int level) {
		this.gmLevel = level;
	}

	public void setGuildId(int _id) {
		this.guildid = _id;
		if (this.guildid > 0) {
			if (this.mgc == null) {
				this.mgc = new MapleGuildCharacter(this);
			} else {
				this.mgc.setGuildId(this.guildid);
			}
		} else {
			this.mgc = null;
		}
	}

	public void setGuildRank(int _rank) {
		this.guildrank = _rank;
		if (this.mgc != null) {
			this.mgc.setGuildRank(_rank);
		}
	}

	public void setHair(int hair) {
		this.hair = hair;
	}

	public void setHasMerchant(boolean set) {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE characters SET HasMerchant = ? WHERE id = ?")) {
				ps.setInt(1, set ? 1 : 0);
				ps.setInt(2, this.id);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		this.hasMerchant = set;
	}

	public void addMerchantMesos(int add) {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE characters SET MerchantMesos = ? WHERE id = ?",
							Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, this.merchantmeso + add);
				ps.setInt(2, this.id);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			return;
		}
		this.merchantmeso += add;
	}

	public void setMerchantMeso(int set) {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE characters SET MerchantMesos = ? WHERE id = ?",
							Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, set);
				ps.setInt(2, this.id);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			return;
		}
		this.merchantmeso = set;
	}

	public void setHiredMerchant(HiredMerchant merchant) {
		this.hiredMerchant = merchant;
	}

	public void setHp(int newhp) {
		this.setHp(newhp, false);
	}

	public void setHp(int newhp, boolean silent) {
		final int oldHp = this.hp;
		int thp = newhp;
		if (thp < 0) {
			thp = 0;
		}
		if (thp > this.localmaxhp) {
			thp = this.localmaxhp;
		}
		this.hp = thp;
		if (!silent) {
			this.updatePartyMemberHP();
		}
		if ((oldHp > this.hp) && !this.isAlive()) {
			this.playerDead();
		}
	}

	public void setHpMpApUsed(int mpApUsed) {
		this.hpMpApUsed = mpApUsed;
	}

	public void setHpMp(int x) {
		this.setHp(x);
		this.setMp(x);
		this.updateSingleStat(MapleStat.HP, this.hp);
		this.updateSingleStat(MapleStat.MP, this.mp);
	}

	public void setInt(int int_) {
		this.int_ = int_;
		this.recalcLocalStats();
	}

	public void setInventory(MapleInventoryType type, MapleInventory inv) {
		this.inventory[type.ordinal()] = inv;
	}

	public void setItemEffect(int itemEffect) {
		this.itemEffect = itemEffect;
	}

	public void setJob(MapleJob job) {
		this.job = job;
	}

	public void setLastHealed(long time) {
		this.lastHealed = time;
	}

	public void setLastUsedCashItem(long time) {
		this.lastUsedCashItem = time;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setLuk(int luk) {
		this.luk = luk;
		this.recalcLocalStats();
	}

	public void setMap(int PmapId) {
		this.mapid = PmapId;
	}

	public void setMap(MapleMap newmap) {
		this.map = newmap;
	}

	public void setMarkedMonster(int markedMonster) {
		this.markedMonster = markedMonster;
	}

	public void setMaxHp(int hp) {
		this.maxhp = hp;
		this.recalcLocalStats();
	}

	public void setMaxHp(int hp, boolean ap) {
		hp = Math.min(30000, hp);
		if (ap) {
			this.setHpMpApUsed(this.getHpMpApUsed() + 1);
		}
		this.maxhp = hp;
		this.recalcLocalStats();
	}

	public void setMaxMp(int mp) {
		this.maxmp = mp;
		this.recalcLocalStats();
	}

	public void setMaxMp(int mp, boolean ap) {
		mp = Math.min(30000, mp);
		if (ap) {
			this.setHpMpApUsed(this.getHpMpApUsed() + 1);
		}
		this.maxmp = mp;
		this.recalcLocalStats();
	}

	public void setMessenger(MapleMessenger messenger) {
		this.messenger = messenger;
	}

	public void setMessengerPosition(int position) {
		this.messengerposition = position;
	}

	public void setMiniGame(MapleMiniGame miniGame) {
		this.miniGame = miniGame;
	}

	public void setMiniGamePoints(MapleCharacter visitor, int winnerslot,
			boolean omok) {
		if (omok) {
			if (winnerslot == 1) {
				this.omokwins++;
				visitor.omoklosses++;
			} else if (winnerslot == 2) {
				visitor.omokwins++;
				this.omoklosses++;
			} else {
				this.omokties++;
				visitor.omokties++;
			}
		} else {
			if (winnerslot == 1) {
				this.matchcardwins++;
				visitor.matchcardlosses++;
			} else if (winnerslot == 2) {
				visitor.matchcardwins++;
				this.matchcardlosses++;
			} else {
				this.matchcardties++;
				visitor.matchcardties++;
			}
		}
	}

	public void setMonsterBookCover(int bookCover) {
		this.bookCover = bookCover;
	}

	public void setMp(int newmp) {
		int tmp = newmp;
		if (tmp < 0) {
			tmp = 0;
		}
		if (tmp > this.localmaxmp) {
			tmp = this.localmaxmp;
		}
		this.mp = tmp;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setParty(MapleParty party) {
		if (party == null) {
			this.mpc = null;
		}
		this.party = party;
	}

	public void setPlayerShop(MaplePlayerShop playerShop) {
		this.playerShop = playerShop;
	}

	public void setRemainingAp(int remainingAp) {
		this.remainingAp = remainingAp;
	}

	public void setRemainingSp(int remainingSp) {
		this.remainingSp = remainingSp;
	}

	public void setSearch(String find) {
		this.search = find;
	}

	public void setSkinColor(MapleSkinColor skinColor) {
		this.skinColor = skinColor;
	}

	public byte getSlots(int type) {
		return type == MapleInventoryType.CASH.getType() ? 96
				: this.inventory[type].getSlotLimit();
	}

	public boolean gainSlots(int type, int slots) {
		return this.gainSlots(type, slots, true);
	}

	public boolean gainSlots(int type, int slots, boolean update) {
		slots += this.inventory[type].getSlotLimit();
		if (slots <= 96) {
			this.inventory[type].setSlotLimit(slots);

			this.saveToDB();
			if (update) {
				this.client.announce(MaplePacketCreator
						.updateInventorySlotLimit(type, slots));
			}

			return true;
		}

		return false;
	}

	public void setShop(MapleShop shop) {
		this.shop = shop;
	}

	public void setSlot(int slotid) {
		this.slots = slotid;
	}

	public void setStr(int str) {
		this.str = str;
		this.recalcLocalStats();
	}

	public void setTrade(MapleTrade trade) {
		this.trade = trade;
	}

	public void setVanquisherKills(int x) {
		this.vanquisherKills = x;
	}

	public void setVanquisherStage(int x) {
		this.vanquisherStage = x;
	}

	public void setWorld(int world) {
		this.world = world;
	}

	public void shiftPetsRight() {
		if (this.pets[2] == null) {
			this.pets[2] = this.pets[1];
			this.pets[1] = this.pets[0];
			this.pets[0] = null;
		}
	}

	public void showDojoClock() {
		final int stage = (this.map.getId() / 100) % 100;
		long time;
		if ((stage % 6) == 1) {
			time = (stage > 36 ? 15 : (stage / 6) + 5) * 60;
		} else {
			time = (this.dojoFinish - System.currentTimeMillis()) / 1000;
		}
		if ((stage % 6) > 0) {
			this.client.announce(MaplePacketCreator.getClock((int) time));
		}
		boolean rightmap = true;
		final int clockid = (this.dojoMap.getId() / 100) % 100;
		if ((this.map.getId() > (((clockid / 6) * 6) + 6))
				|| (this.map.getId() < ((clockid / 6) * 6))) {
			rightmap = false;
		}
		final boolean rightMap = rightmap; // lol
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (rightMap) {
					MapleCharacter.this.client.getPlayer().changeMap(
							MapleCharacter.this.client.getChannelServer()
									.getMapFactory().getMap(925020000));
				}
			}
		}, (time * 1000) + 3000); // let the TIMES UP display for 3 seconds,
									// then
									// warp
	}

	public void showNote() {
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT * FROM notes WHERE `to`=? AND `deleted` = 0",
							ResultSet.TYPE_SCROLL_SENSITIVE,
							ResultSet.CONCUR_UPDATABLE)) {
				ps.setString(1, this.getName());
				try (ResultSet rs = ps.executeQuery()) {
					rs.last();
					final int count = rs.getRow();
					rs.first();
					this.client.announce(MaplePacketCreator
							.showNotes(rs, count));
				}
			}
		} catch (final SQLException e) {
		}
	}

	private void silentEnforceMaxHpMp() {
		this.setMp(this.getMp());
		this.setHp(this.getHp(), true);
	}

	public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
		for (final PlayerBuffValueHolder mbsvh : buffs) {
			mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
		}
	}

	public void silentPartyUpdate() {
		if (this.party != null) {
			Server.getInstance()
					.getWorld(this.world)
					.updateParty(this.party.getId(),
							PartyOperation.SILENT_UPDATE, this.getMPC());
		}
	}

	public static class SkillEntry {

		public int masterlevel;
		public byte skillevel;
		public long expiration;

		public SkillEntry(byte skillevel, int masterlevel, long expiration) {
			this.skillevel = skillevel;
			this.masterlevel = masterlevel;
			this.expiration = expiration;
		}

		@Override
		public String toString() {
			return this.skillevel + ":" + this.masterlevel;
		}
	}

	public boolean skillisCooling(int skillId) {
		return this.coolDowns.containsKey(Integer.valueOf(skillId));
	}

	public void startFullnessSchedule(final int decrease, final MaplePet pet,
			int petSlot) {
		ScheduledFuture<?> schedule;
		schedule = TimerManager.getInstance().register(new Runnable() {
			@Override
			public void run() {
				final int newFullness = pet.getFullness() - decrease;
				if (newFullness <= 5) {
					pet.setFullness(15);
					pet.saveToDb();
					MapleCharacter.this.unequipPet(pet, true);
				} else {
					pet.setFullness(newFullness);
					pet.saveToDb();
					final Item petz = MapleCharacter.this.getInventory(
							MapleInventoryType.CASH).getItem(pet.getPosition());
					MapleCharacter.this.forceUpdateItem(petz);
				}
			}
		}, 180000, 18000);
		this.fullnessSchedule[petSlot] = schedule;

	}

	public void startMapEffect(String msg, int itemId) {
		this.startMapEffect(msg, itemId, 30000);
	}

	public void startMapEffect(String msg, int itemId, int duration) {
		final MapleMapEffect mapEffect = new MapleMapEffect(msg, itemId);
		this.getClient().announce(mapEffect.makeStartData());
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleCharacter.this.getClient().announce(
						mapEffect.makeDestroyData());
			}
		}, duration);
	}

	public void stopControllingMonster(MapleMonster monster) {
		this.controlled.remove(monster);
	}

	public void unequipAllPets() {
		for (int i = 0; i < 3; i++) {
			if (this.pets[i] != null) {
				this.unequipPet(this.pets[i], true);
			}
		}
	}

	public void unequipPet(MaplePet pet, boolean shift_left) {
		this.unequipPet(pet, shift_left, false);
	}

	public void unequipPet(MaplePet pet, boolean shift_left, boolean hunger) {
		if (this.getPet(this.getPetIndex(pet)) != null) {
			this.getPet(this.getPetIndex(pet)).setSummoned(false);
			this.getPet(this.getPetIndex(pet)).saveToDb();
		}
		this.cancelFullnessSchedule(this.getPetIndex(pet));
		this.getMap().broadcastMessage(this,
				MaplePacketCreator.showPet(this, pet, true, hunger), true);
		this.client.announce(MaplePacketCreator.petStatUpdate(this));
		this.client.announce(MaplePacketCreator.enableActions());
		this.removePet(pet, shift_left);
	}

	public void updateMacros(int position, SkillMacro updateMacro) {
		this.skillMacros[position] = updateMacro;
	}

	public void updatePartyMemberHP() {
		if (this.party != null) {
			final int channel = this.client.getChannel();
			for (final MaplePartyCharacter partychar : this.party.getMembers()) {
				if ((partychar.getMapId() == this.getMapId())
						&& (partychar.getChannel() == channel)) {
					final MapleCharacter other = Server.getInstance()
							.getWorld(this.world).getChannel(channel)
							.getPlayerStorage()
							.getCharacterByName(partychar.getName());
					if (other != null) {
						other.client.announce(MaplePacketCreator
								.updatePartyMemberHP(this.getId(), this.hp,
										this.maxhp));
					}
				}
			}
		}
	}

	public void updateQuest(MapleQuestStatus quest) {
		this.quests.put(quest.getQuest(), quest);
		if (quest.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
			this.announce(MaplePacketCreator.questProgress(quest.getQuest()
					.getId(), quest.getProgress(0)));
			if (quest.getQuest().getInfoNumber() > 0) {
				this.announce(MaplePacketCreator.questProgress(quest.getQuest()
						.getInfoNumber(), Integer.toString(quest
						.getMedalProgress())));
			}
			this.announce(MaplePacketCreator.updateQuestInfo(quest.getQuest()
					.getId(), quest.getNpc()));
		} else if (quest.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
			this.announce(MaplePacketCreator.completeQuest(quest.getQuest()
					.getId(), quest.getCompletionTime()));
		} else if (quest.getStatus()
				.equals(MapleQuestStatus.Status.NOT_STARTED)) {
			this.announce(MaplePacketCreator.forfeitQuest(quest.getQuest()
					.getId()));
		}
	}

	public void questTimeLimit(final MapleQuest quest, int time) {
		final ScheduledFuture<?> sf = TimerManager.getInstance().schedule(
				new Runnable() {
					@Override
					public void run() {
						MapleCharacter.this.announce(MaplePacketCreator
								.questExpire(quest.getId()));
						final MapleQuestStatus newStatus = new MapleQuestStatus(
								quest, MapleQuestStatus.Status.NOT_STARTED);
						newStatus.setForfeited(MapleCharacter.this.getQuest(
								quest).getForfeited() + 1);
						MapleCharacter.this.updateQuest(newStatus);
					}
				}, time);
		this.announce(MaplePacketCreator.addQuestTimeLimit(quest.getId(), time));
		this.timers.add(sf);
	}

	public void updateSingleStat(MapleStat stat, int newval) {
		this.updateSingleStat(stat, newval, false);
	}

	private void updateSingleStat(MapleStat stat, int newval,
			boolean itemReaction) {
		this.announce(MaplePacketCreator.updatePlayerStats(Collections
				.singletonList(new Pair<>(stat, Integer.valueOf(newval))),
				itemReaction));
	}

	public void announce(final byte[] packet) {
		this.client.announce(packet);
	}

	@Override
	public int getObjectId() {
		return this.getId();
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.PLAYER;
	}

	@Override
	public void sendDestroyData(MapleClient client) {
		client.announce(MaplePacketCreator.removePlayerFromMap(this
				.getObjectId()));
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		if (!this.isHidden() || (client.getPlayer().gmLevel() > 0)) {
			client.announce(MaplePacketCreator.spawnPlayerMapobject(this));
		}
	}

	@Override
	public void setObjectId(int id) {
	}

	@Override
	public String toString() {
		return this.name;
	}

	private int givenRiceCakes;
	private boolean gottenRiceHat;

	public int getGivenRiceCakes() {
		return this.givenRiceCakes;
	}

	public void increaseGivenRiceCakes(int amount) {
		this.givenRiceCakes += amount;
	}

	public boolean getGottenRiceHat() {
		return this.gottenRiceHat;
	}

	public void setGottenRiceHat(boolean b) {
		this.gottenRiceHat = b;
	}

	public int getLinkedLevel() {
		return this.linkedLevel;
	}

	public String getLinkedName() {
		return this.linkedName;
	}

	public CashShop getCashShop() {
		return this.cashshop;
	}

	public void portalDelay(long delay) {
		this.portaldelay = System.currentTimeMillis() + delay;
	}

	public long portalDelay() {
		return this.portaldelay;
	}

	public void blockPortal(String scriptName) {
		if (!this.blockedPortals.contains(scriptName) && (scriptName != null)) {
			this.blockedPortals.add(scriptName);
			this.client.announce(MaplePacketCreator.enableActions());
		}
	}

	public void unblockPortal(String scriptName) {
		if (this.blockedPortals.contains(scriptName) && (scriptName != null)) {
			this.blockedPortals.remove(scriptName);
		}
	}

	public List<String> getBlockedPortals() {
		return this.blockedPortals;
	}

	public boolean containsAreaInfo(int area, String info) {
		final Short area_ = Short.valueOf((short) area);
		if (this.area_info.containsKey(area_)) {
			return this.area_info.get(area_).contains(info);
		}
		return false;
	}

	public void updateAreaInfo(int area, String info) {
		this.area_info.put(Short.valueOf((short) area), info);
		this.announce(MaplePacketCreator.updateAreaInfo(area, info));
	}

	public String getAreaInfo(int area) {
		return this.area_info.get(Short.valueOf((short) area));
	}

	public Map<Short, String> getAreaInfos() {
		return this.area_info;
	}

	public void autoban(String reason, int greason) {
		final Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
				cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE));
		final Timestamp TS = new Timestamp(cal.getTimeInMillis());
		try {
			final Connection con = DatabaseConnection.getConnection();
			try (PreparedStatement ps = con
					.prepareStatement("UPDATE accounts SET banreason = ?, tempban = ?, greason = ? WHERE id = ?")) {
				ps.setString(1, reason);
				ps.setTimestamp(2, TS);
				ps.setInt(3, greason);
				ps.setInt(4, this.accountid);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public void block(int reason, int days, String desc) {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days);
		final Timestamp TS = new Timestamp(cal.getTimeInMillis());
		try {
			final Connection con = DatabaseConnection.getConnection();
			try (PreparedStatement ps = con
					.prepareStatement("UPDATE accounts SET banreason = ?, tempban = ?, greason = ? WHERE id = ?")) {
				ps.setString(1, desc);
				ps.setTimestamp(2, TS);
				ps.setInt(3, reason);
				ps.setInt(4, this.accountid);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
		}
	}

	public boolean isBanned() {
		return this.isbanned;
	}

	public int[] getTrockMaps() {
		return this.trockmaps;
	}

	public int[] getVipTrockMaps() {
		return this.viptrockmaps;
	}

	public int getTrockSize() {
		int ret = 0;
		for (int i = 0; i < 5; i++) {
			if (this.trockmaps[i] != 999999999) {
				ret++;
			}
		}
		return ret;
	}

	public void deleteFromTrocks(int map) {
		for (int i = 0; i < 5; i++) {
			if (this.trockmaps[i] == map) {
				this.trockmaps[i] = 999999999;
				break;
			}
		}
	}

	public void addTrockMap() {
		if (this.getTrockSize() >= 5) {
			return;
		}
		this.trockmaps[this.getTrockSize()] = this.getMapId();
	}

	public boolean isTrockMap(int id) {
		for (int i = 0; i < 5; i++) {
			if (this.trockmaps[i] == id) {
				return true;
			}
		}
		return false;
	}

	public int getVipTrockSize() {
		int ret = 0;
		for (int i = 0; i < 10; i++) {
			if (this.viptrockmaps[i] != 999999999) {
				ret++;
			}
		}
		return ret;
	}

	public void deleteFromVipTrocks(int map) {
		for (int i = 0; i < 10; i++) {
			if (this.viptrockmaps[i] == map) {
				this.viptrockmaps[i] = 999999999;
				break;
			}
		}
	}

	public void addVipTrockMap() {
		if (this.getVipTrockSize() >= 10) {
			return;
		}

		this.viptrockmaps[this.getVipTrockSize()] = this.getMapId();
	}

	public boolean isVipTrockMap(int id) {
		for (int i = 0; i < 10; i++) {
			if (this.viptrockmaps[i] == id) {
				return true;
			}
		}
		return false;
	}

	// EVENTS
	private byte team = 0;
	private MapleFitness fitness;
	private MapleOla ola;
	private long snowballattack;

	public byte getTeam() {
		return this.team;
	}

	public void setTeam(int team) {
		this.team = (byte) team;
	}

	public MapleOla getOla() {
		return this.ola;
	}

	public void setOla(MapleOla ola) {
		this.ola = ola;
	}

	public MapleFitness getFitness() {
		return this.fitness;
	}

	public void setFitness(MapleFitness fit) {
		this.fitness = fit;
	}

	public long getLastSnowballAttack() {
		return this.snowballattack;
	}

	public void setLastSnowballAttack(long time) {
		this.snowballattack = time;
	}

	// Monster Carnival
	private int cp = 0;
	private int obtainedcp = 0;
	private MonsterCarnivalParty carnivalparty;
	private MonsterCarnival carnival;

	public MonsterCarnivalParty getCarnivalParty() {
		return this.carnivalparty;
	}

	public void setCarnivalParty(MonsterCarnivalParty party) {
		this.carnivalparty = party;
	}

	public MonsterCarnival getCarnival() {
		return this.carnival;
	}

	public void setCarnival(MonsterCarnival car) {
		this.carnival = car;
	}

	public int getCP() {
		return this.cp;
	}

	public int getObtainedCP() {
		return this.obtainedcp;
	}

	public void addCP(int cp) {
		this.cp += cp;
		this.obtainedcp += cp;
	}

	public void useCP(int cp) {
		this.cp -= cp;
	}

	public void setObtainedCP(int cp) {
		this.obtainedcp = cp;
	}

	public int getAndRemoveCP() {
		int rCP = 10;
		if (this.cp < 9) {
			rCP = this.cp;
			this.cp = 0;
		} else {
			this.cp -= 10;
		}

		return rCP;
	}

	public AutobanManager getAutobanManager() {
		return this.autoban;
	}

	public void equipPendantOfSpirit() {
		if (this.pendantOfSpirit == null) {
			this.pendantOfSpirit = TimerManager.getInstance().register(
					new Runnable() {
						@Override
						public void run() {
							if (MapleCharacter.this.pendantExp < 3) {
								MapleCharacter.this.pendantExp++;
								MapleCharacter.this.addMessage("Pendant of the Spirit has been equipped for "
										+ MapleCharacter.this.pendantExp
										+ " hour(s), you will now receive "
										+ MapleCharacter.this.pendantExp
										+ "0% bonus exp.");
							} else {
								MapleCharacter.this.pendantOfSpirit.cancel(false);
							}
						}
					}, 3600000); // 1 hour
		}
	}

	public void unequipPendantOfSpirit() {
		if (this.pendantOfSpirit != null) {
			this.pendantOfSpirit.cancel(false);
			this.pendantOfSpirit = null;
		}
		this.pendantExp = 0;
	}

	public void increaseEquipExp(int mobexp) {
		final MapleItemInformationProvider mii = MapleItemInformationProvider
				.getInstance();
		for (final Item item : this.getInventory(MapleInventoryType.EQUIPPED)
				.list()) {
			final Equip nEquip = (Equip) item;
			final String itemName = mii.getName(nEquip.getItemId());
			if (itemName == null) {
				continue;
			}

			if ((itemName.contains("Reverse") && (nEquip.getItemLevel() < 4))
					|| (itemName.contains("Timeless") && (nEquip.getItemLevel() < 6))) {
				nEquip.gainItemExp(this.client, mobexp,
						itemName.contains("Timeless"));
			}
		}
	}

	public Map<String, MapleEvents> getEvents() {
		return this.events;
	}

	public PartyQuest getPartyQuest() {
		return this.partyQuest;
	}

	public void setPartyQuest(PartyQuest pq) {
		this.partyQuest = pq;
	}

	public final void empty(final boolean remove) {// lol serious shit here
		if (this.dragonBloodSchedule != null) {
			this.dragonBloodSchedule.cancel(false);
		}
		if (this.hpDecreaseTask != null) {
			this.hpDecreaseTask.cancel(false);
		}
		if (this.beholderHealingSchedule != null) {
			this.beholderHealingSchedule.cancel(false);
		}
		if (this.beholderBuffSchedule != null) {
			this.beholderBuffSchedule.cancel(false);
		}
		if (this.BerserkSchedule != null) {
			this.BerserkSchedule.cancel(false);
		}
		if (this.recoveryTask != null) {
			this.recoveryTask.cancel(false);
		}
		this.cancelExpirationTask();
		for (final ScheduledFuture<?> sf : this.timers) {
			sf.cancel(false);
		}
		this.timers.clear();
		if (this.maplemount != null) {
			this.maplemount.empty();
			this.maplemount = null;
		}
		if (remove) {
			this.partyQuest = null;
			this.events = null;
			this.mpc = null;
			this.mgc = null;
			this.events = null;
			this.party = null;
			this.family = null;
			this.client = null;
			this.map = null;
			this.timers = null;
		}
	}

	public void logOff() {
		this.loggedIn = false;
	}

	public boolean isLoggedin() {
		return this.loggedIn;
	}

	public void setMapId(int mapid) {
		this.mapid = mapid;
	}
}
