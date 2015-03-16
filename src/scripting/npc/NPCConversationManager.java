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
package scripting.npc;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import scripting.AbstractPlayerInteraction;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.events.gm.MapleEvent;
import server.expeditions.MapleExpedition;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.partyquest.Pyramid;
import server.partyquest.Pyramid.PyramidMode;
import server.quest.MapleQuest;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Randomizer;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.MapleSkinColor;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.ExpTable;

/**
 *
 * @author Matze
 */
public class NPCConversationManager extends AbstractPlayerInteraction {

	private final int npc;
	private String getText;

	public NPCConversationManager(MapleClient c, int npc) {
		super(c);
		this.npc = npc;
	}

	public int getNpc() {
		return this.npc;
	}

	public void dispose() {
		NPCScriptManager.getInstance().dispose(this);
	}

	public void sendNext(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"00 01", (byte) 0));
	}

	public void sendPrev(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"01 00", (byte) 0));
	}

	public void sendNextPrev(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"01 01", (byte) 0));
	}

	public void sendOk(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"00 00", (byte) 0));
	}

	public void sendYesNo(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 1, text, "",
						(byte) 0));
	}

	public void sendAcceptDecline(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0x0C, text, "",
						(byte) 0));
	}

	public void sendSimple(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 4, text, "",
						(byte) 0));
	}

	public void sendNext(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"00 01", speaker));
	}

	public void sendPrev(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"01 00", speaker));
	}

	public void sendNextPrev(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"01 01", speaker));
	}

	public void sendOk(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0, text,
						"00 00", speaker));
	}

	public void sendYesNo(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 1, text, "",
						speaker));
	}

	public void sendAcceptDecline(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 0x0C, text, "",
						speaker));
	}

	public void sendSimple(String text, byte speaker) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalk(this.npc, (byte) 4, text, "",
						speaker));
	}

	public void sendStyle(String text, int styles[]) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalkStyle(this.npc, text, styles));
	}

	public void sendGetNumber(String text, int def, int min, int max) {
		this.getClient()
				.announce(
						MaplePacketCreator.getNPCTalkNum(this.npc, text, def,
								min, max));
	}

	public void sendGetText(String text) {
		this.getClient().announce(
				MaplePacketCreator.getNPCTalkText(this.npc, text, ""));
	}

	/*
	 * 0 = ariant colliseum 1 = Dojo 2 = Carnival 1 3 = Carnival 2 4 = Ghost
	 * Ship PQ? 5 = Pyramid PQ 6 = Kerning Subway
	 */
	public void sendDimensionalMirror(String text) {
		this.getClient()
				.announce(MaplePacketCreator.getDimensionalMirror(text));
	}

	public void setGetText(String text) {
		this.getText = text;
	}

	public String getText() {
		return this.getText;
	}

	public int getJobId() {
		return this.getPlayer().getJob().getId();
	}

	public void startQuest(short id) {
		try {
			MapleQuest.getInstance(id).forceStart(this.getPlayer(), this.npc);
		} catch (final NullPointerException ex) {
		}
	}

	public void completeQuest(short id) {
		try {
			MapleQuest.getInstance(id)
					.forceComplete(this.getPlayer(), this.npc);
		} catch (final NullPointerException ex) {
		}
	}

	public int getMeso() {
		return this.getPlayer().getMeso();
	}

	public void gainMeso(int gain) {
		this.getPlayer().gainMeso(gain, true, false, true);
	}

	public void gainExp(int gain) {
		this.getPlayer().gainExp(gain, true, true);
	}

	public int getLevel() {
		return this.getPlayer().getLevel();
	}

	public void showEffect(String effect) {
		this.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.environmentChange(effect, 3));
	}

	public void setHair(int hair) {
		this.getPlayer().setHair(hair);
		this.getPlayer().updateSingleStat(MapleStat.HAIR, hair);
		this.getPlayer().equipChanged();
	}

	public void setFace(int face) {
		this.getPlayer().setFace(face);
		this.getPlayer().updateSingleStat(MapleStat.FACE, face);
		this.getPlayer().equipChanged();
	}

	public void setSkin(int color) {
		this.getPlayer().setSkinColor(MapleSkinColor.getById(color));
		this.getPlayer().updateSingleStat(MapleStat.SKIN, color);
		this.getPlayer().equipChanged();
	}

	public int itemQuantity(int itemid) {
		return this
				.getPlayer()
				.getInventory(
						MapleItemInformationProvider.getInstance()
								.getInventoryType(itemid)).countById(itemid);
	}

	public void displayGuildRanks() {
		MapleGuild.displayGuildRanks(this.getClient(), this.npc);
	}

	@Override
	public MapleParty getParty() {
		return this.getPlayer().getParty();
	}

	@Override
	public void resetMap(int mapid) {
		this.getClient().getChannelServer().getMapFactory().getMap(mapid)
				.resetReactors();
	}

	public void gainCloseness(int closeness) {
		for (final MaplePet pet : this.getPlayer().getPets()) {
			if (pet.getCloseness() > 30000) {
				pet.setCloseness(30000);
				return;
			}
			pet.gainCloseness(closeness);
			while (pet.getCloseness() > ExpTable.getClosenessNeededForLevel(pet
					.getLevel())) {
				pet.setLevel((byte) (pet.getLevel() + 1));
				final byte index = this.getPlayer().getPetIndex(pet);
				this.getClient().announce(
						MaplePacketCreator.showOwnPetLevelUp(index));
				this.getPlayer()
						.getMap()
						.broadcastMessage(
								this.getPlayer(),
								MaplePacketCreator.showPetLevelUp(
										this.getPlayer(), index));
			}
			final Item petz = this.getPlayer()
					.getInventory(MapleInventoryType.CASH)
					.getItem(pet.getPosition());
			this.getPlayer().forceUpdateItem(petz);
		}
	}

	public String getName() {
		return this.getPlayer().getName();
	}

	public int getGender() {
		return this.getPlayer().getGender();
	}

	public void changeJobById(int a) {
		this.getPlayer().changeJob(MapleJob.getById(a));
	}

	public void addRandomItem(int id) {
		final MapleItemInformationProvider i = MapleItemInformationProvider
				.getInstance();
		MapleInventoryManipulator.addFromDrop(this.getClient(),
				i.randomizeStats((Equip) i.getEquipById(id)), true);
	}

	public MapleJob getJobName(int id) {
		return MapleJob.getById(id);
	}

	public MapleStatEffect getItemEffect(int itemId) {
		return MapleItemInformationProvider.getInstance().getItemEffect(itemId);
	}

	public void resetStats() {
		this.getPlayer().resetStats();
	}

	public void maxMastery() {
		for (final MapleData skill_ : MapleDataProviderFactory
				.getDataProvider(
						new File(System.getProperty("wzpath") + "/"
								+ "String.wz")).getData("Skill.img")
				.getChildren()) {
			try {
				final Skill skill = SkillFactory.getSkill(Integer
						.parseInt(skill_.getName()));
				this.getPlayer().changeSkillLevel(skill, (byte) 0,
						skill.getMaxLevel(), -1);
			} catch (final NumberFormatException nfe) {
				break;
			} catch (final NullPointerException npe) {
				continue;
			}
		}
	}

	public void processGachapon(int[] id, boolean remote) {
		final int[] gacMap = { 100000000, 101000000, 102000000, 103000000,
				105040300, 800000000, 809000101, 809000201, 600000000,
				120000000 };
		final int itemid = id[Randomizer.nextInt(id.length)];
		this.addRandomItem(itemid);
		if (!remote) {
			this.gainItem(5220000, (short) -1);
		}
		this.sendNext("You have obtained a #b#t" + itemid + "##k.");
		this.getClient()
				.getChannelServer()
				.broadcastPacket(
						MaplePacketCreator.gachaponMessage(
								this.getPlayer()
										.getInventory(
												MapleInventoryType
														.getByType((byte) (itemid / 1000000)))
										.findById(itemid),
								this.c.getChannelServer()
										.getMapFactory()
										.getMap(gacMap[((this.getNpc() != 9100117) && (this
												.getNpc() != 9100109)) ? (this
												.getNpc() - 9100100) : this
												.getNpc() == 9100109 ? 8 : 9])
										.getMapName(), this.getPlayer()));
	}

	public void disbandAlliance(MapleClient c, int allianceId) {
		PreparedStatement ps = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"DELETE FROM `alliance` WHERE id = ?");
			ps.setInt(1, allianceId);
			ps.executeUpdate();
			ps.close();
			Server.getInstance().allianceMessage(
					c.getPlayer().getGuild().getAllianceId(),
					MaplePacketCreator.disbandAlliance(allianceId), -1, -1);
			Server.getInstance().disbandAlliance(allianceId);
		} catch (final SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			try {
				if ((ps != null) && !ps.isClosed()) {
					ps.close();
				}
			} catch (final SQLException ex) {
			}
		}
	}

	public boolean canBeUsedAllianceName(String name) {
		if (name.contains(" ") || (name.length() > 12)) {
			return false;
		}
		try {
			ResultSet rs;
			try (PreparedStatement ps = DatabaseConnection.getConnection()
					.prepareStatement(
							"SELECT name FROM alliance WHERE name = ?")) {
				ps.setString(1, name);
				rs = ps.executeQuery();
				if (rs.next()) {
					ps.close();
					rs.close();
					return false;
				}
			}
			rs.close();
			return true;
		} catch (final SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static MapleAlliance createAlliance(MapleCharacter chr1,
			MapleCharacter chr2, String name) {
		int id;
		final int guild1 = chr1.getGuildId();
		final int guild2 = chr2.getGuildId();
		try {
			try (PreparedStatement ps = DatabaseConnection
					.getConnection()
					.prepareStatement(
							"INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)",
							Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, name);
				ps.setInt(2, guild1);
				ps.setInt(3, guild2);
				ps.executeUpdate();
				try (ResultSet rs = ps.getGeneratedKeys()) {
					rs.next();
					id = rs.getInt(1);
				}
			}
		} catch (final SQLException e) {
			e.printStackTrace();
			return null;
		}
		final MapleAlliance alliance = new MapleAlliance(name, id, guild1,
				guild2);
		try {
			Server.getInstance().setGuildAllianceId(guild1, id);
			Server.getInstance().setGuildAllianceId(guild2, id);
			chr1.setAllianceRank(1);
			chr1.saveGuildStatus();
			chr2.setAllianceRank(2);
			chr2.saveGuildStatus();
			Server.getInstance().addAlliance(id, alliance);
			Server.getInstance().allianceMessage(
					id,
					MaplePacketCreator.makeNewAlliance(alliance,
							chr1.getClient()), -1, -1);
		} catch (final Exception e) {
			return null;
		}
		return alliance;
	}

	public List<MapleCharacter> getPartyMembers() {
		if (this.getPlayer().getParty() == null) {
			return null;
		}
		final List<MapleCharacter> chars = new LinkedList<>();
		for (final Channel channel : Server.getInstance().getChannelsFromWorld(
				this.getPlayer().getWorld())) {
			for (final MapleCharacter chr : channel.getPartyMembers(this
					.getPlayer().getParty())) {
				if (chr != null) {
					chars.add(chr);
				}
			}
		}
		return chars;
	}

	public void warpParty(int id) {
		for (final MapleCharacter mc : this.getPartyMembers()) {
			if (id == 925020100) {
				mc.setDojoParty(true);
			}
			mc.changeMap(this.getWarpMap(id));
		}
	}

	public boolean hasMerchant() {
		return this.getPlayer().hasMerchant();
	}

	public boolean hasMerchantItems() {
		try {
			if (!ItemFactory.MERCHANT
					.loadItems(this.getPlayer().getId(), false).isEmpty()) {
				return true;
			}
		} catch (final SQLException e) {
			return false;
		}
		if (this.getPlayer().getMerchantMeso() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public void showFredrick() {
		this.c.announce(MaplePacketCreator.getFredrick(this.getPlayer()));
	}

	public int partyMembersInMap() {
		int inMap = 0;
		for (final MapleCharacter char2 : this.getPlayer().getMap()
				.getCharacters()) {
			if (char2.getParty() == this.getPlayer().getParty()) {
				inMap++;
			}
		}
		return inMap;
	}

	public MapleEvent getEvent() {
		return this.c.getChannelServer().getEvent();
	}

	public void divideTeams() {
		if (this.getEvent() != null) {
			this.getPlayer().setTeam(this.getEvent().getLimit() % 2); // muhaha
																		// :D
		}
	}

	public MapleExpedition createExpedition(String type, byte min) {
		final MapleParty party = this.getPlayer().getParty();
		if ((party == null) || (party.getMembers().size() < min)) {
			return null;
		}
		return new MapleExpedition(this.getPlayer());
	}

	public boolean createPyramid(String mode, boolean party) {// lol
		final PyramidMode mod = PyramidMode.valueOf(mode);

		MapleParty partyz = this.getPlayer().getParty();
		final MapleMapFactory mf = this.c.getChannelServer().getMapFactory();

		MapleMap map = null;
		int mapid = 926010100;
		if (party) {
			mapid += 10000;
		}
		mapid += (mod.getMode() * 1000);

		for (byte b = 0; b < 5; b++) {// They cannot warp to the next map before
										// the timer ends (:
			map = mf.getMap(mapid + b);
			if (map.getCharacters().size() > 0) {
				continue;
			} else {
				break;
			}
		}

		if (map == null) {
			return false;
		}

		if (!party) {
			partyz = new MapleParty(-1, new MaplePartyCharacter(
					this.getPlayer()));
		}
		final Pyramid py = new Pyramid(partyz, mod, map.getId());
		this.getPlayer().setPartyQuest(py);
		py.warp(mapid);
		this.dispose();
		return true;
	}
}
