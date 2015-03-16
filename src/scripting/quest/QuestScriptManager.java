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
package scripting.quest;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;

import scripting.AbstractScriptManager;
import server.quest.MapleQuest;
import tools.FilePrinter;
import client.MapleClient;
import client.MapleQuestStatus;

/**
 *
 * @author RMZero213
 */
public class QuestScriptManager extends AbstractScriptManager {
	private final Map<MapleClient, QuestActionManager> qms = new HashMap<>();
	private final Map<MapleClient, Invocable> scripts = new HashMap<>();
	private static QuestScriptManager instance = new QuestScriptManager();

	public synchronized static QuestScriptManager getInstance() {
		return instance;
	}

	public void start(MapleClient c, short questid, int npc) {
		final MapleQuest quest = MapleQuest.getInstance(questid);
		if (!c.getPlayer().getQuest(quest).getStatus()
				.equals(MapleQuestStatus.Status.NOT_STARTED)
				|| !c.getPlayer().getMap().containsNPC(npc)) {
			this.dispose(c);
			return;
		}
		try {
			final QuestActionManager qm = new QuestActionManager(c, questid,
					npc, true);
			if (this.qms.containsKey(c)) {
				return;
			}
			this.qms.put(c, qm);
			final Invocable iv = this.getInvocable("quest/" + questid + ".js",
					c);
			if (iv == null) {
				qm.dispose();
				return;
			}
			this.engine.put("qm", qm);
			this.scripts.put(c, iv);
			iv.invokeFunction("start", (byte) 1, (byte) 0, 0);
		} catch (final UndeclaredThrowableException ute) {
			FilePrinter.printError(FilePrinter.QUEST + questid + ".txt", ute);
			this.dispose(c);
		} catch (final Throwable t) {
			FilePrinter.printError(FilePrinter.QUEST + this.getQM(c).getQuest()
					+ ".txt", t);
			this.dispose(c);
		}
	}

	public void start(MapleClient c, byte mode, byte type, int selection) {
		final Invocable iv = this.scripts.get(c);
		if (iv != null) {
			try {
				iv.invokeFunction("start", mode, type, selection);
			} catch (final UndeclaredThrowableException ute) {
				FilePrinter.printError(FilePrinter.QUEST
						+ this.getQM(c).getQuest() + ".txt", ute);
				this.dispose(c);
			} catch (final Throwable t) {
				FilePrinter.printError(FilePrinter.QUEST
						+ this.getQM(c).getQuest() + ".txt", t);
				this.dispose(c);
			}
		}
	}

	public void end(MapleClient c, short questid, int npc) {
		final MapleQuest quest = MapleQuest.getInstance(questid);
		if (!c.getPlayer().getQuest(quest).getStatus()
				.equals(MapleQuestStatus.Status.STARTED)
				|| !c.getPlayer().getMap().containsNPC(npc)) {
			this.dispose(c);
			return;
		}
		try {
			final QuestActionManager qm = new QuestActionManager(c, questid,
					npc, false);
			if (this.qms.containsKey(c)) {
				return;
			}
			this.qms.put(c, qm);
			final Invocable iv = this.getInvocable("quest/" + questid + ".js",
					c);
			if (iv == null) {
				qm.dispose();
				return;
			}
			this.engine.put("qm", qm);
			this.scripts.put(c, iv);
			iv.invokeFunction("end", (byte) 1, (byte) 0, 0);
		} catch (final UndeclaredThrowableException ute) {
			FilePrinter.printError(FilePrinter.QUEST + questid + ".txt", ute);
			this.dispose(c);
		} catch (final Throwable t) {
			FilePrinter.printError(FilePrinter.QUEST + this.getQM(c).getQuest()
					+ ".txt", t);
			this.dispose(c);
		}
	}

	public void end(MapleClient c, byte mode, byte type, int selection) {
		final Invocable iv = this.scripts.get(c);
		if (iv != null) {
			try {
				iv.invokeFunction("end", mode, type, selection);
			} catch (final UndeclaredThrowableException ute) {
				FilePrinter.printError(FilePrinter.QUEST
						+ this.getQM(c).getQuest() + ".txt", ute);
				this.dispose(c);
			} catch (final Throwable t) {
				FilePrinter.printError(FilePrinter.QUEST
						+ this.getQM(c).getQuest() + ".txt", t);
				this.dispose(c);
			}
		}
	}

	public void dispose(QuestActionManager qm, MapleClient c) {
		this.qms.remove(c);
		this.scripts.remove(c);
		this.resetContext("quest/" + qm.getQuest() + ".js", c);
	}

	public void dispose(MapleClient c) {
		final QuestActionManager qm = this.qms.get(c);
		if (qm != null) {
			this.dispose(qm, c);
		}
	}

	public QuestActionManager getQM(MapleClient c) {
		return this.qms.get(c);
	}
}
