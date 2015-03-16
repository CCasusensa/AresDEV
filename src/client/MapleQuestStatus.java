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
package client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import server.quest.MapleQuest;
import tools.StringUtil;

/**
 *
 * @author Matze
 */
public class MapleQuestStatus {
	public enum Status {
		UNDEFINED(-1), NOT_STARTED(0), STARTED(1), COMPLETED(2);
		final int status;

		private Status(int id) {
			this.status = id;
		}

		public int getId() {
			return this.status;
		}

		public static Status getById(int id) {
			for (final Status l : Status.values()) {
				if (l.getId() == id) {
					return l;
				}
			}
			return null;
		}
	}

	private final MapleQuest quest;
	private Status status;
	private final Map<Integer, String> progress = new LinkedHashMap<Integer, String>();
	private final List<Integer> medalProgress = new LinkedList<Integer>();
	private int npc;
	private long completionTime;
	private int forfeited = 0;

	public MapleQuestStatus(MapleQuest quest, Status status) {
		this.quest = quest;
		this.setStatus(status);
		this.completionTime = System.currentTimeMillis();
		if (status == Status.STARTED) {
			this.registerMobs();
		}
	}

	public MapleQuestStatus(MapleQuest quest, Status status, int npc) {
		this.quest = quest;
		this.setStatus(status);
		this.setNpc(npc);
		this.completionTime = System.currentTimeMillis();
		if (status == Status.STARTED) {
			this.registerMobs();
		}
	}

	public MapleQuest getQuest() {
		return this.quest;
	}

	public Status getStatus() {
		return this.status;
	}

	public final void setStatus(Status status) {
		this.status = status;
	}

	public int getNpc() {
		return this.npc;
	}

	public final void setNpc(int npc) {
		this.npc = npc;
	}

	private void registerMobs() {
		for (final int i : this.quest.getRelevantMobs()) {
			this.progress.put(i, "000");
		}
	}

	public boolean addMedalMap(int mapid) {
		if (this.medalProgress.contains(mapid)) {
			return false;
		}
		this.medalProgress.add(mapid);
		return true;
	}

	public int getMedalProgress() {
		return this.medalProgress.size();
	}

	public List<Integer> getMedalMaps() {
		return this.medalProgress;
	}

	public boolean progress(int id) {
		if (this.progress.get(id) != null) {
			final int current = Integer.parseInt(this.progress.get(id));
			final String str = StringUtil.getLeftPaddedStr(
					Integer.toString(current + 1), '0', 3);
			this.progress.put(id, str);
			return true;
		}
		return false;
	}

	public void setProgress(int id, String pr) {
		this.progress.put(id, pr);
	}

	public boolean madeProgress() {
		return this.progress.size() > 0;
	}

	public String getProgress(int id) {
		if (this.progress.get(id) == null) {
			return "";
		}
		return this.progress.get(id);
	}

	public Map<Integer, String> getProgress() {
		return Collections.unmodifiableMap(this.progress);
	}

	public long getCompletionTime() {
		return this.completionTime;
	}

	public void setCompletionTime(long completionTime) {
		this.completionTime = completionTime;
	}

	public int getForfeited() {
		return this.forfeited;
	}

	public void setForfeited(int forfeited) {
		if (forfeited >= this.forfeited) {
			this.forfeited = forfeited;
		} else {
			throw new IllegalArgumentException(
					"Can't set forfeits to something lower than before.");
		}
	}

	public String getQuestData() {
		final StringBuilder str = new StringBuilder();
		for (final String ps : this.progress.values()) {
			str.append(ps);
		}
		return str.toString();
	}
}