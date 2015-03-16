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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import server.TimerManager;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import client.MapleCharacter;

/**
 *
 * @author kevintjuh93 - LOST MOTIVATION >=(
 */
public class MonsterCarnival {
	private final MonsterCarnivalParty red, blue;
	private MapleMap map;
	private final int room;
	private long time = 0;
	private long timeStarted = 0;
	private ScheduledFuture<?> schedule = null;

	public MonsterCarnival(int room, byte channel, MonsterCarnivalParty red1,
			MonsterCarnivalParty blue1) {
		// this.map =
		// Channel.getInstance(channel).getMapFactory().getMap(980000001 + (room
		// * 100));
		this.room = room;
		this.red = red1;
		this.blue = blue1;
		this.timeStarted = System.currentTimeMillis();
		this.time = 600000;
		this.map.broadcastMessage(MaplePacketCreator
				.getClock((int) (this.time / 1000)));

		for (final MapleCharacter chr : this.red.getMembers()) {
			chr.setCarnival(this);
		}
		for (final MapleCharacter chr : this.blue.getMembers()) {
			chr.setCarnival(this);
		}

		this.schedule = TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (MonsterCarnival.this.red.getTotalCP() > MonsterCarnival.this.blue
						.getTotalCP()) {
					MonsterCarnival.this.red.setWinner(true);
					MonsterCarnival.this.blue.setWinner(false);
					MonsterCarnival.this.red.displayMatchResult();
					MonsterCarnival.this.blue.displayMatchResult();
				} else if (MonsterCarnival.this.blue.getTotalCP() > MonsterCarnival.this.red
						.getTotalCP()) {
					MonsterCarnival.this.red.setWinner(false);
					MonsterCarnival.this.blue.setWinner(true);
					MonsterCarnival.this.red.displayMatchResult();
					MonsterCarnival.this.blue.displayMatchResult();
				} else {
					MonsterCarnival.this.red.setWinner(false);
					MonsterCarnival.this.blue.setWinner(false);
					MonsterCarnival.this.red.displayMatchResult();
					MonsterCarnival.this.blue.displayMatchResult();
				}
				MonsterCarnival.this.saveResults();
				MonsterCarnival.this.warpOut();
			}

		}, this.time);
		/*
		 * if (room == 0) { MapleData data =
		 * MapleDataProviderFactory.getDataProvider(new
		 * File(System.getProperty("wzpath") + "/Map.wz")).getData("Map/Map9" +
		 * (980000001 + (room * 100)) +
		 * ".img").getChildByPath("monsterCarnival"); if (data != null) { for
		 * (MapleData p : data.getChildByPath("mobGenPos").getChildren()) {
		 * MapleData team = p.getChildByPath("team"); if (team != null) { if
		 * (team.getData().equals(0)) redmonsterpoints.add(new
		 * Point(MapleDataTool.getInt(p.getChildByPath("x")),
		 * MapleDataTool.getInt(p.getChildByPath("y")))); else
		 * bluemonsterpoints.add(new
		 * Point(MapleDataTool.getInt(p.getChildByPath("x")),
		 * MapleDataTool.getInt(p.getChildByPath("y")))); } else
		 * monsterpoints.add(new
		 * Point(MapleDataTool.getInt(p.getChildByPath("x")),
		 * MapleDataTool.getInt(p.getChildByPath("y")))); } for (MapleData p :
		 * data.getChildByPath("guardianGenPos").getChildren()) { MapleData team
		 * = p.getChildByPath("team"); if (team != null) { if
		 * (team.getData().equals(0)) redreactorpoints.add(new
		 * Point(MapleDataTool.getInt(p.getChildByPath("x")),
		 * MapleDataTool.getInt(p.getChildByPath("y")))); else
		 * bluereactorpoints.add(new
		 * Point(MapleDataTool.getInt(p.getChildByPath("x")),
		 * MapleDataTool.getInt(p.getChildByPath("y")))); } else
		 * reactorpoints.add(new
		 * Point(MapleDataTool.getInt(p.getChildByPath("x")),
		 * MapleDataTool.getInt(p.getChildByPath("y")))); } } }
		 */
	}

	public long getTimeLeft() {
		return this.time - (System.currentTimeMillis() - this.timeStarted);
	}

	public MonsterCarnivalParty getPartyRed() {
		return this.red;
	}

	public MonsterCarnivalParty getPartyBlue() {
		return this.blue;
	}

	public MonsterCarnivalParty oppositeTeam(MonsterCarnivalParty team) {
		if (team == this.red) {
			return this.blue;
		} else {
			return this.red;
		}
	}

	public void playerLeft(MapleCharacter chr) {
		this.map.broadcastMessage(chr, MaplePacketCreator.leaveCPQ(chr));
	}

	private void warpOut() {
		this.schedule = TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MonsterCarnival.this.red.warpOut();
				MonsterCarnival.this.blue.warpOut();
			}
		}, 12000);
	}

	public int getRoom() {
		return this.room;
	}

	public void saveResults() {
		final Connection con = DatabaseConnection.getConnection();
		try {
			final PreparedStatement ps = con
					.prepareStatement("INSERT INTO carnivalresults VALUES (?,?,?,?)");
			for (final MapleCharacter chr : this.red.getMembers()) {
				ps.setInt(1, chr.getId());
				ps.setInt(2, chr.getCP());
				ps.setInt(3, this.red.getTotalCP());
				ps.setInt(4, this.red.isWinner() ? 1 : 0);
				ps.execute();
			}
			for (final MapleCharacter chr : this.blue.getMembers()) {
				ps.setInt(1, chr.getId());
				ps.setInt(2, chr.getCP());
				ps.setInt(3, this.blue.getTotalCP());
				ps.setInt(4, this.blue.isWinner() ? 1 : 0);
				ps.execute();
			}
			ps.close();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
