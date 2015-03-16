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
package net.server.channel.handlers;

import net.AbstractMaplePacketHandler;
import scripting.item.ItemScriptManager;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.scriptedItem;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;
import client.inventory.Item;

/**
 *
 * @author Jay Estrella
 */
public final class ScriptedItemHandler extends AbstractMaplePacketHandler {
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		slea.readInt(); // trash stamp (thx rmzero)
		final byte itemSlot = (byte) slea.readShort(); // item sl0t (thx rmzero)
		final int itemId = slea.readInt(); // itemId
		final scriptedItem info = ii.getScriptedItemInfo(itemId);
		if (info == null) {
			return;
		}
		final ItemScriptManager ism = ItemScriptManager.getInstance();
		final Item item = c.getPlayer()
				.getInventory(ii.getInventoryType(itemId)).getItem(itemSlot);
		if ((item == null) || (item.getItemId() != itemId)
				|| (item.getQuantity() < 1)
				|| !ism.scriptExists(info.getScript())) {
			return;
		}
		ism.getItemScript(c, info.getScript());
		// NPCScriptManager.getInstance().start(c, info.getNpc(), null, null);
	}
}
