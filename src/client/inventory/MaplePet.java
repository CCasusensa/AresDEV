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

import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import server.MapleItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.DatabaseConnection;

/**
 *
 * @author Matze
 */
public class MaplePet extends Item {
	private String name;
	private int uniqueid;
	private int closeness = 0;
	private byte level = 1;
	private int fullness = 100;
	private int Fh;
	private Point pos;
	private int stance;
	private boolean summoned;

	private MaplePet(int id, byte position, int uniqueid) {
		super(id, position, (short) 1);
		this.uniqueid = uniqueid;
	}

	public static MaplePet loadFromDb(int itemid, byte position, int petid) {
		try {
			final MaplePet ret = new MaplePet(itemid, position, petid);
			final PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT name, level, closeness, fullness, summoned FROM pets WHERE petid = ?"); // Get
																											// pet
																											// details..
			ps.setInt(1, petid);
			final ResultSet rs = ps.executeQuery();
			rs.next();
			ret.setName(rs.getString("name"));
			ret.setCloseness(Math.min(rs.getInt("closeness"), 30000));
			ret.setLevel((byte) Math.min(rs.getByte("level"), 30));
			ret.setFullness(Math.min(rs.getInt("fullness"), 100));
			ret.setSummoned(rs.getInt("summoned") == 1);
			rs.close();
			ps.close();
			return ret;
		} catch (final SQLException e) {
			return null;
		}
	}

	public void saveToDb() {
		try {
			final PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, summoned = ? WHERE petid = ?");
			ps.setString(1, this.getName());
			ps.setInt(2, this.getLevel());
			ps.setInt(3, this.getCloseness());
			ps.setInt(4, this.getFullness());
			ps.setInt(5, this.isSummoned() ? 1 : 0);
			ps.setInt(6, this.getUniqueId());
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException e) {
		}
	}

	public static int createPet(int itemid) {
		try {
			final PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"INSERT INTO pets (name, level, closeness, fullness, summoned) VALUES (?, 1, 0, 100, 0)",
							java.sql.Statement.RETURN_GENERATED_KEYS);
			ps.setString(1,
					MapleItemInformationProvider.getInstance().getName(itemid));
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			int ret = -1;
			if (rs.next()) {
				ret = rs.getInt(1);
			}
			rs.close();
			ps.close();
			return ret;
		} catch (final SQLException e) {
			return -1;
		}
	}

	public static int createPet(int itemid, byte level, int closeness,
			int fullness) {
		try {
			final PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"INSERT INTO pets (name, level, closeness, fullness, summoned) VALUES (?, ?, ?, ?, 0)",
							java.sql.Statement.RETURN_GENERATED_KEYS);
			ps.setString(1,
					MapleItemInformationProvider.getInstance().getName(itemid));
			ps.setByte(2, level);
			ps.setInt(3, closeness);
			ps.setInt(4, fullness);
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			int ret = -1;
			if (rs.next()) {
				ret = rs.getInt(1);
				rs.close();
				ps.close();
			}
			return ret;
		} catch (final SQLException e) {
			return -1;
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUniqueId() {
		return this.uniqueid;
	}

	public void setUniqueId(int id) {
		this.uniqueid = id;
	}

	public int getCloseness() {
		return this.closeness;
	}

	public void setCloseness(int closeness) {
		this.closeness = closeness;
	}

	public void gainCloseness(int x) {
		this.closeness += x;
	}

	public byte getLevel() {
		return this.level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public int getFullness() {
		return this.fullness;
	}

	public void setFullness(int fullness) {
		this.fullness = fullness;
	}

	public int getFh() {
		return this.Fh;
	}

	public void setFh(int Fh) {
		this.Fh = Fh;
	}

	public Point getPos() {
		return this.pos;
	}

	public void setPos(Point pos) {
		this.pos = pos;
	}

	public int getStance() {
		return this.stance;
	}

	public void setStance(int stance) {
		this.stance = stance;
	}

	public boolean isSummoned() {
		return this.summoned;
	}

	public void setSummoned(boolean yes) {
		this.summoned = yes;
	}

	public boolean canConsume(int itemId) {
		for (final int petId : MapleItemInformationProvider.getInstance()
				.petsCanConsume(itemId)) {
			if (petId == this.getItemId()) {
				return true;
			}
		}
		return false;
	}

	public void updatePosition(List<LifeMovementFragment> movement) {
		for (final LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					this.setPos(((LifeMovement) move).getPosition());
				}
				this.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}