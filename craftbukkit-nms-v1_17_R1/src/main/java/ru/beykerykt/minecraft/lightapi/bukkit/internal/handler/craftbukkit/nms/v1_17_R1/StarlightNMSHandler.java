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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.v1_17_R1;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.LightEngineThreaded;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.lighting.LightEngineLayerEventListener;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import ca.spottedleaf.starlight.light.BlockStarLightEngine;
import ca.spottedleaf.starlight.light.SkyStarLightEngine;
import ca.spottedleaf.starlight.light.StarLightEngine;
import ca.spottedleaf.starlight.light.StarLightInterface;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class StarlightNMSHandler extends VanillaNMSHandler {

    private final int ALL_DIRECTIONS_BITSET = (1 << 6) - 1;
    private final long FLAG_HAS_SIDED_TRANSPARENT_BLOCKS = Long.MIN_VALUE;
    private final Map<ChunkCoordIntPair, Set<LightPos>> blockQueueMap = new ConcurrentHashMap<>();
    private final Map<ChunkCoordIntPair, Set<LightPos>> skyQueueMap = new ConcurrentHashMap<>();
    // StarLightInterface
    private Field starInterface;
    private Field starInterface_coordinateOffset;
    private Method starInterface_getBlockLightEngine;
    private Method starInterface_getSkyLightEngine;
    // StarLightEngine
    private Method starEngine_setLightLevel;
    private Method starEngine_appendToIncreaseQueue;
    private Method starEngine_appendToDecreaseQueue;
    private Method starEngine_performLightIncrease;
    private Method starEngine_performLightDecrease;
    private Method starEngine_updateVisible;
    private Method starEngine_setupCaches;
    private Method starEngine_destroyCaches;

    private void scheduleChunkLight(StarLightInterface starLightInterface, ChunkCoordIntPair chunkCoordIntPair,
            Runnable runnable) {
        starLightInterface.scheduleChunkLight(chunkCoordIntPair, runnable);
    }

    private void addTaskToQueue(WorldServer worldServer, StarLightInterface starLightInterface, StarLightEngine sle,
            ChunkCoordIntPair chunkCoordIntPair, Set<LightPos> lightPoints) {
        int type = (sle instanceof BlockStarLightEngine) ? LightFlag.BLOCK_LIGHTING : LightFlag.SKY_LIGHTING;
        scheduleChunkLight(starLightInterface, chunkCoordIntPair, () -> {
            try {
                int chunkX = chunkCoordIntPair.b;
                int chunkZ = chunkCoordIntPair.c;

                if (!worldServer.getChunkProvider().isChunkLoaded(chunkX, chunkZ)) {
                    return;
                }

                // blocksChangedInChunk -- start
                // setup cache
                starEngine_setupCaches.invoke(sle, worldServer.getChunkProvider(), chunkX * 16 + 7, 128,
                        chunkZ * 16 + 7, true, true);
                try {
                    // propagateBlockChanges -- start
                    Iterator<LightPos> it = lightPoints.iterator();
                    while (it.hasNext()) {
                        try {
                            LightPos lightPos = it.next();
                            BlockPosition blockPos = lightPos.blockPos;
                            int lightLevel = lightPos.lightLevel;
                            int currentLightLevel = getRawLightLevel(worldServer.getWorld(), blockPos.getX(),
                                    blockPos.getY(), blockPos.getZ(), type);
                            if (lightLevel <= currentLightLevel) {
                                // do nothing
                                continue;
                            }
                            int encodeOffset = starInterface_coordinateOffset.getInt(sle);
                            BlockBase.BlockData blockData = worldServer.getType(blockPos);
                            starEngine_setLightLevel.invoke(sle, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                                    lightLevel);
                            if (lightLevel != 0) {
                                starEngine_appendToIncreaseQueue.invoke(sle,
                                        ((blockPos.getX() + (blockPos.getZ() << 6) + (blockPos.getY() << (6 + 6))
                                                + encodeOffset) & ((1L << (6 + 6 + 16)) - 1)) | (lightLevel & 0xFL) << (
                                                6 + 6 + 16) | (((long) ALL_DIRECTIONS_BITSET) << (6 + 6 + 16 + 4)) | (
                                                blockData.isConditionallyFullOpaque()
                                                        ? FLAG_HAS_SIDED_TRANSPARENT_BLOCKS : 0));
                            }
                        } finally {
                            it.remove();
                        }
                    }
                    starEngine_performLightIncrease.invoke(sle, worldServer.getChunkProvider());
                    // propagateBlockChanges -- end
                    starEngine_updateVisible.invoke(sle, worldServer.getChunkProvider());
                } finally {
                    starEngine_destroyCaches.invoke(sle);
                }
                // blocksChangedInChunk -- end
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    protected void executeSync(LightEngineThreaded lightEngine, Runnable task) {
        if (isMainThread()) {
            task.run();
        } else {
            try {
                CompletableFuture<Void> future = new CompletableFuture();
                ThreadedMailbox<Runnable> threadedMailbox = (ThreadedMailbox<Runnable>) lightEngine_ThreadedMailbox.get(
                        lightEngine);
                threadedMailbox.a(() -> {
                    task.run();
                    future.complete(null);
                });
                future.join();
            } catch (IllegalAccessException e) {
                throw toRuntimeException(e);
            }
        }
    }

    @Override
    public void onInitialization(BukkitPlatformImpl impl) throws Exception {
        super.onInitialization(impl);
        try {
            starEngine_setLightLevel = StarLightEngine.class.getDeclaredMethod("setLightLevel", int.class, int.class,
                    int.class, int.class);
            starEngine_setLightLevel.setAccessible(true);
            starEngine_appendToIncreaseQueue = StarLightEngine.class.getDeclaredMethod("appendToIncreaseQueue",
                    long.class);
            starEngine_appendToIncreaseQueue.setAccessible(true);
            starEngine_appendToDecreaseQueue = StarLightEngine.class.getDeclaredMethod("appendToDecreaseQueue",
                    long.class);
            starEngine_appendToDecreaseQueue.setAccessible(true);
            starEngine_performLightIncrease = StarLightEngine.class.getDeclaredMethod("performLightIncrease",
                    ILightAccess.class);
            starEngine_performLightIncrease.setAccessible(true);
            starEngine_performLightDecrease = StarLightEngine.class.getDeclaredMethod("performLightDecrease",
                    ILightAccess.class);
            starEngine_performLightDecrease.setAccessible(true);
            starEngine_updateVisible = StarLightEngine.class.getDeclaredMethod("updateVisible", ILightAccess.class);
            starEngine_updateVisible.setAccessible(true);
            starEngine_setupCaches = StarLightEngine.class.getDeclaredMethod("setupCaches", ILightAccess.class,
                    int.class, int.class, int.class, boolean.class, boolean.class);
            starEngine_setupCaches.setAccessible(true);
            starEngine_destroyCaches = StarLightEngine.class.getDeclaredMethod("destroyCaches");
            starEngine_destroyCaches.setAccessible(true);
            starInterface = LightEngineThreaded.class.getDeclaredField("theLightEngine");
            starInterface.setAccessible(true);
            starInterface_getBlockLightEngine = StarLightInterface.class.getDeclaredMethod("getBlockLightEngine");
            starInterface_getBlockLightEngine.setAccessible(true);
            starInterface_getSkyLightEngine = StarLightInterface.class.getDeclaredMethod("getSkyLightEngine");
            starInterface_getSkyLightEngine.setAccessible(true);
            starInterface_coordinateOffset = StarLightEngine.class.getDeclaredField("coordinateOffset");
            starInterface_coordinateOffset.setAccessible(true);
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    @Override
    public LightEngineType getLightEngineType() {
        return LightEngineType.STARLIGHT;
    }

    @Override
    public boolean isLightingSupported(World world, int lightFlags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            return lightEngine.a(EnumSkyBlock.a) != null;
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            return lightEngine.a(EnumSkyBlock.b) != null;
        }
        return false;
    }

    @Override
    public int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        final int finalLightLevel = lightLevel < 0 ? 0 : Math.min(lightLevel, 15);
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(blockX >> 4, blockZ >> 4);

        if (!worldServer.getChunkProvider().isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }

        executeSync(lightEngine, () -> {
            // block lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                    LightEngineLayerEventListener lele = lightEngine.a(EnumSkyBlock.b);
                    if (finalLightLevel == 0) {
                        try {
                            StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                            starLightInterface.blockChange(position);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else if (lele.a(SectionPosition.a(position)) != null) {
                        try {
                            if (blockQueueMap.containsKey(chunkCoordIntPair)) {
                                Set<LightPos> lightPoints = blockQueueMap.get(chunkCoordIntPair);
                                lightPoints.add(new LightPos(position, finalLightLevel));
                            } else {
                                Set<LightPos> lightPoints = new HashSet<>();
                                lightPoints.add(new LightPos(position, finalLightLevel));
                                blockQueueMap.put(chunkCoordIntPair, lightPoints);
                            }
                        } catch (NullPointerException ignore) {
                            // To prevent problems with the absence of the NibbleArray, even
                            // if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                        }
                    }
                }
            }

            // sky lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                    LightEngineLayerEventListener lele = lightEngine.a(EnumSkyBlock.a);
                    if (finalLightLevel == 0) {
                        try {
                            StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                            starLightInterface.blockChange(position);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else if (lele.a(SectionPosition.a(position)) != null) {
                        try {
                            if (skyQueueMap.containsKey(chunkCoordIntPair)) {
                                Set<LightPos> lightPoints = skyQueueMap.get(chunkCoordIntPair);
                                lightPoints.add(new LightPos(position, finalLightLevel));
                            } else {
                                Set<LightPos> lightPoints = new HashSet<>();
                                lightPoints.add(new LightPos(position, finalLightLevel));
                                skyQueueMap.put(chunkCoordIntPair, lightPoints);
                            }
                        } catch (NullPointerException ignore) {
                            // To prevent problems with the absence of the NibbleArray, even
                            // if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                        }
                    }
                }
            }
        });
        Map<ChunkCoordIntPair, Set<LightPos>> targetMap = null;
        if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
            targetMap = skyQueueMap;
        } else if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
            targetMap = blockQueueMap;
        }
        if (lightEngine.z_() || targetMap != null && targetMap.containsKey(chunkCoordIntPair)) {
            return ResultCode.SUCCESS;
        }
        return ResultCode.FAILED;
    }

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

        if (!worldServer.getChunkProvider().isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }

        // Do not recalculate if no changes!
        if (!lightEngine.z_() && blockQueueMap.isEmpty() && skyQueueMap.isEmpty()) {
            return ResultCode.RECALCULATE_NO_CHANGES;
        }

        try {
            StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
            Iterator blockIt = blockQueueMap.entrySet().iterator();
            while (blockIt.hasNext()) {
                BlockStarLightEngine bsle = (BlockStarLightEngine) starInterface_getBlockLightEngine.invoke(
                        starLightInterface);
                Map.Entry<ChunkCoordIntPair, Set<LightPos>> pair =
                        (Map.Entry<ChunkCoordIntPair, Set<LightPos>>) blockIt.next();
                ChunkCoordIntPair chunkCoordIntPair = pair.getKey();
                Set<LightPos> lightPoints = pair.getValue();
                addTaskToQueue(worldServer, starLightInterface, bsle, chunkCoordIntPair, lightPoints);
                blockIt.remove();
            }

            Iterator skyIt = skyQueueMap.entrySet().iterator();
            while (skyIt.hasNext()) {
                SkyStarLightEngine ssle = (SkyStarLightEngine) starInterface_getSkyLightEngine.invoke(
                        starLightInterface);
                Map.Entry<ChunkCoordIntPair, Set<LightPos>> pair =
                        (Map.Entry<ChunkCoordIntPair, Set<LightPos>>) skyIt.next();
                ChunkCoordIntPair chunkCoordIntPair = pair.getKey();
                Set<LightPos> lightPoints = pair.getValue();
                addTaskToQueue(worldServer, starLightInterface, ssle, chunkCoordIntPair, lightPoints);
                skyIt.remove();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        executeSync(lightEngine, () -> {
            try {
                StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                starLightInterface.propagateChanges();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        return ResultCode.SUCCESS;
    }

    private static final class LightPos {

        public BlockPosition blockPos;
        public int lightLevel;

        public LightPos(BlockPosition blockPos, int lightLevel) {
            this.blockPos = blockPos;
            this.lightLevel = lightLevel;
        }
    }
}
