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
package scripting;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.server.Server;
import net.server.guild.MapleGuild;
import net.server.world.MapleParty;
import scripting.event.EventManager;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.partyquest.Pyramid;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.ModifyInventory;
import constants.ItemConstants;

public class AbstractPlayerInteraction {

	public MapleClient c;

	public AbstractPlayerInteraction(MapleClient c) {
		this.c = c;
	}

	public MapleClient getClient() {
		return this.c;
	}

	public MapleCharacter getPlayer() {
		return this.c.getPlayer();
	}

	public void warp(int map) {
		this.getPlayer().changeMap(this.getWarpMap(map),
				this.getWarpMap(map).getPortal(0));
	}

	public void warp(int map, int portal) {
		this.getPlayer().changeMap(this.getWarpMap(map),
				this.getWarpMap(map).getPortal(portal));
	}

	public void warp(int map, String portal) {
		this.getPlayer().changeMap(this.getWarpMap(map),
				this.getWarpMap(map).getPortal(portal));
	}

	public void warpMap(int map) {
		for (final MapleCharacter mc : this.getPlayer().getMap()
				.getCharacters()) {
			mc.changeMap(this.getWarpMap(map), this.getWarpMap(map)
					.getPortal(0));
		}
	}

	protected MapleMap getWarpMap(int map) {
		MapleMap target;
		if (this.getPlayer().getEventInstance() == null) {
			target = this.c.getChannelServer().getMapFactory().getMap(map);
		} else {
			target = this.getPlayer().getEventInstance().getMapInstance(map);
		}
		return target;
	}

	public MapleMap getMap(int map) {
		return this.getWarpMap(map);
	}

	public EventManager getEventManager(String event) {
		return this.getClient().getChannelServer().getEventSM()
				.getEventManager(event);
	}

	public boolean haveItem(int itemid) {
		return this.haveItem(itemid, 1);
	}

	public boolean haveItem(int itemid, int quantity) {
		return this.getPlayer().getItemQuantity(itemid, false) >= quantity;
	}

	public boolean canHold(int itemid) {
		return this
				.getPlayer()
				.getInventory(
						MapleItemInformationProvider.getInstance()
								.getInventoryType(itemid)).getNextFreeSlot() > -1;
	}

	public void openNpc(int npcid) {
		NPCScriptManager.getInstance().dispose(this.c);
		NPCScriptManager.getInstance().start(this.c, npcid, null, null);
	}

	public void updateQuest(int questid, String data) {
		final MapleQuestStatus status = this.c.getPlayer().getQuest(
				MapleQuest.getInstance(questid));
		status.setStatus(MapleQuestStatus.Status.STARTED);
		status.setProgress(0, data);// override old if exists
		this.c.getPlayer().updateQuest(status);
	}

	public MapleQuestStatus.Status getQuestStatus(int id) {
		return this.c.getPlayer().getQuest(MapleQuest.getInstance(id))
				.getStatus();
	}

	public boolean isQuestCompleted(int quest) {
		try {
			return this.getQuestStatus(quest) == MapleQuestStatus.Status.COMPLETED;
		} catch (final NullPointerException e) {
			return false;
		}
	}

	public boolean isQuestStarted(int quest) {
		try {
			return this.getQuestStatus(quest) == MapleQuestStatus.Status.STARTED;
		} catch (final NullPointerException e) {
			return false;
		}
	}

	public int getQuestProgress(int qid) {
		return Integer.parseInt(this.getPlayer()
				.getQuest(MapleQuest.getInstance(29932)).getProgress().get(0));
	}

	public void gainItem(int id, short quantity) {
		this.gainItem(id, quantity, false, false);
	}

	public void gainItem(int id, short quantity, boolean show) {// this will fk
																// randomStats
																// equip :P
		this.gainItem(id, quantity, false, show);
	}

	public void gainItem(int id, boolean show) {
		this.gainItem(id, (short) 1, false, show);
	}

	public void gainItem(int id) {
		this.gainItem(id, (short) 1, false, false);
	}

	public void gainItem(int id, short quantity, boolean randomStats,
			boolean showMessage) {
		if ((id >= 5000000) && (id <= 5000100)) {
			MapleInventoryManipulator.addById(this.c, id, (short) 1, null,
					MaplePet.createPet(id), -1);
		}
		if (quantity >= 0) {
			final MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			final Item item = ii.getEquipById(id);
			if (!MapleInventoryManipulator.checkSpace(this.c, id, quantity, "")) {
				this.c.getPlayer().dropMessage(
						1,
						"Your inventory is full. Please remove an item from your "
								+ ii.getInventoryType(id).name()
								+ " inventory.");
				return;
			}
			if (ii.getInventoryType(id).equals(MapleInventoryType.EQUIP)
					&& !ItemConstants.isRechargable(item.getItemId())) {
				if (randomStats) {
					MapleInventoryManipulator.addFromDrop(this.c,
							ii.randomizeStats((Equip) item), false);
				} else {
					MapleInventoryManipulator.addFromDrop(this.c, item, false);
				}
			} else {
				MapleInventoryManipulator.addById(this.c, id, quantity);
			}
		} else {
			MapleInventoryManipulator.removeById(this.c,
					MapleItemInformationProvider.getInstance()
							.getInventoryType(id), id, -quantity, true, false);
		}
		if (showMessage) {
			this.c.announce(MaplePacketCreator.getShowItemGain(id, quantity,
					true));
		}
	}

	public void changeMusic(String songName) {
		this.getPlayer().getMap()
				.broadcastMessage(MaplePacketCreator.musicChange(songName));
	}

	public void playerMessage(int type, String message) {
		this.c.announce(MaplePacketCreator.serverNotice(type, message));
	}

	public void message(String message) {
		this.getPlayer().addMessage(message);
	}

	public void mapMessage(int type, String message) {
		this.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.serverNotice(type, message));
	}

	public void mapEffect(String path) {
		this.c.announce(MaplePacketCreator.mapEffect(path));
	}

	public void mapSound(String path) {
		this.c.announce(MaplePacketCreator.mapSound(path));
	}

	public void showIntro(String path) {
		this.c.announce(MaplePacketCreator.showIntro(path));
	}

	public void showInfo(String path) {
		this.c.announce(MaplePacketCreator.showInfo(path));
		this.c.announce(MaplePacketCreator.enableActions());
	}

	public void guildMessage(int type, String message) {
		if (this.getGuild() != null) {
			this.getGuild().guildMessage(
					MaplePacketCreator.serverNotice(type, message));
		}
	}

	public MapleGuild getGuild() {
		try {
			return Server.getInstance().getGuild(this.getPlayer().getGuildId(),
					null);
		} catch (final Exception e) {
		}
		return null;
	}

	public MapleParty getParty() {
		return this.getPlayer().getParty();
	}

	public boolean isLeader() {
		return this.getParty().getLeader().equals(this.getPlayer().getMPC());
	}

	public void givePartyItems(int id, short quantity,
			List<MapleCharacter> party) {
		for (final MapleCharacter chr : party) {
			final MapleClient cl = chr.getClient();
			if (quantity >= 0) {
				MapleInventoryManipulator.addById(cl, id, quantity);
			} else {
				MapleInventoryManipulator.removeById(cl,
						MapleItemInformationProvider.getInstance()
								.getInventoryType(id), id, -quantity, true,
						false);
			}
			cl.announce(MaplePacketCreator.getShowItemGain(id, quantity, true));
		}
	}

	public void givePartyExp(int amount, List<MapleCharacter> party) {
		for (final MapleCharacter chr : party) {
			chr.gainExp((amount * chr.getExpRate()), true, true);
		}
	}

	public void removeFromParty(int id, List<MapleCharacter> party) {
		for (final MapleCharacter chr : party) {
			final MapleClient cl = chr.getClient();
			final MapleInventoryType type = MapleItemInformationProvider
					.getInstance().getInventoryType(id);
			final MapleInventory iv = cl.getPlayer().getInventory(type);
			final int possesed = iv.countById(id);
			if (possesed > 0) {
				MapleInventoryManipulator.removeById(this.c,
						MapleItemInformationProvider.getInstance()
								.getInventoryType(id), id, possesed, true,
						false);
				cl.announce(MaplePacketCreator.getShowItemGain(id,
						(short) -possesed, true));
			}
		}
	}

	public void removeAll(int id) {
		this.removeAll(id, this.c);
	}

	public void removeAll(int id, MapleClient cl) {
		final int possessed = cl
				.getPlayer()
				.getInventory(
						MapleItemInformationProvider.getInstance()
								.getInventoryType(id)).countById(id);
		if (possessed > 0) {
			MapleInventoryManipulator.removeById(cl,
					MapleItemInformationProvider.getInstance()
							.getInventoryType(id), id, possessed, true, false);
			cl.announce(MaplePacketCreator.getShowItemGain(id,
					(short) -possessed, true));
		}
	}

	public int getMapId() {
		return this.c.getPlayer().getMap().getId();
	}

	public int getPlayerCount(int mapid) {
		return this.c.getChannelServer().getMapFactory().getMap(mapid)
				.getCharacters().size();
	}

	public void showInstruction(String msg, int width, int height) {
		this.c.announce(MaplePacketCreator.sendHint(msg, width, height));
		this.c.announce(MaplePacketCreator.enableActions());
	}

	public void disableMinimap() {
		this.c.announce(MaplePacketCreator.disableMinimap());
	}

	public void resetMap(int mapid) {
		this.getMap(mapid).resetReactors();
		this.getMap(mapid).killAllMonsters();
		for (final MapleMapObject i : this.getMap(mapid).getMapObjectsInRange(
				this.c.getPlayer().getPosition(), Double.POSITIVE_INFINITY,
				Arrays.asList(MapleMapObjectType.ITEM))) {
			this.getMap(mapid).removeMapObject(i);
			this.getMap(mapid).broadcastMessage(
					MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0,
							this.c.getPlayer().getId()));
		}
	}

	public void sendClock(MapleClient d, int time) {
		d.announce(MaplePacketCreator.getClock((int) (time - System
				.currentTimeMillis()) / 1000));
	}

	public void useItem(int id) {
		MapleItemInformationProvider.getInstance().getItemEffect(id)
				.applyTo(this.c.getPlayer());
		this.c.announce(MaplePacketCreator.getItemMessage(id));// Useful shet :3
	}

	public void teachSkill(int skillid, byte level, byte masterLevel,
			long expiration) {
		this.getPlayer().changeSkillLevel(SkillFactory.getSkill(skillid),
				level, masterLevel, expiration);
	}

	public void removeEquipFromSlot(byte slot) {
		final Item tempItem = this.c.getPlayer()
				.getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
		MapleInventoryManipulator.removeFromSlot(this.c,
				MapleInventoryType.EQUIPPED, slot, tempItem.getQuantity(),
				false, false);
	}

	public void gainAndEquip(int itemid, byte slot) {
		final Item old = this.c.getPlayer()
				.getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
		if (old != null) {
			MapleInventoryManipulator.removeFromSlot(this.c,
					MapleInventoryType.EQUIPPED, slot, old.getQuantity(),
					false, false);
		}
		final Item newItem = MapleItemInformationProvider.getInstance()
				.getEquipById(itemid);
		newItem.setPosition(slot);
		this.c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)
				.addFromDB(newItem);
		this.c.announce(MaplePacketCreator.modifyInventory(false,
				Collections.singletonList(new ModifyInventory(0, newItem))));
	}

	public void spawnMonster(int id, int x, int y) {
		final MapleMonster monster = MapleLifeFactory.getMonster(id);
		monster.setPosition(new Point(x, y));
		this.getPlayer().getMap().spawnMonster(monster);
	}

	public void spawnGuide() {
		this.c.announce(MaplePacketCreator.spawnGuide(true));
	}

	public void removeGuide() {
		this.c.announce(MaplePacketCreator.spawnGuide(false));
	}

	public void displayGuide(int num) {
		this.c.announce(MaplePacketCreator.showInfo("UI/tutorial.img/" + num));
	}

	public void talkGuide(String message) {
		this.c.announce(MaplePacketCreator.talkGuide(message));
	}

	public void guideHint(int hint) {
		this.c.announce(MaplePacketCreator.guideHint(hint));
	}

	public void updateAreaInfo(Short area, String info) {
		this.c.getPlayer().updateAreaInfo(area, info);
		this.c.announce(MaplePacketCreator.enableActions());// idk, nexon does
															// the
		// same :P
	}

	public boolean containsAreaInfo(short area, String info) {
		return this.c.getPlayer().containsAreaInfo(area, info);
	}

	public MobSkill getMobSkill(int skill, int level) {
		return MobSkillFactory.getMobSkill(skill, level);
	}

	public void earnTitle(String msg) {
		this.c.announce(MaplePacketCreator.earnTitleMessage(msg));
	}

	public void showInfoText(String msg) {
		this.c.announce(MaplePacketCreator.showInfoText(msg));
	}

	public void openUI(byte ui) {
		this.c.announce(MaplePacketCreator.openUI(ui));
	}

	public void lockUI() {
		this.c.announce(MaplePacketCreator.disableUI(true));
		this.c.announce(MaplePacketCreator.lockUI(true));
	}

	public void unlockUI() {
		this.c.announce(MaplePacketCreator.disableUI(false));
		this.c.announce(MaplePacketCreator.lockUI(false));
	}

	public void playSound(String sound) {
		this.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.environmentChange(sound, 4));
	}

	public void environmentChange(String env, int mode) {
		this.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.environmentChange(env, mode));
	}

	public Pyramid getPyramid() {
		return (Pyramid) this.getPlayer().getPartyQuest();
	}
}
