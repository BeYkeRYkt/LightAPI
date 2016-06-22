package ru.beykerykt.lightapi.request;

import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.chunks.ChunkCache;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.server.ServerModManager;

public class RequestSteamMachine implements Runnable {

	private boolean isStarted;
	private boolean needUpdate;
	private int id;
	protected CopyOnWriteArrayList<DataRequest> REQUEST_QUEUE = new CopyOnWriteArrayList<DataRequest>();
	protected int maxIterationsPerTick;
	protected int iteratorCount;

	public void start(int ticks, int maxIterationsPerTick) {
		if (!isStarted) {
			this.maxIterationsPerTick = maxIterationsPerTick;
			id = Bukkit.getScheduler().runTaskTimerAsynchronously(LightAPI.getInstance(), this, 0, ticks).getTaskId();
			isStarted = true;
			needUpdate = false;
		}
	}

	public void shutdown() {
		if (isStarted) {
			REQUEST_QUEUE.clear();
			this.maxIterationsPerTick = 0;
			Bukkit.getScheduler().cancelTask(id);
			isStarted = false;
			needUpdate = false;
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
		if (needUpdate) {
			needUpdate = false;
			iteratorCount = 0;

			while (!REQUEST_QUEUE.isEmpty() && iteratorCount < maxIterationsPerTick) {
				DataRequest request = REQUEST_QUEUE.get(0);
				request.process();
				iteratorCount++;
				REQUEST_QUEUE.remove(0);
			}
		}

		while (!ChunkCache.CHUNK_INFO_QUEUE.isEmpty()) {
			ChunkInfo info = ChunkCache.CHUNK_INFO_QUEUE.get(0);
			ServerModManager.getNMSHandler().sendChunkUpdate(info.getWorld(), info.getChunkX(), info.getChunkYHeight(), info.getChunkZ(), info.getReceivers());
			ChunkCache.CHUNK_INFO_QUEUE.remove(0);
		}
	}
}
