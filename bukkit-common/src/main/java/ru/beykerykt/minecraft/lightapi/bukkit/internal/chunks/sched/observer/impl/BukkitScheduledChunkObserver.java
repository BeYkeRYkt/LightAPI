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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.sched.observer.impl;

import org.bukkit.Bukkit;
import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.impl.ScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

import java.util.ArrayList;
import java.util.List;

public class BukkitScheduledChunkObserver extends ScheduledChunkObserver {

    private IHandler mHandler;

    public BukkitScheduledChunkObserver(IBukkitPlatformImpl platform, IBackgroundService service, IHandler handler) {
        super(platform, service);
        this.mHandler = handler;
    }

    private IHandler getHandler() {
        return mHandler;
    }

    @Override
    protected IBukkitPlatformImpl getPlatformImpl() {
        return (IBukkitPlatformImpl) super.getPlatformImpl();
    }

    @Override
    public IChunkData createChunkData(String worldName, int chunkX, int chunkZ) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return null;
        }
        return getHandler().createChunkData(worldName, chunkX, chunkZ);
    }

    @Override
    public boolean isValidChunkSection(int sectionY) {
        return getHandler().isValidChunkSection(sectionY);
    }

    @Override
    public List<IChunkData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return new ArrayList<>();
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightType);
    }

    @Override
    public int sendChunk(IChunkData data) {
        return getHandler().sendChunk(data);
    }

    @Override
    public int sendChunk(String worldName, int chunkX, int chunkZ) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().sendChunk(world, chunkX, chunkZ);
    }

    @Override
    public int sendChunk(String worldName, int chunkX, int chunkZ, int chunkSectionY) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().sendChunk(world, chunkX, chunkZ, chunkSectionY);
    }

    @Override
    public int sendChunk(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        World world = Bukkit.getWorld(worldName);
        return getHandler().sendChunk(world, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
    }
}