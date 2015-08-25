package ru.BeYkeRYkt.LightAPI.nms.PaperSpigot;

import ru.BeYkeRYkt.LightAPI.LightAPI;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;

/**
 * 
 * For PaperSpigot core. Implementation by DenAbr
 * 
 * @author DinDev
 *
 */
public class PaperSpigotImpl implements BukkitImpl {

	@Override
	public String getNameImpl() {
		return "PaperSpigot";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.BeYkeRYkt.LightAPI.nms.PaperSpigot." + version + ".NMSHandler";
	}
}
