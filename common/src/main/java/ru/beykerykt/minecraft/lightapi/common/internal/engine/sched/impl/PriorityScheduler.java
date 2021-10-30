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
package ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.impl;

import java.util.List;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.IScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.IScheduledLightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.IScheduler;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.Request;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.RequestFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

/**
 * A scheduler based on a priority system
 */
public class PriorityScheduler implements IScheduler {

    private final IScheduledLightEngine mLightEngine;
    private final IScheduledChunkObserver mChunkObserver;
    private final IBackgroundService mBackgroundService;
    private final long mMaxTimeMsPerTick;

    public PriorityScheduler(IScheduledLightEngine lightEngine, IScheduledChunkObserver chunkObserver,
            IBackgroundService backgroundService, long maxTimeMsPerTick) {
        this.mLightEngine = lightEngine;
        this.mChunkObserver = chunkObserver;
        this.mBackgroundService = backgroundService;
        this.mMaxTimeMsPerTick = maxTimeMsPerTick;
    }

    private IScheduledLightEngine getLightEngine() {
        return mLightEngine;
    }

    private IScheduledChunkObserver getChunkObserver() {
        return mChunkObserver;
    }

    private IBackgroundService getBackgroundService() {
        return mBackgroundService;
    }

    @Override
    public boolean canExecute() {
        return !getChunkObserver().isBusy();
    }

    @Override
    public Request createEmptyRequest(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags, EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback) {
        // keep information about old light level
        int oldLightLevel = getLightEngine().getLightLevel(worldName, blockX, blockY, blockZ, lightFlags);
        return new Request(Request.DEFAULT_PRIORITY, 0, worldName, blockX, blockY, blockZ, oldLightLevel, lightLevel,
                lightFlags, callback);
    }

    @Override
    public Request createRequest(int defaultFlag, String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags, EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback) {
        Request request = createEmptyRequest(worldName, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy,
                sendPolicy, callback);
        request.setPriority(Request.DEFAULT_PRIORITY);
        request.setRequestFlags(defaultFlag);

        switch (editPolicy) {
            case FORCE_IMMEDIATE: {
                request.addRequestFlag(RequestFlag.RECALCULATE);
                request.addRequestFlag(RequestFlag.SEPARATE_SEND);
                request.setPriority(Request.HIGH_PRIORITY);
                break;
            }
            case IMMEDIATE: {
                int newPriority = request.getPriority();
                if (sendPolicy == SendPolicy.IMMEDIATE) {
                    request.addRequestFlag(RequestFlag.SEPARATE_SEND);
                    newPriority += 1;
                } else if (sendPolicy == SendPolicy.DEFERRED) {
                    request.addRequestFlag(RequestFlag.COMBINED_SEND);
                }

                if (getBackgroundService().canExecuteSync(mMaxTimeMsPerTick)) {
                    if (getLightEngine().getRelightPolicy() == RelightPolicy.FORWARD) {
                        request.addRequestFlag(RequestFlag.RECALCULATE);
                        newPriority += 1;
                    } else if (getLightEngine().getRelightPolicy() == RelightPolicy.DEFERRED) {
                        request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
                    }
                } else {
                    // move to queue
                    request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
                }
                request.setPriority(newPriority);
                break;
            }
            case DEFERRED: {
                int newPriority = request.getPriority();
                request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
                if (sendPolicy == SendPolicy.IMMEDIATE) {
                    request.addRequestFlag(RequestFlag.SEPARATE_SEND);
                    newPriority += 1;
                } else if (sendPolicy == SendPolicy.DEFERRED) {
                    request.addRequestFlag(RequestFlag.COMBINED_SEND);
                }
                request.setPriority(newPriority);
                break;
            }
        }
        return request;
    }

    @Override
    public int handleLightRequest(Request request) {
        if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.EDIT)) {
            request.removeRequestFlag(RequestFlag.EDIT);
            int resultCode = getLightEngine().setRawLightLevel(request.getWorldName(), request.getBlockX(),
                    request.getBlockY(), request.getBlockZ(), request.getLightLevel(), request.getLightFlags());
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.EDIT, resultCode);
            }

            if (resultCode == ResultCode.SUCCESS) {
                if (request.getLightLevel() == 0) {
                    // HAX: If the light is successfully removed, then add an additional flag, since the
                    // return value of the recalculation may be equal to RECALCULATE_NO_CHANGES.
                    request.addRequestFlag(RequestFlag.FORCE_SEND);
                }
                handleRelightRequest(request);
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int handleRelightRequest(Request request) {
        if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.RECALCULATE)) {
            request.removeRequestFlag(RequestFlag.RECALCULATE);
            int resultCode = getLightEngine().recalculateLighting(request.getWorldName(), request.getBlockX(),
                    request.getBlockY(), request.getBlockZ(), request.getLightFlags());
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.RECALCULATE, resultCode);
            }

            if (resultCode == ResultCode.SUCCESS || FlagUtils.isFlagSet(request.getRequestFlags(),
                    RequestFlag.FORCE_SEND)) {
                if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.COMBINED_SEND)) {
                    getLightEngine().notifySend(request);
                } else if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.SEPARATE_SEND)) {
                    handleSendRequest(request);
                }
            }
        } else if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.DEFERRED_RECALCULATE)) {
            request.removeRequestFlag(RequestFlag.DEFERRED_RECALCULATE);
            request.addRequestFlag(RequestFlag.RECALCULATE);
            // HAX: Add an additional flag, since the return value of the recalculation can be equal to RECALCULATE_NO_CHANGES.
            request.addRequestFlag(RequestFlag.FORCE_SEND);
            int resultCode = getLightEngine().notifyRecalculate(request);
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.DEFERRED_RECALCULATE, resultCode);
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int handleSendRequest(Request request) {
        if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.COMBINED_SEND)) {
            request.removeRequestFlag(RequestFlag.COMBINED_SEND);
            int resultCode = getChunkObserver().notifyUpdateChunks(request.getWorldName(), request.getBlockX(),
                    request.getBlockY(), request.getBlockZ(),
                    Math.max(request.getOldLightLevel(), request.getLightLevel()), request.getLightFlags());
            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.COMBINED_SEND, resultCode);
            }
        } else if (FlagUtils.isFlagSet(request.getRequestFlags(), RequestFlag.SEPARATE_SEND)) {
            request.removeRequestFlag(RequestFlag.SEPARATE_SEND);
            // send updated chunks now
            List<IChunkData> chunkDataList = getChunkObserver().collectChunkSections(request.getWorldName(),
                    request.getBlockX(), request.getBlockY(), request.getBlockZ(),
                    Math.max(request.getOldLightLevel(), request.getLightLevel()), request.getLightFlags());
            for (int i = 0; i < chunkDataList.size(); i++) {
                IChunkData data = chunkDataList.get(i);
                getChunkObserver().sendChunk(data);
            }

            if (request.getCallback() != null) {
                request.getCallback().onResult(RequestFlag.SEPARATE_SEND, ResultCode.SUCCESS);
            }
        }
        return ResultCode.SUCCESS;
    }
}
