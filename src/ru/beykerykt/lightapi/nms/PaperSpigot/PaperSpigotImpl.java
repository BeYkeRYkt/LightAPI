package ru.beykerykt.lightapi.nms.PaperSpigot;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.nms.IBukkitImpl;

/**
 * 
 * For PaperSpigot core. Implementation by DenAbr
 * 
 * @author DinDev
 *
 */
public class PaperSpigotImpl implements IBukkitImpl {

	@Override
	public String getNameImpl() {
		return "PaperSpigot";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.beykerykt.lightapi.nms.PaperSpigot." + version + ".NMSHandler";
	}
}
