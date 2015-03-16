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

import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;

/**
 *
 * @author LaiLaiNoob
 */
public class MapleMist extends AbstractMapleMapObject {
	private final Rectangle mistPosition;
	private MapleCharacter owner = null;
	private MapleMonster mob = null;
	private MapleStatEffect source;
	private MobSkill skill;
	private final boolean isMobMist;
	private boolean isPoisonMist;
	private final int skillDelay;

	public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
		this.mistPosition = mistPosition;
		this.mob = mob;
		this.skill = skill;
		this.isMobMist = true;
		this.isPoisonMist = true;
		this.skillDelay = 0;
	}

	public MapleMist(Rectangle mistPosition, MapleCharacter owner,
			MapleStatEffect source) {
		this.mistPosition = mistPosition;
		this.owner = owner;
		this.source = source;
		this.skillDelay = 8;
		this.isMobMist = false;
		switch (source.getSourceId()) {
		case 4221006: // Smoke Screen
			this.isPoisonMist = false;
			break;
		case 2111003: // FP mist
		case 12111005: // Flame Gear
		case 14111006: // Poison Bomb
			this.isPoisonMist = true;
			break;
		}
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.MIST;
	}

	@Override
	public Point getPosition() {
		return this.mistPosition.getLocation();
	}

	public Skill getSourceSkill() {
		return SkillFactory.getSkill(this.source.getSourceId());
	}

	public boolean isMobMist() {
		return this.isMobMist;
	}

	public boolean isPoisonMist() {
		return this.isPoisonMist;
	}

	public int getSkillDelay() {
		return this.skillDelay;
	}

	public MapleMonster getMobOwner() {
		return this.mob;
	}

	public MapleCharacter getOwner() {
		return this.owner;
	}

	public Rectangle getBox() {
		return this.mistPosition;
	}

	@Override
	public void setPosition(Point position) {
		throw new UnsupportedOperationException();
	}

	public final byte[] makeDestroyData() {
		return MaplePacketCreator.removeMist(this.getObjectId());
	}

	public final byte[] makeSpawnData() {
		if (this.owner != null) {
			return MaplePacketCreator.spawnMist(this.getObjectId(), this.owner
					.getId(), this.getSourceSkill().getId(), this.owner
					.getSkillLevel(SkillFactory.getSkill(this.source
							.getSourceId())), this);
		}
		return MaplePacketCreator.spawnMist(this.getObjectId(),
				this.mob.getId(), this.skill.getSkillId(),
				this.skill.getSkillLevel(), this);
	}

	public final byte[] makeFakeSpawnData(int level) {
		if (this.owner != null) {
			return MaplePacketCreator.spawnMist(this.getObjectId(),
					this.owner.getId(), this.getSourceSkill().getId(), level,
					this);
		}
		return MaplePacketCreator.spawnMist(this.getObjectId(),
				this.mob.getId(), this.skill.getSkillId(),
				this.skill.getSkillLevel(), this);
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		client.announce(this.makeSpawnData());
	}

	@Override
	public void sendDestroyData(MapleClient client) {
		client.announce(this.makeDestroyData());
	}

	public boolean makeChanceResult() {
		return this.source.makeChanceResult();
	}
}
