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

package server.partyquest;

import java.util.ArrayList;
import java.util.List;

import net.server.Server;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import client.MapleCharacter;

/**
 *
 * @author kevintjuh93
 */
public class PartyQuest {
	int channel, world;
	MapleParty party;
	List<MapleCharacter> participants = new ArrayList<>();

	public PartyQuest(MapleParty party) {
		this.party = party;
		final MaplePartyCharacter leader = party.getLeader();
		this.channel = leader.getChannel();
		this.world = leader.getWorld();
		final int mapid = leader.getMapId();
		for (final MaplePartyCharacter pchr : party.getMembers()) {
			if ((pchr.getChannel() == this.channel)
					&& (pchr.getMapId() == mapid)) {
				final MapleCharacter chr = Server.getInstance()
						.getWorld(this.world).getChannel(this.channel)
						.getPlayerStorage().getCharacterById(pchr.getId());
				if (chr != null) {
					this.participants.add(chr);
				}
			}
		}
	}

	public MapleParty getParty() {
		return this.party;
	}

	public List<MapleCharacter> getParticipants() {
		return this.participants;
	}

	public void removeParticipant(MapleCharacter chr) throws Throwable {
		synchronized (this.participants) {
			this.participants.remove(chr);
			chr.setPartyQuest(null);
			if (this.participants.isEmpty()) {
				super.finalize();
				// System.gc();
			}
		}
	}
}
