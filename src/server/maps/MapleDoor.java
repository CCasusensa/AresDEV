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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import server.MaplePortal;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;

/**
 *
 * @author Matze
 */
public class MapleDoor extends AbstractMapleMapObject {
	private final MapleCharacter owner;
	private final MapleMap town;
	private MaplePortal townPortal;
	private final MapleMap target;
	private final Point targetPosition;

	public MapleDoor(MapleCharacter owner, Point targetPosition) {
		super();
		this.owner = owner;
		this.target = owner.getMap();
		this.targetPosition = targetPosition;
		this.setPosition(this.targetPosition);
		this.town = this.target.getReturnMap();
		this.townPortal = this.getFreePortal();
	}

	public MapleDoor(MapleDoor origDoor) {
		super();
		this.owner = origDoor.owner;
		this.town = origDoor.town;
		this.townPortal = origDoor.townPortal;
		this.target = origDoor.target;
		this.targetPosition = origDoor.targetPosition;
		this.townPortal = origDoor.townPortal;
		this.setPosition(this.townPortal.getPosition());
	}

	private MaplePortal getFreePortal() {
		final List<MaplePortal> freePortals = new ArrayList<MaplePortal>();
		for (final MaplePortal port : this.town.getPortals()) {
			if (port.getType() == 6) {
				freePortals.add(port);
			}
		}
		Collections.sort(freePortals, new Comparator<MaplePortal>() {
			@Override
			public int compare(MaplePortal o1, MaplePortal o2) {
				if (o1.getId() < o2.getId()) {
					return -1;
				} else if (o1.getId() == o2.getId()) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		for (final MapleMapObject obj : this.town.getMapObjects()) {
			if (obj instanceof MapleDoor) {
				final MapleDoor door = (MapleDoor) obj;
				if ((door.getOwner().getParty() != null)
						&& this.owner.getParty().containsMembers(
								door.getOwner().getMPC())) {
					freePortals.remove(door.getTownPortal());
				}
			}
		}
		return freePortals.iterator().next();
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		if ((this.target.getId() == client.getPlayer().getMapId())
				|| ((this.owner == client.getPlayer()) && (this.owner
						.getParty() == null))) {
			client.announce(MaplePacketCreator.spawnDoor(
					this.owner.getId(),
					this.town.getId() == client.getPlayer().getMapId() ? this.townPortal
							.getPosition() : this.targetPosition, true));
			if ((this.owner.getParty() != null)
					&& ((this.owner == client.getPlayer()) || this.owner
							.getParty().containsMembers(
									client.getPlayer().getMPC()))) {
				client.announce(MaplePacketCreator.partyPortal(
						this.town.getId(), this.target.getId(),
						this.targetPosition));
			}
			client.announce(MaplePacketCreator.spawnPortal(this.town.getId(),
					this.target.getId(), this.targetPosition));
		}
	}

	@Override
	public void sendDestroyData(MapleClient client) {
		if ((this.target.getId() == client.getPlayer().getMapId())
				|| (this.owner == client.getPlayer())
				|| ((this.owner.getParty() != null) && this.owner.getParty()
						.containsMembers(client.getPlayer().getMPC()))) {
			if ((this.owner.getParty() != null)
					&& ((this.owner == client.getPlayer()) || this.owner
							.getParty().containsMembers(
									client.getPlayer().getMPC()))) {
				client.announce(MaplePacketCreator.partyPortal(999999999,
						999999999, new Point(-1, -1)));
			}
			client.announce(MaplePacketCreator.removeDoor(this.owner.getId(),
					false));
			client.announce(MaplePacketCreator.removeDoor(this.owner.getId(),
					true));
		}
	}

	public void warp(MapleCharacter chr, boolean toTown) {
		if ((chr == this.owner)
				|| ((this.owner.getParty() != null) && this.owner.getParty()
						.containsMembers(chr.getMPC()))) {
			if (!toTown) {
				chr.changeMap(this.target, this.targetPosition);
			} else {
				chr.changeMap(this.town, this.townPortal);
			}
		} else {
			chr.getClient().announce(MaplePacketCreator.enableActions());
		}
	}

	public MapleCharacter getOwner() {
		return this.owner;
	}

	public MapleMap getTown() {
		return this.town;
	}

	public MaplePortal getTownPortal() {
		return this.townPortal;
	}

	public MapleMap getTarget() {
		return this.target;
	}

	public Point getTargetPosition() {
		return this.targetPosition;
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.DOOR;
	}
}
