package ru.beykerykt.lightapi;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import ru.beykerykt.lightapi.chunks.ChunkCache;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.request.DataRequest;
import ru.beykerykt.lightapi.request.RequestSteamMachine;
import ru.beykerykt.lightapi.server.ServerModInfo;
import ru.beykerykt.lightapi.server.ServerModManager;
import ru.beykerykt.lightapi.server.nms.craftbukkit.NMSHandler_v1_9_R2;
import ru.beykerykt.lightapi.utils.Metrics;

public class LightAPI extends JavaPlugin {

	private static LightAPI plugin;
	private static RequestSteamMachine machine;

	@SuppressWarnings("static-access")
	@Override
	public void onLoad() {
		this.plugin = this;
		this.machine = new RequestSteamMachine();

		ServerModInfo craftbukkit = new ServerModInfo("CraftBukkit");
		craftbukkit.getVersions().put("v1_9_R2", NMSHandler_v1_9_R2.class);
		// craftbukkit.getVersions().put("v1_7_R4", NMSHandler.class);
		ServerModManager.registerServerMod(craftbukkit);

		ServerModInfo spigot = new ServerModInfo("Spigot");
		spigot.getVersions().put("v1_9_R2", NMSHandler_v1_9_R2.class);
		ServerModManager.registerServerMod(spigot);

		ServerModInfo paperspigot = new ServerModInfo("PaperSpigot");
		ServerModManager.registerServerMod(paperspigot);

		ServerModInfo paper = new ServerModInfo("Paper");
		ServerModManager.registerServerMod(paper);

		ServerModInfo tacospigot = new ServerModInfo("TacoSpigot");
		ServerModManager.registerServerMod(tacospigot);
	}

	@Override
	public void onEnable() {
		ServerModManager.init();
		machine.start(2, 40); // TEST

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			// nothing...
		}
	}

	public void log(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.YELLOW + "<Light" + ChatColor.RED + "API" + ChatColor.YELLOW + ">: " + ChatColor.WHITE + message);
	}

	public static LightAPI getInstance() {
		return plugin;
	}

	public static boolean createLight(final World world, final int x, final int y, final int z, final int lightlevel, boolean async) {
		if (getInstance().isEnabled()) {
			Block adjacent = getAdjacentAirBlock(world.getBlockAt(x, y, z));
			final int lx = adjacent.getX();
			final int ly = adjacent.getY();
			final int lz = adjacent.getZ();

			if (async) {
				machine.addToQueue(new DataRequest() {
					@Override
					public void process() {
						ServerModManager.getNMSHandler().createLight(world, x, y, z, lightlevel);
						ServerModManager.getNMSHandler().recalculateLight(world, lx, ly, lz);
					}
				});
				return true;
			}
			ServerModManager.getNMSHandler().createLight(world, x, y, z, lightlevel);
			ServerModManager.getNMSHandler().recalculateLight(world, lx, ly, lz);
			return true;
		}
		return false;
	}

	public static boolean deleteLight(final World world, final int x, final int y, final int z, boolean async) {
		if (getInstance().isEnabled()) {
			if (async) {
				machine.addToQueue(new DataRequest() {
					@Override
					public void process() {
						ServerModManager.getNMSHandler().deleteLight(world, x, y, z);
					}
				});
				return true;
			}
			ServerModManager.getNMSHandler().deleteLight(world, x, y, z);
			return true;
		}
		return false;
	}

	public static List<ChunkInfo> collectChunks(final World world, final int x, final int y, final int z) {
		if (getInstance().isEnabled()) {
			return ServerModManager.getNMSHandler().collectChunks(world, x, y, z);
		}
		return null;
	}

	public static boolean updateChunks(ChunkInfo info) {
		if (getInstance().isEnabled()) {
			int maxY = info.getChunkYHeight();
			if (ChunkCache.CHUNK_INFO_QUEUE.contains(info)) {
				int index = ChunkCache.CHUNK_INFO_QUEUE.indexOf(info);
				ChunkInfo previous = ChunkCache.CHUNK_INFO_QUEUE.get(index);
				if (previous.getChunkYHeight() > maxY) {
					maxY = previous.getChunkYHeight();
				}
				ChunkCache.CHUNK_INFO_QUEUE.remove(index);
			}
			ChunkCache.CHUNK_INFO_QUEUE.add(info);
			return true;
		}
		return false;
	}

	public static boolean updateChunks(World world, int x, int y, int z, Collection<? extends Player> players) {
		if (getInstance().isEnabled()) {
			ServerModManager.getNMSHandler().sendChunkUpdate(world, x >> 4, y, z >> 4, players);
			return true;
		}
		return false;
	}

	private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

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
