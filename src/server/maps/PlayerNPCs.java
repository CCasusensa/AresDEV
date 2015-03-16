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
package server.maps;

import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import client.MapleClient;

/**
 *
 * @author XoticStory
 */
public class PlayerNPCs extends AbstractMapleMapObject {
	private final Map<Byte, Integer> equips = new HashMap<Byte, Integer>();
	private int npcId, face, hair;
	private byte skin;
	private String name = "";
	private int FH, RX0, RX1, CY;

	public PlayerNPCs(ResultSet rs) {
		try {
			this.CY = rs.getInt("cy");
			this.name = rs.getString("name");
			this.hair = rs.getInt("hair");
			this.face = rs.getInt("face");
			this.skin = rs.getByte("skin");
			this.FH = rs.getInt("Foothold");
			this.RX0 = rs.getInt("rx0");
			this.RX1 = rs.getInt("rx1");
			this.npcId = rs.getInt("ScriptId");
			this.setPosition(new Point(rs.getInt("x"), this.CY));
			final PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"SELECT equippos, equipid FROM playernpcs_equip WHERE NpcId = ?");
			ps.setInt(1, rs.getInt("id"));
			final ResultSet rs2 = ps.executeQuery();
			while (rs2.next()) {
				this.equips.put(rs2.getByte("equippos"), rs2.getInt("equipid"));
			}
			rs2.close();
			ps.close();
		} catch (final SQLException e) {
		}
	}

	public Map<Byte, Integer> getEquips() {
		return this.equips;
	}

	public int getId() {
		return this.npcId;
	}

	public int getFH() {
		return this.FH;
	}

	public int getRX0() {
		return this.RX0;
	}

	public int getRX1() {
		return this.RX1;
	}

	public int getCY() {
		return this.CY;
	}

	public byte getSkin() {
		return this.skin;
	}

	public String getName() {
		return this.name;
	}

	public int getFace() {
		return this.face;
	}

	public int getHair() {
		return this.hair;
	}

	@Override
	public void sendDestroyData(MapleClient client) {
		return;
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.PLAYER_NPC;
	}

	@Override
	public void sendSpawnData(MapleClient client) {
		client.announce(MaplePacketCreator.spawnPlayerNPC(this));
		client.announce(MaplePacketCreator.getPlayerNPC(this));
	}
}