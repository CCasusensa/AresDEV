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
 License.te

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.command;

import net.server.Server;
import net.server.channel.Channel;
import scripting.npc.NPCScriptManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;

public class Commands {

	public static boolean executePlayerCommand(MapleClient c, String[] sub,
			char heading) {

		final MapleCharacter player = c.getPlayer();

		if (heading == '/') {
			switch (sub[0]) {
			case "help":
				player.addYellowMessage("If you are stuck, use /dispose!");
				break;
			case "dispose":
				NPCScriptManager.getInstance().dispose(c);
				c.announce(MaplePacketCreator.enableActions());
				player.addMessage("Sorry for the bug! Your character has been disposed!");
				break;
			default:
				if (player.gmLevel() == 0) {
					player.addYellowMessage("Please do /help for more information!");
				}
				return false;
			}
		}
		return true;
	}

	public static boolean executeGMCommand(MapleClient c, String[] sub,
			char heading) {

		final MapleCharacter player = c.getPlayer();
		final Channel channelServer = c.getChannelServer();
		Server.getInstance();

		if (heading == '!') {
			switch (sub[0]) {
			case "level":
				try {
					player.setLevel(Integer.parseInt(sub[1]));
					player.gainExp(-player.getExp(), false, false);
					player.updateSingleStat(MapleStat.LEVEL, player.getLevel());
					player.addMessage("Successfully set your level to: Lv. "
							+ sub[1] + ".");
				} catch (final Exception ex) {
					player.addMessage("Invalid syntax! Please use: !level <LEVEL>");
				}
				break;
			case "warp":
				try {
					final MapleMap mapToWarp = channelServer.getMapFactory().getMap(
							Integer.parseInt(sub[1]));
					player.changeMap(mapToWarp);
					player.addMessage("Successfully warped to: " + sub[1] + ".");
				} catch (final Exception ex) {
					player.addMessage("Invalid syntax! Please use: !warp <MAPID>");
				}
				break;
			case "warpall":
				try {
					for (final MapleCharacter currentCharacter : channelServer
							.getPlayerStorage().getAllCharacters()) {
						final MapleMap mapToWarpAll = player.getMap();
						currentCharacter.changeMap(mapToWarpAll);
					}
				} catch (final Exception ex) {
					player.addMessage("Something went wrong with !warpall command! Please inform the developer!");
					ex.printStackTrace();
				}
				break;
			default:
				player.addMessage("Invalid command!");
			}
		}

		return true;
	}

	public static boolean executeAdminCommand(MapleClient c, String[] sub,
			char heading) {
		final MapleCharacter player = c.getPlayer();

		if (heading == '@') {
			switch (sub[0]) {
			default:
				player.addMessage("Invalid command!");
			}
		}

		return true;
	}
}