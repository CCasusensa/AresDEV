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

import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class TimerManager implements TimerManagerMBean {
	private static TimerManager instance = new TimerManager();
	private ScheduledThreadPoolExecutor ses;

	private TimerManager() {
		final MBeanServer mBeanServer = ManagementFactory
				.getPlatformMBeanServer();
		try {
			mBeanServer.registerMBean(this, new ObjectName(
					"server:type=TimerManger"));
		} catch (final Exception e) {
		}
	}

	public static TimerManager getInstance() {
		return instance;
	}

	public void start() {
		if ((this.ses != null) && !this.ses.isShutdown()
				&& !this.ses.isTerminated()) {
			return;
		}
		final ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(
				4, new ThreadFactory() {
					private final AtomicInteger threadNumber = new AtomicInteger(
							1);

					@Override
					public Thread newThread(Runnable r) {
						final Thread t = new Thread(r);
						t.setName("TimerManager-Worker-"
								+ this.threadNumber.getAndIncrement());
						return t;
					}
				});
		stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		this.ses = stpe;
	}

	public void stop() {
		this.ses.shutdownNow();
	}

	public Runnable purge() {
		return new Runnable() {
			@Override
			public void run() {
				TimerManager.this.ses.purge();
			}
		};
	}

	public ScheduledFuture<?> register(Runnable r, long repeatTime, long delay) {
		return this.ses.scheduleAtFixedRate(new LoggingSaveRunnable(r), delay,
				repeatTime, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> register(Runnable r, long repeatTime) {
		return this.ses.scheduleAtFixedRate(new LoggingSaveRunnable(r), 0,
				repeatTime, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> schedule(Runnable r, long delay) {
		return this.ses.schedule(new LoggingSaveRunnable(r), delay,
				TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> scheduleAtTimestamp(Runnable r, long timestamp) {
		return this.schedule(r, timestamp - System.currentTimeMillis());
	}

	@Override
	public long getActiveCount() {
		return this.ses.getActiveCount();
	}

	@Override
	public long getCompletedTaskCount() {
		return this.ses.getCompletedTaskCount();
	}

	@Override
	public int getQueuedTasks() {
		return this.ses.getQueue().toArray().length;
	}

	@Override
	public long getTaskCount() {
		return this.ses.getTaskCount();
	}

	@Override
	public boolean isShutdown() {
		return this.ses.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.ses.isTerminated();
	}

	private static class LoggingSaveRunnable implements Runnable {
		Runnable r;

		public LoggingSaveRunnable(Runnable r) {
			this.r = r;
		}

		@Override
		public void run() {
			try {
				this.r.run();
			} catch (final Throwable t) {
			}
		}
	}
}
