/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.events;

import client.MapleCharacter;
import client.SkillFactory;

/**
 *
 * @author kevintjuh93
 */
public class RescueGaga extends MapleEvents {
	private byte fallen;
	private int completed;

	public RescueGaga(int completed) {
		super();
		this.completed = completed;
		this.fallen = 0;
	}

	public int fallAndGet() {
		this.fallen++;
		if (this.fallen > 3) {
			this.fallen = 0;
			return 4;
		}
		return this.fallen;
	}

	public byte getFallen() {
		return this.fallen;
	}

	public int getCompleted() {
		return this.completed;
	}

	public void complete() {
		this.completed++;
	}

	public void giveSkill(MapleCharacter chr) {
		int skillid = 0;
		switch (chr.getJobType()) {
		case 0:
			skillid = 1013;
			break;
		case 1:
		case 2:
			skillid = 10001014;
		}
		final long expiration = (System.currentTimeMillis() + (3600 * 24 * 20 * 1000));// 20
																						// days
		if (this.completed < 20) {
			chr.changeSkillLevel(SkillFactory.getSkill(skillid), (byte) 1, 1,
					expiration);
			chr.changeSkillLevel(SkillFactory.getSkill(skillid + 1), (byte) 1,
					1, expiration);
			chr.changeSkillLevel(SkillFactory.getSkill(skillid + 2), (byte) 1,
					1, expiration);
		} else {
			chr.changeSkillLevel(SkillFactory.getSkill(skillid), (byte) 2, 2,
					chr.getSkillExpiration(skillid));
		}
	}
}
