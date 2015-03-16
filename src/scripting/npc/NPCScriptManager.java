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
package scripting.npc;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptException;

import scripting.AbstractScriptManager;
import tools.FilePrinter;
import client.MapleCharacter;
import client.MapleClient;

/**
 *
 * @author Matze
 */
public class NPCScriptManager extends AbstractScriptManager {

	private final Map<MapleClient, NPCConversationManager> cms = new HashMap<>();
	private final Map<MapleClient, Invocable> scripts = new HashMap<>();
	private static NPCScriptManager instance = new NPCScriptManager();

	public synchronized static NPCScriptManager getInstance() {
		return instance;
	}

	public void start(MapleClient c, int npc, String filename,
			MapleCharacter chr) {
		try {
			final NPCConversationManager cm = new NPCConversationManager(c, npc);
			if (this.cms.containsKey(c)) {
				this.dispose(c);
				return;
			}
			this.cms.put(c, cm);
			Invocable iv = null;
			if (filename != null) {
				iv = this.getInvocable("npc/world" + c.getWorld() + "/"
						+ filename + ".js", c);
			}
			if (iv == null) {
				iv = this.getInvocable("npc/world" + c.getWorld() + "/" + npc
						+ ".js", c);
			}
			if ((iv == null) || (NPCScriptManager.getInstance() == null)) {
				this.dispose(c);
				return;
			}
			this.engine.put("cm", cm);
			this.scripts.put(c, iv);
			try {
				iv.invokeFunction("start");
			} catch (final NoSuchMethodException nsme) {
				try {
					iv.invokeFunction("start", chr);
				} catch (final NoSuchMethodException nsma) {
					iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
				}
			}
		} catch (final UndeclaredThrowableException | ScriptException ute) {
			FilePrinter.printError(FilePrinter.NPC + npc + ".txt", ute);
			this.notice(c, npc);
			this.dispose(c);
		} catch (final Exception e) {
			FilePrinter.printError(FilePrinter.NPC + npc + ".txt", e);
			this.notice(c, npc);
			this.dispose(c);
		}
	}

	public void action(MapleClient c, byte mode, byte type, int selection) {
		final Invocable iv = this.scripts.get(c);
		if (iv != null) {
			try {
				iv.invokeFunction("action", mode, type, selection);
			} catch (ScriptException | NoSuchMethodException t) {
				if (this.getCM(c) != null) {
					FilePrinter.printError(FilePrinter.NPC
							+ this.getCM(c).getNpc() + ".txt", t);
					this.notice(c, this.getCM(c).getNpc());
				}
				this.dispose(c);// lol this should be last, not notice fags
			}
		}
	}

	public void dispose(NPCConversationManager cm) {
		final MapleClient c = cm.getClient();
		this.cms.remove(c);
		this.scripts.remove(c);
		this.resetContext("npc/world" + c.getWorld() + "/" + cm.getNpc()
				+ ".js", c);
	}

	public void dispose(MapleClient c) {
		if (this.cms.get(c) != null) {
			this.dispose(this.cms.get(c));
		}
	}

	public NPCConversationManager getCM(MapleClient c) {
		return this.cms.get(c);
	}

	private void notice(MapleClient c, int id) {
		if (c != null) {
			c.getPlayer()
					.dropMessage(
							1,
							"An unknown error occured while executing this npc. Please report it to one of the admins! ID: "
									+ id);
		}
	}
}
