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
package net.server.world;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MapleParty {
	private MaplePartyCharacter leader;
	private final List<MaplePartyCharacter> members = new LinkedList<MaplePartyCharacter>();
	private int id;

	public MapleParty(int id, MaplePartyCharacter chrfor) {
		this.leader = chrfor;
		this.members.add(this.leader);
		this.id = id;
	}

	public boolean containsMembers(MaplePartyCharacter member) {
		return this.members.contains(member);
	}

	public void addMember(MaplePartyCharacter member) {
		this.members.add(member);
	}

	public void removeMember(MaplePartyCharacter member) {
		this.members.remove(member);
	}

	public void setLeader(MaplePartyCharacter victim) {
		this.leader = victim;
	}

	public void updateMember(MaplePartyCharacter member) {
		for (int i = 0; i < this.members.size(); i++) {
			if (this.members.get(i).equals(member)) {
				this.members.set(i, member);
			}
		}
	}

	public MaplePartyCharacter getMemberById(int id) {
		for (final MaplePartyCharacter chr : this.members) {
			if (chr.getId() == id) {
				return chr;
			}
		}
		return null;
	}

	public Collection<MaplePartyCharacter> getMembers() {
		return Collections.unmodifiableList(this.members);
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public MaplePartyCharacter getLeader() {
		return this.leader;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final MapleParty other = (MapleParty) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}
}
