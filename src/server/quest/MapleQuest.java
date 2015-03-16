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
package server.quest;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleQuestStatus.Status;

/**
 *
 * @author Matze
 */
public class MapleQuest {
	private static Map<Integer, MapleQuest> quests = new HashMap<>();
	protected short infoNumber, infoex, id;
	protected int timeLimit, timeLimit2;
	protected List<MapleQuestRequirement> startReqs = new LinkedList<>();
	protected List<MapleQuestRequirement> completeReqs = new LinkedList<>();
	protected List<MapleQuestAction> startActs = new LinkedList<>();
	protected List<MapleQuestAction> completeActs = new LinkedList<>();
	protected List<Integer> relevantMobs = new LinkedList<>();
	private boolean autoStart;
	private boolean autoPreComplete;
	private boolean repeatable = false;
	private final static MapleDataProvider questData = MapleDataProviderFactory
			.getDataProvider(new File(System.getProperty("wzpath")
					+ "/Quest.wz"));
	private static MapleData actions = questData.getData("Act.img");
	private static MapleData requirements = questData.getData("Check.img");
	private static MapleData info = questData.getData("QuestInfo.img");

	private MapleQuest(int id) {
		this.id = (short) id;
		final MapleData reqData = requirements.getChildByPath(String
				.valueOf(id));
		if (reqData == null) {// most likely infoEx
			return;
		}
		final MapleData startReqData = reqData.getChildByPath("0");
		if (startReqData != null) {
			for (final MapleData startReq : startReqData.getChildren()) {
				final MapleQuestRequirementType type = MapleQuestRequirementType
						.getByWZName(startReq.getName());
				if (type.equals(MapleQuestRequirementType.INTERVAL)) {
					this.repeatable = true;
				}
				final MapleQuestRequirement req = new MapleQuestRequirement(
						this, type, startReq);
				if (req.getType().equals(MapleQuestRequirementType.MOB)) {
					for (final MapleData mob : startReq.getChildren()) {
						this.relevantMobs.add(MapleDataTool.getInt(mob
								.getChildByPath("id")));
					}
				}
				this.startReqs.add(req);
			}
		}
		final MapleData completeReqData = reqData.getChildByPath("1");
		if (completeReqData != null) {
			for (final MapleData completeReq : completeReqData.getChildren()) {
				final MapleQuestRequirement req = new MapleQuestRequirement(
						this, MapleQuestRequirementType.getByWZName(completeReq
								.getName()), completeReq);
				if (req.getType().equals(MapleQuestRequirementType.INFO_NUMBER)) {
					this.infoNumber = (short) MapleDataTool.getInt(completeReq,
							0);
				}
				if (req.getType().equals(MapleQuestRequirementType.INFO_EX)) {
					final MapleData zero = completeReq.getChildByPath("0");
					if (zero != null) {
						final MapleData value = zero.getChildByPath("value");
						if (value != null) {
							this.infoex = Short.parseShort(MapleDataTool
									.getString(value, "0"));
						}
					}
				}
				if (req.getType().equals(MapleQuestRequirementType.MOB)) {
					for (final MapleData mob : completeReq.getChildren()) {
						this.relevantMobs.add(MapleDataTool.getInt(mob
								.getChildByPath("id")));
					}
					Collections.sort(this.relevantMobs);
				}
				this.completeReqs.add(req);
			}
		}
		final MapleData actData = actions.getChildByPath(String.valueOf(id));
		if (actData == null) {
			return;
		}
		final MapleData startActData = actData.getChildByPath("0");
		if (startActData != null) {
			for (final MapleData startAct : startActData.getChildren()) {
				final MapleQuestActionType questActionType = MapleQuestActionType
						.getByWZName(startAct.getName());
				this.startActs.add(new MapleQuestAction(questActionType,
						startAct, this));
			}
		}
		final MapleData completeActData = actions.getChildByPath(
				String.valueOf(id)).getChildByPath("1");
		if (completeActData != null) {
			for (final MapleData completeAct : completeActData.getChildren()) {
				this.completeActs
						.add(new MapleQuestAction(MapleQuestActionType
								.getByWZName(completeAct.getName()),
								completeAct, this));
			}
		}
		final MapleData questInfo = info.getChildByPath(String.valueOf(id));

		this.timeLimit = MapleDataTool.getInt("timeLimit", questInfo, 0);
		this.timeLimit2 = MapleDataTool.getInt("timeLimit2", questInfo, 0);
		this.autoStart = MapleDataTool.getInt("autoStart", questInfo, 0) == 1;
		this.autoPreComplete = MapleDataTool.getInt("autoPreComplete",
				questInfo, 0) == 1;
	}

	public static MapleQuest getInstance(int id) {
		MapleQuest ret = quests.get(id);
		if (ret == null) {
			ret = new MapleQuest(id);
			quests.put(id, ret);
		}
		return ret;
	}

	private boolean canStart(MapleCharacter c, int npcid) {
		if ((c.getQuest(this).getStatus() != Status.NOT_STARTED)
				&& !((c.getQuest(this).getStatus() == Status.COMPLETED) && this.repeatable)) {
			return false;
		}
		for (final MapleQuestRequirement r : this.startReqs) {
			if (!r.check(c, npcid)) {
				return false;
			}
		}
		return true;
	}

	public boolean canComplete(MapleCharacter c, Integer npcid) {
		if (!c.getQuest(this).getStatus().equals(Status.STARTED)) {
			return false;
		}
		for (final MapleQuestRequirement r : this.completeReqs) {
			if (!r.check(c, npcid)) {
				return false;
			}
		}
		return true;
	}

	public void start(MapleCharacter c, int npc) {
		if ((this.autoStart || this.checkNPCOnMap(c, npc))
				&& this.canStart(c, npc)) {
			for (final MapleQuestAction a : this.startActs) {
				a.run(c, null);
			}
			this.forceStart(c, npc);
		}
	}

	public void complete(MapleCharacter c, int npc) {
		this.complete(c, npc, null);
	}

	public void complete(MapleCharacter c, int npc, Integer selection) {
		if ((this.autoPreComplete || this.checkNPCOnMap(c, npc))
				&& this.canComplete(c, npc)) {
			/*
			 * for (MapleQuestAction a : completeActs) { if (!a.check(c)) {
			 * return; } }
			 */
			this.forceComplete(c, npc);
			for (final MapleQuestAction a : this.completeActs) {
				a.run(c, selection);
			}
		}
	}

	public void reset(MapleCharacter c) {
		c.updateQuest(new MapleQuestStatus(this,
				MapleQuestStatus.Status.NOT_STARTED));
	}

	public void forfeit(MapleCharacter c) {
		if (!c.getQuest(this).getStatus().equals(Status.STARTED)) {
			return;
		}
		if (this.timeLimit > 0) {
			c.announce(MaplePacketCreator.removeQuestTimeLimit(this.id));
		}
		final MapleQuestStatus newStatus = new MapleQuestStatus(this,
				MapleQuestStatus.Status.NOT_STARTED);
		newStatus.setForfeited(c.getQuest(this).getForfeited() + 1);
		c.updateQuest(newStatus);
	}

	public boolean forceStart(MapleCharacter c, int npc) {
		if (!this.canStart(c, npc)) {
			return false;
		}

		final MapleQuestStatus newStatus = new MapleQuestStatus(this,
				MapleQuestStatus.Status.STARTED, npc);
		newStatus.setForfeited(c.getQuest(this).getForfeited());

		if (this.timeLimit > 0) {
			c.questTimeLimit(this, 30000);// timeLimit * 1000
		}
		if (this.timeLimit2 > 0) {// =\

		}
		c.updateQuest(newStatus);
		return true;
	}

	public boolean forceComplete(MapleCharacter c, int npc) {
		final MapleQuestStatus newStatus = new MapleQuestStatus(this,
				MapleQuestStatus.Status.COMPLETED, npc);
		newStatus.setForfeited(c.getQuest(this).getForfeited());
		newStatus.setCompletionTime(System.currentTimeMillis());
		c.updateQuest(newStatus);
		return true;
	}

	public short getId() {
		return this.id;
	}

	public List<Integer> getRelevantMobs() {
		return this.relevantMobs;
	}

	private boolean checkNPCOnMap(MapleCharacter player, int npcid) {
		return player.getMap().containsNPC(npcid);
	}

	public int getItemAmountNeeded(int itemid) {
		final MapleData data = requirements.getChildByPath(
				String.valueOf(this.id)).getChildByPath("1");
		if (data != null) {
			for (final MapleData req : data.getChildren()) {
				final MapleQuestRequirementType type = MapleQuestRequirementType
						.getByWZName(req.getName());
				if (!type.equals(MapleQuestRequirementType.ITEM)) {
					continue;
				}

				for (final MapleData d : req.getChildren()) {
					if (MapleDataTool.getInt(d.getChildByPath("id"), 0) == itemid) {
						return MapleDataTool.getInt(d.getChildByPath("count"),
								0);
					}
				}
			}
		}
		return 0;
	}

	public int getMobAmountNeeded(int mid) {
		final MapleData data = requirements.getChildByPath(
				String.valueOf(this.id)).getChildByPath("1");
		if (data != null) {
			for (final MapleData req : data.getChildren()) {
				final MapleQuestRequirementType type = MapleQuestRequirementType
						.getByWZName(req.getName());
				if (!type.equals(MapleQuestRequirementType.MOB)) {
					continue;
				}

				for (final MapleData d : req.getChildren()) {
					if (MapleDataTool.getInt(d.getChildByPath("id"), 0) == mid) {
						return MapleDataTool.getInt(d.getChildByPath("count"),
								0);
					}
				}
			}
		}
		return 0;
	}

	public short getInfoNumber() {
		return this.infoNumber;
	}

	public short getInfoEx() {
		return this.infoex;
	}

	public int getTimeLimit() {
		return this.timeLimit;
	}
}
