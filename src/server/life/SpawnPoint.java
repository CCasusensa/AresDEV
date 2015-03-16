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

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

import client.MapleCharacter;

public class SpawnPoint {

	private final int monster, mobTime, team, fh, f;
	private final Point pos;
	private long nextPossibleSpawn;
	private int mobInterval = 5000;
	private final AtomicInteger spawnedMonsters = new AtomicInteger(0);
	private final boolean immobile;

	public SpawnPoint(final MapleMonster monster, Point pos, boolean immobile,
			int mobTime, int mobInterval, int team) {
		this.monster = monster.getId();
		this.pos = new Point(pos);
		this.mobTime = mobTime;
		this.team = team;
		this.fh = monster.getFh();
		this.f = monster.getF();
		this.immobile = immobile;
		this.mobInterval = mobInterval;
		this.nextPossibleSpawn = System.currentTimeMillis();
	}

	public boolean shouldSpawn() {
		if ((this.mobTime < 0)
				|| (((this.mobTime != 0) || this.immobile) && (this.spawnedMonsters
						.get() > 0)) || (this.spawnedMonsters.get() > 2)) {// lol
			return false;
		}
		return this.nextPossibleSpawn <= System.currentTimeMillis();
	}

	public MapleMonster getMonster() {
		final MapleMonster mob = new MapleMonster(
				MapleLifeFactory.getMonster(this.monster));
		mob.setPosition(new Point(this.pos));
		mob.setTeam(this.team);
		mob.setFh(this.fh);
		mob.setF(this.f);
		this.spawnedMonsters.incrementAndGet();
		mob.addListener(new MonsterListener() {
			@Override
			public void monsterKilled(MapleMonster monster,
					MapleCharacter highestDamageChar) {
				SpawnPoint.this.nextPossibleSpawn = System.currentTimeMillis();
				if (SpawnPoint.this.mobTime > 0) {
					SpawnPoint.this.nextPossibleSpawn += SpawnPoint.this.mobTime * 1000;
				} else {
					SpawnPoint.this.nextPossibleSpawn += monster
							.getAnimationTime("die1");
				}
				SpawnPoint.this.spawnedMonsters.decrementAndGet();
			}
		});
		if (this.mobTime == 0) {
			this.nextPossibleSpawn = System.currentTimeMillis()
					+ this.mobInterval;
		}
		return mob;
	}

	public Point getPosition() {
		return this.pos;
	}

	public final int getF() {
		return this.f;
	}

	public final int getFh() {
		return this.fh;
	}
}
