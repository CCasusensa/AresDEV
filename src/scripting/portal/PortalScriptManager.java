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
package scripting.portal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import server.MaplePortal;
import tools.FilePrinter;
import client.MapleClient;

public class PortalScriptManager {

	private static PortalScriptManager instance = new PortalScriptManager();
	private final Map<String, PortalScript> scripts = new HashMap<>();
	private final ScriptEngineFactory sef;

	private PortalScriptManager() {
		final ScriptEngineManager sem = new ScriptEngineManager();
		this.sef = sem.getEngineByName("javascript").getFactory();
	}

	public static PortalScriptManager getInstance() {
		return instance;
	}

	private PortalScript getPortalScript(String scriptName) {
		if (this.scripts.containsKey(scriptName)) {
			return this.scripts.get(scriptName);
		}
		final File scriptFile = new File("scripts/portal/" + scriptName + ".js");
		if (!scriptFile.exists()) {
			this.scripts.put(scriptName, null);
			return null;
		}
		FileReader fr = null;
		final ScriptEngine portal = this.sef.getScriptEngine();
		try {
			fr = new FileReader(scriptFile);
			((Compilable) portal).compile(fr).eval();
		} catch (ScriptException | IOException | UndeclaredThrowableException e) {
			FilePrinter.printError(FilePrinter.PORTAL + scriptName + ".txt", e);
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (final IOException e) {
					System.out.println("ERROR CLOSING " + e);
				}
			}
		}
		final PortalScript script = ((Invocable) portal)
				.getInterface(PortalScript.class);
		this.scripts.put(scriptName, script);
		return script;
	}

	public boolean executePortalScript(MaplePortal portal, MapleClient c) {
		try {
			final PortalScript script = this.getPortalScript(portal
					.getScriptName());
			if (script != null) {
				return script.enter(new PortalPlayerInteraction(c, portal));
			}
		} catch (final UndeclaredThrowableException ute) {
			FilePrinter.printError(FilePrinter.PORTAL + portal.getScriptName()
					+ ".txt", ute);
		} catch (final Exception e) {
			FilePrinter.printError(FilePrinter.PORTAL + portal.getScriptName()
					+ ".txt", e);
		}
		return false;
	}

	public void reloadPortalScripts() {
		this.scripts.clear();
	}
}