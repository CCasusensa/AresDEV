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
package server;

import java.util.LinkedList;
import java.util.List;

import tools.MaplePacketCreator;
import client.MapleCharacter;

/**
 *
 * @author Danny
 */
public class MapleSquad {
	private final MapleCharacter leader;
	private final List<MapleCharacter> members = new LinkedList<>();
	private final List<MapleCharacter> bannedMembers = new LinkedList<>();
	private final int ch;
	private int status = 0;

	public MapleSquad(int ch, MapleCharacter leader) {
		this.leader = leader;
		this.members.add(leader);
		this.ch = ch;
		this.status = 1;
	}

	public MapleCharacter getLeader() {
		return this.leader;
	}

	public boolean containsMember(MapleCharacter member) {
		for (final MapleCharacter mmbr : this.members) {
			if (mmbr.getId() == member.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean isBanned(MapleCharacter member) {
		for (final MapleCharacter banned : this.bannedMembers) {
			if (banned.getId() == member.getId()) {
				return true;
			}
		}
		return false;
	}

	public List<MapleCharacter> getMembers() {
		return this.members;
	}

	public int getSquadSize() {
		return this.members.size();
	}

	public boolean addMember(MapleCharacter member) {
		if (this.isBanned(member)) {
			return false;
		} else {
			this.members.add(member);
			this.getLeader()
					.getClient()
					.announce(
							MaplePacketCreator.serverNotice(5, member.getName()
									+ " has joined the fight!"));
			return true;
		}
	}

	public void banMember(MapleCharacter member, boolean ban) {
		int index = -1;
		for (final MapleCharacter mmbr : this.members) {
			if (mmbr.getId() == member.getId()) {
				index = this.members.indexOf(mmbr);
			}
		}
		this.members.remove(index);
		if (ban) {
			this.bannedMembers.add(member);
		}
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getStatus() {
		return this.status;
	}

	public boolean equals(MapleSquad other) {
		if (other.ch == this.ch) {
			if (other.leader.getId() == this.leader.getId()) {
				return true;
			}
		}
		return false;
	}
}
