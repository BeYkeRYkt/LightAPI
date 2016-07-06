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
	private boolean needUpdate;
	protected CopyOnWriteArrayList<DataRequest> REQUEST_QUEUE = new CopyOnWriteArrayList<DataRequest>();
	protected int maxIterationsPerTick;
	protected int iteratorCount;

	// THREADS
	private ScheduledFuture<?> sch;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public void start(int ticks, int maxIterationsPerTick) {
		if (!isStarted) {
			isStarted = true;
			needUpdate = false;
			this.maxIterationsPerTick = maxIterationsPerTick;
			iteratorCount = 0;
			sch = executor.scheduleWithFixedDelay(this, 0, 50 * ticks, TimeUnit.MILLISECONDS);
		}
	}

	public void shutdown() {
		if (isStarted) {
			isStarted = false;
			needUpdate = false;
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
		if (!needUpdate) {
			needUpdate = true;
		}
		return true;
	}

	@Override
	public void run() {
		if (!ChunkCache.CHUNK_INFO_QUEUE.isEmpty()) {
			needUpdate = true;
		}

		if (needUpdate) {
			needUpdate = false;
			iteratorCount = 0;
			while (!REQUEST_QUEUE.isEmpty() && iteratorCount < maxIterationsPerTick) {
				DataRequest request = REQUEST_QUEUE.get(0);
				request.process();
				iteratorCount++;
				REQUEST_QUEUE.remove(0);
			}

			while (!ChunkCache.CHUNK_INFO_QUEUE.isEmpty()) {
				ChunkInfo info = ChunkCache.CHUNK_INFO_QUEUE.get(0);
				ServerModManager.getNMSHandler().sendChunkUpdate(info.getWorld(), info.getChunkX(), info.getChunkYHeight(), info.getChunkZ(), info.getReceivers());
				ChunkCache.CHUNK_INFO_QUEUE.remove(0);
			}
		}

	}
}
