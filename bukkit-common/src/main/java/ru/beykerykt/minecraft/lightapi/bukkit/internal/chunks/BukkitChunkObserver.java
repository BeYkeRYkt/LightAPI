/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks;

import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitLightAPI;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.BackgroundService;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.chunks.ChunkData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BukkitChunkObserver implements IChunkObserver {
    private IBukkitLightAPI mInternal;
    private IHandler mHandler;
    private BackgroundService mBackgroundService;
    private List<ChunkData> queueChunks = new ArrayList<>();
    private boolean isMergeEnabled;

    private Runnable runnable = () -> onTick();

    public BukkitChunkObserver(IBukkitLightAPI impl, IHandler handler, BackgroundService service) {
        this.mInternal = impl;
        this.mHandler = handler;
        this.mBackgroundService = service;
    }

    private IBukkitLightAPI getInternal() {
        return mInternal;
    }

    private IHandler getHandler() {
        return this.mHandler;
    }

    private ChunkData getQueueChunkData(World world, int chunkX, int chunkZ) {
        Iterator<ChunkData> it = queueChunks.iterator();
        ChunkData data = null;
        while (it.hasNext()) {
            ChunkData data_c = it.next();
            if (data_c.getWorldName().equals(world.getName()) &&
                    data_c.getChunkX() == chunkX &&
                    data_c.getChunkZ() == chunkZ) {
                data = data_c;
                break;
            }
        }
        return data;
    }

    @Override
    public void start() {
        mBackgroundService.addToRepeat(runnable);
        boolean mergeEnable =
                getInternal().getPlugin().getConfig().getBoolean(ConfigurationPath.CHUNK_OBSERVER_MERGE_CHUNK_SECTIONS);
        setMergeChunksEnabled(mergeEnable);
    }

    @Override
    public void shutdown() {
        mBackgroundService.removeRepeat(runnable);
        queueChunks.clear();
        queueChunks = null;
        mInternal = null;
        mHandler = null;
    }

    private void handleChunksLocked() {
        Iterator<ChunkData> it = queueChunks.iterator();
        while (it.hasNext()) {
            ChunkData data = it.next();
            if (data.getSectionMaskBlock() != 0 || data.getSectionMaskSky() != 0) {
                getHandler().sendChunk(data);
            }
            it.remove();
        }
    }

    @Override
    public void onTick() {
        synchronized (queueChunks) {
            handleChunksLocked();
        }
    }

    @Override
    public boolean isMergeChunksEnabled() {
        return this.isMergeEnabled;
    }

    @Override
    public void setMergeChunksEnabled(boolean enabled) {
        this.isMergeEnabled = enabled;
    }

    private int notifyUpdateChunksLocked(World world, int blockX, int blockY, int blockZ, int lightLevel,
                                         int lightType) {
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        List<ChunkData> input = getHandler().collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightType);
        Iterator<ChunkData> iit = input.iterator();
        while (iit.hasNext()) {
            ChunkData data = iit.next();

            if (isMergeChunksEnabled()) {
                Iterator<ChunkData> it = queueChunks.iterator();
                boolean found = false;
                while (it.hasNext()) {
                    ChunkData data_c = it.next();
                    if (data_c.getWorldName().equals(data.getWorldName()) &&
                            data_c.getChunkX() == data.getChunkX() &&
                            data_c.getChunkZ() == data.getChunkZ()) {
                        data_c.addSectionMaskBlock(data.getSectionMaskBlock());
                        data_c.addSectionMaskSky(data.getSectionMaskSky());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    queueChunks.add(data);
                }
            } else {
                if (!queueChunks.contains(data)) {
                    queueChunks.add(data);
                }
            }
            iit.remove();
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyUpdateChunks(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        if (getHandler().isMainThread()) {
            return notifyUpdateChunksLocked(world, blockX, blockY, blockZ, lightLevel, lightType);
        } else {
            synchronized (queueChunks) {
                return notifyUpdateChunksLocked(world, blockX, blockY, blockZ, lightLevel, lightType);
            }
        }
    }

    private int notifyUpdateChunkLocked(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        if (isMergeChunksEnabled()) {
            Iterator<ChunkData> it = queueChunks.iterator();
            boolean found = false;
            while (it.hasNext()) {
                ChunkData data_c = it.next();
                if (data_c.getWorldName().equals(world.getName()) &&
                        data_c.getChunkX() == chunkX &&
                        data_c.getChunkZ() == chunkZ) {
                    data_c.addSectionMaskSky(sectionMaskSky);
                    data_c.addSectionMaskBlock(sectionMaskBlock);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Not found? Create a new one
                ChunkData data = new ChunkData(world.getName(), chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
                queueChunks.add(data);
            }
        } else {
            ChunkData data = new ChunkData(world.getName(), chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
            if (!queueChunks.contains(data)) {
                queueChunks.add(data);
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int notifyUpdateChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (getHandler().isMainThread()) {
            return notifyUpdateChunkLocked(world, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
        } else {
            synchronized (queueChunks) {
                return notifyUpdateChunkLocked(world, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
            }
        }
    }

    @Override
    public void sendUpdateChunks(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        if (world == null) {
            return;
        }
        List<ChunkData> input = getHandler().collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightType);
        Iterator<ChunkData> it = input.iterator();
        while (it.hasNext()) {
            ChunkData data = it.next();
            data.setSectionMaskSky(0);
            getHandler().sendChunk(data);

            ChunkData queueData = getQueueChunkData(world, data.getChunkX(), data.getChunkZ());
            if (queueData != null) {
                queueData.removeSectionMaskBlock(data.getSectionMaskBlock());
                queueData.removeSectionMaskSky(data.getSectionMaskSky());
            }

            it.remove();
        }
    }

    private long getIndexFromChunkCoords(int chunkX, int chunkZ) {
        long l = chunkX;
        l = (l << 32) | (chunkZ & 0xFFFFFFFFL);

        // restore:
        //long x = ((l >> 32) & 0xFFFFFFFF);
        //long z = (l & 0xFFFFFFFFL);
        return l;
    }
}
