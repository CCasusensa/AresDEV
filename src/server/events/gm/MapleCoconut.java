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
// Make them better :)
public class MapleCoconut extends MapleEvent {
	private MapleMap map = null;
	private int MapleScore = 0;
	private int StoryScore = 0;
	private int countBombing = 80;
	private int countFalling = 401;
	private int countStopped = 20;
	private final List<MapleCoconuts> coconuts = new LinkedList<MapleCoconuts>();

	public MapleCoconut(MapleMap map) {
		super(1, 50);
		this.map = map;
	}

	public void startEvent() {
		this.map.startEvent();
		for (int i = 0; i < 506; i++) {
			this.coconuts.add(new MapleCoconuts(i));
		}
		this.map.broadcastMessage(MaplePacketCreator.hitCoconut(true, 0, 0));
		this.setCoconutsHittable(true);
		this.map.broadcastMessage(MaplePacketCreator.getClock(300));

		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (MapleCoconut.this.map.getId() == 109080000) {
					if (MapleCoconut.this.getMapleScore() == MapleCoconut.this.getStoryScore()) {
						MapleCoconut.this.bonusTime();
					} else if (MapleCoconut.this.getMapleScore() > MapleCoconut.this
							.getStoryScore()) {
						for (final MapleCharacter chr : MapleCoconut.this.map
								.getCharacters()) {
							if (chr.getTeam() == 0) {
								chr.getClient()
										.announce(
												MaplePacketCreator
														.showEffect("event/coconut/victory"));
								chr.getClient().announce(
										MaplePacketCreator
												.playSound("Coconut/Victory"));
							} else {
								chr.getClient()
										.announce(
												MaplePacketCreator
														.showEffect("event/coconut/lose"));
								chr.getClient().announce(
										MaplePacketCreator
												.playSound("Coconut/Failed"));
							}
						}
						MapleCoconut.this.warpOut();
					} else {
						for (final MapleCharacter chr : MapleCoconut.this.map
								.getCharacters()) {
							if (chr.getTeam() == 1) {
								chr.getClient()
										.announce(
												MaplePacketCreator
														.showEffect("event/coconut/victory"));
								chr.getClient().announce(
										MaplePacketCreator
												.playSound("Coconut/Victory"));
							} else {
								chr.getClient()
										.announce(
												MaplePacketCreator
														.showEffect("event/coconut/lose"));
								chr.getClient().announce(
										MaplePacketCreator
												.playSound("Coconut/Failed"));
							}
						}
						MapleCoconut.this.warpOut();
					}
				}
			}
		}, 300000);
	}

	public void bonusTime() {
		this.map.broadcastMessage(MaplePacketCreator.getClock(120));
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (MapleCoconut.this.getMapleScore() == MapleCoconut.this.getStoryScore()) {
					for (final MapleCharacter chr : MapleCoconut.this.map
							.getCharacters()) {
						chr.getClient().announce(
								MaplePacketCreator
										.showEffect("event/coconut/lose"));
						chr.getClient().announce(
								MaplePacketCreator.playSound("Coconut/Failed"));
					}
					MapleCoconut.this.warpOut();
				} else if (MapleCoconut.this.getMapleScore() > MapleCoconut.this
						.getStoryScore()) {
					for (final MapleCharacter chr : MapleCoconut.this.map
							.getCharacters()) {
						if (chr.getTeam() == 0) {
							chr.getClient()
									.announce(
											MaplePacketCreator
													.showEffect("event/coconut/victory"));
							chr.getClient().announce(
									MaplePacketCreator
											.playSound("Coconut/Victory"));
						} else {
							chr.getClient().announce(
									MaplePacketCreator
											.showEffect("event/coconut/lose"));
							chr.getClient().announce(
									MaplePacketCreator
											.playSound("Coconut/Failed"));
						}
					}
					MapleCoconut.this.warpOut();
				} else {
					for (final MapleCharacter chr : MapleCoconut.this.map
							.getCharacters()) {
						if (chr.getTeam() == 1) {
							chr.getClient()
									.announce(
											MaplePacketCreator
													.showEffect("event/coconut/victory"));
							chr.getClient().announce(
									MaplePacketCreator
											.playSound("Coconut/Victory"));
						} else {
							chr.getClient().announce(
									MaplePacketCreator
											.showEffect("event/coconut/lose"));
							chr.getClient().announce(
									MaplePacketCreator
											.playSound("Coconut/Failed"));
						}
					}
					MapleCoconut.this.warpOut();
				}
			}
		}, 120000);

	}

	public void warpOut() {
		this.setCoconutsHittable(false);
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				for (final MapleCharacter chr : MapleCoconut.this.map.getCharacters()) {
					if (((MapleCoconut.this.getMapleScore() > MapleCoconut.this
							.getStoryScore()) && (chr.getTeam() == 0))
							|| ((MapleCoconut.this.getStoryScore() > MapleCoconut.this
									.getMapleScore()) && (chr.getTeam() == 1))) {
						chr.changeMap(109050000);
					} else {
						chr.changeMap(109050001);
					}
				}
				MapleCoconut.this.map.setCoconut(null);
			}
		}, 12000);
	}

	public int getMapleScore() {
		return this.MapleScore;
	}

	public int getStoryScore() {
		return this.StoryScore;
	}

	public void addMapleScore() {
		this.MapleScore += 1;
	}

	public void addStoryScore() {
		this.StoryScore += 1;
	}

	public int getBombings() {
		return this.countBombing;
	}

	public void bombCoconut() {
		this.countBombing--;
	}

	public int getFalling() {
		return this.countFalling;
	}

	public void fallCoconut() {
		this.countFalling--;
	}

	public int getStopped() {
		return this.countStopped;
	}

	public void stopCoconut() {
		this.countStopped--;
	}

	public MapleCoconuts getCoconut(int id) {
		return this.coconuts.get(id);
	}

	public List<MapleCoconuts> getAllCoconuts() {
		return this.coconuts;
	}

	public void setCoconutsHittable(boolean hittable) {
		for (final MapleCoconuts nut : this.coconuts) {
			nut.setHittable(hittable);
		}
	}
}