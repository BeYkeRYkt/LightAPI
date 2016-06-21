package ru.beykerykt.lightapi;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import ru.beykerykt.lightapi.chunks.ChunkCache;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.events.DeleteLightEvent;
import ru.beykerykt.lightapi.events.SetLightEvent;
import ru.beykerykt.lightapi.events.UpdateChunkEvent;
import ru.beykerykt.lightapi.request.DataRequest;
import ru.beykerykt.lightapi.request.RequestSteamMachine;
import ru.beykerykt.lightapi.server.ServerModInfo;
import ru.beykerykt.lightapi.server.ServerModManager;
import ru.beykerykt.lightapi.server.nms.craftbukkit.CraftBukkit_v1_10_R1;
import ru.beykerykt.lightapi.server.nms.craftbukkit.CraftBukkit_v1_8_R3;
import ru.beykerykt.lightapi.server.nms.craftbukkit.CraftBukkit_v1_9_R1;
import ru.beykerykt.lightapi.server.nms.craftbukkit.CraftBukkit_v1_9_R2;
import ru.beykerykt.lightapi.server.nms.paperspigot.PaperSpigot_v1_8_R3;
import ru.beykerykt.lightapi.updater.Response;
import ru.beykerykt.lightapi.updater.Updater;
import ru.beykerykt.lightapi.updater.Version;
import ru.beykerykt.lightapi.utils.Metrics;

public class LightAPI extends JavaPlugin implements Listener {

	private static LightAPI plugin;
	private static RequestSteamMachine machine;
	private int configVer = 2;
	private int update_delay_ticks;
	private int max_iterations_per_tick;
	private boolean enableUpdater;

	@SuppressWarnings("static-access")
	@Override
	public void onLoad() {
		this.plugin = this;
		this.machine = new RequestSteamMachine();

		ServerModInfo craftbukkit = new ServerModInfo("CraftBukkit");
		craftbukkit.getVersions().put("v1_8_R3", CraftBukkit_v1_8_R3.class);
		craftbukkit.getVersions().put("v1_9_R1", CraftBukkit_v1_9_R1.class);
		craftbukkit.getVersions().put("v1_9_R2", CraftBukkit_v1_9_R2.class);
		craftbukkit.getVersions().put("v1_10_R1", CraftBukkit_v1_10_R1.class);
		ServerModManager.registerServerMod(craftbukkit);

		ServerModInfo spigot = new ServerModInfo("Spigot");
		spigot.getVersions().put("v1_8_R3", CraftBukkit_v1_8_R3.class);
		spigot.getVersions().put("v1_9_R1", CraftBukkit_v1_9_R1.class);
		spigot.getVersions().put("v1_9_R2", CraftBukkit_v1_9_R2.class);
		spigot.getVersions().put("v1_10_R1", CraftBukkit_v1_10_R1.class);
		ServerModManager.registerServerMod(spigot);

		ServerModInfo paperspigot = new ServerModInfo("PaperSpigot");
		paperspigot.getVersions().put("v1_8_R3", PaperSpigot_v1_8_R3.class);
		ServerModManager.registerServerMod(paperspigot);

		ServerModInfo paper = new ServerModInfo("Paper");
		paper.getVersions().put("v1_9_R1", CraftBukkit_v1_9_R1.class);
		paper.getVersions().put("v1_9_R2", CraftBukkit_v1_9_R2.class);
		paper.getVersions().put("v1_10_R1", CraftBukkit_v1_10_R1.class);
		ServerModManager.registerServerMod(paper);

		ServerModInfo tacospigot = new ServerModInfo("TacoSpigot");
		// tacospigot.getVersions().put("v1_8_R3", PaperSpigot_v1_8_R3.class); - call errors with anti-xray - obfuscate
		tacospigot.getVersions().put("v1_9_R1", CraftBukkit_v1_9_R1.class);
		tacospigot.getVersions().put("v1_9_R2", CraftBukkit_v1_9_R2.class);
		tacospigot.getVersions().put("v1_10_R1", CraftBukkit_v1_10_R1.class);
		ServerModManager.registerServerMod(tacospigot);
	}

	@Override
	public void onEnable() {
		// Config
		try {
			FileConfiguration fc = getConfig();
			File file = new File(getDataFolder(), "config.yml");
			if (file.exists()) {
				if (fc.getInt("version") < configVer) {
					file.delete(); // got a better idea?
					generateConfig(file);
				}
			} else {
				generateConfig(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Init config
		this.enableUpdater = getConfig().getBoolean("enable-updater");
		this.update_delay_ticks = getConfig().getInt("update-delay-ticks");
		this.max_iterations_per_tick = getConfig().getInt("max-iterations-per-tick");

		// init nms
		ServerModManager.init();
		machine.start(2, 40); // TEST
		getServer().getPluginManager().registerEvents(this, this);

		if (enableUpdater) {
			// Starting updater
			Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
				@Override
				public void run() {
					Version version = Version.parse(getDescription().getVersion());
					String repo = "BeYkeRYkt/LightAPI";

					Updater updater;
					try {
						updater = new Updater(version, repo);

						Response response = updater.getResult();
						if (response == Response.SUCCESS) {
							log(Bukkit.getConsoleSender(), ChatColor.GREEN + "New update is available: " + ChatColor.YELLOW + updater.getLatestVersion() + ChatColor.GREEN + "!");
							log(Bukkit.getConsoleSender(), ChatColor.GREEN + "Changes: ");
							getServer().getConsoleSender().sendMessage(updater.getChanges());// for normal view
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 60);
		}

		// init metrics
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			// nothing...
		}
	}

	@Override
	public void onDisable() {
		machine.shutdown();
		ChunkCache.CHUNK_INFO_QUEUE.clear();
	}

	public void log(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.YELLOW + "<Light" + ChatColor.RED + "API" + ChatColor.YELLOW + ">: " + ChatColor.WHITE + message);
	}

	public static LightAPI getInstance() {
		return plugin;
	}

	public static boolean createLight(Location location, int lightlevel, boolean async) {
		return createLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), lightlevel, async);
	}

	public static boolean createLight(final World world, final int x, final int y, final int z, final int lightlevel, boolean async) {
		if (getInstance().isEnabled()) {
			final SetLightEvent event = new SetLightEvent(world, x, y, z, lightlevel, async);
			Bukkit.getPluginManager().callEvent(event);

			Block adjacent = getAdjacentAirBlock(world.getBlockAt(event.getX(), event.getY(), event.getZ()));
			final int lx = adjacent.getX();
			final int ly = adjacent.getY();
			final int lz = adjacent.getZ();

			if (event.isAsync()) {
				machine.addToQueue(new DataRequest() {
					@Override
					public void process() {
						ServerModManager.getNMSHandler().createLight(event.getWorld(), event.getX(), event.getY(), event.getZ(), event.getLightLevel());
						ServerModManager.getNMSHandler().recalculateLight(event.getWorld(), lx, ly, lz);
					}
				});
				return true;
			}
			ServerModManager.getNMSHandler().createLight(event.getWorld(), event.getX(), event.getY(), event.getZ(), event.getLightLevel());
			ServerModManager.getNMSHandler().recalculateLight(event.getWorld(), lx, ly, lz);
			return true;
		}
		return false;
	}

	public static boolean deleteLight(Location location, boolean async) {
		return deleteLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), async);
	}

	public static boolean deleteLight(final World world, final int x, final int y, final int z, boolean async) {
		if (getInstance().isEnabled()) {
			final DeleteLightEvent event = new DeleteLightEvent(world, x, y, z, async);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isAsync()) {
				machine.addToQueue(new DataRequest() {
					@Override
					public void process() {
						ServerModManager.getNMSHandler().deleteLight(event.getWorld(), event.getX(), event.getY(), event.getZ());
					}
				});
				return true;
			}
			ServerModManager.getNMSHandler().deleteLight(event.getWorld(), event.getX(), event.getY(), event.getZ());
			return true;
		}
		return false;
	}

	public static List<ChunkInfo> collectChunks(Location location) {
		if (getInstance().isEnabled()) {
			return ServerModManager.getNMSHandler().collectChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		}
		return null;
	}

	public static List<ChunkInfo> collectChunks(final World world, final int x, final int y, final int z) {
		if (getInstance().isEnabled()) {
			return ServerModManager.getNMSHandler().collectChunks(world, x, y, z);
		}
		return null;
	}

	@Deprecated
	public static boolean updateChunks(ChunkInfo info) {
		return updateChunk(info);
	}

	public static boolean updateChunk(ChunkInfo info) {
		if (getInstance().isEnabled()) {
			UpdateChunkEvent event = new UpdateChunkEvent(info);
			Bukkit.getPluginManager().callEvent(event);
			if (ChunkCache.CHUNK_INFO_QUEUE.contains(event.getChunkInfo())) {
				int index = ChunkCache.CHUNK_INFO_QUEUE.indexOf(event.getChunkInfo());
				ChunkInfo previous = ChunkCache.CHUNK_INFO_QUEUE.get(index);
				if (previous.getChunkYHeight() > event.getChunkInfo().getChunkYHeight()) {
					event.getChunkInfo().setChunkYHeight(previous.getChunkYHeight());
				}
				ChunkCache.CHUNK_INFO_QUEUE.remove(index);
			}
			ChunkCache.CHUNK_INFO_QUEUE.add(event.getChunkInfo());
			return true;
		}
		return false;
	}

	public static boolean updateChunks(Location location, Collection<? extends Player> players) {
		return updateChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), players);
	}

	public static boolean updateChunks(World world, int x, int y, int z, Collection<? extends Player> players) {
		if (getInstance().isEnabled()) {
			for (ChunkInfo info : collectChunks(world, x >> 4, y, z >> 4)) {
				updateChunk(info);
			}
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

	private void generateConfig(File file) {
		FileConfiguration fc = getConfig();
		if (!file.exists()) {
			fc.options().header("LightAPI v" + getDescription().getVersion() + " Configuration" + "\nby BeYkeRYkt");
			fc.set("version", configVer);
			fc.set("enable-updater", true);
			fc.set("update-delay-ticks", 2);
			fc.set("max-iterations-per-tick", 40);
			saveConfig();
		}
	}

	public int getUpdateDelayTicks() {
		return update_delay_ticks;
	}

	public void setUpdateDelayTicks(int update_delay_ticks) {
		this.update_delay_ticks = update_delay_ticks;
	}

	public int getMaxIterationsPerTick() {
		return max_iterations_per_tick;
	}

	public void setMaxIterationsPerTick(int max_iterations_per_tick) {
		this.max_iterations_per_tick = max_iterations_per_tick;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		if (enableUpdater) {
			if (player.hasPermission("lightapi.updater")) {
				Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {

					@Override
					public void run() {
						Version version = Version.parse(getDescription().getVersion());
						String repo = "BeYkeRYkt/LightAPI";

						Updater updater;
						try {
							updater = new Updater(version, repo);

							Response response = updater.getResult();
							if (response == Response.SUCCESS) {
								log(player, ChatColor.GREEN + "New update is available: " + ChatColor.YELLOW + updater.getLatestVersion() + ChatColor.GREEN + "!");
								log(player, ChatColor.GREEN + "Changes: ");
								player.sendMessage(updater.getChanges());// for normal view
								// player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, 60);
			}
		}
	}
}
