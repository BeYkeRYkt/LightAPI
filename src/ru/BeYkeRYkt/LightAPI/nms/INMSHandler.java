package ru.BeYkeRYkt.LightAPI.nms;

import java.util.List;

import org.bukkit.Location;

import ru.BeYkeRYkt.LightAPI.ChunkCoord;

public interface INMSHandler {

	public void createLight(Location location, int light);

	public void deleteLight(Location location);

	public List<ChunkCoord> collectChunks(Location loc);

	public void updateChunk(ChunkCoord cCoord);
}
