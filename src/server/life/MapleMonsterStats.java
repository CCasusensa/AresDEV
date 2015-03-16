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
package server.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import server.life.MapleLifeFactory.BanishInfo;
import server.life.MapleLifeFactory.loseItem;
import server.life.MapleLifeFactory.selfDestruction;
import tools.Pair;

/**
 * @author Frz
 */
public class MapleMonsterStats {
	private int exp, hp, mp, level, PADamage, dropPeriod, cp, buffToGive,
			removeAfter;
	private boolean boss, undead, ffaLoot, isExplosiveReward, firstAttack,
			removeOnMiss;
	private String name;
	private final Map<String, Integer> animationTimes = new HashMap<String, Integer>();
	private final Map<Element, ElementalEffectiveness> resistance = new HashMap<Element, ElementalEffectiveness>();
	private List<Integer> revives = Collections.emptyList();
	private byte tagColor, tagBgColor;
	private final List<Pair<Integer, Integer>> skills = new ArrayList<Pair<Integer, Integer>>();
	private Pair<Integer, Integer> cool = null;
	private BanishInfo banish = null;
	private List<loseItem> loseItem = null;
	private selfDestruction selfDestruction = null;

	public int getExp() {
		return this.exp;
	}

	public void setExp(int exp) {
		this.exp = exp;
	}

	public int getHp() {
		return this.hp;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}

	public int getMp() {
		return this.mp;
	}

	public void setMp(int mp) {
		this.mp = mp;
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int removeAfter() {
		return this.removeAfter;
	}

	public void setRemoveAfter(int removeAfter) {
		this.removeAfter = removeAfter;
	}

	public int getDropPeriod() {
		return this.dropPeriod;
	}

	public void setDropPeriod(int dropPeriod) {
		this.dropPeriod = dropPeriod;
	}

	public void setBoss(boolean boss) {
		this.boss = boss;
	}

	public boolean isBoss() {
		return this.boss;
	}

	public void setFfaLoot(boolean ffaLoot) {
		this.ffaLoot = ffaLoot;
	}

	public boolean isFfaLoot() {
		return this.ffaLoot;
	}

	public void setAnimationTime(String name, int delay) {
		this.animationTimes.put(name, delay);
	}

	public int getAnimationTime(String name) {
		final Integer ret = this.animationTimes.get(name);
		if (ret == null) {
			return 500;
		}
		return ret.intValue();
	}

	public boolean isMobile() {
		return this.animationTimes.containsKey("move")
				|| this.animationTimes.containsKey("fly");
	}

	public List<Integer> getRevives() {
		return this.revives;
	}

	public void setRevives(List<Integer> revives) {
		this.revives = revives;
	}

	public void setUndead(boolean undead) {
		this.undead = undead;
	}

	public boolean getUndead() {
		return this.undead;
	}

	public void setEffectiveness(Element e, ElementalEffectiveness ee) {
		this.resistance.put(e, ee);
	}

	public ElementalEffectiveness getEffectiveness(Element e) {
		final ElementalEffectiveness elementalEffectiveness = this.resistance
				.get(e);
		if (elementalEffectiveness == null) {
			return ElementalEffectiveness.NORMAL;
		} else {
			return elementalEffectiveness;
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte getTagColor() {
		return this.tagColor;
	}

	public void setTagColor(int tagColor) {
		this.tagColor = (byte) tagColor;
	}

	public byte getTagBgColor() {
		return this.tagBgColor;
	}

	public void setTagBgColor(int tagBgColor) {
		this.tagBgColor = (byte) tagBgColor;
	}

	public void setSkills(List<Pair<Integer, Integer>> skills) {
		for (final Pair<Integer, Integer> skill : skills) {
			this.skills.add(skill);
		}
	}

	public List<Pair<Integer, Integer>> getSkills() {
		return Collections.unmodifiableList(this.skills);
	}

	public int getNoSkills() {
		return this.skills.size();
	}

	public boolean hasSkill(int skillId, int level) {
		for (final Pair<Integer, Integer> skill : this.skills) {
			if ((skill.getLeft() == skillId) && (skill.getRight() == level)) {
				return true;
			}
		}
		return false;
	}

	public void setFirstAttack(boolean firstAttack) {
		this.firstAttack = firstAttack;
	}

	public boolean isFirstAttack() {
		return this.firstAttack;
	}

	public void setBuffToGive(int buff) {
		this.buffToGive = buff;
	}

	public int getBuffToGive() {
		return this.buffToGive;
	}

	void removeEffectiveness(Element e) {
		this.resistance.remove(e);
	}

	public BanishInfo getBanishInfo() {
		return this.banish;
	}

	public void setBanishInfo(BanishInfo banish) {
		this.banish = banish;
	}

	public int getPADamage() {
		return this.PADamage;
	}

	public void setPADamage(int PADamage) {
		this.PADamage = PADamage;
	}

	public int getCP() {
		return this.cp;
	}

	public void setCP(int cp) {
		this.cp = cp;
	}

	public List<loseItem> loseItem() {
		return this.loseItem;
	}

	public void addLoseItem(loseItem li) {
		if (this.loseItem == null) {
			this.loseItem = new LinkedList<loseItem>();
		}
		this.loseItem.add(li);
	}

	public selfDestruction selfDestruction() {
		return this.selfDestruction;
	}

	public void setSelfDestruction(selfDestruction sd) {
		this.selfDestruction = sd;
	}

	public void setExplosiveReward(boolean isExplosiveReward) {
		this.isExplosiveReward = isExplosiveReward;
	}

	public boolean isExplosiveReward() {
		return this.isExplosiveReward;
	}

	public void setRemoveOnMiss(boolean removeOnMiss) {
		this.removeOnMiss = removeOnMiss;
	}

	public boolean removeOnMiss() {
		return this.removeOnMiss;
	}

	public void setCool(Pair<Integer, Integer> cool) {
		this.cool = cool;
	}

	public Pair<Integer, Integer> getCool() {
		return this.cool;
	}
}
