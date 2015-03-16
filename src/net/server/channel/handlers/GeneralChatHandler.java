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

import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleClient;
import client.command.Commands;

public final class GeneralChatHandler extends net.AbstractMaplePacketHandler {

	/**
	 * @ - Admin Command ! - GM Command / - Player Command GMS - Like :)
	 */
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final String s = slea.readMapleAsciiString();
		final MapleCharacter player = c.getPlayer();
		final char heading = s.charAt(0);
		if ((heading == '/') || (heading == '!') || (heading == '@')) {
			final String[] sp = s.split(" ");
			sp[0] = sp[0].toLowerCase().substring(1);
			if (!Commands.executePlayerCommand(c, sp, heading)) {
				if (player.isGM()) {
					if (!Commands.executeGMCommand(c, sp, heading)) {
						Commands.executeAdminCommand(c, sp, heading);
					}
				}
			}
		} else {
			if (!player.isHidden()) {
				player.getMap().broadcastMessage(
						MaplePacketCreator.getChatText(player.getId(), s,
								player.isGM(), slea.readByte()));
			} else {
				player.getMap().broadcastGMMessage(
						MaplePacketCreator.getChatText(player.getId(), s,
								player.isGM(), slea.readByte()));
			}
		}
	}
}
