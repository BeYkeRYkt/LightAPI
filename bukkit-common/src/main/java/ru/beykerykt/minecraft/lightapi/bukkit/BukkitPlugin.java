/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.bukkit;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BukkitPlugin extends JavaPlugin {

    private static final int BSTATS_ID = 13051;

    private static BukkitPlugin plugin = null;
    private static BukkitPlatformImpl mImpl = null;

    public static BukkitPlugin getInstance() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        mImpl = new BukkitPlatformImpl(plugin);

        // create config
        try {
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                generateConfig(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set server implementation
        try {
            LightAPI.prepare(mImpl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        // try initialization LightAPI
        try {
            LightAPI.initialization();
        } catch (Exception ex) {
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        enableMetrics();
    }

    @Override
    public void onDisable() {
        LightAPI.shutdown(mImpl);
        HandlerList.unregisterAll(this);
    }

    private void enableMetrics() {
        boolean enableMetrics = getConfig().getBoolean(ConfigurationPath.GENERAL_ENABLE_METRICS);
        if (enableMetrics) {
            Metrics metrics = new Metrics(this, BSTATS_ID);
            // TODO: Add custom charts ?
        }
        getInternal().info("Metrics is " + (enableMetrics ? "en" : "dis") + "abled!");
    }

    public IBukkitPlatformImpl getInternal() {
        if (mImpl == null) {
            throw new IllegalStateException("IBukkitLightAPI not yet initialized!");
        }
        return mImpl;
    }

    public boolean isBackwardAvailable() {
        boolean flag = Build.CURRENT_VERSION <= 1;
        try {
            Class.forName("ru.beykerykt.lightapi.LightAPI");
            return flag & true;
        } catch (ClassNotFoundException ex) {
        }
        return flag & false;
    }

    public void log(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + message);
    }

    private void generateConfig(File file) {
        FileConfiguration fc = getConfig();
        if (!file.exists()) {
            fc.set(ConfigurationPath.GENERAL_DEBUG, true);
            fc.set(ConfigurationPath.GENERAL_ENABLE_METRICS, true);
            fc.set(ConfigurationPath.GENERAL_SPECIFIC_STORAGE_PROVIDER, "none");
            saveConfig();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lightapi")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length == 0) {
                    player.sendMessage(ChatColor.AQUA + " ------- <LightAPI " + ChatColor.WHITE
                            + getDescription().getVersion() + "> ------- ");

                    player.sendMessage(ChatColor.AQUA + " Current version: " + ChatColor.WHITE + getDescription().getVersion());
                    player.sendMessage(ChatColor.AQUA + " Current implementation: " + ChatColor.WHITE +
                            Build.CURRENT_IMPLEMENTATION);
                    player.sendMessage(
                            ChatColor.AQUA + " LightEngine type: " + ChatColor.WHITE + getInternal().getLightEngine().getLightEngineType());
                    player.sendMessage(
                            ChatColor.AQUA + " LightEngine version: " + ChatColor.WHITE + getInternal().getLightEngine().getLightEngineVersion());
                    player.sendMessage(ChatColor.AQUA + " Server name: " + ChatColor.WHITE + getServer().getName());
                    player.sendMessage(
                            ChatColor.AQUA + " Server version: " + ChatColor.WHITE + getServer().getVersion());

                    TextComponent text = new TextComponent(" | ");
                    TextComponent sourcecode = new TextComponent(ChatColor.AQUA + "Source code");
                    sourcecode.setClickEvent(
                            new ClickEvent(ClickEvent.Action.OPEN_URL, "http://github.com/BeYkeRYkt/LightAPI/"));
                    sourcecode.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("Goto the GitHub!").create()));
                    text.addExtra(sourcecode);
                    text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

                    TextComponent developer = new TextComponent(ChatColor.AQUA + "Developers");
                    List<String> authors = getDescription().getAuthors();
                    String authorsLine = "none";
                    if (authors.size() > 0) {
                        authorsLine = authors.get(0);
                        if (authors.size() > 1) {
                            for (int i = 1; i < authors.size() - 1; i++) {
                                authorsLine += ", " + authors.get(i);
                            }
                            authorsLine += " and " + authors.get(authors.size() - 1);
                        }
                    }
                    developer.setHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(authorsLine).create()));
                    text.addExtra(developer);
                    text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

                    TextComponent contributors = new TextComponent(ChatColor.AQUA + "Contributors");
                    contributors.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                            "https://github.com/BeYkeRYkt/LightAPI/graphs/contributors"));
                    contributors.setHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("ALIENS!!").create()));
                    text.addExtra(contributors);
                    text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

                    player.spigot().sendMessage(text);

                    TextComponent licensed = new TextComponent(" Licensed under ");
                    TextComponent MIT = new TextComponent(ChatColor.AQUA + "MIT License");
                    MIT.setClickEvent(
                            new ClickEvent(ClickEvent.Action.OPEN_URL, "https://opensource.org/licenses/MIT/"));
                    MIT.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("Goto for information about license!").create()));
                    licensed.addExtra(MIT);
                    player.spigot().sendMessage(licensed);

                    if (isBackwardAvailable()) {
                        player.sendMessage(ChatColor.BLACK + "backwards compatibility enabled");
                    }
                } else {
                    log(player, ChatColor.RED + "Hmm... This command does not exist. Are you sure write correctly ?");
                }
            } else if (sender instanceof ConsoleCommandSender) {
                ConsoleCommandSender console = (ConsoleCommandSender) sender;
                if (args.length == 0) {
                    console.sendMessage(ChatColor.AQUA + " ------- <LightAPI " + ChatColor.WHITE
                            + getDescription().getVersion() + "> ------- ");
                    console.sendMessage(
                            ChatColor.AQUA + " Current version: " + ChatColor.WHITE + getDescription().getVersion());
                    console.sendMessage(ChatColor.AQUA + " Current implementation: " + ChatColor.WHITE +
                            Build.CURRENT_IMPLEMENTATION);
                    console.sendMessage(
                            ChatColor.AQUA + " LightEngine type: " + ChatColor.WHITE + getInternal().getLightEngine().getLightEngineType());
                    console.sendMessage(
                            ChatColor.AQUA + " LightEngine version: " + ChatColor.WHITE + getInternal().getLightEngine().getLightEngineVersion());
                    console.sendMessage(ChatColor.AQUA + " Server name: " + ChatColor.WHITE + getServer().getName());
                    console.sendMessage(
                            ChatColor.AQUA + " Server version: " + ChatColor.WHITE + getServer().getVersion());
                    console.sendMessage(ChatColor.AQUA + " Source code: " + ChatColor.WHITE
                            + "http://github.com/BeYkeRYkt/LightAPI/");
                    List<String> authors = getDescription().getAuthors();
                    String authorsLine = "none";
                    if (authors.size() > 0) {
                        authorsLine = authors.get(0);
                        if (authors.size() > 1) {
                            for (int i = 1; i < authors.size() - 1; i++) {
                                authorsLine += ", " + authors.get(i);
                            }
                            authorsLine += " and " + authors.get(authors.size() - 1);
                        }
                    }
                    console.sendMessage(ChatColor.AQUA + " Developers: " + ChatColor.WHITE + authorsLine);
                    console.sendMessage("");
                    console.sendMessage(ChatColor.WHITE + " Licensed under: " + ChatColor.AQUA + "MIT License");

                    if (isBackwardAvailable()) {
                        console.sendMessage(ChatColor.BLACK + "backwards compatibility enabled");
                    }
                } else {
                    log(console, ChatColor.RED + "Hmm... This command does not exist. Are you sure write correctly ?");
                }
            }
        }
        return true;
    }
}