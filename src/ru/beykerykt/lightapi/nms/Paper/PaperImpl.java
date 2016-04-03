package ru.beykerykt.lightapi.nms.Paper;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.nms.IBukkitImpl;

/**
 * 
 * For Paper core (1.9+)
 * 
 * @author DinDev
 *
 */
public class PaperImpl implements IBukkitImpl {

	@Override
	public String getNameImpl() {
		return "Paper";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.beykerykt.lightapi.nms.Paper." + version + ".NMSHandler";
	}
}
