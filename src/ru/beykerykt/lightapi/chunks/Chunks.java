package ru.beykerykt.lightapi.chunks;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.light.LightDataRequest;
import ru.beykerykt.lightapi.nms.NMSHelper;

public class Chunks {

	private static ChunkRequestMachine machine;

	public static void init() {
		if (machine == null || !machine.isStarted()) {
			machine = new ChunkRequestMachine();
			machine.start(LightAPI.getInstance().getChunkUpdateDelayTicks(), LightAPI.getInstance().getChunkMaxIterationsPerTick());
		}
	}

	public static void shutdown() {
		if (machine != null || machine.isStarted()) {
			machine.shutdown();
		}
	}

	public static boolean addChunkToQueue(LightDataRequest request) {
		return machine.addToQueue(request);
	}

	public static List<ChunkInfo> collectModifiedChunks(Location location) {
		return NMSHelper.collectChunks(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());
	}

	public static void sendChunkUpdate(ChunkInfo info) {
		NMSHelper.sendChunkUpdate(info.getWorld(), info.getX(), info.getZ(), info.getReceivers());
	}

	public static void sendChunkUpdate(Player player, ChunkInfo info) {
		NMSHelper.sendChunkUpdate(info.getWorld(), info.getX(), info.getZ(), player);
	}
}
