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
package ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.impl;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.ILightListener;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.IScheduledLightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.IScheduler;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.Request;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.RequestFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Abstract class for scheduled light engines
 */
public abstract class ScheduledLightEngine implements IScheduledLightEngine {
    protected long maxTimeMsPerTick;
    protected int maxRequestCount;
    private final IBackgroundService mBackgroundService;

    private final List<ILightListener> mListeners;

    private IPlatformImpl mPlatformImpl;
    private IScheduler mScheduler;
    protected RelightStrategy mRelightStrategy;

    private int requestCount = 0;
    private Queue<Request> lightQueue =
            new PriorityBlockingQueue<>(10, (o1, o2) -> o2.getPriority() - o1.getPriority());
    private Queue<Request> relightQueue = new PriorityBlockingQueue<>(10,
            (o1, o2) -> o2.getPriority() - o1.getPriority());

    public ScheduledLightEngine(IPlatformImpl pluginImpl, IBackgroundService service, RelightStrategy strategy) {
        this(pluginImpl, service, strategy, 250, 50); // with default params
    }

    public ScheduledLightEngine(IPlatformImpl platformImpl, IBackgroundService service, RelightStrategy strategy,
                                int maxRequestCount, int maxTimeMsPerTick) {
        this.mPlatformImpl = platformImpl;
        this.mBackgroundService = service;
        this.mRelightStrategy = strategy;
        this.maxRequestCount = maxRequestCount;
        this.maxTimeMsPerTick = maxTimeMsPerTick;
        this.mListeners = new ArrayList<>();
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatformImpl;
    }

    protected IBackgroundService getBackgroundService() {
        return mBackgroundService;
    }

    protected List<ILightListener> getListeners() {
        return mListeners;
    }

    private void notifyLightChanged(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
                                    int lightType) {
        Iterator<ILightListener> iterator = getListeners().iterator();
        while (iterator.hasNext()) {
            ILightListener listener = iterator.next();
            listener.onLightLevelChanged(worldName, blockX, blockY, blockZ, lightLevel, lightType);
        }
    }

    protected boolean canExecuteSync() {
        return getBackgroundService().canExecuteSync();
    }

    @Override
    public void onStart() {
        if (getScheduler() != null) {
            getBackgroundService().addToRepeat(this);
        }
        getPlatformImpl().info(getClass().getName() + " is started!");
    }

    @Override
    public void onShutdown() {
        getPlatformImpl().info(getClass().getName() + " is shutdown!");
        getBackgroundService().removeRepeat(this);
        lightQueue.clear();
        lightQueue = null;
        relightQueue.clear();
        relightQueue = null;
        mPlatformImpl = null;
    }

    @Override
    public RelightStrategy getRelightStrategy() {
        return mRelightStrategy;
    }

    /* @hide */
    private int setLightLevelLocked(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
                                    EditStrategy editStrategy, SendStrategy sendStrategy, ICallback callback) {
        int resultCode = ResultCode.SUCCESS;
        Request request = getScheduler().createRequest(RequestFlag.EDIT, worldName, blockX, blockY, blockZ,
                lightLevel, lightType, editStrategy, sendStrategy, callback);
        switch (editStrategy) {
            case FORCE_IMMEDIATE: {
                // Execute request immediately
                handleRequest(request);
                break;
            }
            case IMMEDIATE: {
                if (canExecuteSync()) {
                    // Execute the request only if we can provide it
                    handleRequest(request);
                } else {
                    // add request to queue
                    int code = notifyChangeLightLevel(request);
                    if (code == ResultCode.SUCCESS) {
                        resultCode = ResultCode.MOVED_TO_DEFERRED;
                    }
                }
                break;
            }
            case DEFERRED: {
                // add request to queue
                int code = notifyChangeLightLevel(request);
                if (code == ResultCode.SUCCESS) {
                    resultCode = ResultCode.MOVED_TO_DEFERRED;
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Not supported strategy: " + editStrategy.name());
        }
        return resultCode;
    }

    @Override
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
                             EditStrategy editStrategy, SendStrategy sendStrategy, ICallback callback) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return setLightLevelLocked(worldName, blockX, blockY, blockZ, lightLevel, lightType, editStrategy,
                sendStrategy, callback);
    }

    @Override
    public void addListener(ILightListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeListener(ILightListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public IScheduler getScheduler() {
        if (mScheduler == null) {
            throw new NullPointerException("Scheduler is null!");
        }
        return mScheduler;
    }

    @Override
    public void setScheduler(IScheduler scheduler) {
        mScheduler = scheduler;
    }

    /* @hide */
    private int notifyChangeLightLevelLocked(Request request) {
        if (request != null) {
            lightQueue.add(request);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyChangeLightLevel(Request request) {
        if (getBackgroundService().isMainThread()) {
            return notifyChangeLightLevelLocked(request);
        } else {
            synchronized (lightQueue) {
                return notifyChangeLightLevelLocked(request);
            }
        }
    }

    /* @hide */
    private int notifyRecalculateLocked(Request request) {
        if (request != null) {
            relightQueue.add(request);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyRecalculate(Request request) {
        if (getBackgroundService().isMainThread()) {
            return notifyRecalculateLocked(request);
        } else {
            synchronized (relightQueue) {
                return notifyRecalculateLocked(request);
            }
        }
    }

    private void handleRequest(Request request) {
        if (getBackgroundService().isMainThread()) {
            getScheduler().handleRequest(request);
        } else {
            synchronized (request) {
                getScheduler().handleRequest(request);
            }
        }
    }

    private void handleLightQueueLocked() {
        long startTime = System.currentTimeMillis();
        requestCount = 0;
        while (lightQueue.peek() != null) {
            long time = System.currentTimeMillis() - startTime;
            if (time > maxTimeMsPerTick) {
                getPlatformImpl().debug("handleLightQueueLocked: maxRelightTimePerTick is reached (" + time + " ms)");
                break;
            }
            if (requestCount > maxRequestCount) {
                getPlatformImpl().debug("handleLightQueueLocked: maxRequestCount is reached (" + requestCount + ")");
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
                getPlatformImpl().debug("handleRelightQueueLocked: maxRelightTimePerTick is reached (" + time + " ms)");
                break;
            }
            if (requestCount > maxRequestCount) {
                getPlatformImpl().debug("handleRelightQueueLocked: maxRequestCount is reached (" + requestCount + ")");
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
}
