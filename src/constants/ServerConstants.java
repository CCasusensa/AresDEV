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
package constants;

/**
 *
 *
 * **/
public class ServerConstants {

	public static short VERSION = 83;

	public static String[] WORLD_NAMES = { "Scania", "Bera", "Broa", "Windia",
			"Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos",
			"Galicia", "El Nido", "Zenith", "Arcenia", "Kastia", "Judis",
			"Plana", "Kalluna", "Stius", "Croa", "Medere" };;

	public static final byte QUEST_EXP_RATE = 4;
	public static final byte QUEST_MESO_RATE = 3;

	public static final int CHANNEL_COUNT = 5;
	public static final int CHANNEL_PLAYER_COUNT = 10;
	public static final long RANKING_INTERVAL = 3600000;

	public static final boolean ENABLE_GM_SERVER = false;
	public static final boolean ENABLE_PIC = true;

	public static final boolean PERFECT_PITCH = false;
	public static final String EVENTS = "automsg KerningPQ Boats Subway AirPlane elevator";

	public static final String HOST = "127.0.0.1";

	public static final String DB_URL = "jdbc:mysql://localhost:3306/AresDEV?autoReconnect=true";
	public static final String DB_USER = "root";
	public static final String DB_PASS = "";

	public enum WorldConstants {

		Scania(0, 3, "", "Welcome to AresMS! We are on Revision 1!", 1, 1, 1, 1), Bera(
				1, 3, "", "", 1, 1, 1, 1), Broa(2, 3, "", "", 1, 1, 1, 1);
		// Too lazy

		int worldID, flag, expRate, dropRate, mesoRate, bossDropRate;
		String eventMsg, serverMsg;

		WorldConstants(int worldID, int flag, String eventMsg,
				String serverMsg, int expRate, int dropRate, int mesoRate,
				int bossDropRate) {
			this.worldID = worldID;
			this.flag = flag;
			this.eventMsg = eventMsg;
			this.serverMsg = serverMsg;
			this.expRate = expRate;
			this.dropRate = dropRate;
			this.mesoRate = mesoRate;
			this.bossDropRate = bossDropRate;
		}

		public int getWorld() {
			return this.worldID;
		}

		public int getFlag() {
			return this.flag;
		}

		public String getEventMsg() {
			return this.eventMsg;
		}

		public String getServerMsg() {
			return this.serverMsg;
		}

		public int getExpRate() {
			return this.expRate;
		}

		public int getDropRate() {
			return this.dropRate;
		}

		public int getMesoRate() {
			return this.mesoRate;
		}

		public int getBossDropRate() {
			return this.bossDropRate;
		}
	}

}