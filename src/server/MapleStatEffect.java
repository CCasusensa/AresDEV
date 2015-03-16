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
package server;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import net.server.PlayerCoolDownValueHolder;
import provider.MapleData;
import provider.MapleDataTool;
import server.life.MapleMonster;
import server.maps.FieldLimit;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import tools.ArrayMap;
import tools.MaplePacketCreator;
import tools.Pair;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleDisease;
import client.MapleJob;
import client.MapleMount;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ItemConstants;
import constants.skills.Aran;
import constants.skills.Assassin;
import constants.skills.Bandit;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.Crossbowman;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.FPWizard;
import constants.skills.Fighter;
import constants.skills.GM;
import constants.skills.Gunslinger;
import constants.skills.Hermit;
import constants.skills.Hero;
import constants.skills.Hunter;
import constants.skills.ILArchMage;
import constants.skills.ILMage;
import constants.skills.ILWizard;
import constants.skills.Legend;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Noblesse;
import constants.skills.Outlaw;
import constants.skills.Page;
import constants.skills.Paladin;
import constants.skills.Pirate;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.ThunderBreaker;
import constants.skills.WhiteKnight;
import constants.skills.WindArcher;

/**
 * @author Matze
 * @author Frz
 */
public class MapleStatEffect {

	private short watk, matk, wdef, mdef, acc, avoid, speed, jump;
	private short hp, mp;
	private double hpR, mpR;
	private short mpCon, hpCon;
	private int duration;
	private boolean overTime, repeatEffect;
	private int sourceid;
	private int moveTo;
	private boolean skill;
	private List<Pair<MapleBuffStat, Integer>> statups;
	private Map<MonsterStatus, Integer> monsterStatus;
	private int x, y, mobCount, moneyCon, cooldown, morphId = 0, ghost,
			fatigue, berserk, booster;
	private double prop;
	private int itemCon, itemConNo;
	private int damage, attackCount, fixdamage;
	private Point lt, rb;
	private byte bulletCount, bulletConsume;

	public static MapleStatEffect loadSkillEffectFromData(MapleData source,
			int skillid, boolean overtime) {
		return loadFromData(source, skillid, true, overtime);
	}

	public static MapleStatEffect loadItemEffectFromData(MapleData source,
			int itemid) {
		return loadFromData(source, itemid, false, false);
	}

	private static void addBuffStatPairToListIfNotZero(
			List<Pair<MapleBuffStat, Integer>> list, MapleBuffStat buffstat,
			Integer val) {
		if (val.intValue() != 0) {
			list.add(new Pair<>(buffstat, val));
		}
	}

	private static MapleStatEffect loadFromData(MapleData source, int sourceid,
			boolean skill, boolean overTime) {
		final MapleStatEffect ret = new MapleStatEffect();
		ret.duration = MapleDataTool.getIntConvert("time", source, -1);
		ret.hp = (short) MapleDataTool.getInt("hp", source, 0);
		ret.hpR = MapleDataTool.getInt("hpR", source, 0) / 100.0;
		ret.mp = (short) MapleDataTool.getInt("mp", source, 0);
		ret.mpR = MapleDataTool.getInt("mpR", source, 0) / 100.0;
		ret.mpCon = (short) MapleDataTool.getInt("mpCon", source, 0);
		ret.hpCon = (short) MapleDataTool.getInt("hpCon", source, 0);
		final int iprop = MapleDataTool.getInt("prop", source, 100);
		ret.prop = iprop / 100.0;
		ret.mobCount = MapleDataTool.getInt("mobCount", source, 1);
		ret.cooldown = MapleDataTool.getInt("cooltime", source, 0);
		ret.morphId = MapleDataTool.getInt("morph", source, 0);
		ret.ghost = MapleDataTool.getInt("ghost", source, 0);
		ret.fatigue = MapleDataTool.getInt("incFatigue", source, 0);
		ret.repeatEffect = MapleDataTool.getInt("repeatEffect", source, 0) > 0;

		ret.sourceid = sourceid;
		ret.skill = skill;
		if (!ret.skill && (ret.duration > -1)) {
			ret.overTime = true;
		} else {
			ret.duration *= 1000; // items have their times stored in ms, of
									// course
			ret.overTime = overTime;
		}
		final ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<>();
		ret.watk = (short) MapleDataTool.getInt("pad", source, 0);
		ret.wdef = (short) MapleDataTool.getInt("pdd", source, 0);
		ret.matk = (short) MapleDataTool.getInt("mad", source, 0);
		ret.mdef = (short) MapleDataTool.getInt("mdd", source, 0);
		ret.acc = (short) MapleDataTool.getIntConvert("acc", source, 0);
		ret.avoid = (short) MapleDataTool.getInt("eva", source, 0);
		ret.speed = (short) MapleDataTool.getInt("speed", source, 0);
		ret.jump = (short) MapleDataTool.getInt("jump", source, 0);
		ret.berserk = MapleDataTool.getInt("berserk", source, 0);
		ret.booster = MapleDataTool.getInt("booster", source, 0);
		if (ret.overTime && (ret.getSummonMovementType() == null)) {
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WATK,
					Integer.valueOf(ret.watk));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WDEF,
					Integer.valueOf(ret.wdef));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MATK,
					Integer.valueOf(ret.matk));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MDEF,
					Integer.valueOf(ret.mdef));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACC,
					Integer.valueOf(ret.acc));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.AVOID,
					Integer.valueOf(ret.avoid));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.SPEED,
					Integer.valueOf(ret.speed));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.JUMP,
					Integer.valueOf(ret.jump));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.PYRAMID_PQ,
					Integer.valueOf(ret.berserk));
			addBuffStatPairToListIfNotZero(statups, MapleBuffStat.BOOSTER,
					Integer.valueOf(ret.booster));
		}
		final MapleData ltd = source.getChildByPath("lt");
		if (ltd != null) {
			ret.lt = (Point) ltd.getData();
			ret.rb = (Point) source.getChildByPath("rb").getData();
		}
		final int x = MapleDataTool.getInt("x", source, 0);
		ret.x = x;
		ret.y = MapleDataTool.getInt("y", source, 0);
		ret.damage = MapleDataTool.getIntConvert("damage", source, 100);
		ret.fixdamage = MapleDataTool.getIntConvert("fixdamage", source, -1);
		ret.attackCount = MapleDataTool.getIntConvert("attackCount", source, 1);
		ret.bulletCount = (byte) MapleDataTool.getIntConvert("bulletCount",
				source, 1);
		ret.bulletConsume = (byte) MapleDataTool.getIntConvert("bulletConsume",
				source, 0);
		ret.moneyCon = MapleDataTool.getIntConvert("moneyCon", source, 0);
		ret.itemCon = MapleDataTool.getInt("itemCon", source, 0);
		ret.itemConNo = MapleDataTool.getInt("itemConNo", source, 0);
		ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
		final Map<MonsterStatus, Integer> monsterStatus = new ArrayMap<>();
		if (skill) {
			switch (sourceid) {
			// BEGINNER
			case Beginner.RECOVERY:
			case Noblesse.RECOVERY:
			case Legend.RECOVERY:
				statups.add(new Pair<>(MapleBuffStat.RECOVERY, Integer
						.valueOf(x)));
				break;
			case Beginner.ECHO_OF_HERO:
			case Noblesse.ECHO_OF_HERO:
			case Legend.ECHO_OF_HERO:
				statups.add(new Pair<>(MapleBuffStat.ECHO_OF_HERO, Integer
						.valueOf(ret.x)));
				break;
			case Beginner.MONSTER_RIDER:
			case Noblesse.MONSTER_RIDER:
			case Legend.MONSTER_RIDER:
			case Corsair.BATTLE_SHIP:
			case Beginner.SPACESHIP:
			case Noblesse.SPACESHIP:
			case Beginner.YETI_MOUNT1:
			case Beginner.YETI_MOUNT2:
			case Noblesse.YETI_MOUNT1:
			case Noblesse.YETI_MOUNT2:
			case Legend.YETI_MOUNT1:
			case Legend.YETI_MOUNT2:
			case Beginner.WITCH_BROOMSTICK:
			case Noblesse.WITCH_BROOMSTICK:
			case Legend.WITCH_BROOMSTICK:
			case Beginner.BALROG_MOUNT:
			case Noblesse.BALROG_MOUNT:
			case Legend.BALROG_MOUNT:
				statups.add(new Pair<>(MapleBuffStat.MONSTER_RIDING, Integer
						.valueOf(sourceid)));
				break;
			case Beginner.BERSERK_FURY:
			case Noblesse.BERSERK_FURY:
				statups.add(new Pair<>(MapleBuffStat.BERSERK_FURY, Integer
						.valueOf(1)));
				break;
			case Beginner.INVINCIBLE_BARRIER:
			case Noblesse.INVINCIBLE_BARRIER:
			case Legend.INVICIBLE_BARRIER:
				statups.add(new Pair<>(MapleBuffStat.DIVINE_BODY, Integer
						.valueOf(1)));
				break;
			case Fighter.POWER_GUARD:
			case Page.POWER_GUARD:
				statups.add(new Pair<>(MapleBuffStat.POWERGUARD, Integer
						.valueOf(x)));
				break;
			case Spearman.HYPER_BODY:
			case GM.HYPER_BODY:
			case SuperGM.HYPER_BODY:
				statups.add(new Pair<>(MapleBuffStat.HYPERBODYHP, Integer
						.valueOf(x)));
				statups.add(new Pair<>(MapleBuffStat.HYPERBODYMP, Integer
						.valueOf(ret.y)));
				break;
			case Crusader.COMBO:
			case DawnWarrior.COMBO:
				statups.add(new Pair<>(MapleBuffStat.COMBO, Integer.valueOf(1)));
				break;
			case WhiteKnight.BW_FIRE_CHARGE:
			case WhiteKnight.BW_ICE_CHARGE:
			case WhiteKnight.BW_LIT_CHARGE:
			case WhiteKnight.SWORD_FIRE_CHARGE:
			case WhiteKnight.SWORD_ICE_CHARGE:
			case WhiteKnight.SWORD_LIT_CHARGE:
			case Paladin.BW_HOLY_CHARGE:
			case Paladin.SWORD_HOLY_CHARGE:
			case DawnWarrior.SOUL_CHARGE:
			case ThunderBreaker.LIGHTNING_CHARGE:
				statups.add(new Pair<>(MapleBuffStat.WK_CHARGE, Integer
						.valueOf(x)));
				break;
			case DragonKnight.DRAGON_BLOOD:
				statups.add(new Pair<>(MapleBuffStat.DRAGONBLOOD, Integer
						.valueOf(ret.x)));
				break;
			case DragonKnight.DRAGON_ROAR:
				ret.hpR = -x / 100.0;
				break;
			case Hero.STANCE:
			case Paladin.STANCE:
			case DarkKnight.STANCE:
			case Aran.FREEZE_STANDING:
				statups.add(new Pair<>(MapleBuffStat.STANCE, Integer
						.valueOf(iprop)));
				break;
			case DawnWarrior.FINAL_ATTACK:
			case WindArcher.FINAL_ATTACK:
				statups.add(new Pair<>(MapleBuffStat.FINALATTACK, Integer
						.valueOf(x)));
				break;
			// MAGICIAN
			case Magician.MAGIC_GUARD:
			case BlazeWizard.MAGIC_GUARD:
				statups.add(new Pair<>(MapleBuffStat.MAGIC_GUARD, Integer
						.valueOf(x)));
				break;
			case Cleric.INVINCIBLE:
				statups.add(new Pair<>(MapleBuffStat.INVINCIBLE, Integer
						.valueOf(x)));
				break;
			case Priest.HOLY_SYMBOL:
			case SuperGM.HOLY_SYMBOL:
				statups.add(new Pair<>(MapleBuffStat.HOLY_SYMBOL, Integer
						.valueOf(x)));
				break;
			case FPArchMage.INFINITY:
			case ILArchMage.INFINITY:
			case Bishop.INFINITY:
				statups.add(new Pair<>(MapleBuffStat.INFINITY, Integer
						.valueOf(x)));
				break;
			case FPArchMage.MANA_REFLECTION:
			case ILArchMage.MANA_REFLECTION:
			case Bishop.MANA_REFLECTION:
				statups.add(new Pair<>(MapleBuffStat.MANA_REFLECTION, Integer
						.valueOf(1)));
				break;
			case Bishop.HOLY_SHIELD:
				statups.add(new Pair<>(MapleBuffStat.HOLY_SHIELD, Integer
						.valueOf(x)));
				break;
			// BOWMAN
			case Priest.MYSTIC_DOOR:
			case Hunter.SOUL_ARROW:
			case Crossbowman.SOUL_ARROW:
			case WindArcher.SOUL_ARROW:
				statups.add(new Pair<>(MapleBuffStat.SOULARROW, Integer
						.valueOf(x)));
				break;
			case Ranger.PUPPET:
			case Sniper.PUPPET:
			case WindArcher.PUPPET:
			case Outlaw.OCTOPUS:
			case Corsair.WRATH_OF_THE_OCTOPI:
				statups.add(new Pair<>(MapleBuffStat.PUPPET, Integer.valueOf(1)));
				break;
			case Bowmaster.CONCENTRATE:
				statups.add(new Pair<>(MapleBuffStat.CONCENTRATE, x));
				break;
			case Bowmaster.HAMSTRING:
				statups.add(new Pair<>(MapleBuffStat.HAMSTRING, Integer
						.valueOf(x)));
				monsterStatus.put(MonsterStatus.SPEED, x);
				break;
			case Marksman.BLIND:
				statups.add(new Pair<>(MapleBuffStat.BLIND, Integer.valueOf(x)));
				monsterStatus.put(MonsterStatus.ACC, x);
				break;
			case Bowmaster.SHARP_EYES:
			case Marksman.SHARP_EYES:
				statups.add(new Pair<>(MapleBuffStat.SHARP_EYES, Integer
						.valueOf((ret.x << 8) | ret.y)));
				break;
			// THIEF
			case Rogue.DARK_SIGHT:
			case WindArcher.WIND_WALK:
			case NightWalker.DARK_SIGHT:
				statups.add(new Pair<>(MapleBuffStat.DARKSIGHT, Integer
						.valueOf(x)));
				break;
			case Hermit.MESO_UP:
				statups.add(new Pair<>(MapleBuffStat.MESOUP, Integer.valueOf(x)));
				break;
			case Hermit.SHADOW_PARTNER:
			case NightWalker.SHADOW_PARTNER:
				statups.add(new Pair<>(MapleBuffStat.SHADOWPARTNER, Integer
						.valueOf(x)));
				break;
			case ChiefBandit.MESO_GUARD:
				statups.add(new Pair<>(MapleBuffStat.MESOGUARD, Integer
						.valueOf(x)));
				break;
			case ChiefBandit.PICKPOCKET:
				statups.add(new Pair<>(MapleBuffStat.PICKPOCKET, Integer
						.valueOf(x)));
				break;
			case NightLord.SHADOW_STARS:
				statups.add(new Pair<>(MapleBuffStat.SHADOW_CLAW, Integer
						.valueOf(0)));
				break;
			// PIRATE
			case Pirate.DASH:
			case ThunderBreaker.DASH:
			case Beginner.SPACE_DASH:
			case Noblesse.SPACE_DASH:
				statups.add(new Pair<>(MapleBuffStat.DASH2, Integer
						.valueOf(ret.x)));
				statups.add(new Pair<>(MapleBuffStat.DASH, Integer
						.valueOf(ret.y)));
				break;
			case Corsair.SPEED_INFUSION:
			case Buccaneer.SPEED_INFUSION:
			case ThunderBreaker.SPEED_INFUSION:
				statups.add(new Pair<>(MapleBuffStat.SPEED_INFUSION, Integer
						.valueOf(x)));
				break;
			case Outlaw.HOMING_BEACON:
			case Corsair.BULLSEYE:
				statups.add(new Pair<>(MapleBuffStat.HOMING_BEACON, Integer
						.valueOf(x)));
				break;
			case ThunderBreaker.SPARK:
				statups.add(new Pair<>(MapleBuffStat.SPARK, Integer.valueOf(x)));
				break;
			// MULTIPLE
			case Aran.POLEARM_BOOSTER:
			case Fighter.AXE_BOOSTER:
			case Fighter.SWORD_BOOSTER:
			case Page.BW_BOOSTER:
			case Page.SWORD_BOOSTER:
			case Spearman.POLEARM_BOOSTER:
			case Spearman.SPEAR_BOOSTER:
			case Hunter.BOW_BOOSTER:
			case Crossbowman.CROSSBOW_BOOSTER:
			case Assassin.CLAW_BOOSTER:
			case Bandit.DAGGER_BOOSTER:
			case FPMage.SPELL_BOOSTER:
			case ILMage.SPELL_BOOSTER:
			case Brawler.KNUCKLER_BOOSTER:
			case Gunslinger.GUN_BOOSTER:
			case DawnWarrior.SWORD_BOOSTER:
			case BlazeWizard.SPELL_BOOSTER:
			case WindArcher.BOW_BOOSTER:
			case NightWalker.CLAW_BOOSTER:
			case ThunderBreaker.KNUCKLER_BOOSTER:
				statups.add(new Pair<>(MapleBuffStat.BOOSTER, Integer
						.valueOf(x)));
				break;
			case Hero.MAPLE_WARRIOR:
			case Paladin.MAPLE_WARRIOR:
			case DarkKnight.MAPLE_WARRIOR:
			case FPArchMage.MAPLE_WARRIOR:
			case ILArchMage.MAPLE_WARRIOR:
			case Bishop.MAPLE_WARRIOR:
			case Bowmaster.MAPLE_WARRIOR:
			case Marksman.MAPLE_WARRIOR:
			case NightLord.MAPLE_WARRIOR:
			case Shadower.MAPLE_WARRIOR:
			case Corsair.MAPLE_WARRIOR:
			case Buccaneer.MAPLE_WARRIOR:
			case Aran.MAPLE_WARRIOR:
				statups.add(new Pair<>(MapleBuffStat.MAPLE_WARRIOR, Integer
						.valueOf(ret.x)));
				break;
			// SUMMON
			case Ranger.SILVER_HAWK:
			case Sniper.GOLDEN_EAGLE:
				statups.add(new Pair<>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
				monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
				break;
			case FPArchMage.ELQUINES:
			case Marksman.FROST_PREY:
				statups.add(new Pair<>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
				monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
				break;
			case Priest.SUMMON_DRAGON:
			case Bowmaster.PHOENIX:
			case ILArchMage.IFRIT:
			case Bishop.BAHAMUT:
			case DarkKnight.BEHOLDER:
			case Outlaw.GAVIOTA:
			case DawnWarrior.SOUL:
			case BlazeWizard.FLAME:
			case WindArcher.STORM:
			case NightWalker.DARKNESS:
			case ThunderBreaker.LIGHTNING:
			case BlazeWizard.IFRIT:
				statups.add(new Pair<>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
				break;
			// ----------------------------- MONSTER STATUS
			// ---------------------------------- //
			case Rogue.DISORDER:
				monsterStatus.put(MonsterStatus.WATK, Integer.valueOf(ret.x));
				monsterStatus.put(MonsterStatus.WDEF, Integer.valueOf(ret.y));
				break;
			case Corsair.HYPNOTIZE:
				monsterStatus.put(MonsterStatus.INERTMOB, 1);
				break;
			case NightLord.NINJA_AMBUSH:
			case Shadower.NINJA_AMBUSH:
				monsterStatus.put(MonsterStatus.NINJA_AMBUSH,
						Integer.valueOf(ret.damage));
				break;
			case Page.THREATEN:
				monsterStatus.put(MonsterStatus.WATK, Integer.valueOf(ret.x));
				monsterStatus.put(MonsterStatus.WDEF, Integer.valueOf(ret.y));
				break;
			case Crusader.AXE_COMA:
			case Crusader.SWORD_COMA:
			case Crusader.SHOUT:
			case WhiteKnight.CHARGE_BLOW:
			case Hunter.ARROW_BOMB:
			case ChiefBandit.ASSAULTER:
			case Shadower.BOOMERANG_STEP:
			case Brawler.BACK_SPIN_BLOW:
			case Brawler.DOUBLE_UPPERCUT:
			case Buccaneer.DEMOLITION:
			case Buccaneer.SNATCH:
			case Buccaneer.BARRAGE:
			case Gunslinger.BLANK_SHOT:
			case DawnWarrior.COMA:
			case Aran.ROLLING_SPIN:
				monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
				break;
			case NightLord.TAUNT:
			case Shadower.TAUNT:
				monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
				monsterStatus.put(MonsterStatus.MDEF, ret.x);
				monsterStatus.put(MonsterStatus.WDEF, ret.x);
				break;
			case ILWizard.COLD_BEAM:
			case ILMage.ICE_STRIKE:
			case ILArchMage.BLIZZARD:
			case ILMage.ELEMENT_COMPOSITION:
			case Sniper.BLIZZARD:
			case Outlaw.ICE_SPLITTER:
			case FPArchMage.PARALYZE:
			case Aran.COMBO_TEMPEST:
				monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
				ret.duration *= 2; // freezing skills are a little strange
				break;
			case FPWizard.SLOW:
			case ILWizard.SLOW:
			case BlazeWizard.SLOW:
				monsterStatus.put(MonsterStatus.SPEED, Integer.valueOf(ret.x));
				break;
			case FPWizard.POISON_BREATH:
			case FPMage.ELEMENT_COMPOSITION:
				monsterStatus.put(MonsterStatus.POISON, Integer.valueOf(1));
				break;
			case Priest.DOOM:
				monsterStatus.put(MonsterStatus.DOOM, Integer.valueOf(1));
				break;
			case ILMage.SEAL:
			case FPMage.SEAL:
				monsterStatus.put(MonsterStatus.SEAL, Integer.valueOf(1));
				break;
			case Hermit.SHADOW_WEB: // shadow web
			case NightWalker.SHADOW_WEB:
				monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
				break;
			case FPArchMage.FIRE_DEMON:
			case ILArchMage.ICE_DEMON:
				monsterStatus.put(MonsterStatus.POISON, Integer.valueOf(1));
				monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
				break;
			// ARAN
			case Aran.COMBO_ABILITY:
				statups.add(new Pair<>(MapleBuffStat.ARAN_COMBO, 100));
				break;
			case Aran.COMBO_BARRIER:
				statups.add(new Pair<>(MapleBuffStat.COMBO_BARRIER, ret.x));
				break;
			case Aran.COMBO_DRAIN:
				statups.add(new Pair<>(MapleBuffStat.COMBO_DRAIN, ret.x));
				break;
			case Aran.SMART_KNOCKBACK:
				statups.add(new Pair<>(MapleBuffStat.SMART_KNOCKBACK, ret.x));
				break;
			case Aran.BODY_PRESSURE:
				statups.add(new Pair<>(MapleBuffStat.BODY_PRESSURE, ret.x));
				break;
			case Aran.SNOW_CHARGE:
				monsterStatus.put(MonsterStatus.SPEED, ret.x);
				statups.add(new Pair<>(MapleBuffStat.WK_CHARGE, ret.y));
				break;
			default:
				break;
			}
		}
		if (ret.isMorph()) {
			statups.add(new Pair<>(MapleBuffStat.MORPH, Integer.valueOf(ret
					.getMorph())));
		}
		if ((ret.ghost > 0) && !skill) {
			statups.add(new Pair<>(MapleBuffStat.GHOST_MORPH, Integer
					.valueOf(ret.ghost)));
		}
		ret.monsterStatus = monsterStatus;
		statups.trimToSize();
		ret.statups = statups;
		return ret;
	}

	/**
	 * @param applyto
	 * @param obj
	 * @param attack
	 *            damage done by the skill
	 */
	public void applyPassive(MapleCharacter applyto, MapleMapObject obj,
			int attack) {
		if (this.makeChanceResult()) {
			switch (this.sourceid) { // MP eater
			case FPWizard.MP_EATER:
			case ILWizard.MP_EATER:
			case Cleric.MP_EATER:
				if ((obj == null)
						|| (obj.getType() != MapleMapObjectType.MONSTER)) {
					return;
				}
				final MapleMonster mob = (MapleMonster) obj; // x is absorb
																// percentage
				if (!mob.isBoss()) {
					final int absorbMp = Math.min(
							(int) (mob.getMaxMp() * (this.getX() / 100.0)),
							mob.getMp());
					if (absorbMp > 0) {
						mob.setMp(mob.getMp() - absorbMp);
						applyto.addMP(absorbMp);
						applyto.getClient().announce(
								MaplePacketCreator.showOwnBuffEffect(
										this.sourceid, 1));
						applyto.getMap().broadcastMessage(
								applyto,
								MaplePacketCreator.showBuffeffect(
										applyto.getId(), this.sourceid, 1),
								false);
					}
				}
				break;
			}
		}
	}

	public boolean applyTo(MapleCharacter chr) {
		return this.applyTo(chr, chr, true, null);
	}

	public boolean applyTo(MapleCharacter chr, Point pos) {
		return this.applyTo(chr, chr, true, pos);
	}

	private boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto,
			boolean primary, Point pos) {
		if (this.skill
				&& ((this.sourceid == GM.HIDE) || (this.sourceid == SuperGM.HIDE))) {
			applyto.toggleHide(false);
			return true;
		}
		int hpchange = this.calcHPChange(applyfrom, primary);
		final int mpchange = this.calcMPChange(applyfrom, primary);
		if (primary) {
			if (this.itemConNo != 0) {
				MapleInventoryManipulator.removeById(applyto.getClient(),
						MapleItemInformationProvider.getInstance()
								.getInventoryType(this.itemCon), this.itemCon,
						this.itemConNo, false, true);
			}
		}
		final List<Pair<MapleStat, Integer>> hpmpupdate = new ArrayList<>(2);
		if (!primary && this.isResurrection()) {
			hpchange = applyto.getMaxHp();
			applyto.setStance(0);
			applyto.getMap().broadcastMessage(applyto,
					MaplePacketCreator.removePlayerFromMap(applyto.getId()),
					false);
			applyto.getMap().broadcastMessage(applyto,
					MaplePacketCreator.spawnPlayerMapobject(applyto), false);
		}
		if (this.isDispel() && this.makeChanceResult()) {
			applyto.dispelDebuffs();
		} else if (this.isHeroWill()) {
			applyto.dispelDebuff(MapleDisease.SEDUCE);
		}
		if (this.isComboReset()) {
			applyto.setCombo((short) 0);
		}
		/*
		 * if (applyfrom.getMp() < getMpCon()) {
		 * AutobanFactory.MPCON.addPoint(applyfrom.getAutobanManager(),
		 * "mpCon hack for skill:" + sourceid + "; Player MP: " +
		 * applyto.getMp() + " MP Needed: " + getMpCon()); }
		 */
		if (hpchange != 0) {
			if ((hpchange < 0) && ((-hpchange) > applyto.getHp())) {
				return false;
			}
			int newHp = applyto.getHp() + hpchange;
			if (newHp < 1) {
				newHp = 1;
			}
			applyto.setHp(newHp);
			hpmpupdate.add(new Pair<>(MapleStat.HP, Integer.valueOf(applyto
					.getHp())));
		}
		final int newMp = applyto.getMp() + mpchange;
		if (mpchange != 0) {
			if ((mpchange < 0) && (-mpchange > applyto.getMp())) {
				return false;
			}

			applyto.setMp(newMp);
			hpmpupdate.add(new Pair<>(MapleStat.MP, Integer.valueOf(applyto
					.getMp())));
		}
		applyto.getClient().announce(
				MaplePacketCreator.updatePlayerStats(hpmpupdate, true));
		if (this.moveTo != -1) {
			if (applyto.getMap().getReturnMapId() != applyto.getMapId()) {
				MapleMap target;
				if (this.moveTo == 999999999) {
					target = applyto.getMap().getReturnMap();
				} else {
					target = applyto.getClient().getWorldServer()
							.getChannel(applyto.getClient().getChannel())
							.getMapFactory().getMap(this.moveTo);
					final int targetid = target.getId() / 10000000;
					if ((targetid != 60)
							&& ((applyto.getMapId() / 10000000) != 61)
							&& (targetid != (applyto.getMapId() / 10000000))
							&& (targetid != 21) && (targetid != 20)) {
						return false;
					}
				}
				applyto.changeMap(target);
			} else {
				return false;
			}

		}
		if (this.isShadowClaw()) {
			int projectile = 0;
			final MapleInventory use = applyto
					.getInventory(MapleInventoryType.USE);
			for (int i = 0; i < 97; i++) { // impose order...
				final Item item = use.getItem((byte) i);
				if (item != null) {
					if (ItemConstants.isThrowingStar(item.getItemId())
							&& (item.getQuantity() >= 200)) {
						projectile = item.getItemId();
						break;
					}
				}
			}
			if (projectile == 0) {
				return false;
			} else {
				MapleInventoryManipulator.removeById(applyto.getClient(),
						MapleInventoryType.USE, projectile, 200, false, true);
			}

		}
		final SummonMovementType summonMovementType = this
				.getSummonMovementType();
		if (this.overTime || this.isCygnusFA() || (summonMovementType != null)) {
			this.applyBuffEffect(applyfrom, applyto, primary);
		}

		if (primary && (this.overTime || this.isHeal())) {
			this.applyBuff(applyfrom);
		}

		if (primary && this.isMonsterBuff()) {
			this.applyMonsterBuff(applyfrom);
		}

		if (this.getFatigue() != 0) {
			applyto.getMount().setTiredness(
					applyto.getMount().getTiredness() + this.getFatigue());
		}

		if ((summonMovementType != null) && (pos != null)) {
			final MapleSummon tosummon = new MapleSummon(applyfrom,
					this.sourceid, pos, summonMovementType);
			applyfrom.getMap().spawnSummon(tosummon);
			applyfrom.addSummon(this.sourceid, tosummon);
			tosummon.addHP(this.x);
			if (this.isBeholder()) {
				tosummon.addHP(1);
			}
		}
		if (this.isMagicDoor()
				&& !FieldLimit.DOOR.check(applyto.getMap().getFieldLimit())) { // Magic
																				// Door
			final Point doorPosition = new Point(applyto.getPosition());
			MapleDoor door = new MapleDoor(applyto, doorPosition);
			applyto.getMap().spawnDoor(door);
			applyto.addDoor(door);
			door = new MapleDoor(door);
			applyto.addDoor(door);
			door.getTown().spawnDoor(door);
			if (applyto.getParty() != null) {// update town doors
				applyto.silentPartyUpdate();
			}
			applyto.disableDoor();
		} else if (this.isMist()) {
			final Rectangle bounds = this.calculateBoundingBox(
					applyfrom.getPosition(), applyfrom.isFacingLeft());
			final MapleMist mist = new MapleMist(bounds, applyfrom, this);
			applyfrom.getMap().spawnMist(mist, this.getDuration(),
					this.sourceid != Shadower.SMOKE_SCREEN, false);
		} else if (this.isTimeLeap()) { // Time Leap
			for (final PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
				if (i.skillId != Buccaneer.TIME_LEAP) {
					applyto.removeCooldown(i.skillId);
				}
			}
		}
		return true;
	}

	private void applyBuff(MapleCharacter applyfrom) {
		if (this.isPartyBuff()
				&& ((applyfrom.getParty() != null) || this.isGmBuff())) {
			final Rectangle bounds = this.calculateBoundingBox(
					applyfrom.getPosition(), applyfrom.isFacingLeft());
			final List<MapleMapObject> affecteds = applyfrom.getMap()
					.getMapObjectsInRect(bounds,
							Arrays.asList(MapleMapObjectType.PLAYER));
			final List<MapleCharacter> affectedp = new ArrayList<>(
					affecteds.size());
			for (final MapleMapObject affectedmo : affecteds) {
				final MapleCharacter affected = (MapleCharacter) affectedmo;
				if ((affected != applyfrom)
						&& (this.isGmBuff() || applyfrom.getParty().equals(
								affected.getParty()))) {
					if ((this.isResurrection() && !affected.isAlive())
							|| (!this.isResurrection() && affected.isAlive())) {
						affectedp.add(affected);
					}
					if (this.isTimeLeap()) {
						for (final PlayerCoolDownValueHolder i : affected
								.getAllCooldowns()) {
							affected.removeCooldown(i.skillId);
						}
					}
				}
			}
			for (final MapleCharacter affected : affectedp) {
				this.applyTo(applyfrom, affected, false, null);
				affected.getClient().announce(
						MaplePacketCreator.showOwnBuffEffect(this.sourceid, 2));
				affected.getMap().broadcastMessage(
						affected,
						MaplePacketCreator.showBuffeffect(affected.getId(),
								this.sourceid, 2), false);
			}
		}
	}

	private void applyMonsterBuff(MapleCharacter applyfrom) {
		final Rectangle bounds = this.calculateBoundingBox(
				applyfrom.getPosition(), applyfrom.isFacingLeft());
		final List<MapleMapObject> affected = applyfrom.getMap()
				.getMapObjectsInRect(bounds,
						Arrays.asList(MapleMapObjectType.MONSTER));
		final Skill skill_ = SkillFactory.getSkill(this.sourceid);
		int i = 0;
		for (final MapleMapObject mo : affected) {
			final MapleMonster monster = (MapleMonster) mo;
			if (this.makeChanceResult()) {
				monster.applyStatus(applyfrom,
						new MonsterStatusEffect(this.getMonsterStati(), skill_,
								null, false), this.isPoison(), this
								.getDuration());
			}
			i++;
			if (i >= this.mobCount) {
				break;
			}
		}
	}

	private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
		Point mylt;
		Point myrb;
		if (facingLeft) {
			mylt = new Point(this.lt.x + posFrom.x, this.lt.y + posFrom.y);
			myrb = new Point(this.rb.x + posFrom.x, this.rb.y + posFrom.y);
		} else {
			myrb = new Point(-this.lt.x + posFrom.x, this.rb.y + posFrom.y);
			mylt = new Point(-this.rb.x + posFrom.x, this.lt.y + posFrom.y);
		}
		final Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x,
				myrb.y - mylt.y);
		return bounds;
	}

	public void silentApplyBuff(MapleCharacter chr, long starttime) {
		int localDuration = this.duration;
		localDuration = this.alchemistModifyVal(chr, localDuration, false);
		final CancelEffectAction cancelAction = new CancelEffectAction(chr,
				this, starttime);
		final ScheduledFuture<?> schedule = TimerManager.getInstance()
				.schedule(
						cancelAction,
						((starttime + localDuration) - System
								.currentTimeMillis()));
		chr.registerEffect(this, starttime, schedule);
		final SummonMovementType summonMovementType = this
				.getSummonMovementType();
		if (summonMovementType != null) {
			final MapleSummon tosummon = new MapleSummon(chr, this.sourceid,
					chr.getPosition(), summonMovementType);
			if (!tosummon.isStationary()) {
				chr.addSummon(this.sourceid, tosummon);
				tosummon.addHP(this.x);
			}
		}
		if (this.sourceid == Corsair.BATTLE_SHIP) {
			chr.announce(MaplePacketCreator.skillCooldown(5221999,
					chr.getBattleshipHp()));
		}
	}

	public final void applyComboBuff(final MapleCharacter applyto, int combo) {
		final List<Pair<MapleBuffStat, Integer>> stat = Collections
				.singletonList(new Pair<>(MapleBuffStat.ARAN_COMBO, combo));
		applyto.getClient().announce(
				MaplePacketCreator.giveBuff(this.sourceid, 99999, stat));

		final long starttime = System.currentTimeMillis();
		// final CancelEffectAction cancelAction = new
		// CancelEffectAction(applyto, this, starttime);
		// final ScheduledFuture<?> schedule =
		// TimerManager.getInstance().schedule(cancelAction, ((starttime +
		// 99999) - System.currentTimeMillis()));
		applyto.registerEffect(this, starttime, null);
	}

	private void applyBuffEffect(MapleCharacter applyfrom,
			MapleCharacter applyto, boolean primary) {
		if (!this.isMonsterRiding()) {
			applyto.cancelEffect(this, true, -1);
		}

		List<Pair<MapleBuffStat, Integer>> localstatups = this.statups;
		int localDuration = this.duration;
		int localsourceid = this.sourceid;
		final int seconds = localDuration / 1000;
		MapleMount givemount = null;
		if (this.isMonsterRiding()) {
			int ridingLevel = 0;
			final Item mount = applyfrom.getInventory(
					MapleInventoryType.EQUIPPED).getItem((byte) -18);
			if (mount != null) {
				ridingLevel = mount.getItemId();
			}
			if (this.sourceid == Corsair.BATTLE_SHIP) {
				ridingLevel = 1932000;
			} else if ((this.sourceid == Beginner.SPACESHIP)
					|| (this.sourceid == Noblesse.SPACESHIP)) {
				ridingLevel = 1932000 + applyto.getSkillLevel(this.sourceid);
			} else if ((this.sourceid == Beginner.YETI_MOUNT1)
					|| (this.sourceid == Noblesse.YETI_MOUNT1)
					|| (this.sourceid == Legend.YETI_MOUNT1)) {
				ridingLevel = 1932003;
			} else if ((this.sourceid == Beginner.YETI_MOUNT2)
					|| (this.sourceid == Noblesse.YETI_MOUNT2)
					|| (this.sourceid == Legend.YETI_MOUNT2)) {
				ridingLevel = 1932004;
			} else if ((this.sourceid == Beginner.WITCH_BROOMSTICK)
					|| (this.sourceid == Noblesse.WITCH_BROOMSTICK)
					|| (this.sourceid == Legend.WITCH_BROOMSTICK)) {
				ridingLevel = 1932005;
			} else if ((this.sourceid == Beginner.BALROG_MOUNT)
					|| (this.sourceid == Noblesse.BALROG_MOUNT)
					|| (this.sourceid == Legend.BALROG_MOUNT)) {
				ridingLevel = 1932010;
			} else {
				if (applyto.getMount() == null) {
					applyto.mount(ridingLevel, this.sourceid);
				}
				applyto.getMount().startSchedule();
			}
			if (this.sourceid == Corsair.BATTLE_SHIP) {
				givemount = new MapleMount(applyto, 1932000, this.sourceid);
			} else if ((this.sourceid == Beginner.SPACESHIP)
					|| (this.sourceid == Noblesse.SPACESHIP)) {
				givemount = new MapleMount(applyto,
						1932000 + applyto.getSkillLevel(this.sourceid),
						this.sourceid);
			} else if ((this.sourceid == Beginner.YETI_MOUNT1)
					|| (this.sourceid == Noblesse.YETI_MOUNT1)
					|| (this.sourceid == Legend.YETI_MOUNT1)) {
				givemount = new MapleMount(applyto, 1932003, this.sourceid);
			} else if ((this.sourceid == Beginner.YETI_MOUNT2)
					|| (this.sourceid == Noblesse.YETI_MOUNT2)
					|| (this.sourceid == Legend.YETI_MOUNT2)) {
				givemount = new MapleMount(applyto, 1932004, this.sourceid);
			} else if ((this.sourceid == Beginner.WITCH_BROOMSTICK)
					|| (this.sourceid == Noblesse.WITCH_BROOMSTICK)
					|| (this.sourceid == Legend.WITCH_BROOMSTICK)) {
				givemount = new MapleMount(applyto, 1932005, this.sourceid);
			} else if ((this.sourceid == Beginner.BALROG_MOUNT)
					|| (this.sourceid == Noblesse.BALROG_MOUNT)
					|| (this.sourceid == Legend.BALROG_MOUNT)) {
				givemount = new MapleMount(applyto, 1932010, this.sourceid);
			} else {
				givemount = applyto.getMount();
			}
			localDuration = this.sourceid;
			localsourceid = ridingLevel;
			localstatups = Collections.singletonList(new Pair<>(
					MapleBuffStat.MONSTER_RIDING, 0));
		} else if (this.isSkillMorph()) {
			localstatups = Collections.singletonList(new Pair<>(
					MapleBuffStat.MORPH, this.getMorph(applyto)));
		}
		if (primary) {
			localDuration = this.alchemistModifyVal(applyfrom, localDuration,
					false);
			applyto.getMap().broadcastMessage(
					applyto,
					MaplePacketCreator.showBuffeffect(applyto.getId(),
							this.sourceid, 1, (byte) 3), false);
		}
		if (localstatups.size() > 0) {
			byte[] buff = null;
			byte[] mbuff = null;
			if (this.getSummonMovementType() == null) {
				buff = MaplePacketCreator.giveBuff((this.skill ? this.sourceid
						: -this.sourceid), localDuration, localstatups);
			}
			if (this.isDash()) {
				buff = MaplePacketCreator.givePirateBuff(this.statups,
						this.sourceid, seconds);
				mbuff = MaplePacketCreator.giveForeignDash(applyto.getId(),
						this.sourceid, seconds, localstatups);
			} else if (this.isInfusion()) {
				buff = MaplePacketCreator.givePirateBuff(this.statups,
						this.sourceid, seconds);
				mbuff = MaplePacketCreator.giveForeignInfusion(applyto.getId(),
						this.x, localDuration);
			} else if (this.isDs()) {
				final List<Pair<MapleBuffStat, Integer>> dsstat = Collections
						.singletonList(new Pair<>(MapleBuffStat.DARKSIGHT, 0));
				mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(),
						dsstat);
			} else if (this.isCombo()) {
				mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(),
						this.statups);
			} else if (this.isMonsterRiding()) {
				buff = MaplePacketCreator.giveBuff(localsourceid,
						localDuration, localstatups);
				mbuff = MaplePacketCreator.showMonsterRiding(applyto.getId(),
						givemount);
				localDuration = this.duration;
				if (this.sourceid == Corsair.BATTLE_SHIP) {// hp
					if (applyto.getBattleshipHp() == 0) {
						applyto.resetBattleshipHp();
					}
				}
			} else if (this.isShadowPartner()) {
				final List<Pair<MapleBuffStat, Integer>> stat = Collections
						.singletonList(new Pair<>(MapleBuffStat.SHADOWPARTNER,
								0));
				mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(),
						stat);
			} else if (this.isSoulArrow()) {
				final List<Pair<MapleBuffStat, Integer>> stat = Collections
						.singletonList(new Pair<>(MapleBuffStat.SOULARROW, 0));
				mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(),
						stat);
			} else if (this.isEnrage()) {
				applyto.handleOrbconsume();
			} else if (this.isMorph()) {
				final List<Pair<MapleBuffStat, Integer>> stat = Collections
						.singletonList(new Pair<>(MapleBuffStat.MORPH, Integer
								.valueOf(this.getMorph(applyto))));
				mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(),
						stat);
			} else if (this.isTimeLeap()) {
				for (final PlayerCoolDownValueHolder i : applyto
						.getAllCooldowns()) {
					if (i.skillId != Buccaneer.TIME_LEAP) {
						applyto.removeCooldown(i.skillId);
					}
				}
			}
			final long starttime = System.currentTimeMillis();
			final CancelEffectAction cancelAction = new CancelEffectAction(
					applyto, this, starttime);
			final ScheduledFuture<?> schedule = TimerManager.getInstance()
					.schedule(cancelAction, localDuration);
			applyto.registerEffect(this, starttime, schedule);

			if (buff != null) {
				applyto.getClient().announce(buff);
			}
			if (mbuff != null) {
				applyto.getMap().broadcastMessage(applyto, mbuff, false);
			}
			if (this.sourceid == Corsair.BATTLE_SHIP) {
				applyto.announce(MaplePacketCreator.skillCooldown(5221999,
						applyto.getBattleshipHp() / 10));
			}
		}
	}

	private int calcHPChange(MapleCharacter applyfrom, boolean primary) {
		int hpchange = 0;
		if (this.hp != 0) {
			if (!this.skill) {
				if (primary) {
					hpchange += this.alchemistModifyVal(applyfrom, this.hp,
							true);
				} else {
					hpchange += this.hp;
				}
			} else {
				hpchange += this.makeHealHP(this.hp / 100.0,
						applyfrom.getTotalMagic(), 3, 5);
			}
		}
		if (this.hpR != 0) {
			hpchange += (int) (applyfrom.getCurrentMaxHp() * this.hpR);
			applyfrom.checkBerserk();
		}
		if (primary) {
			if (this.hpCon != 0) {
				hpchange -= this.hpCon;
			}
		}
		if (this.isChakra()) {
			hpchange += this.makeHealHP(this.getY() / 100.0,
					applyfrom.getTotalLuk(), 2.3, 3.5);
		} else if (this.sourceid == SuperGM.HEAL_PLUS_DISPEL) {
			hpchange += (applyfrom.getMaxHp() - applyfrom.getHp());
		}

		return hpchange;
	}

	private int makeHealHP(double rate, double stat, double lowerfactor,
			double upperfactor) {
		return (int) ((Math.random() * (((int) (stat * upperfactor * rate) - (int) (stat
				* lowerfactor * rate)) + 1)) + (int) (stat * lowerfactor * rate));
	}

	private int calcMPChange(MapleCharacter applyfrom, boolean primary) {
		int mpchange = 0;
		if (this.mp != 0) {
			if (primary) {
				mpchange += this.alchemistModifyVal(applyfrom, this.mp, true);
			} else {
				mpchange += this.mp;
			}
		}
		if (this.mpR != 0) {
			mpchange += (int) (applyfrom.getCurrentMaxMp() * this.mpR);
		}
		if (primary) {
			if (this.mpCon != 0) {
				double mod = 1.0;
				final boolean isAFpMage = applyfrom.getJob().isA(
						MapleJob.FP_MAGE);
				final boolean isCygnus = applyfrom.getJob().isA(
						MapleJob.BLAZEWIZARD2);
				if (isAFpMage || isCygnus
						|| applyfrom.getJob().isA(MapleJob.IL_MAGE)) {
					final Skill amp = isAFpMage ? SkillFactory
							.getSkill(FPMage.ELEMENT_AMPLIFICATION)
							: (isCygnus ? SkillFactory
									.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION)
									: SkillFactory
											.getSkill(ILMage.ELEMENT_AMPLIFICATION));
					final int ampLevel = applyfrom.getSkillLevel(amp);
					if (ampLevel > 0) {
						mod = amp.getEffect(ampLevel).getX() / 100.0;
					}
				}
				mpchange -= this.mpCon * mod;
				if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
					mpchange = 0;
				} else if (applyfrom.getBuffedValue(MapleBuffStat.CONCENTRATE) != null) {
					mpchange -= (int) (mpchange * (applyfrom.getBuffedValue(
							MapleBuffStat.CONCENTRATE).doubleValue() / 100));
				}
			}
		}
		if (this.sourceid == SuperGM.HEAL_PLUS_DISPEL) {
			mpchange += (applyfrom.getMaxMp() - applyfrom.getMp());
		}

		return mpchange;
	}

	private int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
		if (!this.skill
				&& (chr.getJob().isA(MapleJob.HERMIT) || chr.getJob().isA(
						MapleJob.NIGHTWALKER3))) {
			final MapleStatEffect alchemistEffect = this
					.getAlchemistEffect(chr);
			if (alchemistEffect != null) {
				return (int) (val * ((withX ? alchemistEffect.getX()
						: alchemistEffect.getY()) / 100.0));
			}
		}
		return val;
	}

	private MapleStatEffect getAlchemistEffect(MapleCharacter chr) {
		int id = Hermit.ALCHEMIST;
		if (chr.isCygnus()) {
			id = NightWalker.ALCHEMIST;
		}
		final int alchemistLevel = chr.getSkillLevel(SkillFactory.getSkill(id));
		return alchemistLevel == 0 ? null : SkillFactory.getSkill(id)
				.getEffect(alchemistLevel);
	}

	private boolean isGmBuff() {
		switch (this.sourceid) {
		case Beginner.ECHO_OF_HERO:
		case Noblesse.ECHO_OF_HERO:
		case Legend.ECHO_OF_HERO:
		case SuperGM.HEAL_PLUS_DISPEL:
		case SuperGM.HASTE:
		case SuperGM.HOLY_SYMBOL:
		case SuperGM.BLESS:
		case SuperGM.RESURRECTION:
		case SuperGM.HYPER_BODY:
			return true;
		default:
			return false;
		}
	}

	private boolean isMonsterBuff() {
		if (!this.skill) {
			return false;
		}
		switch (this.sourceid) {
		case Page.THREATEN:
		case FPWizard.SLOW:
		case ILWizard.SLOW:
		case FPMage.SEAL:
		case ILMage.SEAL:
		case Priest.DOOM:
		case Hermit.SHADOW_WEB:
		case NightLord.NINJA_AMBUSH:
		case Shadower.NINJA_AMBUSH:
		case BlazeWizard.SLOW:
		case BlazeWizard.SEAL:
		case NightWalker.SHADOW_WEB:
			return true;
		}
		return false;
	}

	private boolean isPartyBuff() {
		if ((this.lt == null) || (this.rb == null)) {
			return false;
		}
		if (((this.sourceid >= 1211003) && (this.sourceid <= 1211008))
				|| (this.sourceid == Paladin.SWORD_HOLY_CHARGE)
				|| (this.sourceid == Paladin.BW_HOLY_CHARGE)
				|| (this.sourceid == DawnWarrior.SOUL_CHARGE)) {// wk charges
																// have lt
			// and rb set but
			// are neither
			// player nor
			// monster buffs
			return false;
		}
		return true;
	}

	private boolean isHeal() {
		return (this.sourceid == Cleric.HEAL)
				|| (this.sourceid == SuperGM.HEAL_PLUS_DISPEL);
	}

	private boolean isResurrection() {
		return (this.sourceid == Bishop.RESURRECTION)
				|| (this.sourceid == GM.RESURRECTION)
				|| (this.sourceid == SuperGM.RESURRECTION);
	}

	private boolean isTimeLeap() {
		return this.sourceid == Buccaneer.TIME_LEAP;
	}

	public boolean isDragonBlood() {
		return this.skill && (this.sourceid == DragonKnight.DRAGON_BLOOD);
	}

	public boolean isBerserk() {
		return this.skill && (this.sourceid == DarkKnight.BERSERK);
	}

	public boolean isRecovery() {
		return (this.sourceid == Beginner.RECOVERY)
				|| (this.sourceid == Noblesse.RECOVERY)
				|| (this.sourceid == Legend.RECOVERY);
	}

	private boolean isDs() {
		return this.skill
				&& ((this.sourceid == Rogue.DARK_SIGHT)
						|| (this.sourceid == WindArcher.WIND_WALK) || (this.sourceid == NightWalker.DARK_SIGHT));
	}

	private boolean isCombo() {
		return this.skill
				&& ((this.sourceid == Crusader.COMBO) || (this.sourceid == DawnWarrior.COMBO));
	}

	private boolean isEnrage() {
		return this.skill && (this.sourceid == Hero.ENRAGE);
	}

	public boolean isBeholder() {
		return this.skill && (this.sourceid == DarkKnight.BEHOLDER);
	}

	private boolean isShadowPartner() {
		return this.skill
				&& ((this.sourceid == Hermit.SHADOW_PARTNER) || (this.sourceid == NightWalker.SHADOW_PARTNER));
	}

	private boolean isChakra() {
		return this.skill && (this.sourceid == ChiefBandit.CHAKRA);
	}

	public boolean isMonsterRiding() {
		return this.skill
				&& (((this.sourceid % 10000000) == 1004)
						|| (this.sourceid == Corsair.BATTLE_SHIP)
						|| (this.sourceid == Beginner.SPACESHIP)
						|| (this.sourceid == Noblesse.SPACESHIP)
						|| (this.sourceid == Beginner.YETI_MOUNT1)
						|| (this.sourceid == Beginner.YETI_MOUNT2)
						|| (this.sourceid == Beginner.WITCH_BROOMSTICK)
						|| (this.sourceid == Beginner.BALROG_MOUNT)
						|| (this.sourceid == Noblesse.YETI_MOUNT1)
						|| (this.sourceid == Noblesse.YETI_MOUNT2)
						|| (this.sourceid == Noblesse.WITCH_BROOMSTICK)
						|| (this.sourceid == Noblesse.BALROG_MOUNT)
						|| (this.sourceid == Legend.YETI_MOUNT1)
						|| (this.sourceid == Legend.YETI_MOUNT2)
						|| (this.sourceid == Legend.WITCH_BROOMSTICK) || (this.sourceid == Legend.BALROG_MOUNT));
	}

	public boolean isMagicDoor() {
		return this.skill && (this.sourceid == Priest.MYSTIC_DOOR);
	}

	public boolean isPoison() {
		return this.skill
				&& ((this.sourceid == FPMage.POISON_MIST)
						|| (this.sourceid == FPWizard.POISON_BREATH)
						|| (this.sourceid == FPMage.ELEMENT_COMPOSITION) || (this.sourceid == NightWalker.POISON_BOMB));
	}

	private boolean isMist() {
		return this.skill
				&& ((this.sourceid == FPMage.POISON_MIST)
						|| (this.sourceid == Shadower.SMOKE_SCREEN)
						|| (this.sourceid == BlazeWizard.FLAME_GEAR) || (this.sourceid == NightWalker.POISON_BOMB));
	}

	private boolean isSoulArrow() {
		return this.skill
				&& ((this.sourceid == Hunter.SOUL_ARROW)
						|| (this.sourceid == Crossbowman.SOUL_ARROW) || (this.sourceid == WindArcher.SOUL_ARROW));
	}

	private boolean isShadowClaw() {
		return this.skill && (this.sourceid == NightLord.SHADOW_STARS);
	}

	private boolean isDispel() {
		return this.skill
				&& ((this.sourceid == Priest.DISPEL) || (this.sourceid == SuperGM.HEAL_PLUS_DISPEL));
	}

	private boolean isHeroWill() {
		if (this.skill) {
			switch (this.sourceid) {
			case Hero.HEROS_WILL:
			case Paladin.HEROS_WILL:
			case DarkKnight.HEROS_WILL:
			case FPArchMage.HEROS_WILL:
			case ILArchMage.HEROS_WILL:
			case Bishop.HEROS_WILL:
			case Bowmaster.HEROS_WILL:
			case Marksman.HEROS_WILL:
			case NightLord.HEROS_WILL:
			case Shadower.HEROS_WILL:
			case Buccaneer.PIRATES_RAGE:
			case Aran.HEROS_WILL:
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	private boolean isDash() {
		return this.skill
				&& ((this.sourceid == Pirate.DASH)
						|| (this.sourceid == ThunderBreaker.DASH)
						|| (this.sourceid == Beginner.SPACE_DASH) || (this.sourceid == Noblesse.SPACE_DASH));
	}

	private boolean isSkillMorph() {
		return this.skill
				&& ((this.sourceid == Buccaneer.SUPER_TRANSFORMATION)
						|| (this.sourceid == Marauder.TRANSFORMATION)
						|| (this.sourceid == WindArcher.EAGLE_EYE) || (this.sourceid == ThunderBreaker.TRANSFORMATION));
	}

	private boolean isInfusion() {
		return this.skill
				&& ((this.sourceid == Buccaneer.SPEED_INFUSION)
						|| (this.sourceid == Corsair.SPEED_INFUSION) || (this.sourceid == ThunderBreaker.SPEED_INFUSION));
	}

	private boolean isCygnusFA() {
		return this.skill
				&& ((this.sourceid == DawnWarrior.FINAL_ATTACK) || (this.sourceid == WindArcher.FINAL_ATTACK));
	}

	private boolean isMorph() {
		return this.morphId > 0;
	}

	private boolean isComboReset() {
		return (this.sourceid == Aran.COMBO_BARRIER)
				|| (this.sourceid == Aran.COMBO_DRAIN);
	}

	private int getFatigue() {
		return this.fatigue;
	}

	private int getMorph() {
		return this.morphId;
	}

	private int getMorph(MapleCharacter chr) {
		if ((this.morphId % 10) == 0) {
			return this.morphId + chr.getGender();
		}
		return this.morphId + (100 * chr.getGender());
	}

	private SummonMovementType getSummonMovementType() {
		if (!this.skill) {
			return null;
		}
		switch (this.sourceid) {
		case Ranger.PUPPET:
		case Sniper.PUPPET:
		case WindArcher.PUPPET:
		case Outlaw.OCTOPUS:
		case Corsair.WRATH_OF_THE_OCTOPI:
			return SummonMovementType.STATIONARY;
		case Ranger.SILVER_HAWK:
		case Sniper.GOLDEN_EAGLE:
		case Priest.SUMMON_DRAGON:
		case Marksman.FROST_PREY:
		case Bowmaster.PHOENIX:
		case Outlaw.GAVIOTA:
			return SummonMovementType.CIRCLE_FOLLOW;
		case DarkKnight.BEHOLDER:
		case FPArchMage.ELQUINES:
		case ILArchMage.IFRIT:
		case Bishop.BAHAMUT:
		case DawnWarrior.SOUL:
		case BlazeWizard.FLAME:
		case BlazeWizard.IFRIT:
		case WindArcher.STORM:
		case NightWalker.DARKNESS:
		case ThunderBreaker.LIGHTNING:
			return SummonMovementType.FOLLOW;
		}
		return null;
	}

	public boolean isSkill() {
		return this.skill;
	}

	public int getSourceId() {
		return this.sourceid;
	}

	public boolean makeChanceResult() {
		return (this.prop == 1.0) || (Math.random() < this.prop);
	}

	private static class CancelEffectAction implements Runnable {

		private final MapleStatEffect effect;
		private final WeakReference<MapleCharacter> target;
		private final long startTime;

		public CancelEffectAction(MapleCharacter target,
				MapleStatEffect effect, long startTime) {
			this.effect = effect;
			this.target = new WeakReference<>(target);
			this.startTime = startTime;
		}

		@Override
		public void run() {
			final MapleCharacter realTarget = this.target.get();
			if (realTarget != null) {
				realTarget.cancelEffect(this.effect, false, this.startTime);
			}
		}
	}

	public short getHp() {
		return this.hp;
	}

	public short getMp() {
		return this.mp;
	}

	public short getHpCon() {
		return this.hpCon;
	}

	public short getMpCon() {
		return this.mpCon;
	}

	public short getMatk() {
		return this.matk;
	}

	public int getDuration() {
		return this.duration;
	}

	public List<Pair<MapleBuffStat, Integer>> getStatups() {
		return this.statups;
	}

	public boolean sameSource(MapleStatEffect effect) {
		return (this.sourceid == effect.sourceid)
				&& (this.skill == effect.skill);
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getDamage() {
		return this.damage;
	}

	public int getAttackCount() {
		return this.attackCount;
	}

	public int getMobCount() {
		return this.mobCount;
	}

	public int getFixDamage() {
		return this.fixdamage;
	}

	public byte getBulletCount() {
		return this.bulletCount;
	}

	public byte getBulletConsume() {
		return this.bulletConsume;
	}

	public int getMoneyCon() {
		return this.moneyCon;
	}

	public int getCooldown() {
		return this.cooldown;
	}

	public Map<MonsterStatus, Integer> getMonsterStati() {
		return this.monsterStatus;
	}
}