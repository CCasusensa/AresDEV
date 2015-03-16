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
package server;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Matze
 */
public class MapleShopFactory {
	private final Map<Integer, MapleShop> shops = new HashMap<Integer, MapleShop>();
	private final Map<Integer, MapleShop> npcShops = new HashMap<Integer, MapleShop>();
	private static MapleShopFactory instance = new MapleShopFactory();

	public static MapleShopFactory getInstance() {
		return instance;
	}

	public void reloadShops() {
		this.shops.clear();
	}

	private MapleShop loadShop(int id, boolean isShopId) {
		final MapleShop ret = MapleShop.createFromDB(id, isShopId);
		if (ret != null) {
			this.shops.put(ret.getId(), ret);
			this.npcShops.put(ret.getNpcId(), ret);
		} else if (isShopId) {
			this.shops.put(id, null);
		} else {
			this.npcShops.put(id, null);
		}
		return ret;
	}

	public MapleShop getShop(int shopId) {
		if (this.shops.containsKey(shopId)) {
			return this.shops.get(shopId);
		}
		return this.loadShop(shopId, true);
	}

	public MapleShop getShopForNPC(int npcId) {
		if (this.npcShops.containsKey(npcId)) {
			this.npcShops.get(npcId);
		}
		return this.loadShop(npcId, false);
	}
}
