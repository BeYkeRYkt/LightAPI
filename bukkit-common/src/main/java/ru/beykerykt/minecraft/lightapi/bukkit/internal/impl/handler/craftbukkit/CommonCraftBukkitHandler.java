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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.craftbukkit;

import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.BukkitHandlerInternal;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCodes;
import ru.beykerykt.minecraft.lightapi.common.api.SendMode;
import ru.beykerykt.minecraft.lightapi.common.api.impl.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class CommonCraftBukkitHandler extends BukkitHandlerInternal {

    private IPlatformImpl mImpl;

    protected IPlatformImpl getPlatformImpl() {
        return mImpl;
    }

    protected IChunkObserver getChunkObserver() {
        return getPlatformImpl().getChunkObserver();
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.CRAFTBUKKIT;
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags,
                             SendMode mode, List<ChunkData> outputChunks) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }

        // go set
        int blockLightLevel = getRawLightLevel(world, blockX, blockY, blockZ, flags);
        int setResultCode = setRawLightLevel(world, blockX, blockY, blockZ, lightLevel, flags);
        switch (setResultCode) {
            case ResultCodes.SUCCESS: {
                // go recalculate
                int recalcResultCode = recalculateLighting(world, blockX, blockY, blockZ, flags);
                switch (recalcResultCode) {
                    // go send chunks
                    case ResultCodes.SUCCESS: {
                        switch (mode) {
                            case INSTANT: {
                                List<ChunkData> chunks = collectChunkSections(world, blockX, blockY, blockZ,
                                        lightLevel == 0 ? blockLightLevel : lightLevel);
                                for (int i = 0; i < chunks.size(); i++) {
                                    ChunkData data = chunks.get(i);
                                    sendChunk(data);
                                }
                                return ResultCodes.SUCCESS;
                            }
                            case DELAYED: {
                                getChunkObserver().notifyUpdateChunks(world.getName(), blockX, blockY, blockZ,
                                        lightLevel == 0 ? blockLightLevel : lightLevel);
                                return ResultCodes.SUCCESS;
                            }
                            case MANUAL: {
                                if (outputChunks == null) {
                                    return ResultCodes.MANUAL_MODE_CHUNK_LIST_IS_NULL;
                                }
                                List<ChunkData> chunks = collectChunkSections(world, blockX, blockY, blockZ,
                                        lightLevel == 0 ? blockLightLevel : lightLevel);
                                outputChunks.addAll(chunks);
                                return ResultCodes.SUCCESS;
                            }
                        }
                        break;
                    }
                    default:
                        return recalcResultCode;
                }
                break;
            }
            default:
                return setResultCode;
        }
        return ResultCodes.FAILED;
    }

    /**
     * @hide
     **/
    protected abstract int setRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int lightLevel,
                                                  int flags);

    @Override
    public int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }

        int resultCode = ResultCodes.FAILED;
        if (isMainThread()) {
            resultCode = setRawLightLevelLocked(world, blockX, blockY, blockZ, lightLevel, flags);
        } else {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> setRawLightLevelLocked(world,
                    blockX, blockY, blockZ, lightLevel, flags));
            try {
                resultCode = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return resultCode;
    }

    /**
     * @hide
     **/
    protected abstract int getRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int flags);

    @Override
    public int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int flags) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        int lightLevel = -1;
        if (isMainThread()) {
            lightLevel = getRawLightLevelLocked(world, blockX, blockY, blockZ, flags);
        } else {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> getRawLightLevelLocked(world,
                    blockX, blockY, blockZ, flags));
            try {
                lightLevel = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return lightLevel;
    }

    protected abstract int recalculateLightingLocked(World world, int blockX, int blockY, int blockZ, int flags);

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int flags) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }

        int resultCode = ResultCodes.FAILED;
        if (isMainThread()) {
            resultCode = recalculateLightingLocked(world, blockX, blockY, blockZ, flags);
        } else {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> recalculateLightingLocked(world,
                    blockX, blockY, blockZ, flags));
            try {
                resultCode = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return resultCode;
    }

    @Override
    public void initialization(IPlatformImpl impl) throws Exception {
        mImpl = impl;
    }

    @Override
    public boolean isRequireRecalculateLighting() {
        return true;
    }

    @Override
    public boolean isRequireManuallySendingChanges() {
        return true;
    }
}
