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

import java.util.LinkedList;
import java.util.List;

import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.Pair;
import client.MapleClient;

public class Equip extends Item {

	public static enum ScrollResult {

		FAIL(0), SUCCESS(1), CURSE(2);
		private int value = -1;

		private ScrollResult(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	private byte upgradeSlots;
	private byte level, flag, itemLevel;
	private short str, dex, _int, luk, hp, mp, watk, matk, wdef, mdef, acc,
			avoid, hands, speed, jump, vicious;
	private float itemExp;
	private int ringid = -1;
	private boolean wear = false;

	public Equip(int id, byte position) {
		super(id, position, (short) 1);
		this.itemExp = 0;
		this.itemLevel = 1;
	}

	public Equip(int id, byte position, int slots) {
		super(id, position, (short) 1);
		this.upgradeSlots = (byte) slots;
		this.itemExp = 0;
		this.itemLevel = 1;
	}

	@Override
	public Item copy() {
		final Equip ret = new Equip(this.getItemId(), this.getPosition(),
				this.getUpgradeSlots());
		ret.str = this.str;
		ret.dex = this.dex;
		ret._int = this._int;
		ret.luk = this.luk;
		ret.hp = this.hp;
		ret.mp = this.mp;
		ret.matk = this.matk;
		ret.mdef = this.mdef;
		ret.watk = this.watk;
		ret.wdef = this.wdef;
		ret.acc = this.acc;
		ret.avoid = this.avoid;
		ret.hands = this.hands;
		ret.speed = this.speed;
		ret.jump = this.jump;
		ret.flag = this.flag;
		ret.vicious = this.vicious;
		ret.upgradeSlots = this.upgradeSlots;
		ret.itemLevel = this.itemLevel;
		ret.itemExp = this.itemExp;
		ret.level = this.level;
		ret.log = new LinkedList<>(this.log);
		ret.setOwner(this.getOwner());
		ret.setQuantity(this.getQuantity());
		ret.setExpiration(this.getExpiration());
		ret.setGiftFrom(this.getGiftFrom());
		return ret;
	}

	@Override
	public byte getFlag() {
		return this.flag;
	}

	@Override
	public byte getType() {
		return 1;
	}

	public byte getUpgradeSlots() {
		return this.upgradeSlots;
	}

	public short getStr() {
		return this.str;
	}

	public short getDex() {
		return this.dex;
	}

	public short getInt() {
		return this._int;
	}

	public short getLuk() {
		return this.luk;
	}

	public short getHp() {
		return this.hp;
	}

	public short getMp() {
		return this.mp;
	}

	public short getWatk() {
		return this.watk;
	}

	public short getMatk() {
		return this.matk;
	}

	public short getWdef() {
		return this.wdef;
	}

	public short getMdef() {
		return this.mdef;
	}

	public short getAcc() {
		return this.acc;
	}

	public short getAvoid() {
		return this.avoid;
	}

	public short getHands() {
		return this.hands;
	}

	public short getSpeed() {
		return this.speed;
	}

	public short getJump() {
		return this.jump;
	}

	public short getVicious() {
		return this.vicious;
	}

	@Override
	public void setFlag(byte flag) {
		this.flag = flag;
	}

	public void setStr(short str) {
		this.str = str;
	}

	public void setDex(short dex) {
		this.dex = dex;
	}

	public void setInt(short _int) {
		this._int = _int;
	}

	public void setLuk(short luk) {
		this.luk = luk;
	}

	public void setHp(short hp) {
		this.hp = hp;
	}

	public void setMp(short mp) {
		this.mp = mp;
	}

	public void setWatk(short watk) {
		this.watk = watk;
	}

	public void setMatk(short matk) {
		this.matk = matk;
	}

	public void setWdef(short wdef) {
		this.wdef = wdef;
	}

	public void setMdef(short mdef) {
		this.mdef = mdef;
	}

	public void setAcc(short acc) {
		this.acc = acc;
	}

	public void setAvoid(short avoid) {
		this.avoid = avoid;
	}

	public void setHands(short hands) {
		this.hands = hands;
	}

	public void setSpeed(short speed) {
		this.speed = speed;
	}

	public void setJump(short jump) {
		this.jump = jump;
	}

	public void setVicious(short vicious) {
		this.vicious = vicious;
	}

	public void setUpgradeSlots(byte upgradeSlots) {
		this.upgradeSlots = upgradeSlots;
	}

	public byte getLevel() {
		return this.level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public void gainLevel(MapleClient c, boolean timeless) {
		final List<Pair<String, Integer>> stats = MapleItemInformationProvider
				.getInstance().getItemLevelupStats(this.getItemId(),
						this.itemLevel, timeless);
		for (final Pair<String, Integer> stat : stats) {
			switch (stat.getLeft()) {
			case "incDEX":
				this.dex += stat.getRight();
				break;
			case "incSTR":
				this.str += stat.getRight();
				break;
			case "incINT":
				this._int += stat.getRight();
				break;
			case "incLUK":
				this.luk += stat.getRight();
				break;
			case "incMHP":
				this.hp += stat.getRight();
				break;
			case "incMMP":
				this.mp += stat.getRight();
				break;
			case "incPAD":
				this.watk += stat.getRight();
				break;
			case "incMAD":
				this.matk += stat.getRight();
				break;
			case "incPDD":
				this.wdef += stat.getRight();
				break;
			case "incMDD":
				this.mdef += stat.getRight();
				break;
			case "incEVA":
				this.avoid += stat.getRight();
				break;
			case "incACC":
				this.acc += stat.getRight();
				break;
			case "incSpeed":
				this.speed += stat.getRight();
				break;
			case "incJump":
				this.jump += stat.getRight();
				break;
			}
		}
		this.itemLevel++;
		c.announce(MaplePacketCreator.showEquipmentLevelUp());
		c.getPlayer()
				.getMap()
				.broadcastMessage(
						c.getPlayer(),
						MaplePacketCreator.showForeignEffect(c.getPlayer()
								.getId(), 15));
		c.getPlayer().forceUpdateItem(this);
	}

	public int getItemExp() {
		return (int) this.itemExp;
	}

	public void gainItemExp(MapleClient c, int gain, boolean timeless) {
		final int expneeded = timeless ? ((10 * this.itemLevel) + 70)
				: ((5 * this.itemLevel) + 65);
		final float modifier = 364 / expneeded;
		final float exp = (expneeded / (1000000 * modifier * modifier)) * gain;
		this.itemExp += exp;
		if (this.itemExp >= 364) {
			this.itemExp = (this.itemExp - 364);
			this.gainLevel(c, timeless);
		} else {
			c.getPlayer().forceUpdateItem(this);
		}
	}

	public void setItemExp(int exp) {
		this.itemExp = exp;
	}

	public void setItemLevel(byte level) {
		this.itemLevel = level;
	}

	@Override
	public void setQuantity(short quantity) {
		if ((quantity < 0) || (quantity > 1)) {
			throw new RuntimeException("Setting the quantity to " + quantity
					+ " on an equip (itemid: " + this.getItemId() + ")");
		}
		super.setQuantity(quantity);
	}

	public void setUpgradeSlots(int i) {
		this.upgradeSlots = (byte) i;
	}

	public void setVicious(int i) {
		this.vicious = (short) i;
	}

	public int getRingId() {
		return this.ringid;
	}

	public void setRingId(int id) {
		this.ringid = id;
	}

	public boolean isWearing() {
		return this.wear;
	}

	public void wear(boolean yes) {
		this.wear = yes;
	}

	public byte getItemLevel() {
		return this.itemLevel;
	}
}