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
package ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.IScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

public abstract class ScheduledChunkObserver implements IScheduledChunkObserver {

    private final IBackgroundService mBackgroundService;
    private final Map<Long, IChunkData> observedChunks = new HashMap<>();
    private final IPlatformImpl mPlatformImpl;
    private boolean isBusy = false;

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
        getPlatformImpl().debug(getClass().getName() + " is started!");
    }

    @Override
    public void onShutdown() {
        getPlatformImpl().debug(getClass().getName() + " is shutdown!");
        observedChunks.clear();
    }

    @Override
    public boolean isBusy() {
        return isBusy;
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

    public abstract boolean isChunkLoaded(String worldName, int chunkX, int chunkZ);

    /* @hide */
    private int notifyUpdateChunksLocked(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightType) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }

        int finalLightLevel = lightLevel < 0 ? 0 : lightLevel > 15 ? 15 : lightLevel;

        // start watching chunks
        int CHUNK_RADIUS = 1;
        for (int dX = -CHUNK_RADIUS; dX <= CHUNK_RADIUS; dX++) {
            int lightLevelX = finalLightLevel - getDeltaLight(blockX & 15, dX);
            if (lightLevelX > 0) {
                for (int dZ = -CHUNK_RADIUS; dZ <= CHUNK_RADIUS; dZ++) {
                    int lightLevelZ = lightLevelX - getDeltaLight(blockZ & 15, dZ);
                    if (lightLevelZ > 0) {
                        int chunkX = (blockX >> 4) + dX;
                        int chunkZ = (blockZ >> 4) + dZ;
                        if (!isChunkLoaded(worldName, chunkX, chunkZ)) {
                            continue;
                        }
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidChunkSection(sectionY)) {
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
    public int notifyUpdateChunks(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags) {
        if (getBackgroundService().isMainThread()) {
            return notifyUpdateChunksLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags);
        } else {
            synchronized (observedChunks) {
                return notifyUpdateChunksLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags);
            }
        }
    }

    private void handleChunksLocked() {
        isBusy = true;
        Iterator it = observedChunks.entrySet().iterator();
        if (observedChunks.size() > 0) {
            getPlatformImpl().debug("observedChunks size: " + observedChunks.size());
        }
        while (it.hasNext()) {
            Map.Entry<Long, IChunkData> pair = (Map.Entry<Long, IChunkData>) it.next();
            IChunkData data = pair.getValue();
            int resultCode = sendChunk(data);
            if (resultCode == ResultCode.SUCCESS) {
                data.clearUpdate();
                it.remove();
            }
        }
        isBusy = false;
    }

    @Override
    public void run() {
        synchronized (observedChunks) {
            handleChunksLocked();
        }
    }
}
