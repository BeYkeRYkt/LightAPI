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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.engine.sched;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightPolicy;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.IScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.IScheduler;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.PriorityScheduler;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.ScheduledLightEngineImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageProvider;

public class BukkitScheduledLightEngineImpl extends ScheduledLightEngineImpl {

    /**
     * CONFIG
     */
    private final String CONFIG_TITLE = getClass().getSimpleName();
    private final String CONFIG_RELIGHT_STRATEGY = CONFIG_TITLE + ".relight-strategy";
    private final String CONFIG_TICK_PERIOD = CONFIG_TITLE + ".tick-period";
    private final String CONFIG_MAX_TIME_MS_IN_PER_TICK = CONFIG_TITLE + ".max-time-ms-in-per-tick";
    private final String CONFIG_MAX_ITERATIONS_IN_PER_TICK = CONFIG_TITLE + ".max-iterations-in-per-tick";

    private final IHandler mHandler;
    private final Object mLock = new Object();
    private ScheduledFuture mScheduledFuture;
    private int mTaskId = -1;

    /**
     * @hide
     */
    public BukkitScheduledLightEngineImpl(BukkitPlatformImpl pluginImpl, IBackgroundService service,
            IStorageProvider storageProvider, IHandler handler) {
        this(pluginImpl, service, storageProvider, RelightPolicy.DEFERRED, handler, 250, 250);
    }

    public BukkitScheduledLightEngineImpl(BukkitPlatformImpl pluginImpl, IBackgroundService service,
            IStorageProvider storageProvider, RelightPolicy strategy, IHandler handler, int maxRequestCount,
            int maxTimeMsPerTick) {
        super(pluginImpl, service, storageProvider, strategy, maxRequestCount, maxTimeMsPerTick);
        this.mHandler = handler;
    }

    protected IHandler getHandler() {
        return mHandler;
    }

    @Override
    protected BukkitPlatformImpl getPlatformImpl() {
        return (BukkitPlatformImpl) super.getPlatformImpl();
    }

    private void checkAndSetDefaults() {
        boolean needSave = false;
        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();
        if (!fc.isSet(CONFIG_RELIGHT_STRATEGY)) {
            fc.set(CONFIG_RELIGHT_STRATEGY, RelightPolicy.DEFERRED.name());
            needSave = true;
        }
        if (!fc.isSet(CONFIG_TICK_PERIOD)) {
            fc.set(CONFIG_TICK_PERIOD, 1);
            needSave = true;
        }
        if (!fc.isSet(CONFIG_MAX_TIME_MS_IN_PER_TICK)) {
            fc.set(CONFIG_MAX_TIME_MS_IN_PER_TICK, 50);
            needSave = true;
        }
        if (!fc.isSet(CONFIG_MAX_ITERATIONS_IN_PER_TICK)) {
            fc.set(CONFIG_MAX_ITERATIONS_IN_PER_TICK, 256);
            needSave = true;
        }

        if (needSave) {
            getPlatformImpl().getPlugin().saveConfig();
        }
    }

    private void configure() {
        checkAndSetDefaults();
        // load config
        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();
        String relightStrategyName = fc.getString(CONFIG_RELIGHT_STRATEGY);
        try {
            // TODO: move to throw exception
            RelightPolicy relightPolicy = RelightPolicy.valueOf(relightStrategyName);
            mRelightPolicy = relightPolicy;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        maxRequestCount = fc.getInt(CONFIG_MAX_ITERATIONS_IN_PER_TICK);
        maxTimeMsPerTick = fc.getInt(CONFIG_MAX_TIME_MS_IN_PER_TICK);

        this.mTaskId = getPlatformImpl().getPlugin().getServer().getScheduler().runTaskTimer(
                getPlatformImpl().getPlugin(), this::onTickPenaltyTime, 0, 1).getTaskId();

        // scheduler
        // TODO: Make config (?)
        IScheduler scheduler = new PriorityScheduler(this,
                (IScheduledChunkObserver) getPlatformImpl().getChunkObserver(), getBackgroundService(),
                getPlatformImpl().getStorageProvider(),
                maxTimeMsPerTick); setScheduler(scheduler);

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
        if (mTaskId != -1) {
            getPlatformImpl().getPlugin().getServer().getScheduler().cancelTask(mTaskId);
        }
        if (mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
        }
        super.onShutdown();
    }

    @Override
    public LightEngineType getLightEngineType() {
        return getHandler().getLightEngineType();
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return getHandler().getLightEngineVersion();
    }

    /* @hide */
    protected int getLightLevelLocked(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().getRawLightLevel(world, blockX, blockY, blockZ, lightFlags);
    }

    @Override
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        if (getHandler().isMainThread()) {
            return getLightLevelLocked(worldName, blockX, blockY, blockZ, lightFlags);
        } else {
            synchronized (mLock) {
                return getLightLevelLocked(worldName, blockX, blockY, blockZ, lightFlags);
            }
        }
    }

    /* @hide */
    private int setRawLightLevelLocked(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().setRawLightLevel(world, blockX, blockY, blockZ, lightLevel, lightFlags);
    }

    @Override
    public int setRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags) {
        if (getHandler().isMainThread()) {
            return setRawLightLevelLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags);
        } else {
            synchronized (mLock) {
                return setRawLightLevelLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags);
            }
        }
    }

    /* @hide */
    private int recalculateLightingLocked(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().recalculateLighting(world, blockX, blockY, blockZ, lightFlags);
    }

    @Override
    public int recalculateLighting(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        if (getHandler().isMainThread()) {
            return recalculateLightingLocked(worldName, blockX, blockY, blockZ, lightFlags);
        } else {
            synchronized (mLock) {
                return recalculateLightingLocked(worldName, blockX, blockY, blockZ, lightFlags);
            }
        }
    }
}
