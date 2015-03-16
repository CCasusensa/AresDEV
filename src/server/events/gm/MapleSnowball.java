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

package server.events.gm;

import java.util.LinkedList;
import java.util.List;

import server.TimerManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import client.MapleCharacter;

/**
 *
 * @author kevintjuh93
 */
public class MapleSnowball {
	private final MapleMap map;
	private int position = 0;
	private int hits = 25;
	private int snowmanhp = 7500;
	private boolean hittable = false;
	private final int team;
	private boolean winner = false;
	List<MapleCharacter> characters = new LinkedList<MapleCharacter>();

	public MapleSnowball(int team, MapleMap map) {
		this.map = map;
		this.team = team;

		for (final MapleCharacter chr : map.getCharacters()) {
			if (chr.getTeam() == team) {
				this.characters.add(chr);
			}
		}
	}

	public void startEvent() {
		if (this.hittable == true) {
			return;
		}

		for (final MapleCharacter chr : this.characters) {
			if (chr != null) {
				chr.announce(MaplePacketCreator.rollSnowBall(false, 1,
						this.map.getSnowball(0), this.map.getSnowball(1)));
				chr.announce(MaplePacketCreator.getClock(600));
			}
		}
		this.hittable = true;
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (MapleSnowball.this.map.getSnowball(MapleSnowball.this.team)
						.getPosition() > MapleSnowball.this.map.getSnowball(
						MapleSnowball.this.team == 0 ? 1 : 0).getPosition()) {
					for (final MapleCharacter chr : MapleSnowball.this.characters) {
						if (chr != null) {
							chr.announce(MaplePacketCreator.rollSnowBall(false,
									3, MapleSnowball.this.map.getSnowball(0),
									MapleSnowball.this.map.getSnowball(0)));
						}
					}
					MapleSnowball.this.winner = true;
				} else if (MapleSnowball.this.map.getSnowball(
						MapleSnowball.this.team == 0 ? 1 : 0).getPosition() > MapleSnowball.this.map
						.getSnowball(MapleSnowball.this.team).getPosition()) {
					for (final MapleCharacter chr : MapleSnowball.this.characters) {
						if (chr != null) {
							chr.announce(MaplePacketCreator.rollSnowBall(false,
									4, MapleSnowball.this.map.getSnowball(0),
									MapleSnowball.this.map.getSnowball(0)));
						}
					}
					MapleSnowball.this.winner = true;
				} // Else
				MapleSnowball.this.warpOut();
			}
		}, 600000);

	}

	public boolean isHittable() {
		return this.hittable;
	}

	public void setHittable(boolean hit) {
		this.hittable = hit;
	}

	public int getPosition() {
		return this.position;
	}

	public int getSnowmanHP() {
		return this.snowmanhp;
	}

	public void setSnowmanHP(int hp) {
		this.snowmanhp = hp;
	}

	public void hit(int what, int damage) {
		if (what < 2) {
			if (damage > 0) {
				this.hits--;
			} else {
				if ((this.snowmanhp - damage) < 0) {
					this.snowmanhp = 0;

					TimerManager.getInstance().schedule(new Runnable() {

						@Override
						public void run() {
							MapleSnowball.this.setSnowmanHP(7500);
							MapleSnowball.this.message(5);
						}
					}, 10000);
				} else {
					this.snowmanhp -= damage;
				}
				this.map.broadcastMessage(MaplePacketCreator.rollSnowBall(
						false, 1, this.map.getSnowball(0),
						this.map.getSnowball(1)));
			}
		}

		if (this.hits == 0) {
			this.position += 1;
			if (this.position == 45) {
				this.map.getSnowball(this.team == 0 ? 1 : 0).message(1);
			} else if (this.position == 290) {
				this.map.getSnowball(this.team == 0 ? 1 : 0).message(2);
			} else if (this.position == 560) {
				this.map.getSnowball(this.team == 0 ? 1 : 0).message(3);
			}

			this.hits = 25;
			this.map.broadcastMessage(MaplePacketCreator.rollSnowBall(false, 0,
					this.map.getSnowball(0), this.map.getSnowball(1)));
			this.map.broadcastMessage(MaplePacketCreator.rollSnowBall(false, 1,
					this.map.getSnowball(0), this.map.getSnowball(1)));
		}
		this.map.broadcastMessage(MaplePacketCreator.hitSnowBall(what, damage));
	}

	public void message(int message) {
		for (final MapleCharacter chr : this.characters) {
			if (chr != null) {
				chr.announce(MaplePacketCreator.snowballMessage(this.team,
						message));
			}
		}
	}

	public void warpOut() {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (MapleSnowball.this.winner == true) {
					MapleSnowball.this.map.warpOutByTeam(
							MapleSnowball.this.team, 109050000);
				} else {
					MapleSnowball.this.map.warpOutByTeam(
							MapleSnowball.this.team, 109050001);
				}

				MapleSnowball.this.map.setSnowball(MapleSnowball.this.team,
						null);
			}
		}, 10000);
	}
}