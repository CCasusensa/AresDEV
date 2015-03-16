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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class MapleMessenger {
	private final List<MapleMessengerCharacter> members = new ArrayList<MapleMessengerCharacter>(
			3);
	private final int id;
	private final boolean[] pos = new boolean[3];

	public MapleMessenger(int id, MapleMessengerCharacter chrfor) {
		this.members.add(chrfor);
		chrfor.setPosition(this.getLowestPosition());
		this.id = id;
	}

	public void addMember(MapleMessengerCharacter member) {
		this.members.add(member);
		member.setPosition(this.getLowestPosition());
	}

	public void removeMember(MapleMessengerCharacter member) {
		this.pos[member.getPosition()] = true;
		this.members.remove(member);
	}

	public void silentRemoveMember(MapleMessengerCharacter member) {
		this.members.remove(member);
	}

	public void silentAddMember(MapleMessengerCharacter member, int position) {
		this.members.add(member);
		member.setPosition(position);
	}

	public Collection<MapleMessengerCharacter> getMembers() {
		return Collections.unmodifiableList(this.members);
	}

	public int getLowestPosition() {// (:
		for (byte b = 0; b < 3; b++) {
			if (this.pos[b]) {
				this.pos[b] = false;
				return b;
			}
		}
		return -1;
	}

	public int getPositionByName(String name) {
		for (final MapleMessengerCharacter messengerchar : this.members) {
			if (messengerchar.getName().equals(name)) {
				return messengerchar.getPosition();
			}
		}
		return 4;
	}

	public int getId() {
		return this.id;
	}
}
