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

import server.MapleStatEffect;
import server.TimerManager;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleCharacter.CancelCooldownAction;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;

public final class MagicDamageHandler extends AbstractDealDamageHandler {
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final MapleCharacter player = c.getPlayer();
		final AttackInfo attack = this.parseDamage(slea, player, false);
		byte[] packet = MaplePacketCreator.magicAttack(player, attack.skill,
				attack.skilllevel, attack.stance, attack.numAttackedAndDamage,
				attack.allDamage, -1, attack.speed, attack.direction,
				attack.display);
		if ((attack.skill == 2121001) || (attack.skill == 2221001)
				|| (attack.skill == 2321001)) {
			packet = MaplePacketCreator.magicAttack(player, attack.skill,
					attack.skilllevel, attack.stance,
					attack.numAttackedAndDamage, attack.allDamage,
					attack.charge, attack.speed, attack.direction,
					attack.display);
		}
		player.getMap().broadcastMessage(player, packet, false, true);
		final MapleStatEffect effect = attack.getAttackEffect(player, null);
		final Skill skill = SkillFactory.getSkill(attack.skill);
		final MapleStatEffect effect_ = skill.getEffect(player
				.getSkillLevel(skill));
		if (effect_.getCooldown() > 0) {
			if (player.skillisCooling(attack.skill)) {
				return;
			} else {
				c.announce(MaplePacketCreator.skillCooldown(attack.skill,
						effect_.getCooldown()));
				player.addCooldown(
						attack.skill,
						System.currentTimeMillis(),
						effect_.getCooldown() * 1000,
						TimerManager.getInstance().schedule(
								new CancelCooldownAction(player, attack.skill),
								effect_.getCooldown() * 1000));
			}
		}
		this.applyAttack(attack, player, effect.getAttackCount());
		final Skill eaterSkill = SkillFactory
				.getSkill((player.getJob().getId() - (player.getJob().getId() % 10)) * 10000);// MP
																								// Eater,
																								// works
																								// with
																								// right
																								// job
		final int eaterLevel = player.getSkillLevel(eaterSkill);
		if (eaterLevel > 0) {
			for (final Integer singleDamage : attack.allDamage.keySet()) {
				eaterSkill.getEffect(eaterLevel).applyPassive(player,
						player.getMap().getMapObject(singleDamage), 0);
			}
		}
	}
}
