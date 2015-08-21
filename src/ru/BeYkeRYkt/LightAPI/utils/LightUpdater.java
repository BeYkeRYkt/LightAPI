package ru.BeYkeRYkt.LightAPI.utils;

import ru.BeYkeRYkt.LightAPI.LightRegistry;

public class LightUpdater implements Runnable {

	private LightRegistry registry;

	public LightUpdater(LightRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void run() {
		registry.sendChunkChanges();
	}

}
