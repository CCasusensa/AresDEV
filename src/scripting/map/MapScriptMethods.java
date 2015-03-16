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
package scripting.map;

import scripting.AbstractPlayerInteraction;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import client.MapleClient;
import client.MapleQuestStatus;

public class MapScriptMethods extends AbstractPlayerInteraction {

	public MapScriptMethods(MapleClient c) {
		super(c);
	}

	String rewardstring = " title has been rewarded. Please see NPC Dalair to receive your Medal.";

	public void displayAranIntro() {
		switch (this.c.getPlayer().getMapId()) {
		case 914090010:
			this.lockUI();
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction1.img/aranTutorial/Scene0"));
			break;
		case 914090011:
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction1.img/aranTutorial/Scene1"
							+ this.c.getPlayer().getGender()));
			break;
		case 914090012:
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction1.img/aranTutorial/Scene2"
							+ this.c.getPlayer().getGender()));
			break;
		case 914090013:
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction1.img/aranTutorial/Scene3"));
			break;
		case 914090100:
			this.lockUI();
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction1.img/aranTutorial/HandedPoleArm"
							+ this.c.getPlayer().getGender()));
			break;
		}
	}

	public void startExplorerExperience() {
		if (this.c.getPlayer().getMapId() == 1020100) // Swordman
		{
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction3.img/swordman/Scene"
							+ this.c.getPlayer().getGender()));
		} else if (this.c.getPlayer().getMapId() == 1020200) // Magician
		{
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction3.img/magician/Scene"
							+ this.c.getPlayer().getGender()));
		} else if (this.c.getPlayer().getMapId() == 1020300) // Archer
		{
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction3.img/archer/Scene"
							+ this.c.getPlayer().getGender()));
		} else if (this.c.getPlayer().getMapId() == 1020400) // Rogue
		{
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction3.img/rogue/Scene"
							+ this.c.getPlayer().getGender()));
		} else if (this.c.getPlayer().getMapId() == 1020500) // Pirate
		{
			this.c.announce(MaplePacketCreator
					.showIntro("Effect/Direction3.img/pirate/Scene"
							+ this.c.getPlayer().getGender()));
		}
	}

	public void goAdventure() {
		this.lockUI();
		this.c.announce(MaplePacketCreator
				.showIntro("Effect/Direction3.img/goAdventure/Scene"
						+ this.c.getPlayer().getGender()));
	}

	public void goLith() {
		this.lockUI();
		this.c.announce(MaplePacketCreator
				.showIntro("Effect/Direction3.img/goLith/Scene"
						+ this.c.getPlayer().getGender()));
	}

	public void explorerQuest(short questid, String questName) {
		final MapleQuest quest = MapleQuest.getInstance(questid);
		if (!this.isQuestStarted(questid)) {
			if (!quest.forceStart(this.getPlayer(), 9000066)) {
				return;
			}
		}
		final MapleQuestStatus q = this.getPlayer().getQuest(quest);
		if (!q.addMedalMap(this.getPlayer().getMapId())) {
			return;
		}
		final String status = Integer.toString(q.getMedalProgress());
		final int infoex = quest.getInfoEx();
		this.getPlayer()
				.announce(
						MaplePacketCreator.questProgress(quest.getInfoNumber(),
								status));
		final StringBuilder smp = new StringBuilder();
		final StringBuilder etm = new StringBuilder();
		if (q.getMedalProgress() == infoex) {
			etm.append("Earned the ").append(questName).append(" title!");
			smp.append("You have earned the <").append(questName).append(">")
					.append(this.rewardstring);
			this.getPlayer().announce(
					MaplePacketCreator.getShowQuestCompletion(quest.getId()));
		} else {
			this.getPlayer().announce(
					MaplePacketCreator.earnTitleMessage(status + "/" + infoex
							+ " regions explored."));
			etm.append("Trying for the ").append(questName).append(" title.");
			smp.append("You made progress on the ").append(questName)
					.append(" title. ").append(status).append("/")
					.append(infoex);
		}
		this.getPlayer().announce(
				MaplePacketCreator.earnTitleMessage(etm.toString()));
		this.showInfoText(smp.toString());
	}

	public void touchTheSky() { // 29004
		final MapleQuest quest = MapleQuest.getInstance(29004);
		if (!this.isQuestStarted(29004)) {
			if (!quest.forceStart(this.getPlayer(), 9000066)) {
				return;
			}
		}
		final MapleQuestStatus q = this.getPlayer().getQuest(quest);
		if (!q.addMedalMap(this.getPlayer().getMapId())) {
			return;
		}
		final String status = Integer.toString(q.getMedalProgress());
		this.getPlayer()
				.announce(
						MaplePacketCreator.questProgress(quest.getInfoNumber(),
								status));
		this.getPlayer().announce(
				MaplePacketCreator.earnTitleMessage(status + "/5 Completed"));
		this.getPlayer()
				.announce(
						MaplePacketCreator
								.earnTitleMessage("The One Who's Touched the Sky title in progress."));
		if (q.getMedalProgress() == quest.getInfoEx()) {
			this.showInfoText("The One Who's Touched the Sky"
					+ this.rewardstring);
			this.getPlayer().announce(
					MaplePacketCreator.getShowQuestCompletion(quest.getId()));
		} else {
			this.showInfoText("The One Who's Touched the Sky title in progress. "
					+ status + "/5 Completed");
		}
	}
}
