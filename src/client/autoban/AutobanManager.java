/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package client.autoban;

import java.util.HashMap;
import java.util.Map;

import client.MapleCharacter;

/**
 *
 * @author kevintjuh93
 */
public class AutobanManager {
	private final MapleCharacter chr;
	private final Map<AutobanFactory, Integer> points = new HashMap<>();
	private final Map<AutobanFactory, Long> lastTime = new HashMap<>();
	private int misses = 0;
	private int lastmisses = 0;
	private int samemisscount = 0;
	private final long spam[] = new long[20];
	private final int timestamp[] = new int[20];
	private final byte timestampcounter[] = new byte[20];

	public AutobanManager(MapleCharacter chr) {
		this.chr = chr;
	}

	public void addPoint(AutobanFactory fac, String reason) {
		if (this.lastTime.containsKey(fac)) {
			if (this.lastTime.get(fac) < (System.currentTimeMillis() - fac
					.getExpire())) {
				this.points.put(fac, this.points.get(fac) / 2); // So the points
																// are not
				// completely gone.
			}
		}
		if (fac.getExpire() != -1) {
			this.lastTime.put(fac, System.currentTimeMillis());
		}

		if (this.points.containsKey(fac)) {
			this.points.put(fac, this.points.get(fac) + 1);
		} else {
			this.points.put(fac, 1);
		}

		if (this.points.get(fac) >= fac.getMaximum()) {
			this.chr.autoban("Autobanned for " + fac.name() + " ;" + reason, 1);
			this.chr.sendPolice("You have been blocked by #bMooplePolice for the HACK reason#k.");
		}
	}

	public void addMiss() {
		this.misses++;
	}

	public void resetMisses() {
		if ((this.lastmisses == this.misses) && (this.misses > 6)) {
			this.samemisscount++;
		}
		if (this.samemisscount > 4) {
			this.chr.autoban("Autobanned for : " + this.misses
					+ " Miss godmode", 1);
		} else if (this.samemisscount > 0) {
			this.lastmisses = this.misses;
		}
		this.misses = 0;
	}

	// Don't use the same type for more than 1 thing
	public void spam(int type) {
		this.spam[type] = System.currentTimeMillis();
	}

	public long getLastSpam(int type) {
		return this.spam[type];
	}

	/**
	 * Timestamp checker
	 *
	 * <code>type</code>:<br>
	 * 0: HealOverTime<br>
	 * 1: Pet Food<br>
	 * 2: ItemSort<br>
	 * 3: ItemIdSort<br>
	 * 4: SpecialMove<br>
	 * 5: UseCatchItem<br>
	 *
	 * @param type
	 *            type
	 * @return Timestamp checker
	 */
	public void setTimestamp(int type, int time) {
		if (this.timestamp[type] == time) {
			this.timestampcounter[type]++;
			if (this.timestampcounter[type] > 3) {
				this.chr.getClient().disconnect(false, false);
				// System.out.println("Same timestamp for type: " + type +
				// "; Character: " + chr);
			}
			return;
		}
		this.timestamp[type] = time;
	}
}
