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
package net.server.channel.handlers;

import java.util.ArrayList;
import java.util.List;

import net.AbstractMaplePacketHandler;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.maps.MapleSummon;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.status.MonsterStatusEffect;

public final class SummonDamageHandler extends AbstractMaplePacketHandler {
	public final class SummonAttackEntry {

		private final int monsterOid;
		private final int damage;

		public SummonAttackEntry(int monsterOid, int damage) {
			this.monsterOid = monsterOid;
			this.damage = damage;
		}

		public int getMonsterOid() {
			return this.monsterOid;
		}

		public int getDamage() {
			return this.damage;
		}
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		final int oid = slea.readInt();
		final MapleCharacter player = c.getPlayer();
		if (!player.isAlive()) {
			return;
		}
		MapleSummon summon = null;
		for (final MapleSummon sum : player.getSummons().values()) {
			if (sum.getObjectId() == oid) {
				summon = sum;
			}
		}
		if (summon == null) {
			return;
		}
		final Skill summonSkill = SkillFactory.getSkill(summon.getSkill());
		final MapleStatEffect summonEffect = summonSkill.getEffect(summon
				.getSkillLevel());
		slea.skip(4);
		final List<SummonAttackEntry> allDamage = new ArrayList<>();
		final byte direction = slea.readByte();
		final int numAttacked = slea.readByte();
		slea.skip(8); // Thanks Gerald :D, I failed lol (mob x,y and summon x,y)
		for (int x = 0; x < numAttacked; x++) {
			final int monsterOid = slea.readInt(); // attacked oid
			slea.skip(18);
			final int damage = slea.readInt();
			allDamage.add(new SummonAttackEntry(monsterOid, damage));
		}
		player.getMap().broadcastMessage(
				player,
				MaplePacketCreator.summonAttack(player.getId(),
						summon.getSkill(), direction, allDamage),
				summon.getPosition());
		for (final SummonAttackEntry attackEntry : allDamage) {
			final int damage = attackEntry.getDamage();
			final MapleMonster target = player.getMap().getMonsterByOid(
					attackEntry.getMonsterOid());
			if (target != null) {
				if ((damage > 0) && (summonEffect.getMonsterStati().size() > 0)) {
					if (summonEffect.makeChanceResult()) {
						target.applyStatus(player, new MonsterStatusEffect(
								summonEffect.getMonsterStati(), summonSkill,
								null, false), summonEffect.isPoison(), 4000);
					}
				}
				player.getMap().damageMonster(player, target, damage);
			}
		}
	}
}