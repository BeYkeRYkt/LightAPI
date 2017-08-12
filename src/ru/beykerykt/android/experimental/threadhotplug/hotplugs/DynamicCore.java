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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import ru.beykerykt.android.experimental.threadhotplug.PseudoCore;
import ru.beykerykt.android.experimental.threadhotplug.Task;

public class DynamicCore implements PseudoCore {

	private Collection<Task> queue;
	private boolean idle;
	private int idleCycles;
	private int maxIdleCycles;

	private ScheduledFuture<?> thread;

	public DynamicCore(Collection<Task> queue, int maxIdleCycles) {
		this.queue = queue;
		this.maxIdleCycles = maxIdleCycles;
	}

	@Override
	public void run() {
		synchronized (queue) {
			if (idleCycles == maxIdleCycles && !isIdle()) {
				setIdle(true);
				return;
			}

			if (queue.isEmpty() && !isIdle()) {
				idleCycles++;
				return;
			}

			idleCycles = 0;

			// Sorting
			Collections.sort((List) queue, new Comparator<Task>() {

				@Override
				public int compare(Task arg0, Task arg1) {
					if (arg0.getPriority() < arg1.getPriority()) {
						return 1;
					} else {
						return 0;
					}
				}
			});

			Iterator<Task> it = queue.iterator();
			while (it.hasNext()) {
				try {
					Task obj = it.next();
					obj.process();
					if (!obj.isRepeatable()) {
						it.remove();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onCreate() {
		this.idleCycles = 0;
	}

	@Override
	public void onCoreEnable() {
		setIdle(false);
	}

	@Override
	public void onCoreDisable() {
		queue.clear();
		idleCycles = 0;
	}

	@Override
	public void onDestroy() {
		if (thread != null && !thread.isCancelled()) {
			thread.cancel(false);
		}
	}

	@Override
	public void addTask(Task task) {
		synchronized (queue) {
			if (queue.contains(task)) {
				queue.remove(task);
			}
			queue.add(task);
			if (isIdle()) {
				setIdle(false);
			}
		}
	}

	@Override
	public Collection<Task> getTaskQueue() {
		return queue;
	}

	@Override
	public boolean canDestroy() {
		return isIdle() && idleCycles >= maxIdleCycles;
	}

	@Override
	public boolean isIdle() {
		return idle;
	}

	@Override
	public void setIdle(boolean flag) {
		if (!flag) {
			idleCycles = 0;
		}
		idle = flag;
	}

	public ScheduledFuture<?> getThread() {
		return thread;
	}

	public void setThread(ScheduledFuture<?> thread) {
		this.thread = thread;
	}

}
