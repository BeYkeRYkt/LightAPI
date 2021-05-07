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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler;

import org.bukkit.Bukkit;
import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitLightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.chunks.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.ILightAPI;

import java.util.ArrayList;
import java.util.List;

public abstract class BukkitHandler implements IBukkitHandler {

    private IBukkitLightAPI mInternal;

    protected IBukkitLightAPI getInternal() {
        return mInternal;
    }

    @Override
    public void initialization(ILightAPI impl) throws Exception {
        this.mInternal = (IBukkitLightAPI) impl;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public int setRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return setRawLightLevel(world, blockX, blockY, blockZ, lightLevel, lightType);
    }

    @Override
    public int getRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return getRawLightLevel(world, blockX, blockY, blockZ, lightType);
    }

    @Override
    public int recalculateLighting(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return recalculateLighting(world, blockX, blockY, blockZ, lightType);
    }

    @Override
    public List<ChunkData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ,
                                                int lightLevel, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return new ArrayList<>();
        }
        return collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightType);
    }

    @Override
    public int sendChunk(ChunkData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, data.getChunkX(), data.getChunkZ(), data.getSectionMaskSky(),
                data.getSectionMaskBlock());
    }

    @Override
    public int sendChunk(String worldName, int chunkX, int chunkZ) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, chunkX, chunkZ);
    }

    @Override
    public int sendChunk(String worldName, int chunkX, int chunkZ, int chunkSectionY) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, chunkX, chunkZ, chunkSectionY);
    }

    @Override
    public int sendChunk(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
    }
}
