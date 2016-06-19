package ru.beykerykt.lightapi.request;

import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;

import ru.beykerykt.lightapi.LightAPI;

public class RequestSteamMachine implements Runnable {

	private boolean isStarted;
	private boolean needUpdate;
	private int id;
	protected CopyOnWriteArrayList<DataRequest> REQUEST_QUEUE = new CopyOnWriteArrayList<DataRequest>();
	protected int maxIterationsPerTick;
	protected int iteratorCount;
	protected int waitingTicks;

	public void start(int ticks, int maxIterationsPerTick, int waitingTicks) {
		if (!isStarted) {
			this.maxIterationsPerTick = maxIterationsPerTick;
			this.waitingTicks = waitingTicks;
			id = Bukkit.getScheduler().runTaskTimerAsynchronously(LightAPI.getInstance(), this, 0, ticks).getTaskId();
			isStarted = true;
			needUpdate = false;
		}
	}

	public void shutdown() {
		if (isStarted) {
			REQUEST_QUEUE.clear();
			this.maxIterationsPerTick = 0;
			this.waitingTicks = 0;
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
				final DataRequest request = REQUEST_QUEUE.get(0);
				if (request != null && !request.process()) {
					Bukkit.getScheduler().runTaskLater(LightAPI.getInstance(), new Runnable() {
						@Override
						public void run() {
							request.process();
						}
					}, waitingTicks);
				}
				iteratorCount++;
				REQUEST_QUEUE.remove(0);
			}
		}
	}
}
