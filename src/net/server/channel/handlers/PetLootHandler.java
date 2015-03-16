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
import net.server.world.MaplePartyCharacter;
import scripting.item.ItemScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.scriptedItem;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;

/**
 * @author TheRamon
 */
public final class PetLootHandler extends AbstractMaplePacketHandler {
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		final MapleCharacter chr = c.getPlayer();
		final MaplePet pet = chr.getPet(chr.getPetIndex(slea.readInt()));// why
																			// would
		// it be an
		// int...?
		if (!pet.isSummoned()) {
			return;
		}

		slea.skip(13);
		final int oid = slea.readInt();
		final MapleMapObject ob = chr.getMap().getMapObject(oid);
		if ((ob == null) || (pet == null)) {
			c.announce(MaplePacketCreator.getInventoryFull());
			return;
		}
		if (ob instanceof MapleMapItem) {
			final MapleMapItem mapitem = (MapleMapItem) ob;
			synchronized (mapitem) {
				if (!chr.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
					c.announce(MaplePacketCreator.showItemUnavailable());
					c.announce(MaplePacketCreator.enableActions());
					return;
				}
				if (mapitem.isPickedUp()) {
					c.announce(MaplePacketCreator.getInventoryFull());
					return;
				}
				if (mapitem.getDropper() == c.getPlayer()) {
					return;
				}
				if (mapitem.getMeso() > 0) {
					if (chr.getParty() != null) {
						final int mesosamm = mapitem.getMeso();
						if (mesosamm > (50000 * chr.getMesoRate())) {
							return;
						}
						int partynum = 0;
						for (final MaplePartyCharacter partymem : chr
								.getParty().getMembers()) {
							if (partymem.isOnline()
									&& (partymem.getMapId() == chr.getMap()
											.getId())
									&& (partymem.getChannel() == c.getChannel())) {
								partynum++;
							}
						}
						for (final MaplePartyCharacter partymem : chr
								.getParty().getMembers()) {
							if (partymem.isOnline()
									&& (partymem.getMapId() == chr.getMap()
											.getId())) {
								final MapleCharacter somecharacter = c
										.getChannelServer().getPlayerStorage()
										.getCharacterById(partymem.getId());
								if (somecharacter != null) {
									somecharacter.gainMeso(mesosamm / partynum,
											true, true, false);
								}
							}
						}
						chr.getMap().broadcastMessage(
								MaplePacketCreator.removeItemFromMap(
										mapitem.getObjectId(), 5, chr.getId(),
										true, chr.getPetIndex(pet)),
								mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else if (chr.getInventory(MapleInventoryType.EQUIPPED)
							.findById(1812000) != null) {
						chr.gainMeso(mapitem.getMeso(), true, true, false);
						chr.getMap().broadcastMessage(
								MaplePacketCreator.removeItemFromMap(
										mapitem.getObjectId(), 5, chr.getId(),
										true, chr.getPetIndex(pet)),
								mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else {
						mapitem.setPickedUp(false);
						c.announce(MaplePacketCreator.enableActions());
						return;
					}
				} else if (ItemPickupHandler.useItem(c, mapitem.getItem()
						.getItemId())) {
					if ((mapitem.getItem().getItemId() / 10000) == 238) {
						chr.getMonsterBook().addCard(c,
								mapitem.getItem().getItemId());
					}
					mapitem.setPickedUp(true);
					chr.getMap().broadcastMessage(
							MaplePacketCreator.removeItemFromMap(
									mapitem.getObjectId(), 5, chr.getId(),
									true, chr.getPetIndex(pet)),
							mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				} else if ((mapitem.getItem().getItemId() / 100) == 50000) {
					if (chr.getInventory(MapleInventoryType.EQUIPPED).findById(
							1812007) != null) {
						for (final int i : chr.getExcluded()) {
							if (mapitem.getItem().getItemId() == i) {
								return;
							}
						}
					} else if (MapleInventoryManipulator.addById(c, mapitem
							.getItem().getItemId(), mapitem.getItem()
							.getQuantity(), null, -1, mapitem.getItem()
							.getExpiration())) {
						chr.getMap().broadcastMessage(
								MaplePacketCreator.removeItemFromMap(
										mapitem.getObjectId(), 5, chr.getId(),
										true, chr.getPetIndex(pet)),
								mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else {
						return;
					}
				} else if ((mapitem.getItem().getItemId() / 10000) == 243) {
					final MapleItemInformationProvider ii = MapleItemInformationProvider
							.getInstance();
					final scriptedItem info = ii.getScriptedItemInfo(mapitem
							.getItem().getItemId());
					if (info.runOnPickup()) {
						final ItemScriptManager ism = ItemScriptManager
								.getInstance();
						final String scriptName = info.getScript();
						if (ism.scriptExists(scriptName)) {
							ism.getItemScript(c, scriptName);
						}

					} else {
						MapleInventoryManipulator.addFromDrop(c,
								mapitem.getItem(), true);
					}
					chr.getMap().broadcastMessage(
							MaplePacketCreator.removeItemFromMap(
									mapitem.getObjectId(), 5, chr.getId(),
									true, chr.getPetIndex(pet)),
							mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				} else if (MapleInventoryManipulator.addFromDrop(c,
						mapitem.getItem(), true)) {
					chr.getMap().broadcastMessage(
							MaplePacketCreator.removeItemFromMap(
									mapitem.getObjectId(), 5, chr.getId(),
									true, chr.getPetIndex(pet)),
							mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				} else {
					return;
				}
				mapitem.setPickedUp(true);
			}
		}
		// c.announce(MaplePacketCreator.enableActions());
	}
}
