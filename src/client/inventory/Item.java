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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Item implements Comparable<Item> {

	private final int id;
	private int cashId;
	private int sn;
	private byte position;
	private short quantity;
	private int petid = -1;
	private MaplePet pet = null;
	private String owner = "";
	protected List<String> log;
	private byte flag;
	private long expiration = -1;
	private String giftFrom = "";

	public Item(int id, byte position, short quantity) {
		this.id = id;
		this.position = position;
		this.quantity = quantity;
		this.log = new LinkedList<>();
		this.flag = 0;
	}

	public Item(int id, byte position, short quantity, int petid) {
		this.id = id;
		this.position = position;
		this.quantity = quantity;
		this.petid = petid;
		if (petid > -1) {
			this.pet = MaplePet.loadFromDb(id, position, petid);
		}
		this.flag = 0;
		this.log = new LinkedList<>();
	}

	public Item copy() {
		final Item ret = new Item(this.id, this.position, this.quantity,
				this.petid);
		ret.flag = this.flag;
		ret.owner = this.owner;
		ret.expiration = this.expiration;
		ret.log = new LinkedList<>(this.log);
		return ret;
	}

	public void setPosition(byte position) {
		this.position = position;
	}

	public void setQuantity(short quantity) {
		this.quantity = quantity;
	}

	public int getItemId() {
		return this.id;
	}

	public int getCashId() {
		if (this.cashId == 0) {
			this.cashId = new Random().nextInt(Integer.MAX_VALUE) + 1;
		}
		return this.cashId;
	}

	public byte getPosition() {
		return this.position;
	}

	public short getQuantity() {
		return this.quantity;
	}

	public byte getType() {
		if (this.getPetId() > -1) {
			return 3;
		}
		return 2;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getPetId() {
		return this.petid;
	}

	public void setPetId(int id) {
		this.petid = id;
	}

	@Override
	public int compareTo(Item other) {
		if (this.id < other.getItemId()) {
			return -1;
		} else if (this.id > other.getItemId()) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "Item: " + this.id + " quantity: " + this.quantity;
	}

	public List<String> getLog() {
		return Collections.unmodifiableList(this.log);
	}

	public byte getFlag() {
		return this.flag;
	}

	public void setFlag(byte b) {
		this.flag = b;
	}

	public long getExpiration() {
		return this.expiration;
	}

	public void setExpiration(long expire) {
		this.expiration = expire;
	}

	public int getSN() {
		return this.sn;
	}

	public void setSN(int sn) {
		this.sn = sn;
	}

	public String getGiftFrom() {
		return this.giftFrom;
	}

	public void setGiftFrom(String giftFrom) {
		this.giftFrom = giftFrom;
	}

	public MaplePet getPet() {
		return this.pet;
	}
}
