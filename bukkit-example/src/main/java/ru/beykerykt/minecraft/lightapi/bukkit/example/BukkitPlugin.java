/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.example;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.api.impl.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.api.impl.IBukkitHandler;
import ru.beykerykt.minecraft.lightapi.common.api.*;

import java.util.ArrayList;
import java.util.List;

public class BukkitPlugin extends JavaPlugin {
    public LightAPI mLightAPI;
    public IBukkitHandler mHandler;
    public IBukkitExtension mExtension;

    @Override
    public void onEnable() {
        mLightAPI = LightAPI.get();
        mHandler = (IBukkitHandler) LightAPI.get().getImplHandler();
        mExtension = (IBukkitExtension) LightAPI.get().getImplExtension();

        getServer().getPluginManager().registerEvents(new DebugListener(this), this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public void log(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + message);
    }

    private void setLightLevel(Location location, int var, int lightLevel, int flag, SendMode mode) {
        List<ChunkData> chunks = new ArrayList<>();
        switch (var) {
            case 0: // lightapi
            {
                int code = mLightAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), lightLevel, flag, mode, chunks);
                if (code == ResultCodes.SUCCESS) {
                    switch (mode) {
                        case INSTANT:
                            // Nothing. Chunks will be sent immediately after completion.
                            break;
                        case DELAYED:
                            // Nothing. Chunks will be sent after a certain number of ticks.
                            break;
                        case MANUAL:
                            // You need to manually send chunks.
                            for (int i = 0; i < chunks.size(); i++) {
                                ChunkData data = chunks.get(i);
                                mLightAPI.getImplHandler().sendChunk(data);
                            }
                            break;
                    }
                }
                break;
            }
            case 1: // handler
            {
                // Keep the light level information, as after removing the light source, chunks may not be updated
                // correctly.
                int blockLightLevel = mHandler.getRawLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), flag);
                mHandler.setRawLightLevel(location.getWorld().getName(), location.getBlockX(), location.getBlockY(),
                        location.getBlockZ(), lightLevel, flag);
                if (mHandler.isRequireRecalculateLighting()) {
                    mHandler.recalculateLighting(location.getWorld().getName(), location.getBlockX(),
                            location.getBlockY(), location.getBlockZ(), flag);
                }
                if (mHandler.isRequireManuallySendingChanges()) {
                    List<ChunkData> chunkList = mHandler.collectChunkSections(location.getWorld().getName(),
                            location.getBlockX(), location.getBlockY(), location.getBlockZ(), lightLevel == 0 ?
                                    blockLightLevel : lightLevel);
                    for (int i = 0; i < chunkList.size(); i++) {
                        ChunkData data = chunkList.get(i);
                        mHandler.sendChunk(data);
                    }
                }
                break;
            }
            case 2: // extension
            {
                // TODO: make extensions
                IBukkitExtension extension = (IBukkitExtension) mLightAPI.getImplExtension();
                int code = extension.setLightLevel(location, lightLevel, flag, mode, chunks);
                if (code == ResultCodes.SUCCESS) {
                    switch (mode) {
                        case INSTANT:
                            // Nothing. Chunks will be sent immediately after completion.
                            break;
                        case DELAYED:
                            // Nothing. Chunks will be sent after a certain number of ticks.
                            break;
                        case MANUAL:
                            // You need to manually send chunks.
                            for (int i = 0; i < chunks.size(); i++) {
                                ChunkData data = chunks.get(i);
                                mLightAPI.getImplHandler().sendChunk(data);
                            }
                            break;
                    }
                }
                break;
            }
        }
    }

    private void setLightLevelManual(Location location, int lightLevel, int flag, List<ChunkData> outputChunks) {
        // Keep the light level information, as after removing the light source, chunks may not be updated
        // correctly.
        int blockLightLevel = mHandler.getRawLightLevel(location.getWorld().getName(), location.getBlockX(),
                location.getBlockY(), location.getBlockZ(), flag);
        mHandler.setRawLightLevel(location.getWorld().getName(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ(), lightLevel, flag);
        if (mHandler.isRequireRecalculateLighting()) {
            mHandler.recalculateLighting(location.getWorld().getName(), location.getBlockX(),
                    location.getBlockY(), location.getBlockZ(), flag);
        }
        if (mHandler.isRequireManuallySendingChanges()) {
            List<ChunkData> chunkList = mHandler.collectChunkSections(location.getWorld().getName(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), lightLevel == 0 ?
                            blockLightLevel : lightLevel);
            //outputChunks.addAll(ChunkUtils.mergeChunks(chunkList));
            outputChunks.addAll(chunkList);
            chunkList.clear();
        }
    }

    private void runBenchmark(Location loc) {
        int oldBlockLight = 15;
        int flag = LightFlags.BLOCK_LIGHTING;
        int cycle = 99;

        // Be careful, asynchronous thread can be blocked
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            // LightAPI (instant)
            ///////////////////////////////////////////
            log(getServer().getConsoleSender(), "< #1 LightAPI (instant mode : raw + recalc cycle): start");
            long time_start = System.currentTimeMillis();
            for (int i = 0; i < cycle; i++) {
                setLightLevel(loc, 0, 0, flag, SendMode.INSTANT);
                setLightLevel(loc, 0, oldBlockLight, flag, SendMode.INSTANT);
            }
            setLightLevel(loc, 0, 0, flag, SendMode.INSTANT);
            long time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #1 LightAPI (instant mode : raw + recalc cycle): " + time_end);

            // LightAPI (delayed)
            ///////////////////////////////////////////
            log(getServer().getConsoleSender(), "< #2 LightAPI (delayed mode : raw + recalc cycle): start");
            time_start = System.currentTimeMillis();
            for (int i = 0; i < cycle; i++) {
                setLightLevel(loc, 0, 0, flag, SendMode.DELAYED);
                setLightLevel(loc, 0, oldBlockLight, flag, SendMode.DELAYED);
            }
            setLightLevel(loc, 0, 0, flag, SendMode.DELAYED);
            time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #2 LightAPI (delayed mode : raw + recalc cycle): " + time_end);

            // LightAPI (manual)
            ///////////////////////////////////////////
            log(getServer().getConsoleSender(), "< #3 LightAPI (manual mode : raw + recalc cycle): start");
            time_start = System.currentTimeMillis();
            for (int i = 0; i < cycle; i++) {
                setLightLevel(loc, 0, 0, flag, SendMode.MANUAL);
                setLightLevel(loc, 0, oldBlockLight, flag, SendMode.MANUAL);
            }
            setLightLevel(loc, 0, 0, flag, SendMode.MANUAL);
            time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #3 LightAPI (manual mode : raw + recalc cycle): " + time_end);

            // handler (instant)
            ///////////////////////////////////////////
            log(getServer().getConsoleSender(), "< #4 Handler (instant mode : raw + recalc cycle): start");
            time_start = System.currentTimeMillis();
            for (int i = 0; i < cycle; i++) {
                setLightLevel(loc, 1, 0, flag, SendMode.INSTANT);
                setLightLevel(loc, 1, oldBlockLight, flag, SendMode.INSTANT);
            }
            setLightLevel(loc, 1, 0, flag, SendMode.INSTANT);
            time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #4 Handler (instant mode : raw + recalc cycle): " + time_end);

            // handler (delayed)
            ///////////////////////////////////////////
            log(getServer().getConsoleSender(), "< #5 Handler (delayed mode : raw + recalc cycle): start");
            time_start = System.currentTimeMillis();
            for (int i = 0; i < cycle; i++) {
                setLightLevel(loc, 1, 0, flag, SendMode.DELAYED);
                setLightLevel(loc, 1, oldBlockLight, flag, SendMode.DELAYED);
            }
            setLightLevel(loc, 1, 0, flag, SendMode.DELAYED);
            time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #5 Handler (delayed mode : raw + recalc cycle): " + time_end);

            // handler (manual)
            ///////////////////////////////////////////
            log(getServer().getConsoleSender(), "< #6 Handler (manual mode : raw + recalc cycle): start");
            time_start = System.currentTimeMillis();
            for (int i = 0; i < cycle; i++) {
                setLightLevel(loc, 1, 0, flag, SendMode.MANUAL);
                setLightLevel(loc, 1, oldBlockLight, flag, SendMode.MANUAL);
            }
            setLightLevel(loc, 1, 0, flag, SendMode.MANUAL);
            time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #6 Handler (manual mode : raw + recalc cycle): " + time_end);

            // Advanced handler (manual)
            ///////////////////////////////////////////
            time_start = System.currentTimeMillis();
            List<ChunkData> chunks = new ArrayList<>();
            for (int i = 0; i < cycle; i++) {
                setLightLevelManual(loc, 0, flag, chunks);
                setLightLevelManual(loc, oldBlockLight, flag, chunks);
            }
            setLightLevelManual(loc, 0, flag, chunks);

            List<ChunkData> mergedChunks = ChunkUtils.mergeChunks(chunks);
            for (int i = 0; i < mergedChunks.size(); i++) {
                ChunkData data = mergedChunks.get(i);
                mLightAPI.getImplHandler().sendChunk(data);
            }

            time_end = System.currentTimeMillis() - time_start;
            log(getServer().getConsoleSender(), "< #7 chunksSize: " + chunks.size());
            log(getServer().getConsoleSender(), "< #7 mergedChunksSize: " + mergedChunks.size());
            log(getServer().getConsoleSender(),
                    "< #7 Advanced Handler (manual mode : raw + recalc cycle): " + time_end);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lighttest")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length == 1) {
                    String cmd = args[0];
                    if (cmd.equals("bench")) {
                        log(player, "Start benchmark");
                        runBenchmark(player.getLocation());
                    }
                } else if (args.length == 3) {
                    String cmd = args[0];
                    if (cmd.equals("create")) {
                        log(player, "Create light");
                        int val = Integer.parseInt(args[1]);
                        int lightLevel = 15;
                        SendMode mode = SendMode.valueOf(args[2].toUpperCase());
                        setLightLevel(player.getLocation(), val, lightLevel, LightFlags.BLOCK_LIGHTING, mode);
                    } else if (cmd.equals("delete")) {
                        log(player, "Delete light");
                        int val = Integer.parseInt(args[1]);
                        SendMode mode = SendMode.valueOf(args[2].toUpperCase());
                        setLightLevel(player.getLocation(), val, 0, LightFlags.BLOCK_LIGHTING, mode);
                    }
                } else {
                    log(player, ChatColor.RED + "lighttest (bench | create | delete) (0 | 1 | 2) (INSTANT | DELAYED| " +
                            "MANUAL) ");
                }
            } else if (sender instanceof ConsoleCommandSender) {
                // nothing...
            }
        }
        return true;
    }
}