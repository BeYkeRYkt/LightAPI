package ru.beykerykt.lightapi.nms;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.nms.Cauldron.CauldronImpl;
import ru.beykerykt.lightapi.nms.CraftBukkit.CraftBukkitImpl;
import ru.beykerykt.lightapi.nms.CraftBukkit.SpigotImpl;
import ru.beykerykt.lightapi.nms.Paper.PaperImpl;
import ru.beykerykt.lightapi.nms.PaperSpigot.PaperSpigotImpl;

public class NMSHelper {

	private static INMSHandler handler;
	private static List<IBukkitImpl> supportImpl; // Maybe others platforms for Bukkit. Example Glowstone...

	public static void init() {
		if (supportImpl == null) {
			supportImpl = new CopyOnWriteArrayList<IBukkitImpl>();
		}

		if (handler != null) {
			handler = null;
		}

		initDefaultImpl();

		// Init handler...
		String version = Bukkit.getVersion();
		IBukkitImpl impl = checkSupport(version);
		if (impl != null) {
			try {
				final Class<?> clazz = Class.forName(impl.getPath());
				// Check if we have a NMSHandler class at that location.
				if (INMSHandler.class.isAssignableFrom(clazz)) {
					handler = (INMSHandler) clazz.getConstructor().newInstance();
				}
			} catch (final Exception e) {
				// e.printStackTrace();
				LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Could not find support for this " + version + " version.");
			}
			LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Loading support for " + impl.getNameImpl() + " " + Bukkit.getVersion());
		} else {
			LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Could not find support for this Bukkit implementation.");
		}
	}

	private static void initDefaultImpl() {
		addSupportImplement(new CauldronImpl()); // Cauldron
		addSupportImplement(new PaperSpigotImpl()); // PaperSpigot
		addSupportImplement(new PaperImpl()); // New PaperSpigot
		addSupportImplement(new SpigotImpl()); // Spigot
		addSupportImplement(new CraftBukkitImpl()); // CraftBukkit
	}

	public static boolean isInitialized() {
		return handler != null;
	}

	public static void addSupportImplement(IBukkitImpl impl) {
		if (checkSupport(impl.getNameImpl()) == null) {
			supportImpl.add(impl);
		}
	}

	public List<IBukkitImpl> getSupportImplements() {
		return supportImpl;
	}

	private static IBukkitImpl checkSupport(String name) {
		for (IBukkitImpl impl : supportImpl) {
			if (name.contains(impl.getNameImpl())) {
				return impl;
			}
		}
		return null;
	}

	// Lights...
	public static void createLight(World world, int x, int y, int z, int light) {
		if (isInitialized()) {
			handler.createLight(world, x, y, z, light);
		}
	}

	public static void deleteLight(World world, int x, int y, int z) {
		if (isInitialized()) {
			handler.deleteLight(world, x, y, z);
		}
	}

	public static void recalculateLight(World world, int x, int y, int z) {
		if (isInitialized()) {
			handler.recalculateLight(world, x, y, z);
		}
	}

	// Chunks...
	public static List<ChunkInfo> collectChunks(World world, int chunkX, int chunkZ) {
		if (isInitialized()) {
			return handler.collectChunks(world, chunkX, chunkZ);
		}
		return null;
	}

	public static void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		if (isInitialized()) {
			handler.sendChunkUpdate(world, chunkX, chunkZ, players);
		}
	}

	public static void sendChunkUpdate(World world, int chunkX, int chunkZ) {
		sendChunkUpdate(world, chunkX, chunkZ, world.getPlayers());
	}

	public static void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player) {
		if (isInitialized()) {
			handler.sendChunkUpdate(world, chunkX, chunkZ, player);
		}
	}
}
