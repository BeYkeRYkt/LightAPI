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
package ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.impl;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.IScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class ScheduledChunkObserver implements IScheduledChunkObserver {

    private IPlatformImpl mPlatformImpl;

    private IBackgroundService mBackgroundService;
    private Map<Long, IChunkData> observedChunks = new HashMap<>();

    public ScheduledChunkObserver(IPlatformImpl platform, IBackgroundService service) {
        this.mPlatformImpl = platform;
        this.mBackgroundService = service;
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatformImpl;
    }

    protected IBackgroundService getBackgroundService() {
        return mBackgroundService;
    }

    @Override
    public void onStart() {
        getBackgroundService().addToRepeat(this);
    }

    @Override
    public void onShutdown() {
        getBackgroundService().removeRepeat(this);
        observedChunks.clear();
        mPlatformImpl = null;
    }

    private int getDeltaLight(int x, int dx) {
        return (((x ^ ((-dx >> 4) & 15)) + 1) & (-(dx & 1)));
    }

    private long chunkCoordToLong(int chunkX, int chunkZ) {
        long l = chunkX;
        l = (l << 32) | (chunkZ & 0xFFFFFFFFL);
        return l;
    }

    public abstract IChunkData createChunkData(String worldName, int chunkX, int chunkZ);

    public abstract boolean isValidChunkSection(int sectionY);

    /* @hide */
    private int notifyUpdateChunksLocked(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
                                         int lightType) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }

        int finalLightLevel = lightLevel;
        if (lightLevel < 0) {
            finalLightLevel = 0;
        } else if (lightLevel > 15) {
            finalLightLevel = 15;
        }

        // start watching chunks
        int CHUNK_RADIUS = 1;
        for (int dX = -CHUNK_RADIUS; dX <= CHUNK_RADIUS; dX++) {
            int lightLevelX = finalLightLevel - getDeltaLight(blockX & 15, dX);
            if (lightLevelX > 0) {
                for (int dZ = -CHUNK_RADIUS; dZ <= CHUNK_RADIUS; dZ++) {
                    int lightLevelZ = lightLevelX - getDeltaLight(blockZ & 15, dZ);
                    if (lightLevelZ > 0) {
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidChunkSection(sectionY)) {
                                    int chunkX = (blockX >> 4) + dX;
                                    int chunkZ = (blockZ >> 4) + dZ;
                                    long chunkCoord = chunkCoordToLong(chunkX, chunkZ);

                                    IChunkData data;
                                    if (observedChunks.containsKey(chunkCoord)) {
                                        data = observedChunks.get(chunkCoord);
                                    } else {
                                        data = createChunkData(worldName, chunkX, chunkZ);
                                        // register new chunk data
                                        observedChunks.put(chunkCoord, data);
                                    }
                                    data.markSectionForUpdate(lightType, sectionY);
                                }
                            }
                        }
                    }
                }
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyUpdateChunks(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        if (getBackgroundService().isMainThread()) {
            return notifyUpdateChunksLocked(worldName, blockX, blockY, blockZ, lightLevel, lightType);
        } else {
            synchronized (observedChunks) {
                return notifyUpdateChunksLocked(worldName, blockX, blockY, blockZ, lightLevel, lightType);
            }
        }
    }

    private void handleChunksLocked() {
        Iterator it = observedChunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, IChunkData> pair = (Map.Entry<Long, IChunkData>) it.next();
            IChunkData data = pair.getValue();
            int resultCode = sendChunk(data);
            if (resultCode == ResultCode.SUCCESS) {
                data.clearUpdate();
                it.remove();
            }
        }
    }

    @Override
    public void onTick() {
        synchronized (observedChunks) {
            handleChunksLocked();
        }
    }
}
