/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.maps;

import java.awt.Point;
import java.util.concurrent.locks.ReentrantLock;

import tools.MaplePacketCreator;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;

public class MapleMapItem extends AbstractMapleMapObject {

	protected Item item;
	protected MapleMapObject dropper;
	protected int character_ownerid, meso, questid = -1;
	protected byte type;
	protected boolean pickedUp = false, playerDrop;
	public ReentrantLock itemLock = new ReentrantLock();

	public MapleMapItem(Item item, Point position, MapleMapObject dropper,
			MapleCharacter owner, byte type, boolean playerDrop) {
		this.setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public MapleMapItem(Item item, Point position, MapleMapObject dropper,
			MapleCharacter owner, byte type, boolean playerDrop, int questid) {
		this.setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getParty() == null ? owner.getId()
				: owner.getPartyId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
		this.questid = questid;
	}

	public MapleMapItem(int meso, Point position, MapleMapObject dropper,
			MapleCharacter owner, byte type, boolean playerDrop) {
		this.setPosition(position);
		this.item = null;
		this.dropper = dropper;
		this.character_ownerid = owner.getParty() == null ? owner.getId()
				: owner.getPartyId();
		this.meso = meso;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public final Item getItem() {
		return this.item;
	}

	public final int getQuest() {
		return this.questid;
	}

	public final int getItemId() {
		if (this.getMeso() > 0) {
			return this.meso;
		}
		return this.item.getItemId();
	}

	public final MapleMapObject getDropper() {
		return this.dropper;
	}

	public final int getOwner() {
		return this.character_ownerid;
	}

	public final int getMeso() {
		return this.meso;
	}

	public final boolean isPlayerDrop() {
		return this.playerDrop;
	}

	public final boolean isPickedUp() {
		return this.pickedUp;
	}

	public void setPickedUp(final boolean pickedUp) {
		this.pickedUp = pickedUp;
	}

	public byte getDropType() {
		return this.type;
	}

	@Override
	public final MapleMapObjectType getType() {
		return MapleMapObjectType.ITEM;
	}

	@Override
	public void sendSpawnData(final MapleClient client) {
		if ((this.questid <= 0)
				|| ((client.getPlayer().getQuestStatus(this.questid) == 1) && client
						.getPlayer().needQuestItem(this.questid,
								this.item.getItemId()))) {
			client.announce(MaplePacketCreator.dropItemFromMapObject(this,
					null, this.getPosition(), (byte) 2));
		}
	}

	@Override
	public void sendDestroyData(final MapleClient client) {
		client.announce(MaplePacketCreator.removeItemFromMap(
				this.getObjectId(), 1, 0));
	}
}