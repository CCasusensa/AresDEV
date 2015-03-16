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

import net.AbstractMaplePacketHandler;
import server.maps.MapleSummon;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;

public final class DamageSummonHandler extends AbstractMaplePacketHandler {
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final int skillid = slea.readInt(); // Bugged? might not be skillid.
		final int unkByte = slea.readByte();
		final int damage = slea.readInt();
		final int monsterIdFrom = slea.readInt();
		if (SkillFactory.getSkill(skillid) != null) {
			final MapleCharacter player = c.getPlayer();
			final MapleSummon summon = player.getSummons().get(skillid);
			if (summon != null) {
				summon.addHP(-damage);
				if (summon.getHP() <= 0) {
					player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
				}
			}
			player.getMap().broadcastMessage(
					player,
					MaplePacketCreator.damageSummon(player.getId(), skillid,
							damage, unkByte, monsterIdFrom),
					summon.getPosition());
		}
	}
}
