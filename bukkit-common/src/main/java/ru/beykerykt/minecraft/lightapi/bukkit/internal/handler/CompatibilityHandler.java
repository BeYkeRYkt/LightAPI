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

package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.ArrayList;
import java.util.List;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.BitChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class CompatibilityHandler implements IHandler {

    private static final BlockFace[] SIDES = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };
    private BukkitPlatformImpl mPlatform;

    private BukkitPlatformImpl getPlatformImpl() {
        return mPlatform;
    }

    @Override
    public void onInitialization(BukkitPlatformImpl impl) throws Exception {
        this.mPlatform = impl;
    }

    @Override
    public void onShutdown(BukkitPlatformImpl impl) {
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public LightEngineType getLightEngineType() {
        return LightEngineType.COMPATIBILITY;
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return LightEngineVersion.UNKNOWN;
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void onWorldLoad(WorldLoadEvent event) {
    }

    @Override
    public void onWorldUnload(WorldUnloadEvent event) {
    }

    @Override
    public boolean isLightingSupported(World world, int lightFlags) {
        return FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING);
    }

    private void setLightBlock(Block block, int finalLightLevel) {
        Material newMaterial = finalLightLevel > 0 ? Material.LIGHT : Material.AIR;
        block.setType(newMaterial);
        if (newMaterial == Material.LIGHT) {
            Levelled level = (Levelled) Material.LIGHT.createBlockData();
            level.setLevel(finalLightLevel);
            block.setBlockData(level, true);
        }
    }

    private int setRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags) {
        if (!isLightingSupported(world, lightFlags)) {
            return ResultCode.NOT_IMPLEMENTED;
        }
        int finalLightLevel = lightLevel < 0 ? 0 : Math.min(lightLevel, 15);
        Block block = world.getBlockAt(blockX, blockY, blockZ);
        Material material = block.getType();

        if (material.isAir() || material == Material.LIGHT) {
            setLightBlock(block, finalLightLevel);
        } else {
            for (BlockFace side : SIDES) {
                Block sideBlock = block.getRelative(side);
                if (sideBlock.getType().isAir() || sideBlock.getType() == Material.LIGHT) {
                    setLightBlock(sideBlock, Math.max(finalLightLevel - 1, 0));
                }
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags) {
        if (isMainThread()) {
            return setRawLightLevelLocked(world, blockX, blockY, blockZ, lightLevel, lightFlags);
        } else {
            Bukkit.getScheduler().runTask(getPlatformImpl().getPlugin(),
                    () -> setRawLightLevelLocked(world, blockX, blockY, blockZ, lightLevel, lightFlags));
            return ResultCode.SUCCESS;
        }
    }

    private int getLightFromBlock(Block block, int lightFlags) {
        int lightLevel = -1;
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(lightFlags,
                LightFlag.SKY_LIGHTING)) {
            return block.getLightLevel();
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            return block.getLightFromBlocks();
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            return block.getLightFromSky();
        }
        return lightLevel;
    }

    @Override
    public int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightFlags) {
        Block block = world.getBlockAt(blockX, blockY, blockZ);
        Material material = block.getType();
        int lightLevel = 0;

        if (material.isAir() || material == Material.LIGHT) {
            lightLevel = getLightFromBlock(block, lightFlags);
        } else {
            for (BlockFace side : SIDES) {
                Block sideBlock = block.getRelative(side);
                if (sideBlock.getType().isAir() || sideBlock.getType() == Material.LIGHT) {
                    int blockLightLevel = getLightFromBlock(sideBlock, lightFlags);
                    if (blockLightLevel > lightLevel) {
                        lightLevel = blockLightLevel;
                    }
                }
            }
        }
        return lightLevel;
    }

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int lightFlags) {
        return ResultCode.NOT_IMPLEMENTED;
    }

    @Override
    public IChunkData createChunkData(String worldName, int chunkX, int chunkZ) {
        return new BitChunkData(worldName, chunkX, chunkZ, 0, 0);
    }

    @Override
    public List<IChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags) {
        return new ArrayList<>();
    }

    @Override
    public boolean isValidChunkSection(World world, int sectionY) {
        return true;
    }

    @Override
    public int sendChunk(IChunkData data) {
        getPlatformImpl().debug("sendChunk: Not implemented for compatibility mode");
        return ResultCode.NOT_IMPLEMENTED;
    }

    @Override
    public int sendCmd(int cmdId, Object... args) {
        getPlatformImpl().debug("sendCmd: Not implemented for compatibility mode");
        return ResultCode.NOT_IMPLEMENTED;
    }
}
