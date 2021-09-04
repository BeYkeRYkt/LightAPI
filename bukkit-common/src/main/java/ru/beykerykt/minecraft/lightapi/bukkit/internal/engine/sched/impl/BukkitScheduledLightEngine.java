/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.engine.sched.impl;

import org.bukkit.Bukkit;
import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightStrategy;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.impl.ScheduledLightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

public class BukkitScheduledLightEngine extends ScheduledLightEngine {

    private final IHandler mHandler;

    public BukkitScheduledLightEngine(IBukkitPlatformImpl pluginImpl, IBackgroundService service, RelightStrategy strategy, IHandler handler) {
        super(pluginImpl, service, strategy);
        this.mHandler = handler;
    }

    public BukkitScheduledLightEngine(IBukkitPlatformImpl pluginImpl, IBackgroundService service, RelightStrategy strategy, IHandler handler, int maxRequestCount,
                                      int maxTimeMsPerTick) {
        super(pluginImpl, service, strategy, maxRequestCount, maxTimeMsPerTick);
        this.mHandler = handler;
    }

    protected IHandler getHandler() {
        return mHandler;
    }

    @Override
    protected IBukkitPlatformImpl getPlatformImpl() {
        return (IBukkitPlatformImpl) super.getPlatformImpl();
    }

    @Override
    public void onStart() {
        super.onStart();

        // load config
        String relightStrategyName = getPlatformImpl().getPlugin().getConfig().getString(ConfigurationPath.GENERAL_RELIGHT_STRATEGY);
        try {
            // TODO: move to throw exception
            RelightStrategy relightStrategy = RelightStrategy.valueOf(relightStrategyName);
            mRelightStrategy = relightStrategy;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int c_maxIterations =
                getPlatformImpl().getPlugin().getConfig().getInt(ConfigurationPath.LIGHT_OBSERVER_MAX_ITERATIONS_IN_PER_TICK);
        int c_maxTimeMsPerTick =
                getPlatformImpl().getPlugin().getConfig().getInt(ConfigurationPath.LIGHT_OBSERVER_MAX_TIME_MS_IN_PER_TICK);
        maxRequestCount = c_maxIterations;
        maxTimeMsPerTick = c_maxTimeMsPerTick;
    }

    @Override
    public LightEngineType getLightEngineType() {
        return getHandler().getLightEngineType();
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return getHandler().getLightEngineVersion();
    }

    @Override
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().getRawLightLevel(world, blockX, blockY, blockZ, lightType);
    }

    @Override
    public int setRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().setRawLightLevel(world, blockX, blockY, blockZ, lightLevel, lightType);
    }

    @Override
    public int recalculateLighting(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().recalculateLighting(world, blockX, blockY, blockZ, lightType);
    }
}
