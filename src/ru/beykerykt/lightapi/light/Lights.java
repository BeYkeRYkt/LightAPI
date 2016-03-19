package ru.beykerykt.lightapi.light;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.light.LightDataRequest.RequestType;
import ru.beykerykt.lightapi.nms.NMSHelper;

public class Lights {

	private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	private static int maxlevel = 15;
	private static LightRequestMachine machine;

	public static void init() {
		if (machine == null || !machine.isStarted()) {
			machine = new LightRequestMachine();
			machine.start(LightAPI.getInstance().getLigthingUpdateDelayTicks(), LightAPI.getInstance().getLightingMaxIterationsPerTick());
		}
	}

	public static void shutdown() {
		if (machine != null || machine.isStarted()) {
			machine.shutdown();
		}
	}

	public static LightDataRequest createLight(World world, int x, int y, int z, int light, boolean async) {
		if (light > 15) {
			light = maxlevel;
		}

		Block adjacent = getAdjacentAirBlock(world.getBlockAt(x, y, z));
		int lx = adjacent.getX();
		int ly = adjacent.getY();
		int lz = adjacent.getZ();
		if (async) {
			LightDataRequest request = new LightDataRequest(world, x, y, z, light, RequestType.CREATE);
			machine.addToQueue(request);
			recalculateLight(world, lx, ly, lz, true);
			return request;
		}
		NMSHelper.createLight(world, x, y, z, light);
		NMSHelper.recalculateLight(world, lx, ly, lz);
		return null;
	}

	public static LightDataRequest createLight(Location location, int light, boolean async) {
		return createLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), light, async);
	}

	public static LightDataRequest deleteLight(World world, int x, int y, int z, boolean async) {
		if (async) {
			LightDataRequest request = new LightDataRequest(world, x, y, z, 0, RequestType.DELETE);
			machine.addToQueue(request);
			return request;
		}
		NMSHelper.deleteLight(world, x, y, z);
		return null;
	}

	public static LightDataRequest deleteLight(Location location, boolean async) {
		return deleteLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), async);
	}

	public static LightDataRequest recalculateLight(World world, int x, int y, int z, boolean async) {
		if (async) {
			LightDataRequest request = new LightDataRequest(world, x, y, z, 0, RequestType.RECALCULATE);
			machine.addToQueue(request);
			return request;
		}
		NMSHelper.recalculateLight(world, x, y, z);
		return null;
	}

	public static LightDataRequest recalculateLight(Location location, boolean async) {
		return recalculateLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), async);
	}

	public static Block getAdjacentAirBlock(Block block) {
		for (BlockFace face : SIDES) {
			if (block.getY() == 0x0 && face == BlockFace.DOWN)
				continue;
			if (block.getY() == 0xFF && face == BlockFace.UP)
				continue;

			Block candidate = block.getRelative(face);

			if (candidate.getType().isTransparent()) {
				return candidate;
			}
		}
		return block;
	}
}
