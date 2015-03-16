package server.partyquest;

import java.util.LinkedList;
import java.util.List;

import server.maps.MapleMap;
import tools.MaplePacketCreator;
import client.MapleCharacter;

/**
 * @author Rob //Thanks :3 - LOST MOTIVATION >=(
 */
public class MonsterCarnivalParty {

	private List<MapleCharacter> members = new LinkedList<>();
	private final MapleCharacter leader;
	private final byte team;
	private short availableCP = 0, totalCP = 0;
	private int summons = 7;
	private boolean winner = false;

	public MonsterCarnivalParty(final MapleCharacter owner,
			final List<MapleCharacter> members1, final byte team1) {
		this.leader = owner;
		this.members = members1;
		this.team = team1;

		for (final MapleCharacter chr : this.members) {
			chr.setCarnivalParty(this);
			chr.setTeam(this.team);
		}
	}

	public final MapleCharacter getLeader() {
		return this.leader;
	}

	public void addCP(MapleCharacter player, int ammount) {
		this.totalCP += ammount;
		this.availableCP += ammount;
		player.addCP(ammount);
	}

	public int getTotalCP() {
		return this.totalCP;
	}

	public int getAvailableCP() {
		return this.availableCP;
	}

	public void useCP(MapleCharacter player, int ammount) {
		this.availableCP -= ammount;
		player.useCP(ammount);
	}

	public List<MapleCharacter> getMembers() {
		return this.members;
	}

	public int getTeam() {
		return this.team;
	}

	public void warpOut(final int map) {
		for (final MapleCharacter chr : this.members) {
			chr.changeMap(map, 0);
			chr.setCarnivalParty(null);
			chr.setCarnival(null);
		}
		this.members.clear();
	}

	public void warp(final MapleMap map, final int portalid) {
		for (final MapleCharacter chr : this.members) {
			chr.changeMap(map, map.getPortal(portalid));
		}
	}

	public void warpOut() {
		if (this.winner == true) {
			this.warpOut(980000003 + (this.leader.getCarnival().getRoom() * 100));
		} else {
			this.warpOut(980000004 + (this.leader.getCarnival().getRoom() * 100));
		}
	}

	public boolean allInMap(MapleMap map) {
		boolean status = true;
		for (final MapleCharacter chr : this.members) {
			if (chr.getMap() != map) {
				status = false;
			}
		}
		return status;
	}

	public void removeMember(MapleCharacter chr) {
		this.members.remove(chr);
		chr.changeMap(980000010);
		chr.setCarnivalParty(null);
		chr.setCarnival(null);
	}

	public boolean isWinner() {
		return this.winner;
	}

	public void setWinner(boolean status) {
		this.winner = status;
	}

	public void displayMatchResult() {
		final String effect = this.winner ? "quest/carnival/win"
				: "quest/carnival/lose";

		for (final MapleCharacter chr : this.members) {
			chr.announce(MaplePacketCreator.showEffect(effect));
		}
	}

	public void summon() {
		this.summons--;
	}

	public boolean canSummon() {
		return this.summons > 0;
	}
}
