package ru.BeYkeRYkt.LightAPI;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import ru.BeYkeRYkt.LightAPI.events.DeleteLightEvent;
import ru.BeYkeRYkt.LightAPI.events.SetLightEvent;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;
import ru.BeYkeRYkt.LightAPI.nms.INMSHandler;
import ru.BeYkeRYkt.LightAPI.nms.Cauldron.CauldronImpl;
import ru.BeYkeRYkt.LightAPI.nms.CraftBukkit.CraftBukkitImpl;

public class LightAPI extends JavaPlugin {

    private static INMSHandler nmsHandler;
    private List<BukkitImpl> support; // Maybe others
                                      // platforms for
                                      // Bukkit. Example
                                      // Glowstone
    private static LightAPI plugin;

    @Override
    public void onEnable() {
        LightAPI.plugin = this;
        this.support = new ArrayList<BukkitImpl>();
        support.add(new CraftBukkitImpl());
        support.add(new CauldronImpl());
        // support.add("MCPC-Plus");
        // support.add("Glowstone");
        reloadInitHandler();
    }

    @Override
    public void onDisable() {
        LightAPI.plugin = null;
        this.support.clear();
        LightAPI.nmsHandler.unloadWorlds();
        LightAPI.nmsHandler = null;
    }

    public static LightAPI getInstance() {
        return plugin;
    }
    
    public void addSupportImplement(BukkitImpl impl){
        if(checkSupport(impl.getNameImpl()) == null){
            support.add(impl);
        }
    }
    
    public List<BukkitImpl> getSupportImplements(){
        return support;
    }

    public void reloadInitHandler() {
        if (nmsHandler != null) {
            nmsHandler.unloadWorlds();
            nmsHandler = null;
        }
        String name = getServer().getName();
        if (checkSupport(name) != null) {
            BukkitImpl impl = checkSupport(name);
            try {
                final Class<?> clazz = Class.forName(impl.getPath());
                // Check if we have a NMSHandler class at that location.
                if (INMSHandler.class.isAssignableFrom(clazz)) {
                    LightAPI.nmsHandler = (INMSHandler) clazz.getConstructor().newInstance();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                this.getLogger().severe("Could not find support for this " + name + " version.");
                this.setEnabled(false);
                return;
            }
            this.getLogger().info("Loading support for " + impl.getNameImpl() + " " +Bukkit.getVersion());
            LightAPI.nmsHandler.initWorlds();
        } else {
            this.getLogger().severe("Could not find support for this Bukkit implementation.");
            this.setEnabled(false);
        }
    }

    private BukkitImpl checkSupport(String name) {
        for (BukkitImpl impl : support) {
            if (impl.getNameImpl().startsWith(name)) {
                return impl;
            }
        }
        return null;
    }

    public static void createLight(Location location, int lightlevel) {
        SetLightEvent event = new SetLightEvent(location, lightlevel);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;
        nmsHandler.createLight(event.getLocation(), event.getLightLevel());
    }

    public static void deleteLight(Location location) {
        DeleteLightEvent event = new DeleteLightEvent(location);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;
        nmsHandler.deleteLight(event.getLocation());
    }
}