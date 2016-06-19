package ru.beykerykt.lightapi.server.nms;

import java.util.Collection;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.beykerykt.lightapi.chunks.ChunkInfo;

public interface INMSHandler {

	// Lights...
	public void createLight(World world, int x, int y, int z, int light);

	public void deleteLight(World world, int x, int y, int z);

	public void recalculateLight(World world, int x, int y, int z);

	// Chunks...
	public List<ChunkInfo> collectChunks(World world, int chunkX, int chunkZ);

	public void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players);

	public void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player);

	public void sendChunkUpdate(World world, int x, int y, int z, Collection<? extends Player> players);

	public void sendChunkUpdate(World world, int x, int y, int z, Player player);
}
