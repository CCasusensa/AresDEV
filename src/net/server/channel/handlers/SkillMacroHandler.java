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
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;
import client.SkillMacro;

public final class SkillMacroHandler extends AbstractMaplePacketHandler {
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final int num = slea.readByte();
		for (int i = 0; i < num; i++) {
			final String name = slea.readMapleAsciiString();
			final int shout = slea.readByte();
			final int skill1 = slea.readInt();
			final int skill2 = slea.readInt();
			final int skill3 = slea.readInt();
			final SkillMacro macro = new SkillMacro(skill1, skill2, skill3,
					name, shout, i);
			c.getPlayer().updateMacros(i, macro);
		}
	}
}
