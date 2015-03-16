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
package net.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import client.MapleCharacter;

public class PlayerStorage {
	private final ReentrantReadWriteLock locks = new ReentrantReadWriteLock();
	private final Lock rlock = this.locks.readLock();
	private final Lock wlock = this.locks.writeLock();
	private final Map<Integer, MapleCharacter> storage = new LinkedHashMap<>();

	public void addPlayer(MapleCharacter chr) {
		this.wlock.lock();
		try {
			this.storage.put(chr.getId(), chr);
		} finally {
			this.wlock.unlock();
		}
	}

	public MapleCharacter removePlayer(int chr) {
		this.wlock.lock();
		try {
			return this.storage.remove(chr);
		} finally {
			this.wlock.unlock();
		}
	}

	public MapleCharacter getCharacterByName(String name) {
		this.rlock.lock();
		try {
			for (final MapleCharacter chr : this.storage.values()) {
				if (chr.getName().toLowerCase().equals(name.toLowerCase())) {
					return chr;
				}
			}
			return null;
		} finally {
			this.rlock.unlock();
		}
	}

	public MapleCharacter getCharacterById(int id) {
		this.rlock.lock();
		try {
			return this.storage.get(id);
		} finally {
			this.rlock.unlock();
		}
	}

	public Collection<MapleCharacter> getAllCharacters() {
		this.rlock.lock();
		try {
			return this.storage.values();
		} finally {
			this.rlock.unlock();
		}
	}

	public final void disconnectAll() {
		this.wlock.lock();
		try {
			final Iterator<MapleCharacter> chrit = this.storage.values()
					.iterator();
			while (chrit.hasNext()) {
				chrit.next().getClient().disconnect(true, false);
				chrit.remove();
			}
		} finally {
			this.wlock.unlock();
		}
	}
}