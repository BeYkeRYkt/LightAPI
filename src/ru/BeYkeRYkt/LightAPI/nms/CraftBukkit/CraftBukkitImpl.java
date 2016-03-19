package ru.beykerykt.lightapi.nms.CraftBukkit;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.nms.IBukkitImpl;

/**
 * For CraftBukkit core
 * 
 * @author DinDev
 *
 */
public class CraftBukkitImpl implements IBukkitImpl {

	@Override
	public String getNameImpl() {
		return "CraftBukkit";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.beykerykt.lightapi.nms.CraftBukkit." + version + ".NMSHandler";
	}
}
