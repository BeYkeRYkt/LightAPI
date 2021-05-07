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
package ru.beykerykt.minecraft.lightapi.bukkit.internal;

import org.bukkit.ChatColor;
import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.bukkit.api.extension.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.BukkitChunkObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.light.ILightObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.light.LightObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.HandlerFactory;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IBukkitHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.BackgroundService;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.Request;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.RequestFlag;
import ru.beykerykt.minecraft.lightapi.common.api.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.service.ICallback;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.EditStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.RelightStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.SendStrategy;

public class BukkitLightAPI implements IBukkitLightAPI, IBukkitExtension {

    private boolean DEBUG = false;

    private IBukkitHandler mHandler;
    private BukkitPlugin mPlugin;
    private IChunkObserver mChunkObserver;
    private ILightObserver mLightObserver;
    private BackgroundService mBackgroundService;
    private boolean isInit = false;
    private RelightStrategy mRelightStrategy;

    public BukkitLightAPI(BukkitPlugin mPlugin) {
        this.mPlugin = mPlugin;
    }

    @Override
    public int prepare() {
        return ResultCode.SUCCESS;
    }

    @Override
    public int initialization() {
        // debug mode
        this.DEBUG = mPlugin.getConfig().getBoolean(ConfigurationPath.GENERAL_DEBUG);

        // strategy
        String relightStrategyName = mPlugin.getConfig().getString(ConfigurationPath.GENERAL_RELIGHT_STRATEGY);
        try {
            RelightStrategy relightStrategy = RelightStrategy.valueOf(relightStrategyName);
            mRelightStrategy = relightStrategy;
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultCode.FAILED;
        }

        // load adapter
        HandlerFactory factory = new HandlerFactory(mPlugin, this);
        try {
            mHandler = (IBukkitHandler) factory.createHandler();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultCode.FAILED;
        }

        // start background service
        mBackgroundService = new BackgroundService(this);
        mBackgroundService.start();

        // start light observer
        mLightObserver = new LightObserver(this, mHandler, mBackgroundService);
        mLightObserver.start();

        // start chunk observer
        mChunkObserver = new BukkitChunkObserver(this, mHandler, mBackgroundService);
        mChunkObserver.start();

        // start handler initialization
        try {
            mHandler.initialization(this);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultCode.FAILED;
        }

        isInit = true;
        return ResultCode.SUCCESS;
    }

    @Override
    public void shutdown() {
        isInit = false;
        mChunkObserver.shutdown();
        mChunkObserver = null;
        mLightObserver.shutdown();
        mLightObserver = null;
        mBackgroundService.shutdown();
        mBackgroundService = null;
        mHandler.shutdown(this);
        mHandler = null;
    }

    @Override
    public boolean isInitialized() {
        return isInit;
    }

    @Override
    public void log(String msg) {
        mPlugin.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + msg);
    }

    @Override
    public void debug(String msg) {
        if (DEBUG) {
            log("[DEBUG] " + msg);
        }
    }

    @Override
    public IBukkitHandler getHandler() {
        if (mHandler == null) {
            throw new IllegalStateException("IHandler not yet initialized!");
        }
        return mHandler;
    }

    @Override
    public ILightObserver getLightObserver() {
        return mLightObserver;
    }

    public IChunkObserver getChunkObserver() {
        return mChunkObserver;
    }

    public BackgroundService getBackgroundService() {
        return mBackgroundService;
    }

    @Override
    public IBukkitExtension getExtension() {
        return this;
    }

    @Override
    public RelightStrategy getRelightStrategy() {
        return mRelightStrategy;
    }

    @Override
    public PlatformType getPlatformType() {
        return getHandler().getPlatformType();
    }

    @Override
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        World world = mPlugin.getServer().getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return getHandler().getRawLightLevel(world, blockX, blockY, blockZ, lightType);
    }

    /* @hide */
    private int setLightLevelLocked(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
                                    EditStrategy editStrategy, SendStrategy sendStrategy, ICallback callback) {
        int resultCode = ResultCode.SUCCESS;
        int requestFlags = RequestFlag.EDIT;
        int priority = Request.DEFAULT_PRIORITY;
        int oldLightLevel = getHandler().getRawLightLevel(world, blockX, blockY, blockZ, lightType);

        Request request = new Request(priority, requestFlags, world, blockX, blockY, blockZ, oldLightLevel, lightLevel,
                lightType, callback);
        switch (editStrategy) {
            case FORCE_IMMEDIATE: {
                request.addRequestFlag(RequestFlag.RECALCULATE);
                request.addRequestFlag(RequestFlag.SEND);
                request.setPriority(Request.HIGH_PRIORITY);
                getLightObserver().executeSync(request);
                break;
            }
            case IMMEDIATE: {
                int newPriority = request.getPriority();
                if (sendStrategy == SendStrategy.IMMEDIATE) {
                    request.addRequestFlag(RequestFlag.SEND);
                    newPriority += 1;
                } else if (sendStrategy == SendStrategy.DEFERRED) {
                    request.addRequestFlag(RequestFlag.COMBINED_SEND);
                }
                if (getBackgroundService().canExecuteSync()) {
                    if (getRelightStrategy() == RelightStrategy.FORWARD) {
                        request.addRequestFlag(RequestFlag.RECALCULATE);
                        newPriority += 1;
                    } else if (getRelightStrategy() == RelightStrategy.DEFERRED) {
                        request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
                    }
                    request.setPriority(newPriority);
                    getLightObserver().executeSync(request);
                } else {
                    // move to queue
                    request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
                    request.setPriority(newPriority);
                    int code = getLightObserver().notifyChangeLightLevel(request);
                    if (code == ResultCode.SUCCESS) {
                        resultCode = ResultCode.MOVED_TO_DEFERRED;
                    }
                }
                break;
            }
            case DEFERRED: {
                int newPriority = request.getPriority();
                request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
                if (sendStrategy == SendStrategy.IMMEDIATE) {
                    request.addRequestFlag(RequestFlag.SEND);
                    newPriority += 1;
                } else if (sendStrategy == SendStrategy.DEFERRED) {
                    request.addRequestFlag(RequestFlag.COMBINED_SEND);
                }
                request.setPriority(newPriority);

                int code = getLightObserver().notifyChangeLightLevel(request);
                if (code == ResultCode.SUCCESS) {
                    resultCode = ResultCode.MOVED_TO_DEFERRED;
                }
                break;
            }
        }
        return resultCode;
    }

    @Override
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
                             EditStrategy editStrategy, SendStrategy sendStrategy, ICallback callback) {
        World world = mPlugin.getServer().getWorld(worldName);
        if (mPlugin.getServer().getWorld(worldName) == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return setLightLevelLocked(world, blockX, blockY, blockZ, lightLevel, lightType, editStrategy,
                sendStrategy, callback);
    }

    /* @hide */
    public int sendCmdLocked(int cmdId, Object... args) {
        return getHandler().sendCmd(cmdId, args);
    }

    @Override
    public int sendCmd(int cmdId, Object... args) {
        return sendCmdLocked(cmdId, args);
    }

    @Override
    public BukkitPlugin getPlugin() {
        return mPlugin;
    }

    @Override
    public boolean isMainThread() {
        return getHandler().isMainThread();
    }
}
