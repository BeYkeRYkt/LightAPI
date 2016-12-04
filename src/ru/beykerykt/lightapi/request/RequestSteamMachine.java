/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2016
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
 */
package ru.beykerykt.lightapi.request;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ru.beykerykt.lightapi.chunks.ChunkCache;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.server.ServerModManager;

public class RequestSteamMachine implements Runnable {

	private boolean isStarted;
	private boolean needRequestUpdate;
	private boolean needChunkUpdate;
	protected CopyOnWriteArrayList<DataRequest> REQUEST_QUEUE = new CopyOnWriteArrayList<DataRequest>();
	protected int maxIterationsPerTick;
	protected int iteratorCount;

	// THREADS
	private ScheduledFuture<?> sch;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public void start(int ticks, int maxIterationsPerTick) {
		if (!isStarted) {
			isStarted = true;
			needRequestUpdate = false;
			needChunkUpdate = false;
			this.maxIterationsPerTick = maxIterationsPerTick;
			iteratorCount = 0;
			sch = executor.scheduleWithFixedDelay(this, 0, 50 * ticks, TimeUnit.MILLISECONDS);
		}
	}

	public void shutdown() {
		if (isStarted) {
			isStarted = false;
			needRequestUpdate = false;
			needChunkUpdate = false;
			REQUEST_QUEUE.clear();
			maxIterationsPerTick = 0;
			iteratorCount = 0;
			sch.cancel(false);
		}
	}

	public boolean isStarted() {
		return isStarted;
	}

	public boolean addToQueue(DataRequest request) {
		if (request == null || REQUEST_QUEUE.contains(request)) {
			return false;
		}
		REQUEST_QUEUE.add(request);
		if (!needRequestUpdate) {
			needRequestUpdate = true;
		}
		return true;
	}

	@Override
	public void run() {
		if (!ChunkCache.CHUNK_INFO_QUEUE.isEmpty()) {
			needChunkUpdate = true;
		}

		if (needRequestUpdate) {
			needRequestUpdate = false;
			iteratorCount = 0;
			while (!REQUEST_QUEUE.isEmpty() && iteratorCount < maxIterationsPerTick) {
				DataRequest request = REQUEST_QUEUE.get(0);
				request.process();
				iteratorCount++;
				REQUEST_QUEUE.remove(0);
			}

		}

		if (needChunkUpdate) {
			needChunkUpdate = false;
			while (!ChunkCache.CHUNK_INFO_QUEUE.isEmpty()) {
				ChunkInfo info = ChunkCache.CHUNK_INFO_QUEUE.get(0);
				ServerModManager.getNMSHandler().sendChunkUpdate(info.getWorld(), info.getChunkX(), info.getChunkYHeight(), info.getChunkZ(), info.getReceivers());
				ChunkCache.CHUNK_INFO_QUEUE.remove(0);
			}
		}
	}
}
