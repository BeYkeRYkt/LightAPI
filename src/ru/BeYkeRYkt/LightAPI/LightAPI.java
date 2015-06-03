package ru.BeYkeRYkt.LightAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import ru.BeYkeRYkt.LightAPI.albionco.updater.Response;
import ru.BeYkeRYkt.LightAPI.albionco.updater.Updater;
import ru.BeYkeRYkt.LightAPI.albionco.updater.Version;
import ru.BeYkeRYkt.LightAPI.events.DeleteLightEvent;
import ru.BeYkeRYkt.LightAPI.events.SetLightEvent;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;
import ru.BeYkeRYkt.LightAPI.nms.ILightRegistry;
import ru.BeYkeRYkt.LightAPI.nms.Cauldron.CauldronImpl;
import ru.BeYkeRYkt.LightAPI.nms.CraftBukkit.CraftBukkitImpl;

public class LightAPI extends JavaPlugin implements Listener {

    private static ILightRegistry registry;
    private List<BukkitImpl> support; // Maybe others
                                      // platforms for
                                      // Bukkit. Example
                                      // Glowstone
    private static LightAPI plugin;

    @Override
    public void onEnable() {
        LightAPI.plugin = this;

        this.support = new ArrayList<BukkitImpl>();
        addSupportImplement(new CraftBukkitImpl());
        addSupportImplement(new CauldronImpl());
        // support.add("MCPC-Plus");
        // support.add("Glowstone");
        reloadInitRegistry();
        getServer().getPluginManager().registerEvents(this, this);

        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {}

        Bukkit.getScheduler().runTaskTimer(this, new LightUpdater(this), 0, 2);

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {

            @Override
            public void run() {
                Version version = Version.parse(getDescription().getVersion());
                String repo = "BeYkeRYkt/LightAPI";

                Updater updater;
                try {
                    updater = new Updater(version, repo);

                    Response response = updater.getResult();
                    if (response == Response.SUCCESS) {
                        log(getServer().getConsoleSender(), ChatColor.GREEN + "New update is available: " + ChatColor.YELLOW + updater.getLatestVersion() + ChatColor.GREEN + "!");
                        log(getServer().getConsoleSender(), ChatColor.GREEN + "Changes: ");
                        getServer().getConsoleSender().sendMessage(updater.getChanges());// for
                        // normal
                        // view
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 60);
    }

    public void log(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.YELLOW + "<Light" + ChatColor.RED + "API" + ChatColor.YELLOW + ">: " + ChatColor.WHITE + message);
    }

    @Override
    public void onDisable() {
        LightAPI.plugin = null;
        this.support.clear();
        LightAPI.registry = null;
    }

    public static LightAPI getInstance() {
        return plugin;
    }

    public ILightRegistry getRegistry() {
        return registry;
    }

    public void reloadInitRegistry() {
        if (registry != null) {
            registry = null;
        }
        String name = getServer().getName();
        if (checkSupport(name) != null) {
            BukkitImpl impl = checkSupport(name);
            try {
                final Class<?> clazz = Class.forName(impl.getPath());
                // Check if we have a NMSHandler class at that location.
                if (ILightRegistry.class.isAssignableFrom(clazz)) {
                    LightAPI.registry = (ILightRegistry) clazz.getConstructor().newInstance();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                this.getLogger().severe("Could not find support for this " + name + " version.");
                this.setEnabled(false);
                return;
            }
            this.getLogger().info("Loading support for " + impl.getNameImpl() + " " + Bukkit.getVersion());
        } else {
            this.getLogger().severe("Could not find support for this Bukkit implementation.");
            this.setEnabled(false);
        }
    }

    public void addSupportImplement(BukkitImpl impl) {
        if (checkSupport(impl.getNameImpl()) == null) {
            support.add(impl);
        }
    }

    public List<BukkitImpl> getSupportImplements() {
        return support;
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
        registry.createLight(location, lightlevel);
    }

    public static void deleteLight(Location location) {
        DeleteLightEvent event = new DeleteLightEvent(location);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        registry.deleteLight(location);
    }

    public static void createLight(List<Location> list, int lightlevel) {
        for (Location location : list) {
            createLight(location, lightlevel);
        }
    }

    public static void deleteLight(List<Location> list) {
        for (Location location : list) {
            deleteLight(location);
        }
    }

    public static void updateChunks(Location loc) {
        registry.collectChunks(loc);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (player.isOp() || player.hasPermission("lightapi.updater")) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {

                @Override
                public void run() {
                    Version version = Version.parse(getDescription().getVersion());
                    String repo = "BeYkeRYkt/LightAPI";

                    Updater updater;
                    try {
                        updater = new Updater(version, repo);

                        Response response = updater.getResult();
                        if (response == Response.SUCCESS) {
                            log(player, ChatColor.GREEN + "New update is available: " + ChatColor.YELLOW + updater.getLatestVersion() + ChatColor.GREEN + "!");
                            log(player, ChatColor.GREEN + "Changes: ");
                            player.sendMessage(updater.getChanges());// for
                                                                     // normal
                                                                     // view
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 60);
        }
    }
}