package ru.beykerykt.lightapi.nms.CraftBukkit;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.nms.IBukkitImpl;

/**
 * For Spigot core
 * 
 * @author DinDev
 *
 */
public class SpigotImpl implements IBukkitImpl {

	@Override
	public String getNameImpl() {
		return "Spigot";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.beykerykt.lightapi.nms.CraftBukkit." + version + ".NMSHandler";
	}
}
