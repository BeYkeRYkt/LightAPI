/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Vladimir Mikhailov <beykerykt@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.bukkit.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

import ru.beykerykt.minecraft.lightapi.bukkit.api.extension.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;

public class BukkitPlugin extends JavaPlugin {

    public LightAPI mLightAPI;
    public IHandler mHandler;
    public IBukkitExtension mExtension;
    private EntityTracker mEntityTracker;

    @Override
    public void onLoad() {
        generateConfig();
    }

    @Override
    public void onEnable() {
        mLightAPI = LightAPI.get();
        mExtension = (IBukkitExtension) LightAPI.get().getExtension();
        mHandler = mExtension.getHandler();
        mEntityTracker = new EntityTracker(this);

        getServer().getPluginManager().registerEvents(new DebugListener(this), this);
        mEntityTracker.start();
    }

    @Override
    public void onDisable() {
        mEntityTracker.shutdown();
        HandlerList.unregisterAll(this);
    }

    public void log(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + message);
    }

    private void generateConfig() {
        File file = new File(getDataFolder(), "config.yml");
        FileConfiguration fc = getConfig();
        if (!file.exists()) {
            fc.set("debug.offsetY", 1);
            saveConfig();
        }
    }

    public int getOffsetY() {
        return getConfig().getInt("debug.offsetY");
    }

    private void setLightLevel(Location location, int var, int lightLevel, int lightType, EditPolicy editPolicy,
            SendPolicy sendPolicy) {
        switch (var) {
            case 0: // lightapi interface
            {
                // Remove light for re-set lightlevel
                mLightAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(), location.getBlockY(),
                        location.getBlockZ(), 0, lightType, editPolicy, sendPolicy, null);
                int code = mLightAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), lightLevel, lightType, editPolicy, sendPolicy,
                        null);
                getLogger().info(
                        "setLightLevel()- " + " loc: " + location + " var: " + var + " lightLevel: " + lightLevel
                                + " resultCode: " + code);
                break;
            }
            case 1: // handler
            {
                // Keep the light level information, as after removing the light source, chunks may not be
                // updated
                // correctly.
                int blockLightLevel = mHandler.getRawLightLevel(location.getWorld(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), lightType);
                mHandler.setRawLightLevel(location.getWorld(), location.getBlockX(), location.getBlockY(),
                        location.getBlockZ(), lightLevel, lightType);
                mHandler.recalculateLighting(location.getWorld(), location.getBlockX(), location.getBlockY(),
                        location.getBlockZ(), lightType);
                List<IChunkData> chunkList = mHandler.collectChunkSections(location.getWorld(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), lightLevel == 0 ? blockLightLevel : lightLevel,
                        lightType);
                for (int i = 0; i < chunkList.size(); i++) {
                    IChunkData data = chunkList.get(i);
                    mHandler.sendChunk(data);
                }
                break;
            }
        }
    }

    private void runBenchmark(Location loc, boolean async, String strategy, int cycleCount) {
        int oldBlockLight = 15;
        int flag = LightFlag.BLOCK_LIGHTING | LightFlag.SKY_LIGHTING;

        Runnable run = () -> {
            long startTime = System.currentTimeMillis();
            EditPolicy editPolicy;
            SendPolicy sendPolicy;

            switch (strategy) {
                case "DEFERRED":
                    editPolicy = EditPolicy.DEFERRED;
                    sendPolicy = SendPolicy.DEFERRED;
                    break;
                case "FORCE_IMMEDIATE":
                    editPolicy = EditPolicy.FORCE_IMMEDIATE;
                    sendPolicy = SendPolicy.IMMEDIATE;
                    break;
                case "IMMEDIATE":
                default:
                    editPolicy = EditPolicy.IMMEDIATE;
                    sendPolicy = SendPolicy.IMMEDIATE;
                    break;
            }

            for (int i = 0; i < cycleCount; i++) {
                setLightLevel(loc, 0, oldBlockLight, flag, editPolicy, sendPolicy);
                setLightLevel(loc, 0, 0, flag, editPolicy, sendPolicy);
            }

            long endTime = System.currentTimeMillis();
            Bukkit.broadcastMessage("Time: " + (endTime - startTime) + " ms");
        };

        // Be careful, asynchronous thread can be blocked
        if (async) {
            getServer().getScheduler().runTaskAsynchronously(this, run);
        } else {
            getServer().getScheduler().runTask(this, run);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lighttest")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                int argsLength = args.length;
                if (argsLength > 0) {
                    String cmd = args[0];
                    if (cmd.equals("bench")) {
                        if (argsLength < 3) {
                            log(player, ChatColor.RED
                                    + "lighttest bench (FORCE_IMMEDIATE | IMMEDIATE | DEFERRED) (cycle count) (async)");
                            return false;
                        }
                        String strategy = args[1];
                        int cycle = Integer.parseInt(args[2]);
                        boolean async = argsLength > 3 ? args[3].equals("async") : false;
                        log(player, "Start benchmark: " + strategy + " (" + cycle + ")");
                        runBenchmark(player.getLocation(), async, strategy.toUpperCase(), cycle);
                    } else if (cmd.equals("set")) {
                        if (argsLength < 5) {
                            log(player, ChatColor.RED
                                    + "lighttest set (LightLevel: 0 - 15) (0 - LightAPI | 1 - Handler) (FORCE_IMMEDIATE |"
                                    + " IMMEDIATE | " + "DEFERRED) (MANUAL | IMMEDIATE| DEFERRED)");
                            return false;
                        }
                        log(player, "Set light level");
                        int lightLevel = Integer.parseInt(args[1]);
                        int val = Integer.parseInt(args[2]);
                        EditPolicy edit = EditPolicy.valueOf(args[3].toUpperCase());
                        SendPolicy sendPolicy = SendPolicy.valueOf(args[4].toUpperCase());
                        setLightLevel(player.getLocation(), val, lightLevel, LightFlag.BLOCK_LIGHTING, edit,
                                sendPolicy);
                    } else {
                        log(player, "lighttest bench (FORCE_IMMEDIATE | IMMEDIATE | DEFERRED) (cycle count) (async)");
                        log(player, "lighttest set (LightLevel: 0 - 15) (0 - LightAPI | 1 - Handler) (FORCE_IMMEDIATE |"
                                + " IMMEDIATE | " + "DEFERRED) (MANUAL | IMMEDIATE | DEFERRED)");
                    }
                } else {
                    log(player, "(bench | set)");
                }
            } else if (sender instanceof ConsoleCommandSender) {
                // nothing...
                log(sender, "Not supported.");
            }
        }
        return true;
    }
}
