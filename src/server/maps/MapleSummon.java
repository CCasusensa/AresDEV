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

import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;

/**
 *
 * @author Jan
 */
public class MapleSummon extends AbstractAnimatedMapleMapObject {
	private final MapleCharacter owner;
	private final byte skillLevel;
	private final int skill;
	private int hp;
	private final SummonMovementType movementType;

	public MapleSummon(MapleCharacter owner, int skill, Point pos,
			SummonMovementType movementType) {
		this.owner = owner;
		this.skill = skill;
		this.skillLevel = owner.getSkillLevel(SkillFactory.getSkill(skill));
		if (this.skillLevel == 0) {
			throw new RuntimeException();
		}

		this.movementType = movementType;
		this.setPosition(pos);
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		if (this != null) {
			client.announce(MaplePacketCreator.spawnSummon(this, false));
		}

	}

	@Override
	public void sendDestroyData(MapleClient client) {
		client.announce(MaplePacketCreator.removeSummon(this, true));
	}

	public MapleCharacter getOwner() {
		return this.owner;
	}

	public int getSkill() {
		return this.skill;
	}

	public int getHP() {
		return this.hp;
	}

	public void addHP(int delta) {
		this.hp += delta;
	}

	public SummonMovementType getMovementType() {
		return this.movementType;
	}

	public boolean isStationary() {
		return ((this.skill == 3111002) || (this.skill == 3211002)
				|| (this.skill == 5211001) || (this.skill == 13111004));
	}

	public byte getSkillLevel() {
		return this.skillLevel;
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.SUMMON;
	}

	public final boolean isPuppet() {
		switch (this.skill) {
		case 3111002:
		case 3211002:
		case 13111004:
			return true;
		}
		return false;
	}
}
