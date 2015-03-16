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
package client.status;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import server.life.MobSkill;
import tools.ArrayMap;
import client.Skill;

public class MonsterStatusEffect {

	private final Map<MonsterStatus, Integer> stati;
	private final Skill skill;
	private final MobSkill mobskill;
	private final boolean monsterSkill;
	private ScheduledFuture<?> cancelTask;
	private ScheduledFuture<?> damageSchedule;

	public MonsterStatusEffect(Map<MonsterStatus, Integer> stati,
			Skill skillId, MobSkill mobskill, boolean monsterSkill) {
		this.stati = new ArrayMap<>(stati);
		this.skill = skillId;
		this.monsterSkill = monsterSkill;
		this.mobskill = mobskill;
	}

	public Map<MonsterStatus, Integer> getStati() {
		return this.stati;
	}

	public Integer setValue(MonsterStatus status, Integer newVal) {
		return this.stati.put(status, newVal);
	}

	public Skill getSkill() {
		return this.skill;
	}

	public boolean isMonsterSkill() {
		return this.monsterSkill;
	}

	public final void cancelTask() {
		if (this.cancelTask != null) {
			this.cancelTask.cancel(false);
		}
		this.cancelTask = null;
	}

	public ScheduledFuture<?> getCancelTask() {
		return this.cancelTask;
	}

	public void setCancelTask(ScheduledFuture<?> cancelTask) {
		this.cancelTask = cancelTask;
	}

	public void removeActiveStatus(MonsterStatus stat) {
		this.stati.remove(stat);
	}

	public void setDamageSchedule(ScheduledFuture<?> damageSchedule) {
		this.damageSchedule = damageSchedule;
	}

	public void cancelDamageSchedule() {
		if (this.damageSchedule != null) {
			this.damageSchedule.cancel(false);
		}
	}

	public MobSkill getMobSkill() {
		return this.mobskill;
	}
}
