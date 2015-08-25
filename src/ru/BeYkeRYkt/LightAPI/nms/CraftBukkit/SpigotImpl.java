package ru.BeYkeRYkt.LightAPI.nms.CraftBukkit;

import ru.BeYkeRYkt.LightAPI.LightAPI;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;

/**
 * For Spigot core
 * 
 * @author DinDev
 *
 */
public class SpigotImpl implements BukkitImpl {

	@Override
	public String getNameImpl() {
		return "Spigot";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.BeYkeRYkt.LightAPI.nms.CraftBukkit." + version + ".NMSHandler";
	}
}
