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

import java.io.File;

import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.TimerManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;
import client.MapleCharacter;

/**
 *
 * @author FloppyDisk
 */
public final class MapleOxQuiz {
	private int round = 1;
	private int question = 1;
	private MapleMap map = null;
	private final int expGain = 200;
	private static MapleDataProvider stringData = MapleDataProviderFactory
			.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));

	public MapleOxQuiz(MapleMap map) {
		this.map = map;
		this.round = Randomizer.nextInt(9);
		this.question = 1;
	}

	private boolean isCorrectAnswer(MapleCharacter chr, int answer) {
		final double x = chr.getPosition().getX();
		final double y = chr.getPosition().getY();
		if (((x > -234) && (y > -26) && (answer == 0))
				|| ((x < -234) && (y > -26) && (answer == 1))) {
			chr.dropMessage("Correct!");
			return true;
		}
		return false;
	}

	public void sendQuestion() {
		int gm = 0;
		for (final MapleCharacter mc : this.map.getCharacters()) {
			if (mc.gmLevel() > 0) {
				gm++;
			}
		}
		final int number = gm;
		this.map.broadcastMessage(MaplePacketCreator.showOXQuiz(this.round,
				this.question, true));
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				MapleOxQuiz.this.map.broadcastMessage(MaplePacketCreator
						.showOXQuiz(MapleOxQuiz.this.round,
								MapleOxQuiz.this.question, true));
				for (final MapleCharacter chr : MapleOxQuiz.this.map.getCharacters()) {
					if (chr != null) // make sure they aren't null... maybe
										// something can happen in 12 seconds.
					{
						if (!MapleOxQuiz.this.isCorrectAnswer(
								chr,
								getOXAnswer(MapleOxQuiz.this.round,
										MapleOxQuiz.this.question))
								&& !chr.isGM()) {
							chr.changeMap(chr.getMap().getReturnMap());
						} else {
							chr.gainExp(MapleOxQuiz.this.expGain, true, true);
						}
					}
				}
				// do question
				if (((MapleOxQuiz.this.round == 1) && (MapleOxQuiz.this.question == 29))
						|| (((MapleOxQuiz.this.round == 2) || (MapleOxQuiz.this.round == 3)) && (MapleOxQuiz.this.question == 17))
						|| (((MapleOxQuiz.this.round == 4) || (MapleOxQuiz.this.round == 8)) && (MapleOxQuiz.this.question == 12))
						|| ((MapleOxQuiz.this.round == 5) && (MapleOxQuiz.this.question == 26))
						|| ((MapleOxQuiz.this.round == 9) && (MapleOxQuiz.this.question == 44))
						|| (((MapleOxQuiz.this.round == 6) || (MapleOxQuiz.this.round == 7)) && (MapleOxQuiz.this.question == 16))) {
					MapleOxQuiz.this.question = 100;
				} else {
					MapleOxQuiz.this.question++;
				}
				// send question
				if ((MapleOxQuiz.this.map.getCharacters().size() - number) <= 1) {
					MapleOxQuiz.this.map.broadcastMessage(MaplePacketCreator
							.serverNotice(6, "The event has ended"));
					MapleOxQuiz.this.map.getPortal("join00").setPortalStatus(
							true);
					MapleOxQuiz.this.map.setOx(null);
					MapleOxQuiz.this.map.setOxQuiz(false);
					// prizes here
					return;
				}
				MapleOxQuiz.this.sendQuestion();
			}
		}, 30000); // Time to answer = 30 seconds ( Ox Quiz packet shows a 30
					// second timer.
	}

	private static int getOXAnswer(int imgdir, int id) {
		return MapleDataTool.getInt(stringData.getData("OXQuiz.img")
				.getChildByPath("" + imgdir + "").getChildByPath("" + id + "")
				.getChildByPath("a"));
	}
}
