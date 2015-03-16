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

package server.partyquest;

import java.util.concurrent.ScheduledFuture;

import net.server.world.MapleParty;
import server.MapleItemInformationProvider;
import server.TimerManager;
import tools.MaplePacketCreator;
import client.MapleCharacter;

/**
 *
 * @author kevintjuh93
 */
public class Pyramid extends PartyQuest {
	public enum PyramidMode {
		EASY(0), NORMAL(1), HARD(2), HELL(3);
		int mode;

		PyramidMode(int mode) {
			this.mode = mode;
		}

		public int getMode() {
			return this.mode;
		}
	}

	int kill = 0, miss = 0, cool = 0, exp = 0, map, count;
	byte coolAdd = 5, missSub = 4, decrease = 1;// hmmm
	short gauge;
	byte rank, skill = 0, stage = 0, buffcount = 0;// buffcount includes buffs +
													// skills
	PyramidMode mode;

	ScheduledFuture<?> timer = null;
	ScheduledFuture<?> gaugeSchedule = null;

	public Pyramid(MapleParty party, PyramidMode mode, int mapid) {
		super(party);
		this.mode = mode;
		this.map = mapid;

		final byte plus = (byte) mode.getMode();
		this.coolAdd += plus;
		this.missSub += plus;
		switch (plus) {
		case 0:
			this.decrease = 1;
		case 1:
		case 2:
			this.decrease = 2;
		case 3:
			this.decrease = 3;
		}
	}

	public void startGaugeSchedule() {
		if (this.gaugeSchedule == null) {
			this.gauge = 100;
			this.count = 0;
			this.gaugeSchedule = TimerManager.getInstance().register(
					new Runnable() {
						@Override
						public void run() {
							Pyramid.this.gauge -= Pyramid.this.decrease;
							if (Pyramid.this.gauge <= 0) {
								Pyramid.this.warp(926010001);
							}

						}
					}, 1000);
		}
	}

	public void kill() {
		this.kill++;
		if (this.gauge < 100) {
			this.count++;
		}
		this.gauge++;
		this.broadcastInfo("hit", this.kill);
		if (this.gauge >= 100) {
			this.gauge = 100;
		}
		this.checkBuffs();
	}

	public void cool() {
		this.cool++;
		int plus = this.coolAdd;
		if ((this.gauge + this.coolAdd) > 100) {
			plus -= ((this.gauge + this.coolAdd) - 100);
		}
		this.gauge += plus;
		this.count += plus;
		if (this.gauge >= 100) {
			this.gauge = 100;
		}
		this.broadcastInfo("cool", this.cool);
		this.checkBuffs();

	}

	public void miss() {
		this.miss++;
		this.count -= this.missSub;
		this.gauge -= this.missSub;
		this.broadcastInfo("miss", this.miss);
	}

	public int timer() {
		int value;
		if (this.stage > 0) {
			value = 180;
		} else {
			value = 120;
		}

		this.timer = TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				Pyramid.this.stage++;
				Pyramid.this.warp(Pyramid.this.map + (Pyramid.this.stage * 100));// Should
																					// work
																					// :D
			}
		}, value * 1000);// , 4000
		this.broadcastInfo("party", this.getParticipants().size() > 1 ? 1 : 0);
		this.broadcastInfo("hit", this.kill);
		this.broadcastInfo("miss", this.miss);
		this.broadcastInfo("cool", this.cool);
		this.broadcastInfo("skill", this.skill);
		this.broadcastInfo("laststage", this.stage);
		this.startGaugeSchedule();
		return value;
	}

	public void warp(int mapid) {
		for (final MapleCharacter chr : this.getParticipants()) {
			chr.changeMap(mapid);
		}
		if (this.stage > -1) {
			this.gaugeSchedule.cancel(false);
			this.gaugeSchedule = null;
			this.timer.cancel(false);
			this.timer = null;
		} else {
			this.stage = 0;
		}
	}

	public void broadcastInfo(String info, int amount) {
		for (final MapleCharacter chr : this.getParticipants()) {
			chr.announce(MaplePacketCreator.getEnergy("massacre_" + info,
					amount));
			chr.announce(MaplePacketCreator.pyramidGauge(this.count));
		}
	}

	public boolean useSkill() {
		if (this.skill < 1) {
			return false;
		}

		this.skill--;
		this.broadcastInfo("skill", this.skill);
		return true;
	}

	public void checkBuffs() {
		final int total = (this.kill + this.cool);
		if ((this.buffcount == 0) && (total >= 250)) {
			this.buffcount++;
			final MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			for (final MapleCharacter chr : this.getParticipants()) {
				ii.getItemEffect(2022585).applyTo(chr);
			}

		} else if ((this.buffcount == 1) && (total >= 500)) {
			this.buffcount++;
			this.skill++;
			final MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			for (final MapleCharacter chr : this.getParticipants()) {
				chr.announce(MaplePacketCreator.getEnergy("massacre_skill",
						this.skill));
				ii.getItemEffect(2022586).applyTo(chr);
			}
		} else if ((this.buffcount == 2) && (total >= 1000)) {
			this.buffcount++;
			this.skill++;
			final MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			for (final MapleCharacter chr : this.getParticipants()) {
				chr.announce(MaplePacketCreator.getEnergy("massacre_skill",
						this.skill));
				ii.getItemEffect(2022587).applyTo(chr);
			}
		} else if ((this.buffcount == 3) && (total >= 1500)) {
			this.skill++;
			this.broadcastInfo("skill", this.skill);
		} else if ((this.buffcount == 4) && (total >= 2000)) {
			this.buffcount++;
			this.skill++;
			final MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			for (final MapleCharacter chr : this.getParticipants()) {
				chr.announce(MaplePacketCreator.getEnergy("massacre_skill",
						this.skill));
				ii.getItemEffect(2022588).applyTo(chr);
			}
		} else if ((this.buffcount == 5) && (total >= 2500)) {
			this.skill++;
			this.broadcastInfo("skill", this.skill);
		} else if ((this.buffcount == 6) && (total >= 3000)) {
			this.skill++;
			this.broadcastInfo("skill", this.skill);
		}
	}

	public void sendScore(MapleCharacter chr) {
		if (this.exp == 0) {
			final int totalkills = (this.kill + this.cool);
			if (this.stage == 5) {
				if (totalkills >= 3000) {
					this.rank = 0;
				} else if (totalkills >= 2000) {
					this.rank = 1;
				} else if (totalkills >= 1500) {
					this.rank = 2;
				} else if (totalkills >= 500) {
					this.rank = 3;
				} else {
					this.rank = 4;
				}
			} else {
				if (totalkills >= 2000) {
					this.rank = 3;
				} else {
					this.rank = 4;
				}
			}

			if (this.rank == 0) {
				this.exp = (60500 + (5500 * this.mode.getMode()));
			} else if (this.rank == 1) {
				this.exp = (55000 + (5000 * this.mode.getMode()));
			} else if (this.rank == 2) {
				this.exp = (46750 + (4250 * this.mode.getMode()));
			} else if (this.rank == 3) {
				this.exp = (22000 + (2000 * this.mode.getMode()));
			}

			this.exp += ((this.kill * 2) + (this.cool * 10));
		}
		chr.announce(MaplePacketCreator.pyramidScore(this.rank, this.exp));
		chr.gainExp(this.exp, true, true);
	}
}
