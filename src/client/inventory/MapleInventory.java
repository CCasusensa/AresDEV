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
package client.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import constants.ItemConstants;

/**
 *
 * @author Matze
 */
public class MapleInventory implements Iterable<Item> {
	private Map<Byte, Item> inventory = new LinkedHashMap<>();
	private byte slotLimit;
	private final MapleInventoryType type;
	private boolean checked = false;

	public MapleInventory(MapleInventoryType type, byte slotLimit) {
		this.inventory = new LinkedHashMap<>();
		this.type = type;
		this.slotLimit = slotLimit;
	}

	public boolean isExtendableInventory() { // not sure about cash, basing this
												// on the previous one.
		return !(this.type.equals(MapleInventoryType.UNDEFINED)
				|| this.type.equals(MapleInventoryType.EQUIPPED) || this.type
					.equals(MapleInventoryType.CASH));
	}

	public boolean isEquipInventory() {
		return this.type.equals(MapleInventoryType.EQUIP)
				|| this.type.equals(MapleInventoryType.EQUIPPED);
	}

	public byte getSlotLimit() {
		return this.slotLimit;
	}

	public void setSlotLimit(int newLimit) {
		this.slotLimit = (byte) newLimit;
	}

	public Item findById(int itemId) {
		for (final Item item : this.inventory.values()) {
			if (item.getItemId() == itemId) {
				return item;
			}
		}
		return null;
	}

	public int countById(int itemId) {
		int possesed = 0;
		for (final Item item : this.inventory.values()) {
			if (item.getItemId() == itemId) {
				possesed += item.getQuantity();
			}
		}
		return possesed;
	}

	public List<Item> listById(int itemId) {
		final List<Item> ret = new ArrayList<>();
		for (final Item item : this.inventory.values()) {
			if (item.getItemId() == itemId) {
				ret.add(item);
			}
		}
		if (ret.size() > 1) {
			Collections.sort(ret);
		}
		return ret;
	}

	public Collection<Item> list() {
		return this.inventory.values();
	}

	public byte addItem(Item item) {
		final byte slotId = this.getNextFreeSlot();
		if ((slotId < 0) || (item == null)) {
			return -1;
		}
		this.inventory.put(slotId, item);
		item.setPosition(slotId);
		return slotId;
	}

	public void addFromDB(Item item) {
		if ((item.getPosition() < 0)
				&& !this.type.equals(MapleInventoryType.EQUIPPED)) {
			return;
		}
		this.inventory.put(item.getPosition(), item);
	}

	public void move(byte sSlot, byte dSlot, short slotMax) {
		final Item source = this.inventory.get(sSlot);
		final Item target = this.inventory.get(dSlot);
		if (source == null) {
			return;
		}
		if (target == null) {
			source.setPosition(dSlot);
			this.inventory.put(dSlot, source);
			this.inventory.remove(sSlot);
		} else if ((target.getItemId() == source.getItemId())
				&& !ItemConstants.isRechargable(source.getItemId())) {
			if (this.type.getType() == MapleInventoryType.EQUIP.getType()) {
				this.swap(target, source);
			}
			if ((source.getQuantity() + target.getQuantity()) > slotMax) {
				final short rest = (short) ((source.getQuantity() + target
						.getQuantity()) - slotMax);
				source.setQuantity(rest);
				target.setQuantity(slotMax);
			} else {
				target.setQuantity((short) (source.getQuantity() + target
						.getQuantity()));
				this.inventory.remove(sSlot);
			}
		} else {
			this.swap(target, source);
		}
	}

	private void swap(Item source, Item target) {
		this.inventory.remove(source.getPosition());
		this.inventory.remove(target.getPosition());
		final byte swapPos = source.getPosition();
		source.setPosition(target.getPosition());
		target.setPosition(swapPos);
		this.inventory.put(source.getPosition(), source);
		this.inventory.put(target.getPosition(), target);
	}

	public Item getItem(byte slot) {
		return this.inventory.get(slot);
	}

	public void removeItem(byte slot) {
		this.removeItem(slot, (short) 1, false);
	}

	public void removeItem(byte slot, short quantity, boolean allowZero) {
		final Item item = this.inventory.get(slot);
		if (item == null) {// TODO is it ok not to throw an exception here?
			return;
		}
		item.setQuantity((short) (item.getQuantity() - quantity));
		if (item.getQuantity() < 0) {
			item.setQuantity((short) 0);
		}
		if ((item.getQuantity() == 0) && !allowZero) {
			this.removeSlot(slot);
		}
	}

	public void removeSlot(byte slot) {
		this.inventory.remove(slot);
	}

	public boolean isFull() {
		return this.inventory.size() >= this.slotLimit;
	}

	public boolean isFull(int margin) {
		return (this.inventory.size() + margin) >= this.slotLimit;
	}

	public byte getNextFreeSlot() {
		if (this.isFull()) {
			return -1;
		}
		for (byte i = 1; i <= this.slotLimit; i++) {
			if (!this.inventory.keySet().contains(i)) {
				return i;
			}
		}
		return -1;
	}

	public byte getNumFreeSlot() {
		if (this.isFull()) {
			return 0;
		}
		byte free = 0;
		for (byte i = 1; i <= this.slotLimit; i++) {
			if (!this.inventory.keySet().contains(i)) {
				free++;
			}
		}
		return free;
	}

	public MapleInventoryType getType() {
		return this.type;
	}

	@Override
	public Iterator<Item> iterator() {
		return Collections.unmodifiableCollection(this.inventory.values())
				.iterator();
	}

	public Collection<MapleInventory> allInventories() {
		return Collections.singletonList(this);
	}

	public Item findByCashId(int cashId) {
		boolean isRing = false;
		Equip equip = null;
		for (final Item item : this.inventory.values()) {
			if (item.getType() == 1) {
				equip = (Equip) item;
				isRing = equip.getRingId() > -1;
			}
			if ((item.getPetId() > -1 ? item.getPetId() : isRing ? equip
					.getRingId() : item.getCashId()) == cashId) {
				return item;
			}
		}

		return null;
	}

	public boolean checked() {
		return this.checked;
	}

	public void checked(boolean yes) {
		this.checked = yes;
	}
}