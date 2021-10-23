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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.observer.sched.impl;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.impl.ScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

public class BukkitScheduledChunkObserver extends ScheduledChunkObserver {

    /**
     * CONFIG
     */
    private final String CONFIG_TITLE = getClass().getSimpleName();

    private final String CONFIG_TICK_PERIOD = CONFIG_TITLE + ".tick-period";

    private final IHandler mHandler;
    private ScheduledFuture mScheduledFuture;

    public BukkitScheduledChunkObserver(BukkitPlatformImpl platform, IBackgroundService service, IHandler handler) {
        super(platform, service);
        this.mHandler = handler;
    }

    private IHandler getHandler() {
        return mHandler;
    }

    @Override
    protected BukkitPlatformImpl getPlatformImpl() {
        return (BukkitPlatformImpl) super.getPlatformImpl();
    }

    private void checkAndSetDefaults() {
        boolean needSave = false;
        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();
        if (!fc.isSet(CONFIG_TICK_PERIOD)) {
            fc.set(CONFIG_TICK_PERIOD, 2);
            needSave = true;
        }

        if (needSave) {
            getPlatformImpl().getPlugin().saveConfig();
        }
    }

    private void configure() {
        checkAndSetDefaults();

        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();
        int period = fc.getInt(CONFIG_TICK_PERIOD);
        mScheduledFuture = getBackgroundService().scheduleWithFixedDelay(this, 0, 50 * period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onStart() {
        configure();
        super.onStart();
    }

    @Override
    public void onShutdown() {
        if (mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
        }
        super.onShutdown();
    }

    @Override
    public IChunkData createChunkData(String worldName, int chunkX, int chunkZ) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return null;
        }
        return getHandler().createChunkData(worldName, chunkX, chunkZ);
    }

    @Override
    public boolean isValidChunkSection(int sectionY) {
        return getHandler().isValidChunkSection(sectionY);
    }

    @Override
    public List<IChunkData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return new ArrayList<>();
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightFlags);
    }

    @Override
    public int sendChunk(IChunkData data) {
        if (!getPlatformImpl().isWorldAvailable(data.getWorldName())) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return getHandler().sendChunk(data);
    }
}
