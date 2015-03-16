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

import java.awt.Rectangle;
import java.util.List;

import scripting.reactor.ReactorScriptManager;
import server.TimerManager;
import tools.MaplePacketCreator;
import tools.Pair;
import client.MapleClient;

/**
 *
 * @author Lerk
 */
public class MapleReactor extends AbstractMapleMapObject {
	private final int rid;
	private final MapleReactorStats stats;
	private byte state;
	private int delay;
	private MapleMap map;
	private String name;
	private boolean timerActive;
	private boolean alive;

	public MapleReactor(MapleReactorStats stats, int rid) {
		this.stats = stats;
		this.rid = rid;
		this.alive = true;
	}

	public void setTimerActive(boolean active) {
		this.timerActive = active;
	}

	public boolean isTimerActive() {
		return this.timerActive;
	}

	public void setState(byte state) {
		this.state = state;
	}

	public byte getState() {
		return this.state;
	}

	public int getId() {
		return this.rid;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getDelay() {
		return this.delay;
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.REACTOR;
	}

	public int getReactorType() {
		return this.stats.getType(this.state);
	}

	public void setMap(MapleMap map) {
		this.map = map;
	}

	public MapleMap getMap() {
		return this.map;
	}

	public Pair<Integer, Integer> getReactItem(byte index) {
		return this.stats.getReactItem(this.state, index);
	}

	public boolean isAlive() {
		return this.alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	@Override
	public void sendDestroyData(MapleClient client) {
		client.announce(this.makeDestroyData());
	}

	public final byte[] makeDestroyData() {
		return MaplePacketCreator.destroyReactor(this);
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		client.announce(this.makeSpawnData());
	}

	public final byte[] makeSpawnData() {
		return MaplePacketCreator.spawnReactor(this);
	}

	public void delayedHitReactor(final MapleClient c, long delay) {
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleReactor.this.hitReactor(c);
			}
		}, delay);
	}

	public void hitReactor(MapleClient c) {
		this.hitReactor(0, (short) 0, 0, c);
	}

	public void hitReactor(int charPos, short stance, int skillid, MapleClient c) {
		try {
			if ((this.stats.getType(this.state) < 999)
					&& (this.stats.getType(this.state) != -1)) {// type
				// 2
				// =
				// only
				// hit
				// from
				// right
				// (kerning
				// swamp
				// plants),
				// 00
				// is
				// air
				// left
				// 02
				// is
				// ground
				// left
				if (!((this.stats.getType(this.state) == 2) && ((charPos == 0) || (charPos == 2)))) { // get
					// next
					// state
					for (byte b = 0; b < this.stats.getStateSize(this.state); b++) {// YAY?
						final List<Integer> activeSkills = this.stats
								.getActiveSkills(this.state, b);
						if (activeSkills != null) {
							if (!activeSkills.contains(skillid)) {
								continue;
							}
						}
						this.state = this.stats.getNextState(this.state, b);
						if (this.stats.getNextState(this.state, b) == -1) {// end
																			// of
							// reactor
							if (this.stats.getType(this.state) < 100) {// reactor
																		// broken
								if (this.delay > 0) {
									this.map.destroyReactor(this.getObjectId());
								} else {// trigger as normal
									this.map.broadcastMessage(MaplePacketCreator
											.triggerReactor(this, stance));
								}
							} else {// item-triggered on final step
								this.map.broadcastMessage(MaplePacketCreator
										.triggerReactor(this, stance));
							}
							ReactorScriptManager.getInstance().act(c, this);
						} else { // reactor not broken yet
							this.map.broadcastMessage(MaplePacketCreator
									.triggerReactor(this, stance));
							if (this.state == this.stats.getNextState(
									this.state, b)) {// current
								// state
								// =
								// next
								// state,
								// looping
								// reactor
								ReactorScriptManager.getInstance().act(c, this);
							}
						}
						break;
					}
				}
			} else {
				this.state++;
				this.map.broadcastMessage(MaplePacketCreator.triggerReactor(
						this, stance));
				ReactorScriptManager.getInstance().act(c, this);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public Rectangle getArea() {
		return new Rectangle(this.getPosition().x + this.stats.getTL().x,
				this.getPosition().y + this.stats.getTL().y,
				this.stats.getBR().x - this.stats.getTL().x,
				this.stats.getBR().y - this.stats.getTL().y);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
