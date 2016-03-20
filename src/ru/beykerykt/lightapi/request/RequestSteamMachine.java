package ru.beykerykt.lightapi.request;

import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;

import ru.beykerykt.lightapi.LightAPI;

public abstract class RequestSteamMachine implements Runnable {

	private boolean isStarted;
	private int id;
	protected CopyOnWriteArrayList<DataRequest> REQUEST_QUEUE;
	protected int maxIterationsPerTick = 20;
	protected int iteratorCount = 0;

	public void start(int ticks, int maxIterationsPerTick) {
		if (!isStarted) {
			REQUEST_QUEUE = new CopyOnWriteArrayList<DataRequest>();
			this.maxIterationsPerTick = maxIterationsPerTick;
			id = Bukkit.getScheduler().runTaskTimerAsynchronously(LightAPI.getInstance(), this, 0, ticks).getTaskId();
			isStarted = true;
		}
	}

	public void shutdown() {
		if (isStarted) {
			REQUEST_QUEUE.clear();
			REQUEST_QUEUE = null;
			Bukkit.getScheduler().cancelTask(id);
			isStarted = false;
		}
	}

	public boolean isStarted() {
		return isStarted;
	}

	public synchronized boolean addToQueue(DataRequest request) {
		if (request == null && REQUEST_QUEUE.contains(request)) {
			return false;
		}
		REQUEST_QUEUE.add(request);
		return true;
	}

	@Override
	public void run() {
		iteratorCount = 0;
		while (!REQUEST_QUEUE.isEmpty() && iteratorCount < maxIterationsPerTick) {
			DataRequest request = REQUEST_QUEUE.get(0);
			if (process(request)) {
				REQUEST_QUEUE.remove(0);
			}
			iteratorCount++;
		}
	}

	public abstract boolean process(DataRequest request);
}
