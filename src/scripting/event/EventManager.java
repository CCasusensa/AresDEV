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
package scripting.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptException;

import net.server.channel.Channel;
import net.server.world.MapleParty;
import server.TimerManager;
import server.maps.MapleMap;

/**
 *
 * @author Matze
 */
public class EventManager {
	private final Invocable iv;
	private final Channel cserv;
	private final Map<String, EventInstanceManager> instances = new HashMap<String, EventInstanceManager>();
	private final Properties props = new Properties();
	private final String name;
	private ScheduledFuture<?> schedule = null;

	public EventManager(Channel cserv, Invocable iv, String name) {
		this.iv = iv;
		this.cserv = cserv;
		this.name = name;
	}

	public void cancel() {
		try {
			this.iv.invokeFunction("cancelSchedule", (Object) null);
		} catch (final ScriptException ex) {
			ex.printStackTrace();
		} catch (final NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}

	public void schedule(String methodName, long delay) {
		this.schedule(methodName, null, delay);
	}

	public void schedule(final String methodName,
			final EventInstanceManager eim, long delay) {
		this.schedule = TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				try {
					EventManager.this.iv.invokeFunction(methodName, eim);
				} catch (final ScriptException ex) {
					Logger.getLogger(EventManager.class.getName()).log(
							Level.SEVERE, null, ex);
				} catch (final NoSuchMethodException ex) {
					Logger.getLogger(EventManager.class.getName()).log(
							Level.SEVERE, null, ex);
				}
			}
		}, delay);
	}

	public void cancelSchedule() {
		this.schedule.cancel(true);
	}

	public ScheduledFuture<?> scheduleAtTimestamp(final String methodName,
			long timestamp) {
		return TimerManager.getInstance().scheduleAtTimestamp(new Runnable() {
			@Override
			public void run() {
				try {
					EventManager.this.iv.invokeFunction(methodName,
							(Object) null);
				} catch (final ScriptException ex) {
					Logger.getLogger(EventManager.class.getName()).log(
							Level.SEVERE, null, ex);
				} catch (final NoSuchMethodException ex) {
					Logger.getLogger(EventManager.class.getName()).log(
							Level.SEVERE, null, ex);
				}
			}
		}, timestamp);
	}

	public Channel getChannelServer() {
		return this.cserv;
	}

	public EventInstanceManager getInstance(String name) {
		return this.instances.get(name);
	}

	public Collection<EventInstanceManager> getInstances() {
		return Collections.unmodifiableCollection(this.instances.values());
	}

	public EventInstanceManager newInstance(String name) {
		final EventInstanceManager ret = new EventInstanceManager(this, name);
		this.instances.put(name, ret);
		return ret;
	}

	public void disposeInstance(String name) {
		this.instances.remove(name);
	}

	public Invocable getIv() {
		return this.iv;
	}

	public void setProperty(String key, String value) {
		this.props.setProperty(key, value);
	}

	public String getProperty(String key) {
		return this.props.getProperty(key);
	}

	public String getName() {
		return this.name;
	}

	// PQ method: starts a PQ
	public void startInstance(MapleParty party, MapleMap map) {
		try {
			final EventInstanceManager eim = (EventInstanceManager) (this.iv
					.invokeFunction("setup", (Object) null));
			eim.registerParty(party, map);
		} catch (final ScriptException ex) {
			Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final NoSuchMethodException ex) {
			Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	// non-PQ method for starting instance
	public void startInstance(EventInstanceManager eim, String leader) {
		try {
			this.iv.invokeFunction("setup", eim);
			eim.setProperty("leader", leader);
		} catch (final ScriptException ex) {
			Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final NoSuchMethodException ex) {
			Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}
}
