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
package scripting.portal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import scripting.AbstractPlayerInteraction;
import server.MaplePortal;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import client.MapleClient;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {

	private final MaplePortal portal;

	public PortalPlayerInteraction(MapleClient c, MaplePortal portal) {
		super(c);
		this.portal = portal;
	}

	public MaplePortal getPortal() {
		return this.portal;
	}

	public boolean hasLevel30Character() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"SELECT `level` FROM `characters` WHERE accountid = ?");
			ps.setInt(1, this.getPlayer().getAccountID());
			rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getInt("level") >= 30) {
					return true;
				}
			}
		} catch (final SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			try {
				if ((ps != null) && !ps.isClosed()) {
					ps.close();
				}
				if ((rs != null) && !rs.isClosed()) {
					rs.close();
				}
			} catch (final SQLException ex) {
			}
		}
		return false;
	}

	public void blockPortal() {
		this.c.getPlayer().blockPortal(this.getPortal().getScriptName());
	}

	public void unblockPortal() {
		this.c.getPlayer().unblockPortal(this.getPortal().getScriptName());
	}

	public void playPortalSound() {
		this.c.announce(MaplePacketCreator.playPortalSound());
	}
}