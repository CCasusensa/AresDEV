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

/**
 *
 * @author Traitor
 */
public class MTSItemInfo {
	private final int price;
	private final Item item;
	private final String seller;
	private final int id;
	private final int year, month;
	private int day = 1;

	public MTSItemInfo(Item item, int price, int id, int cid, String seller,
			String date) {
		this.item = item;
		this.price = price;
		this.seller = seller;
		this.id = id;
		this.year = Integer.parseInt(date.substring(0, 4));
		this.month = Integer.parseInt(date.substring(5, 7));
		this.day = Integer.parseInt(date.substring(8, 10));
	}

	public Item getItem() {
		return this.item;
	}

	public int getPrice() {
		return this.price;
	}

	public int getTaxes() {
		return 100 + (this.price / 10);
	}

	public int getID() {
		return this.id;
	}

	public long getEndingDate() {
		final Calendar now = Calendar.getInstance();
		now.set(this.year, this.month - 1, this.day);
		return now.getTimeInMillis();
	}

	public String getSeller() {
		return this.seller;
	}
}
