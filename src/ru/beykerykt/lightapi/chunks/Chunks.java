package ru.beykerykt.lightapi.chunks;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.nms.NMSHelper;
import ru.beykerykt.lightapi.request.DataRequest;

public class Chunks {

	private static ChunkRequestMachine machine;

	public static void init() {
		if (machine == null || !machine.isStarted()) {
			machine = new ChunkRequestMachine();
			machine.start(LightAPI.getInstance().getChunkUpdateDelayTicks(), LightAPI.getInstance().getChunkMaxIterationsPerTick(), LightAPI.getInstance().getChunkWaitingDelayTicks());
		}
	}

	public static void shutdown() {
		if (machine != null && machine.isStarted()) {
			machine.shutdown();
		}
	}

	public static boolean addChunkToQueue(DataRequest request) {
		return machine.addToQueue(request);
	}

	public static List<ChunkInfo> collectModifiedChunks(Location location) {
		return NMSHelper.collectChunks(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());
	}

	public static void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		NMSHelper.sendChunkUpdate(world, chunkX, chunkZ, players);
	}

	public static void sendChunkUpdate(World world, int chunkX, int chunkZ) {
		NMSHelper.sendChunkUpdate(world, chunkX, chunkZ, world.getPlayers());
	}

	public static void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player) {
		NMSHelper.sendChunkUpdate(world, chunkX, chunkZ, player);
	}

	public static void sendChunkUpdate(ChunkInfo info) {
		sendChunkUpdate(info.getWorld(), info.getX(), info.getZ(), info.getReceivers());
	}

	public static void sendChunkUpdate(ChunkInfo info, Player player) {
		sendChunkUpdate(info.getWorld(), info.getX(), info.getZ(), player);
	}

}
