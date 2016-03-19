package ru.beykerykt.lightapi.nms.Cauldron;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.nms.IBukkitImpl;

/**
 * For Cauldron cores
 * 
 * @author DinDev
 *
 */
public class CauldronImpl implements IBukkitImpl {

	@Override
	public String getNameImpl() {
		return "Cauldron-MCPC-Plus";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.beykerykt.lightapi.nms.Cauldron." + version + ".NMSHandler";
	}
}
