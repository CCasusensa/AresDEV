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

import java.util.ArrayList;
import java.util.Collections;

import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;

/**
 *
 * @author BubblesDev
 */
public final class ItemIdSortHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final MapleCharacter chr = c.getPlayer();
		chr.getAutobanManager().setTimestamp(4, slea.readInt());
		final byte inv = slea.readByte();
		if ((inv < 0) || (inv > 5)) {
			c.disconnect(false, false);
			return;
		}
		final MapleInventory Inv = chr.getInventory(MapleInventoryType
				.getByType(inv));
		final ArrayList<Item> itemarray = new ArrayList<>();
		for (final Item item2 : Inv) {
			final Item item = item2;
			itemarray.add(item.copy());
		}
		Collections.sort(itemarray);
		for (final Item item : itemarray) {
			MapleInventoryManipulator.removeById(c,
					MapleInventoryType.getByType(inv), item.getItemId(),
					item.getQuantity(), false, false);
		}
		for (final Item i : itemarray) {
			MapleInventoryManipulator.addFromDrop(c, i, false);
		}
		c.announce(MaplePacketCreator.finishedSort2(inv));
	}
}
