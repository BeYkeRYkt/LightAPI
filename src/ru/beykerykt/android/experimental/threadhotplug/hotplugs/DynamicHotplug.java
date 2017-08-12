/**
 * Copyright 2016 - 2017 Vladimir Mikhaylov
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */
package ru.beykerykt.android.experimental.threadhotplug.hotplugs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ru.beykerykt.android.experimental.threadhotplug.IHotplug;
import ru.beykerykt.android.experimental.threadhotplug.PseudoCore;
import ru.beykerykt.android.experimental.threadhotplug.Task;
import ru.beykerykt.android.experimental.threadhotplug.TaskScheduler;

public class DynamicHotplug implements IHotplug, Runnable {

	private boolean started;

	private ScheduledExecutorService executor;
	private ScheduledExecutorService syncExecutor;
	private List<PseudoCore> cores;

	private int maxQueueSize = 1000;
	private int maxCores = Runtime.getRuntime().availableProcessors() * 2;

	private long DELAY_TICK = 50;

	@Override
	public void start() {
		if (started) {
			return;
		}
		started = true;
		cores = new CopyOnWriteArrayList<PseudoCore>();
		syncExecutor = Executors.newSingleThreadScheduledExecutor();
		executor = Executors.newScheduledThreadPool(maxCores);
		createNewSyncThread(this, 0, DELAY_TICK);
		setCurrentThreads(0);
		setMaxThreads(maxCores);
		TaskScheduler.debug("Hotplug is enabled!");
	}

	@Override
	public boolean isInit() {
		return started;
	}

	@Override
	public void shutdown() {
		if (!started) {
			return;
		}

		executor.shutdown();
		syncExecutor.shutdown();
		cores.clear();
		started = false;
		TaskScheduler.debug("Hotplug is disabled!");
	}

	@Override
	public void execute(Task task) {
		if (!isInit())
			return;
		synchronized (cores) {
			PseudoCore low = getLowestCore();

			if (low.getTaskQueue().size() == getMaxQueueSize()) {
				low = createCore();
			}

			low.addTask(task);
		}
	}

	@Override
	public void execute(Runnable runnable) {
		if (!isInit())
			return;
		syncExecutor.execute(runnable);
	}

	private PseudoCore getLowestCore() {
		if (!isInit())
			return null;
		synchronized (cores) {
			if (cores.isEmpty()) {
				return createCore();
			}

			PseudoCore runn = cores.get(0);
			int temp_size = runn.getTaskQueue().size();

			for (int i = 0; i < cores.size(); i++) {
				PseudoCore hr = cores.get(i);
				if (hr.getTaskQueue().size() < temp_size) {
					runn = hr;
					temp_size = hr.getTaskQueue().size();
				}
			}
			return runn;
		}
	}

	private PseudoCore createCore() {
		if (!isInit())
			return null;
		synchronized (cores) {
			if (cores.size() >= getMaxThreads()) {
				return getLowestCore();
			}

			DynamicCore runnable = new DynamicCore(new ArrayList<Task>(), 20);
			runnable.setThread(createNewThread(runnable, 0, DELAY_TICK));
			runnable.onCreate();

			if (runnable.getThread() != null) {
				cores.add(runnable);
				setCurrentThreads(getCurrentThreads() + 1);
				runnable.onCoreEnable();
				TaskScheduler.debug("Core is created!");
				return runnable;
			}

			return getLowestCore();
		}
	}

	@Override
	public int getCurrentThreads() {
		return ((ThreadPoolExecutor) executor).getCorePoolSize();
	}

	@Override
	public void setCurrentThreads(int current_threads) {
		((ThreadPoolExecutor) executor).setCorePoolSize(current_threads);
	}

	@Override
	public int getMaxThreads() {
		if (isInit()) {
			return ((ThreadPoolExecutor) executor).getMaximumPoolSize();
		}
		return this.maxCores;
	}

	@Override
	public void setMaxThreads(int max_threads) {
		this.maxCores = max_threads;
		if (isInit()) {
			((ThreadPoolExecutor) executor).setMaximumPoolSize(maxCores);
		}
	}

	private ScheduledFuture<?> createNewThread(Runnable r, long startAfter, long delay) {
		ScheduledFuture<?> sch = executor.scheduleWithFixedDelay(r, startAfter, delay, TimeUnit.MILLISECONDS);
		return sch;
	}

	private ScheduledFuture<?> createNewSyncThread(Runnable r, long startAfter, long delay) {
		ScheduledFuture<?> sch = syncExecutor.scheduleWithFixedDelay(r, startAfter, delay, TimeUnit.MILLISECONDS);
		return sch;
	}

	@Override
	public void run() {
		synchronized (cores) {
			int allTasks = 0;

			for (int i = 0; i < cores.size(); i++) {
				PseudoCore core = cores.get(i);
				allTasks += core.getTaskQueue().size();
				if (core.canDestroy()) {
					cores.remove(core);
					core.onCoreDisable();
					core.onDestroy();
					setCurrentThreads(cores.size());
					TaskScheduler.debug("Core is dead!");
					i--; // back
				}
			}
			if (allTasks > 0) {
				TaskScheduler.debug("AllTask: " + allTasks);
				allTasks = 0;
			}
		}
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}
}
