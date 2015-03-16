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

import java.util.ArrayList;
import java.util.List;

import net.server.Server;
import server.TimerManager;
import tools.MaplePacketCreator;
import client.MapleCharacter;

/*
 * MapleTVEffect
 * @author MrXotic
 */
public class MapleTVEffect {
	private List<String> message = new ArrayList<>(5);
	private final MapleCharacter user;
	private static boolean active;
	private final int type;
	private final int world;
	private final MapleCharacter partner;

	public MapleTVEffect(MapleCharacter user_, MapleCharacter partner_,
			List<String> msg, int type_, int world_) {
		this.message = msg;
		this.user = user_;
		this.type = type_;
		this.world = world_;
		this.partner = partner_;
		this.broadcastTV(true);
	}

	public static boolean isActive() {
		return active;
	}

	private void setActive(boolean set) {
		active = set;
	}

	private void broadcastTV(boolean active_) {
		final Server server = Server.getInstance();
		this.setActive(active_);
		if (active_) {
			server.broadcastMessage(this.world, MaplePacketCreator.enableTV());
			server.broadcastMessage(this.world, MaplePacketCreator.sendTV(
					this.user, this.message, this.type <= 2 ? this.type
							: this.type - 3, this.partner));
			int delay = 15000;
			if (this.type == 4) {
				delay = 30000;
			} else if (this.type == 5) {
				delay = 60000;
			}
			TimerManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					MapleTVEffect.this.broadcastTV(false);
				}
			}, delay);
		} else {
			server.broadcastMessage(this.world, MaplePacketCreator.removeTV());
		}
	}
}
