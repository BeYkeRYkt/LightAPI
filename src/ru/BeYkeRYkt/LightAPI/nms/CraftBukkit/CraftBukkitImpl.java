package ru.BeYkeRYkt.LightAPI.nms.CraftBukkit;

import ru.BeYkeRYkt.LightAPI.LightAPI;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;

/**
 * For CraftBukkit/Spigot cores
 * 
 * @author DinDev
 *
 */
public class CraftBukkitImpl implements BukkitImpl {

    @Override
    public String getNameImpl() {
        return "CraftBukkit";
    }

    @Override
    public String getPath() {
        String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        return "ru.BeYkeRYkt.LightAPI.nms.CraftBukkit." + version + ".LightRegistry";
    }

}