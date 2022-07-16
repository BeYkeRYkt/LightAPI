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
package ru.beykerykt.minecraft.lightapi.common.internal.engine.sched;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

/**
 * Abstract class for scheduled light engines
 */
public abstract class ScheduledLightEngineImpl implements IScheduledLightEngine {

    protected final Queue<Request> lightQueue = new PriorityBlockingQueue<>(20,
            (o1, o2) -> o2.getPriority() - o1.getPriority());
    protected final Queue<Request> relightQueue = new PriorityBlockingQueue<>(20,
            (o1, o2) -> o2.getPriority() - o1.getPriority());
    protected final Queue<Request> sendQueue = new PriorityBlockingQueue<>(20,
            (o1, o2) -> o2.getPriority() - o1.getPriority());
    private final IBackgroundService mBackgroundService;
    private final long TICK_MS = 50;
    private final IPlatformImpl mPlatformImpl;
    protected long maxTimeMsPerTick;
    protected int maxRequestCount;
    protected RelightPolicy mRelightPolicy;
    private IScheduler mScheduler;
    private int requestCount = 0;
    private long penaltyTime = 0;

    public ScheduledLightEngineImpl(IPlatformImpl platformImpl, IBackgroundService service, RelightPolicy strategy,
            int maxRequestCount, int maxTimeMsPerTick) {
        this.mPlatformImpl = platformImpl;
        this.mBackgroundService = service;
        this.mRelightPolicy = strategy;
        this.maxRequestCount = maxRequestCount;
        this.maxTimeMsPerTick = maxTimeMsPerTick;
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatformImpl;
    }

    protected IBackgroundService getBackgroundService() {
        return mBackgroundService;
    }

    protected boolean canExecuteSync() {
        return getBackgroundService().canExecuteSync(maxTimeMsPerTick) && (penaltyTime < maxTimeMsPerTick)
                && getScheduler().canExecute();
    }

    @Override
    public void onStart() {
        if (getScheduler() != null) {
            getPlatformImpl().debug(getClass().getName() + " is started!");
        }
    }

    @Override
    public void onShutdown() {
        getPlatformImpl().debug(getClass().getName() + " is shutdown!");
        while (lightQueue.peek() != null) {
            Request request = lightQueue.poll();
            handleLightRequest(request);
        }
        while (relightQueue.peek() != null) {
            Request request = relightQueue.poll();
            handleRelightRequest(request);
        }
        while (sendQueue.peek() != null) {
            Request request = sendQueue.poll();
            handleSendRequest(request);
        }
        lightQueue.clear();
        relightQueue.clear();
        sendQueue.clear();
    }

    @Override
    public RelightPolicy getRelightPolicy() {
        return mRelightPolicy;
    }

    /* @hide */
    private int checkLightLocked(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        return ResultCode.NOT_IMPLEMENTED;
    }

    @Override
    public int checkLight(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        if (getBackgroundService().isMainThread()) {
            return checkLightLocked(worldName, blockX, blockY, blockZ, lightFlags);
        } else {
            synchronized (lightQueue) {
                return checkLightLocked(worldName, blockX, blockY, blockZ, lightFlags);
            }
        }
    }

    /* @hide */
    private int setLightLevelLocked(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
            EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback) {
        int resultCode = ResultCode.SUCCESS;
        Request request = getScheduler().createRequest(RequestFlag.EDIT, worldName, blockX, blockY, blockZ, lightLevel,
                lightType, editPolicy, sendPolicy, callback);
        switch (editPolicy) {
            case FORCE_IMMEDIATE: {
                // Execute request immediately
                handleLightRequest(request);
                break;
            }
            case IMMEDIATE: {
                if (canExecuteSync()) {
                    // Execute the request only if we can provide it
                    long startTime = System.currentTimeMillis();
                    handleLightRequest(request);
                    long time = System.currentTimeMillis() - startTime;
                    penaltyTime += time;
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
                throw new IllegalArgumentException("Not supported strategy: " + editPolicy.name());
        }
        return resultCode;
    }

    @Override
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        if (getBackgroundService().isMainThread()) {
            return setLightLevelLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy,
                    sendPolicy,
                    callback);
        } else {
            synchronized (lightQueue) {
                return setLightLevelLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy,
                        sendPolicy,
                        callback);
            }
        }
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

    /* @hide */
    private int notifySendLocked(Request request) {
        if (request != null) {
            sendQueue.add(request);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifySend(Request request) {
        if (getBackgroundService().isMainThread()) {
            return notifySendLocked(request);
        } else {
            synchronized (sendQueue) {
                return notifySendLocked(request);
            }
        }
    }

    private void handleLightRequest(Request request) {
        if (getBackgroundService().isMainThread()) {
            getScheduler().handleLightRequest(request);
        } else {
            synchronized (request) {
                getScheduler().handleLightRequest(request);
            }
        }
    }

    private void handleRelightRequest(Request request) {
        if (getBackgroundService().isMainThread()) {
            getScheduler().handleRelightRequest(request);
        } else {
            synchronized (request) {
                getScheduler().handleRelightRequest(request);
            }
        }
    }

    private void handleSendRequest(Request request) {
        if (getBackgroundService().isMainThread()) {
            getScheduler().handleSendRequest(request);
        } else {
            synchronized (request) {
                getScheduler().handleSendRequest(request);
            }
        }
    }

    private void handleLightQueueLocked() {
        if (!getScheduler().canExecute()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        requestCount = 0;
        while (lightQueue.peek() != null) {
            getPlatformImpl().debug("handleLightQueueLocked()");
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
            handleLightRequest(request);
            requestCount++;
        }
    }

    private void handleRelightQueueLocked() {
        if (!getScheduler().canExecute()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        requestCount = 0;
        while (relightQueue.peek() != null) {
            getPlatformImpl().debug("handleRelightQueueLocked()");
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
            handleRelightRequest(request);
            requestCount++;
        }
    }

    private void handleSendQueueLocked() {
        if (!getScheduler().canExecute()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        requestCount = 0;
        while (sendQueue.peek() != null) {
            getPlatformImpl().debug("handleSendQueueLocked()");
            long time = System.currentTimeMillis() - startTime;
            if (time > maxTimeMsPerTick) {
                getPlatformImpl().debug("handleSendQueueLocked: maxRelightTimePerTick is reached (" + time + " ms)");
                break;
            }
            if (requestCount > maxRequestCount) {
                getPlatformImpl().debug("handleSendQueueLocked: maxRequestCount is reached (" + requestCount + ")");
                break;
            }
            Request request = sendQueue.poll();
            handleSendRequest(request);
            requestCount++;
        }
    }

    @Override
    public void run() {
        synchronized (lightQueue) {
            handleLightQueueLocked();
        }

        synchronized (relightQueue) {
            handleRelightQueueLocked();
        }

        synchronized (sendQueue) {
            handleSendQueueLocked();
        }
    }

    protected void onTickPenaltyTime() {
        if (penaltyTime > 0) {
            penaltyTime -= TICK_MS;
        } else if (penaltyTime < 0) {
            penaltyTime = 0;
        }
    }
}
