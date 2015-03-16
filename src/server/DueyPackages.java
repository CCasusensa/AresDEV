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

import java.util.Calendar;

import client.inventory.Item;

public class DueyPackages {
	private String sender = null;
	private Item item = null;
	private int mesos = 0;
	private int day;
	private int month;
	private int year;
	private int packageId = 0;

	public DueyPackages(int pId, Item item) {
		this.item = item;
		this.packageId = pId;
	}

	public DueyPackages(int pId) { // Meso only package.
		this.packageId = pId;
	}

	public String getSender() {
		return this.sender;
	}

	public void setSender(String name) {
		this.sender = name;
	}

	public Item getItem() {
		return this.item;
	}

	public int getMesos() {
		return this.mesos;
	}

	public void setMesos(int set) {
		this.mesos = set;
	}

	public int getPackageId() {
		return this.packageId;
	}

	public long sentTimeInMilliseconds() {
		final Calendar cal = Calendar.getInstance();
		cal.set(this.year, this.month, this.day);
		return cal.getTimeInMillis();
	}

	public void setSentTime(String sentTime) {
		this.day = Integer.parseInt(sentTime.substring(0, 2));
		this.month = Integer.parseInt(sentTime.substring(3, 5));
		this.year = Integer.parseInt(sentTime.substring(6, 10));
	}
}
