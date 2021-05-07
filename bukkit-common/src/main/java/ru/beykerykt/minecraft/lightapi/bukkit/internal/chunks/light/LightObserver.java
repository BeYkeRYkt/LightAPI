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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.light;

import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitLightAPI;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IBukkitHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.BackgroundService;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.Request;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.RequestFlag;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class LightObserver implements ILightObserver {
    private long maxTimeMsPerTick = 25;
    private int maxRequestCount = 128;
    private int requestCount = 0;

    private IBukkitLightAPI mInternal;
    private IBukkitHandler mHandler;
    private BackgroundService mBackgroundService;
    private Queue<Request> lightQueue =
            new PriorityBlockingQueue<>(10, (o1, o2) -> o2.getPriority() - o1.getPriority());
    private Queue<Request> relightQueue = new PriorityBlockingQueue<>(10,
            (o1, o2) -> o2.getPriority() - o1.getPriority());
    private Runnable runnable = () -> onTick();

    public LightObserver(IBukkitLightAPI impl, IBukkitHandler handler, BackgroundService service) {
        this.mInternal = impl;
        this.mHandler = handler;
        this.mBackgroundService = service;
    }

    @Override
    public void start() {
        int c_maxIterations =
                getInternal().getPlugin().getConfig().getInt(ConfigurationPath.LIGHT_OBSERVER_MAX_ITERATIONS_IN_PER_TICK);
        this.maxRequestCount = c_maxIterations;

        int c_maxTimeMsPerTick =
                getInternal().getPlugin().getConfig().getInt(ConfigurationPath.LIGHT_OBSERVER_MAX_TIME_MS_IN_PER_TICK);
        this.maxTimeMsPerTick = c_maxTimeMsPerTick;

        mBackgroundService.addToRepeat(runnable);
    }

    @Override
    public void shutdown() {
        mBackgroundService.removeRepeat(runnable);
        lightQueue.clear();
        lightQueue = null;
        relightQueue.clear();
        relightQueue = null;
        mInternal = null;
        mHandler = null;
    }

    @Override
    public void executeSync(Request request) {
        handleRequestLocked(request);
    }

    private IBukkitLightAPI getInternal() {
        return mInternal;
    }

    private IBukkitHandler getHandler() {
        return this.mHandler;
    }

    private void handleRequestLocked(Request request) {
        getInternal().debug("Current Thread: " + Thread.currentThread().getName());

        if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.EDIT)) {
            request.removeRequestFlag(RequestFlag.EDIT);
            int resultCode = getHandler().setRawLightLevel(request.getWorld(), request.getBlockX(), request.getBlockY()
                    , request.getBlockZ(), request.getLightLevel(), request.getLightType());
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.EDIT, resultCode);
            }
        }

        if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.RECALCULATE)) {
            request.removeRequestFlag(RequestFlag.RECALCULATE);
            int resultCode = getHandler().recalculateLighting(request.getWorld(), request.getBlockX(),
                    request.getBlockY(), request.getBlockZ(),
                    request.getLightType());
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.RECALCULATE, resultCode);
            }

            if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.COMBINED_SEND)) {
                request.removeRequestFlag(RequestFlag.COMBINED_SEND);
                resultCode = getInternal().getChunkObserver().notifyUpdateChunks(request.getWorld(),
                        request.getBlockX(), request.getBlockY(),
                        request.getBlockZ(), request.getOldLightLevel() > request.getLightLevel() ?
                                request.getOldLightLevel() :
                                request.getLightLevel(), request.getLightType());
                if (request.getCallback() != null) {
                    request.getCallback().onResult(RequestFlag.COMBINED_SEND, resultCode);
                }
            } else if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.SEND)) {
                request.removeRequestFlag(RequestFlag.SEND);
                if (resultCode == ResultCode.SUCCESS || FlagUtils.isFlagSet(request.getRequestFlags(),
                        RequestFlag.MOVED_TO_FORWARD)) {
                    getInternal().getChunkObserver().sendUpdateChunks(request.getWorld(), request.getBlockX(),
                            request.getBlockY(),
                            request.getBlockZ(), request.getOldLightLevel() > request.getLightLevel() ?
                                    request.getOldLightLevel() :
                                    request.getLightLevel(), request.getLightType());
                    if (request.getCallback() != null) {
                        request.getCallback().onResult(RequestFlag.SEND, ResultCode.SUCCESS);
                    }
                }
            }
        } else if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.DEFERRED_RECALCULATE)) {
            request.removeRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
            request.addRequestFlag(RequestFlag.RECALCULATE);
            request.addRequestFlag(RequestFlag.MOVED_TO_FORWARD);
            int resultCode = notifyRecalculate(request);
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.DEFERRED_RECALCULATE, resultCode);
            }
        }
    }

    private void handleRequest(Request request) {
        if (getInternal().isMainThread()) {
            handleRequestLocked(request);
        } else {
            synchronized (request) {
                handleRequestLocked(request);
            }
        }
    }

    private void handleLightQueueLocked() {
        long startTime = System.currentTimeMillis();
        requestCount = 0;
        while (lightQueue.peek() != null) {
            long time = System.currentTimeMillis() - startTime;
            if (time > maxTimeMsPerTick) {
                getInternal().debug("handlePreUpdateQueueLocked: maxRelightTimePerTick (" + time + " ms)");
                break;
            }
            if (requestCount > maxRequestCount) {
                getInternal().debug("handlePreUpdateQueueLocked: maxRequestCount");
                break;
            }
            Request request = lightQueue.poll();
            handleRequest(request);
            requestCount++;
        }
    }

    private void handleRelightQueueLocked() {
        long startTime = System.currentTimeMillis();
        requestCount = 0;
        while (relightQueue.peek() != null) {
            long time = System.currentTimeMillis() - startTime;
            if (time > maxTimeMsPerTick) {
                getInternal().debug("handleUpdateQueueLocked: maxRelightTimePerTick (" + time + " ms)");
                break;
            }
            if (requestCount > maxRequestCount) {
                getInternal().debug("handleUpdateQueueLocked: maxRequestCount");
                break;
            }
            Request request = relightQueue.poll();
            handleRequest(request);
            requestCount++;
        }
    }

    @Override
    public void onTick() {
        synchronized (lightQueue) {
            handleLightQueueLocked();
        }

        synchronized (relightQueue) {
            handleRelightQueueLocked();
        }
    }

    public int notifyChangeLightLevelLocked(Request request) {
        if (request != null) {
            lightQueue.add(request);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyChangeLightLevel(Request request) {
        if (getHandler().isMainThread()) {
            return notifyChangeLightLevelLocked(request);
        } else {
            synchronized (lightQueue) {
                return notifyChangeLightLevelLocked(request);
            }
        }
    }

    private int notifyRecalculateLocked(Request request) {
        if (request != null) {
            relightQueue.add(request);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyRecalculate(Request request) {
        if (getHandler().isMainThread()) {
            return notifyRecalculateLocked(request);
        } else {
            synchronized (relightQueue) {
                return notifyRecalculateLocked(request);
            }
        }
    }
}
