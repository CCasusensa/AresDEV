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
package server.maps;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.Pair;

/**
 * @author Lerk
 */
public class MapleReactorStats {
	private Point tl;
	private Point br;
	private final Map<Byte, List<StateData>> stateInfo = new HashMap<Byte, List<StateData>>();

	public void setTL(Point tl) {
		this.tl = tl;
	}

	public void setBR(Point br) {
		this.br = br;
	}

	public Point getTL() {
		return this.tl;
	}

	public Point getBR() {
		return this.br;
	}

	public void addState(byte state, List<StateData> data) {
		this.stateInfo.put(state, data);
	}

	public byte getStateSize(byte state) {
		return (byte) this.stateInfo.get(state).size();
	}

	public byte getNextState(byte state, byte index) {
		if ((this.stateInfo.get(state) == null)
				|| (this.stateInfo.get(state).size() < (index + 1))) {
			return -1;
		}
		final StateData nextState = this.stateInfo.get(state).get(index);
		if (nextState != null) {
			return nextState.getNextState();
		} else {
			return -1;
		}
	}

	public List<Integer> getActiveSkills(byte state, byte index) {
		final StateData nextState = this.stateInfo.get(state).get(index);
		if (nextState != null) {
			return nextState.getActiveSkills();
		} else {
			return null;
		}
	}

	public int getType(byte state) {
		final List<StateData> list = this.stateInfo.get(state);
		if (list != null) {
			return list.get(0).getType();
		} else {
			return -1;
		}
	}

	public Pair<Integer, Integer> getReactItem(byte state, byte index) {
		final StateData nextState = this.stateInfo.get(state).get(index);
		if (nextState != null) {
			return nextState.getReactItem();
		} else {
			return null;
		}
	}

	public static class StateData {
		private final int type;
		private final Pair<Integer, Integer> reactItem;
		private final List<Integer> activeSkills;
		private final byte nextState;

		public StateData(int type, Pair<Integer, Integer> reactItem,
				List<Integer> activeSkills, byte nextState) {
			this.type = type;
			this.reactItem = reactItem;
			this.activeSkills = activeSkills;
			this.nextState = nextState;
		}

		private int getType() {
			return this.type;
		}

		private byte getNextState() {
			return this.nextState;
		}

		private Pair<Integer, Integer> getReactItem() {
			return this.reactItem;
		}

		private List<Integer> getActiveSkills() {
			return this.activeSkills;
		}
	}
}
