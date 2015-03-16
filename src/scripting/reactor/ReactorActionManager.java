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
package scripting.reactor;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import scripting.AbstractPlayerInteraction;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleNPC;
import server.maps.MapMonitor;
import server.maps.MapleReactor;
import server.maps.ReactorDropEntry;
import tools.MaplePacketCreator;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;

/**
 * @author Lerk
 */
public class ReactorActionManager extends AbstractPlayerInteraction {
	private final MapleReactor reactor;
	private final MapleClient client;

	public ReactorActionManager(MapleClient c, MapleReactor reactor) {
		super(c);
		this.reactor = reactor;
		this.client = c;
	}

	public void dropItems() {
		this.dropItems(false, 0, 0, 0, 0);
	}

	public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
		this.dropItems(meso, mesoChance, minMeso, maxMeso, 0);
	}

	public void dropItems(boolean meso, int mesoChance, int minMeso,
			int maxMeso, int minItems) {
		final List<ReactorDropEntry> chances = this.getDropChances();
		final List<ReactorDropEntry> items = new LinkedList<>();
		int numItems = 0;
		if (meso && (Math.random() < (1 / (double) mesoChance))) {
			items.add(new ReactorDropEntry(0, mesoChance, -1));
		}
		final Iterator<ReactorDropEntry> iter = chances.iterator();
		while (iter.hasNext()) {
			final ReactorDropEntry d = iter.next();
			if (Math.random() < (1 / (double) d.chance)) {
				numItems++;
				items.add(d);
			}
		}
		while (items.size() < minItems) {
			items.add(new ReactorDropEntry(0, mesoChance, -1));
			numItems++;
		}
		java.util.Collections.shuffle(items);
		final Point dropPos = this.reactor.getPosition();
		dropPos.x -= (12 * numItems);
		for (final ReactorDropEntry d : items) {
			if (d.itemId == 0) {
				final int range = maxMeso - minMeso;
				final int displayDrop = (int) (Math.random() * range) + minMeso;
				final int mesoDrop = (displayDrop * this.client
						.getWorldServer().getMesoRate());
				this.reactor.getMap().spawnMesoDrop(mesoDrop, dropPos,
						this.reactor, this.client.getPlayer(), false, (byte) 0);
			} else {
				Item drop;
				final MapleItemInformationProvider ii = MapleItemInformationProvider
						.getInstance();
				if (ii.getInventoryType(d.itemId) != MapleInventoryType.EQUIP) {
					drop = new Item(d.itemId, (byte) 0, (short) 1);
				} else {
					drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
				}
				this.reactor.getMap().spawnItemDrop(this.reactor,
						this.getPlayer(), drop, dropPos, false, true);
			}
			dropPos.x += 25;

		}
	}

	private List<ReactorDropEntry> getDropChances() {
		return ReactorScriptManager.getInstance()
				.getDrops(this.reactor.getId());
	}

	public void spawnMonster(int id) {
		this.spawnMonster(id, 1, this.getPosition());
	}

	public void createMapMonitor(int mapId, String portal) {
		new MapMonitor(this.client.getChannelServer().getMapFactory()
				.getMap(mapId), portal);
	}

	public void spawnMonster(int id, int qty) {
		this.spawnMonster(id, qty, this.getPosition());
	}

	public void spawnMonster(int id, int qty, int x, int y) {
		this.spawnMonster(id, qty, new Point(x, y));
	}

	private void spawnMonster(int id, int qty, Point pos) {
		for (int i = 0; i < qty; i++) {
			this.reactor.getMap().spawnMonsterOnGroudBelow(
					MapleLifeFactory.getMonster(id), pos);
		}
	}

	public Point getPosition() {
		final Point pos = this.reactor.getPosition();
		pos.y -= 10;
		return pos;
	}

	public void spawnNpc(int npcId) {
		this.spawnNpc(npcId, this.getPosition());
	}

	public void spawnNpc(int npcId, Point pos) {
		final MapleNPC npc = MapleLifeFactory.getNPC(npcId);
		if (npc != null) {
			npc.setPosition(pos);
			npc.setCy(pos.y);
			npc.setRx0(pos.x + 50);
			npc.setRx1(pos.x - 50);
			npc.setFh(this.reactor.getMap().getFootholds().findBelow(pos)
					.getId());
			this.reactor.getMap().addMapObject(npc);
			this.reactor.getMap().broadcastMessage(
					MaplePacketCreator.spawnNPC(npc));
		}
	}

	public MapleReactor getReactor() {
		return this.reactor;
	}

	public void spawnFakeMonster(int id) {
		this.reactor.getMap().spawnFakeMonsterOnGroundBelow(
				MapleLifeFactory.getMonster(id), this.getPosition());
	}
}