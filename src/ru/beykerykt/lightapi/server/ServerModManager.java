package ru.beykerykt.lightapi.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.server.nms.INMSHandler;

public class ServerModManager {

	// private static List<ServerModInfo> supportImpl = new CopyOnWriteArrayList<>();
	private static Map<String, ServerModInfo> supportImpl;
	private static INMSHandler handler;

	public static void init() {
		if (supportImpl == null) {
			supportImpl = new ConcurrentHashMap<String, ServerModInfo>();
		}

		if (handler != null) {
			handler = null;
		}

		// Init handler...
		// String version = Bukkit.getVersion();
		String modName = Bukkit.getVersion().split("-")[1];
		if(!supportImpl.containsKey(modName)){
			modName = Bukkit.getName();
		}
		ServerModInfo impl = supportImpl.get(modName);
		if (impl != null) {
			try {
				String folder_version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
				if (impl.getVersions().containsKey(folder_version)) {
					final Class<INMSHandler> clazz = impl.getVersions().get(folder_version);
					// Check if we have a NMSHandler class at that location.
					if (INMSHandler.class.isAssignableFrom(clazz)) {
						handler = clazz.getConstructor().newInstance();
					}
				}
			} catch (final Exception e) {
				// e.printStackTrace();
				LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Could not find support for this " + Bukkit.getVersion() + " version.");
			}
			LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Loading support for " + impl.getModName() + " " + Bukkit.getVersion());
		} else {
			LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Could not find support for this Bukkit implementation.");
		}
	}

	public static boolean registerServerMod(ServerModInfo info) {
		if (supportImpl.containsKey(info.getModName())) {
			return false;
		}
		supportImpl.put(info.getModName(), info);
		return true;
	}

	public static boolean unregisterServerMod(String modName) {
		if (supportImpl.containsKey(modName)) {
			return false;
		}
		supportImpl.remove(modName);
		return true;
	}

	public static INMSHandler getNMSHandler() {
		return handler;
	}
}
