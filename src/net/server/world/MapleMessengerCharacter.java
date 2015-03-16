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

import client.MapleCharacter;

public class MapleMessengerCharacter {
	private final String name;
	private final int id;
	private int position;
	private final int channel;
	private final boolean online;

	public MapleMessengerCharacter(MapleCharacter maplechar) {
		this.name = maplechar.getName();
		this.channel = maplechar.getClient().getChannel();
		this.id = maplechar.getId();
		this.online = true;
		this.position = 0;
	}

	public MapleMessengerCharacter(MapleCharacter maplechar, int position) {
		this.name = maplechar.getName();
		this.channel = maplechar.getClient().getChannel();
		this.id = maplechar.getId();
		this.online = true;
		this.position = position;
	}

	public int getId() {
		return this.id;
	}

	public int getChannel() {
		return this.channel;
	}

	public String getName() {
		return this.name;
	}

	public boolean isOnline() {
		return this.online;
	}

	public int getPosition() {
		return this.position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result)
				+ ((this.name == null) ? 0 : this.name.hashCode());
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
		final MapleMessengerCharacter other = (MapleMessengerCharacter) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
