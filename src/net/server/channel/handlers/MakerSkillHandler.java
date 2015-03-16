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
import server.MakerItemFactory;
import server.MakerItemFactory.MakerItemCreateEntry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;

/**
 *
 * @author Jay Estrella
 */
public final class MakerSkillHandler extends AbstractMaplePacketHandler {
	private final MapleItemInformationProvider ii = MapleItemInformationProvider
			.getInstance();

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readInt();
		final int toCreate = slea.readInt();
		final MakerItemCreateEntry recipe = MakerItemFactory
				.getItemCreateEntry(toCreate);
		if (this.canCreate(c, recipe)
				&& !c.getPlayer()
						.getInventory(this.ii.getInventoryType(toCreate))
						.isFull()) {
			for (final Pair<Integer, Integer> p : recipe.getReqItems()) {
				final int toRemove = p.getLeft();
				MapleInventoryManipulator.removeById(c,
						this.ii.getInventoryType(toRemove), toRemove,
						p.getRight(), false, false);
			}
			MapleInventoryManipulator.addById(c, toCreate,
					(short) recipe.getRewardAmount());
		}
	}

	private boolean canCreate(MapleClient c, MakerItemCreateEntry recipe) {
		return this.hasItems(c, recipe)
				&& (c.getPlayer().getMeso() >= recipe.getCost())
				&& (c.getPlayer().getLevel() >= recipe.getReqLevel())
				&& (c.getPlayer()
						.getSkillLevel(
								((c.getPlayer().getJob().getId() / 1000) * 1000) + 1007) >= recipe
						.getReqSkillLevel());
	}

	private boolean hasItems(MapleClient c, MakerItemCreateEntry recipe) {
		for (final Pair<Integer, Integer> p : recipe.getReqItems()) {
			final int itemId = p.getLeft();
			if (c.getPlayer().getInventory(this.ii.getInventoryType(itemId))
					.countById(itemId) < p.getRight()) {
				return false;
			}
		}
		return true;
	}
}
