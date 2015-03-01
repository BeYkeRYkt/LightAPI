package ru.BeYkeRYkt.LightAPI.nms.Cauldron;

import ru.BeYkeRYkt.LightAPI.LightAPI;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;

/**
 * For Cauldron cores
 * 
 * @author DinDev
 *
 */
public class CauldronImpl implements BukkitImpl {

    @Override
    public String getNameImpl() {
        return "Cauldron-MCPC-Plus";
    }

    @Override
    public String getPath() {
        String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        return "ru.BeYkeRYkt.LightAPI.nms.Cauldron." + version + ".NMSHandler";
    }

}