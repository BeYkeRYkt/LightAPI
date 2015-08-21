package ru.BeYkeRYkt.LightAPI;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import ru.BeYkeRYkt.LightAPI.events.DeleteLightEvent;
import ru.BeYkeRYkt.LightAPI.events.SetLightEvent;
import ru.BeYkeRYkt.LightAPI.events.UpdateChunkEvent;
import ru.BeYkeRYkt.LightAPI.nms.INMSHandler;
import ru.BeYkeRYkt.LightAPI.utils.LightUpdater;

public class LightRegistry {

	private boolean autoUpdate;
	private int id;
	private Plugin plugin;
	private INMSHandler handler;
	private List<ChunkCoord> chunkList;

	public LightRegistry(INMSHandler handler, Plugin plugin) {
		this.handler = handler;
		this.plugin = plugin;
		this.chunkList = new CopyOnWriteArrayList<ChunkCoord>();
	}

	public void createLight(Location location, int light) {
		SetLightEvent event = new SetLightEvent(location, light);
		getPlugin().getServer().getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;
		handler.createLight(location, light);
	}

	public void deleteLight(Location location) {
		DeleteLightEvent event = new DeleteLightEvent(location);
		getPlugin().getServer().getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;
		handler.deleteLight(location);
	}

	public List<ChunkCoord> collectChunks(Location loc) {
		List<ChunkCoord> list = handler.collectChunks(loc);
		for (ChunkCoord cCoord : list) {
			if (ChunkCache.CHUNK_COORD_CACHE.contains(cCoord)) {
				list.remove(cCoord);
			} else {
				ChunkCache.CHUNK_COORD_CACHE.add(cCoord);
				getChunkCoordsList().add(cCoord);
			}
		}
		return list;
	}

	public void updateChunks(List<ChunkCoord> list) {
		for (ChunkCoord cCoord : list) {
			updateChunk(cCoord);
		}
	}

	public void updateChunk(ChunkCoord cCoord) {
		UpdateChunkEvent event = new UpdateChunkEvent(cCoord);
		getPlugin().getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		handler.updateChunk(event.getChunkCoord());

		if (getChunkCoordsList().contains(cCoord)) {
			getChunkCoordsList().remove(cCoord);
		}

		if (ChunkCache.CHUNK_COORD_CACHE.contains(cCoord)) {
			ChunkCache.CHUNK_COORD_CACHE.remove(cCoord);
		}
	}

	public void sendChunkChanges() {
		updateChunks(getChunkCoordsList());
	}

	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	public void startAutoUpdate(int delay) {
		if (!isAutoUpdate()) {
			autoUpdate = true;
			id = getPlugin().getServer().getScheduler().runTaskTimer(getPlugin(), new LightUpdater(this), 0, delay).getTaskId();
		}
	}

	public void stopAutoUpdate() {
		if (isAutoUpdate()) {
			autoUpdate = false;
			getPlugin().getServer().getScheduler().cancelTask(id);
			id = 0;
		}
	}

	public Plugin getPlugin() {
		return plugin;
	}

	public List<ChunkCoord> getChunkCoordsList() {
		return chunkList;
	}
}
