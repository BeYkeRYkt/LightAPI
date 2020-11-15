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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.impl;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.api.impl.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.BukkitChunkObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.IBukkitChunkObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.BukkitHandlerFactory;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.IBukkitHandlerInternal;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCodes;
import ru.beykerykt.minecraft.lightapi.common.api.SendMode;
import ru.beykerykt.minecraft.lightapi.common.api.impl.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.handler.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.common.internal.service.BackgroundService;

import java.util.List;

public class BukkitImpl implements IBukkitImpl, IBukkitExtension {

    private IBukkitHandlerInternal mHandler;
    private BukkitPlugin mPlugin;
    private IBukkitChunkObserver mChunkObserver;
    private BackgroundService mService;
    private int taskId = -1;

    public BukkitImpl(BukkitPlugin plugin) {
        this.mPlugin = plugin;
    }

    @Override
    public void initialization() throws Exception {
        // load adapter
        IHandlerFactory factory = new BukkitHandlerFactory(mPlugin, this);
        mHandler = (IBukkitHandlerInternal) factory.createHandler();

        // load chunk observer
        mChunkObserver = new BukkitChunkObserver(mHandler);
        boolean mergeEnable = mPlugin.getConfig().getBoolean("merge-chunk-sections");
        getChunkObserver().setMergeChunksEnabled(mergeEnable);

        // start background service
        mService = new BackgroundService(this);
        long period = mPlugin.getConfig().getLong("background-service-delay-ticks");
        taskId =
                mPlugin.getServer().getScheduler().runTaskTimer(mPlugin, getBackgroundService(), 0, period).getTaskId();

        // start handler initialization
        mHandler.initialization(this);
        getBackgroundService().addToRepeat(() -> getChunkObserver().onTick());
    }

    @Override
    public void shutdown() {
        if (taskId != -1) {
            mPlugin.getServer().getScheduler().cancelTask(taskId);
            getBackgroundService().shutdown();
        }
        mChunkObserver.shutdown();
        mHandler.shutdown(this);
        mHandler = null;
    }

    @Override
    public PlatformType getPlatformType() {
        return getHandler().getPlatformType();
    }

    @Override
    public IBukkitHandlerInternal getHandler() {
        if (mHandler == null) {
            throw new IllegalStateException("IHandler not yet initialized!");
        }
        return mHandler;
    }

    @Override
    public IBukkitExtension getExtension() {
        return this;
    }

    @Override
    public IBukkitChunkObserver getChunkObserver() {
        return mChunkObserver;
    }

    @Override
    public BackgroundService getBackgroundService() {
        return mService;
    }

    @Override
    public void log(String msg) {
        mPlugin.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + msg);
    }

    @Override
    public int getLightLevel(Location location) {
        if (location == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        return getLightLevel(location, LightFlags.COMBO_LIGHTING);
    }

    @Override
    public int getLightLevel(Location location, int flags) {
        if (location == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        return getLightLevel(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public int getLightLevel(World world, int blockX, int blockY, int blockZ) {
        return getLightLevel(world, blockX, blockY, blockZ, LightFlags.COMBO_LIGHTING);
    }

    @Override
    public int getLightLevel(World world, int blockX, int blockY, int blockZ, int flags) {
        return getHandler().getRawLightLevel(world.getName(), blockX, blockY, blockZ, flags);
    }

    @Override
    public int setLightLevel(Location location, int lightLevel, SendMode mode, List<ChunkData> outputChunks) {
        if (location == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        return setLightLevel(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                lightLevel, LightFlags.COMBO_LIGHTING, mode, outputChunks);
    }

    @Override
    public int setLightLevel(Location location, int lightLevel, int flags, SendMode mode,
                             List<ChunkData> outputChunks) {
        if (location == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        return setLightLevel(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                lightLevel, flags, mode, outputChunks);
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, SendMode mode,
                             List<ChunkData> outputChunks) {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, LightFlags.COMBO_LIGHTING, mode, outputChunks);
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags,
                             SendMode mode, List<ChunkData> outputChunks) {
        return getHandler().setLightLevel(world, blockX, blockY, blockZ, lightLevel, flags, mode, outputChunks);
    }
}
