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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.v1_17_R1;

import ca.spottedleaf.starlight.light.BlockStarLightEngine;
import ca.spottedleaf.starlight.light.SkyStarLightEngine;
import ca.spottedleaf.starlight.light.StarLightEngine;
import ca.spottedleaf.starlight.light.StarLightInterface;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.protocol.game.PacketPlayOutLightUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.LightEngineThreaded;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.lighting.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.BaseNMSHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.BitChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class NMSHandler extends BaseNMSHandler {

    private Field lightEngine_ThreadedMailbox;
    private Field threadedMailbox_State;
    private Method threadedMailbox_DoLoopStep;
    private Field lightEngineLayer_d;
    private Method lightEngineStorage_d;
    private Method lightEngineGraph_a;

    // beykerykt -- starlight engine
    private Map<ChunkCoordIntPair, List<LightPos>> blockQueueMap = new HashMap<>();
    private Map<ChunkCoordIntPair, List<LightPos>> skyQueueMap = new HashMap<>();

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

    private final int ALL_DIRECTIONS_BITSET = (1 << 6) - 1;
    private final long FLAG_HAS_SIDED_TRANSPARENT_BLOCKS = Long.MIN_VALUE;

    private static final class LightPos {
        public BlockPosition blockPos;
        public int lightLevel;

        public LightPos(BlockPosition blockPos, int lightLevel) {
            this.blockPos = blockPos;
            this.lightLevel = lightLevel;
        }
    }
    // beykerykt -- starlight engine

    private static RuntimeException toRuntimeException(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        Class<? extends Throwable> cls = e.getClass();
        return new RuntimeException(String.format("(%s) %s",
                RuntimeException.class.getPackage().equals(cls.getPackage()) ? cls.getSimpleName() : cls.getName(),
                e.getMessage()), e);
    }

    private int getDeltaLight(int x, int dx) {
        return (((x ^ ((-dx >> 4) & 15)) + 1) & (-(dx & 1)));
    }

    private void executeSync(LightEngineThreaded lightEngine, Runnable task) {
        try {
            // ##### STEP 1: Pause light engine mailbox to process its tasks. #####
            ThreadedMailbox<Runnable> threadedMailbox = (ThreadedMailbox<Runnable>) lightEngine_ThreadedMailbox
                    .get(lightEngine);
            // State flags bit mask:
            // 0x0001 - Closing flag (ThreadedMailbox is closing if non zero).
            // 0x0002 - Busy flag (ThreadedMailbox performs a task from queue if non zero).
            AtomicInteger stateFlags = (AtomicInteger) threadedMailbox_State.get(threadedMailbox);
            int flags; // to hold values from stateFlags
            long timeToWait = -1;
            // Trying to set bit 1 in state bit mask when it is not set yet.
            // This will break the loop in other thread where light engine mailbox processes the taks.
            while (!stateFlags.compareAndSet(flags = stateFlags.get() & ~2, flags | 2)) {
                if ((flags & 1) != 0) {
                    // ThreadedMailbox is closing. The light engine mailbox may also stop processing tasks.
                    // The light engine mailbox can be close due to server shutdown or unloading (closing) the world.
                    // I am not sure is it unsafe to process our tasks while the world is closing is closing,
                    // but will try it (one can throw exception here if it crashes the server).
                    if (timeToWait == -1) {
                        // Try to wait 3 seconds until light engine mailbox is busy.
                        timeToWait = System.currentTimeMillis() + 3 * 1000;
                        getPlatformImpl().debug("ThreadedMailbox is closing. Will wait...");
                    } else if (System.currentTimeMillis() >= timeToWait) {
                        throw new RuntimeException("Failed to enter critical section while ThreadedMailbox is closing");
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            try {
                // ##### STEP 2: Safely running the task while the mailbox process is stopped. #####
                task.run();
            } finally {
                // STEP 3: ##### Continue light engine mailbox to process its tasks. #####
                // Firstly: Clearing busy flag to allow ThreadedMailbox to use it for running light engine tasks.
                while (!stateFlags.compareAndSet(flags = stateFlags.get(), flags & ~2)) ;
                // Secondly: IMPORTANT! The main loop of ThreadedMailbox was broken. Not completed tasks may still be
                // in the queue. Therefore, it is important to start the loop again to process tasks from the queue.
                // Otherwise, the main server thread may be frozen due to tasks stuck in the queue.
                threadedMailbox_DoLoopStep.invoke(threadedMailbox);
            }
        } catch (InvocationTargetException e) {
            throw toRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw toRuntimeException(e);
        }
    }

    private void lightEngineLayer_a(LightEngineLayer<?, ?> les, BlockPosition var0, int var1) {
        try {
            LightEngineStorage<?> ls = (LightEngineStorage<?>) lightEngineLayer_d.get(les);
            lightEngineStorage_d.invoke(ls);
            lightEngineGraph_a.invoke(les, 9223372036854775807L, var0.asLong(), 15 - var1, true);
        } catch (InvocationTargetException e) {
            throw toRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw toRuntimeException(e);
        }
    }

    private IChunkData createChunkData(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        World world = Bukkit.getWorld(worldName);
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        int bottom = lightEngine.c();
        int top = lightEngine.d();
        return new BitChunkData(worldName, chunkX, chunkZ, top, bottom);
    }

    // beykerykt -- starlight engine
    private void scheduleChunkLight(StarLightInterface starLightInterface, ChunkCoordIntPair chunkCoordIntPair, Runnable runnable) {
        getPlatformImpl().debug("scheduleChunkLight");
        starLightInterface.scheduleChunkLight(chunkCoordIntPair, runnable);
    }

    private void addTaskToQueue(WorldServer worldServer, StarLightInterface starLightInterface, StarLightEngine sle, ChunkCoordIntPair chunkCoordIntPair, List<LightPos> lightPoints) {
        getPlatformImpl().debug("addTaskToQueue");
        // beykerykt -- add task to starlight engine queue
        int type = (sle instanceof BlockStarLightEngine) ? LightType.BLOCK_LIGHTING : LightType.SKY_LIGHTING;
        scheduleChunkLight(starLightInterface, chunkCoordIntPair, () -> {
            try {
                int chunkX = chunkCoordIntPair.b;
                int chunkZ = chunkCoordIntPair.c;

                // blocksChangedInChunk -- start
                // setup cache
                starEngine_setupCaches.invoke(sle, worldServer.getChunkProvider(), chunkX * 16 + 7, 128, chunkZ * 16 + 7, true, true);
                try {
                    // propagateBlockChanges -- start
                    Iterator<LightPos> it = lightPoints.iterator();
                    while (it.hasNext()) {
                        try {
                            LightPos lightPos = it.next();
                            BlockPosition blockPos = lightPos.blockPos;
                            int lightLevel = lightPos.lightLevel;
                            int currentLightLevel = getRawLightLevel(worldServer.getWorld(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), type);
                            if (lightLevel <= currentLightLevel) {
                                // do nothing
                                continue;
                            }
                            int encodeOffset = starInterface_coordinateOffset.getInt(sle);
                            BlockBase.BlockData blockData = worldServer.getType(blockPos);
                            starEngine_setLightLevel.invoke(sle, blockPos.getX(), blockPos.getY(), blockPos.getZ(), lightLevel);
                            getPlatformImpl().debug("blockX :" + blockPos.getX() + " blockY: " + blockPos.getY() + " blockZ: " + blockPos.getZ()
                                    + " lightLevel: " + lightLevel + " encodeOffset: " + encodeOffset);
                            if (lightLevel != 0) {
                                starEngine_appendToIncreaseQueue.invoke(sle, ((blockPos.getX() + (blockPos.getZ() << 6) + (blockPos.getY() << (6 + 6)) + encodeOffset) & ((1L << (6 + 6 + 16)) - 1))
                                        | (lightLevel & 0xFL) << (6 + 6 + 16)
                                        | (((long) ALL_DIRECTIONS_BITSET) << (6 + 6 + 16 + 4))
                                        | (blockData.isConditionallyFullOpaque() ? FLAG_HAS_SIDED_TRANSPARENT_BLOCKS : 0));
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
        // beykerykt -- add task to starlight engine queue
    }
    // beykerykt -- starlight engine

    @Override
    public void onInitialization(IPlatformImpl impl) throws Exception {
        super.onInitialization(impl);
        try {
            threadedMailbox_DoLoopStep = ThreadedMailbox.class.getDeclaredMethod(
                    Utils.compareBukkitVersionTo("1.17.1") >= 0 ? "h" : "g");
            threadedMailbox_DoLoopStep.setAccessible(true);
            threadedMailbox_State = ThreadedMailbox.class.getDeclaredField("d");
            threadedMailbox_State.setAccessible(true);
            lightEngine_ThreadedMailbox = LightEngineThreaded.class.getDeclaredField("e");
            lightEngine_ThreadedMailbox.setAccessible(true);

            lightEngineLayer_d = LightEngineLayer.class.getDeclaredField("d");
            lightEngineLayer_d.setAccessible(true);
            lightEngineStorage_d = LightEngineStorage.class.getDeclaredMethod("d");
            lightEngineStorage_d.setAccessible(true);
            lightEngineGraph_a = LightEngineGraph.class.getDeclaredMethod(
                    "a", long.class, long.class, int.class, boolean.class);
            lightEngineGraph_a.setAccessible(true);
            impl.info("Handler initialization is done");

            // beykerykt -- starlight engine
            if (getLightEngineType() == LightEngineType.STARLIGHT) {
                starEngine_setLightLevel = StarLightEngine.class.getDeclaredMethod("setLightLevel", int.class, int.class, int.class, int.class);
                starEngine_setLightLevel.setAccessible(true);
                starEngine_appendToIncreaseQueue = StarLightEngine.class.getDeclaredMethod("appendToIncreaseQueue", long.class);
                starEngine_appendToIncreaseQueue.setAccessible(true);
                starEngine_appendToDecreaseQueue = StarLightEngine.class.getDeclaredMethod("appendToDecreaseQueue", long.class);
                starEngine_appendToDecreaseQueue.setAccessible(true);
                starEngine_performLightIncrease = StarLightEngine.class.getDeclaredMethod("performLightIncrease", ILightAccess.class);
                starEngine_performLightIncrease.setAccessible(true);
                starEngine_performLightDecrease = StarLightEngine.class.getDeclaredMethod("performLightDecrease", ILightAccess.class);
                starEngine_performLightDecrease.setAccessible(true);
                starEngine_updateVisible = StarLightEngine.class.getDeclaredMethod("updateVisible", ILightAccess.class);
                starEngine_updateVisible.setAccessible(true);
                starEngine_setupCaches = StarLightEngine.class.getDeclaredMethod("setupCaches", ILightAccess.class, int.class, int.class, int.class, boolean.class, boolean.class);
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
            }
            // beykerykt -- starlight engine
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    @Override
    public void onShutdown(IPlatformImpl impl) {
    }

    @Override
    public void onWorldLoad(WorldLoadEvent event) {

    }

    @Override
    public void onWorldUnload(WorldUnloadEvent event) {

    }

    @Override
    public boolean isLightingSupported(World world, int lightFlags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        if ((lightFlags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
            if (getLightEngineType() == LightEngineType.VANILLA) {
                return lightEngine.a(EnumSkyBlock.a) instanceof LightEngineSky;
            } else if (getLightEngineType() == LightEngineType.STARLIGHT) {
                return true;
            }
        } else if ((lightFlags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
            if (getLightEngineType() == LightEngineType.VANILLA) {
                return lightEngine.a(EnumSkyBlock.b) instanceof LightEngineBlock;
            } else if (getLightEngineType() == LightEngineType.STARLIGHT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return LightEngineVersion.V2;
    }

    @Override
    public boolean isMainThread() {
        return MinecraftServer.getServer().isMainThread();
    }

    @Override
    public int asSectionMask(int sectionY) {
        return 1 << sectionY + 1;
    }

    @Override
    public int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        final int finalLightLevel = lightLevel < 0 ? 0 : lightLevel > 15 ? 15 : lightLevel;

        executeSync(lightEngine, () -> {
            // block lighting
            if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
                if (isLightingSupported(world, LightType.BLOCK_LIGHTING)) {
                    if (getLightEngineType() == LightEngineType.VANILLA) {
                        LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.b);
                        if (finalLightLevel == 0) {
                            leb.a(position);
                        } else if (leb.a(SectionPosition.a(position)) != null) {
                            try {
                                leb.a(position, finalLightLevel);
                            } catch (NullPointerException ignore) {
                                // To prevent problems with the absence of the NibbleArray, even
                                // if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                            }
                        }
                    } else if (getLightEngineType() == LightEngineType.STARLIGHT) {
                        // STARLIGHT
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
                                getPlatformImpl().debug("setRawLightLevel");
                                // beykerykt -- add to queue
                                ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(blockX >> 4, blockZ >> 4);
                                if (blockQueueMap.containsKey(chunkCoordIntPair)) {
                                    List<LightPos> lightPoints = blockQueueMap.get(chunkCoordIntPair);
                                    lightPoints.add(new LightPos(position, finalLightLevel));
                                } else {
                                    List<LightPos> lightPoints = new ArrayList<>();
                                    lightPoints.add(new LightPos(position, finalLightLevel));
                                    blockQueueMap.put(chunkCoordIntPair, lightPoints);
                                }
                                // beykerykt -- add to queue
                            } catch (NullPointerException ignore) {
                                // To prevent problems with the absence of the NibbleArray, even
                                // if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                            }
                        }
                    }
                }
            }

            // sky lighting
            if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                if (isLightingSupported(world, LightType.SKY_LIGHTING)) {
                    if (getLightEngineType() == LightEngineType.VANILLA) {
                        LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.a);
                        if (finalLightLevel == 0) {
                            les.a(position);
                        } else if (les.a(SectionPosition.a(position)) != null) {
                            try {
                                lightEngineLayer_a(les, position, finalLightLevel);
                            } catch (NullPointerException ignore) {
                                // To prevent problems with the absence of the NibbleArray, even
                                // if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                            }
                        }
                    } else if (getLightEngineType() == LightEngineType.STARLIGHT) {
                        // STARLIGHT
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
                                // beykerykt -- add to queue
                                ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(blockX >> 4, blockZ >> 4);
                                if (skyQueueMap.containsKey(chunkCoordIntPair)) {
                                    List<LightPos> lightPoints = skyQueueMap.get(chunkCoordIntPair);
                                    lightPoints.add(new LightPos(position, finalLightLevel));
                                } else {
                                    List<LightPos> lightPoints = new ArrayList<>();
                                    lightPoints.add(new LightPos(position, finalLightLevel));
                                    skyQueueMap.put(chunkCoordIntPair, lightPoints);
                                }
                                // beykerykt -- add to queue
                            } catch (NullPointerException ignore) {
                                // To prevent problems with the absence of the NibbleArray, even
                                // if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                            }
                        }
                    }
                }
            }
        });
        return ResultCode.SUCCESS;
    }

    @Override
    public int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int flags) {
        int lightLevel = -1;
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING
                && (flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
            lightLevel = worldServer.getLightLevel(position);
        } else if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.b, position);
        } else if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.a, position);
        }
        return lightLevel;
    }

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

        // beykerykt -- add task to starlight engine queue
        try {
            StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
            Iterator blockIt = blockQueueMap.entrySet().iterator();
            while (blockIt.hasNext()) {
                BlockStarLightEngine bsle = (BlockStarLightEngine) starInterface_getBlockLightEngine.invoke(starLightInterface);
                Map.Entry<ChunkCoordIntPair, List<LightPos>> pair = (Map.Entry<ChunkCoordIntPair, List<LightPos>>) blockIt.next();
                ChunkCoordIntPair chunkCoordIntPair = pair.getKey();
                List<LightPos> lightPoints = pair.getValue();
                addTaskToQueue(worldServer, starLightInterface, bsle, chunkCoordIntPair, lightPoints);
                blockIt.remove();
            }

            Iterator skyIt = skyQueueMap.entrySet().iterator();
            while (skyIt.hasNext()) {
                SkyStarLightEngine ssle = (SkyStarLightEngine) starInterface_getSkyLightEngine.invoke(starLightInterface);
                Map.Entry<ChunkCoordIntPair, List<LightPos>> pair = (Map.Entry<ChunkCoordIntPair, List<LightPos>>) skyIt.next();
                ChunkCoordIntPair chunkCoordIntPair = pair.getKey();
                List<LightPos> lightPoints = pair.getValue();
                addTaskToQueue(worldServer, starLightInterface, ssle, chunkCoordIntPair, lightPoints);
                skyIt.remove();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        // beykerykt -- add task to starlight engine queue

        // Do not recalculate if no changes!
        if (!lightEngine.z_()) {
            getPlatformImpl().debug("RECALCULATE_NO_CHANGES");
            return ResultCode.RECALCULATE_NO_CHANGES;
        }

        executeSync(lightEngine, () -> {
            if (((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING)
                    && ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING)) {
                if (isLightingSupported(world, LightType.SKY_LIGHTING) && isLightingSupported(world,
                        LightType.BLOCK_LIGHTING)) {
                    if (getLightEngineType() == LightEngineType.STARLIGHT) {
                        // beykerykt -- tell starlight engine propagate changes
                        try {
                            StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                            starLightInterface.propagateChanges();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        // beykerykt -- tell starlight engine propagate changes
                    } else if (getLightEngineType() == LightEngineType.VANILLA) {
                        LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.b);
                        LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.a);

                        // nms
                        int maxUpdateCount = Integer.MAX_VALUE;
                        int integer4 = maxUpdateCount / 2;
                        int integer5 = leb.a(integer4, true, true);
                        int integer6 = maxUpdateCount - integer4 + integer5;
                        int integer7 = les.a(integer6, true, true);
                        if (integer5 == 0 && integer7 > 0) {
                            leb.a(integer7, true, true);
                        }
                    }
                } else {
                    // block lighting
                    if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
                        if (isLightingSupported(world, LightType.BLOCK_LIGHTING)) {
                            if (getLightEngineType() == LightEngineType.STARLIGHT) {
                                // beykerykt -- tell starlight engine propagate changes
                                try {
                                    StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                                    starLightInterface.propagateChanges();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                // beykerykt -- tell starlight engine propagate changes
                                getPlatformImpl().debug("recalculateLighting");
                            } else if (getLightEngineType() == LightEngineType.VANILLA) {
                                LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.b);
                                leb.a(Integer.MAX_VALUE, true, true);
                            }
                        }
                    }

                    // sky lighting
                    if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                        if (isLightingSupported(world, LightType.SKY_LIGHTING)) {
                            if (getLightEngineType() == LightEngineType.STARLIGHT) {
                                // beykerykt -- tell starlight engine propagate changes
                                try {
                                    StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                                    starLightInterface.propagateChanges();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                // beykerykt -- tell starlight engine propagate changes
                            } else if (getLightEngineType() == LightEngineType.VANILLA) {
                                LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.a);
                                les.a(Integer.MAX_VALUE, true, true);
                            }
                        }
                    }
                }
            } else {
                // block lighting
                if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
                    if (isLightingSupported(world, LightType.BLOCK_LIGHTING)) {
                        if (getLightEngineType() == LightEngineType.STARLIGHT) {
                            // beykerykt -- tell starlight engine propagate changes
                            try {
                                StarLightInterface starLightInterface = (StarLightInterface) starInterface.get(lightEngine);
                                starLightInterface.propagateChanges();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            // beykerykt -- tell starlight engine propagate changes

                            getPlatformImpl().debug("recalculateLighting");
                        } else if (getLightEngineType() == LightEngineType.VANILLA) {
                            LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.b);
                            leb.a(Integer.MAX_VALUE, true, true);
                        }
                    }
                }

                // sky lighting
                if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                    if (isLightingSupported(world, LightType.SKY_LIGHTING)) {
                        if (getLightEngineType() == LightEngineType.STARLIGHT) {
                            LightEngineLayerEventListener lele = lightEngine.a(EnumSkyBlock.a);
                            lele.a(Integer.MAX_VALUE, true, true);
                        } else if (getLightEngineType() == LightEngineType.VANILLA) {
                            LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.a);
                            les.a(Integer.MAX_VALUE, true, true);
                        }
                    }
                }
            }
        });
        return ResultCode.SUCCESS;
    }

    @Override
    public IChunkData createChunkData(String worldName, int chunkX, int chunkZ) {
        return createChunkData(worldName, chunkX, chunkZ, 0, 0);
    }

    private IChunkData searchChunkDataFromList(List<IChunkData> list, World world, int chunkX, int chunkZ) {
        for (int i = 0; i < list.size(); i++) {
            IChunkData data = list.get(i);
            if (data.getWorldName().equals(world.getName()) && data.getChunkX() == chunkX
                    && data.getChunkZ() == chunkZ) {
                return data;
            }
        }
        return createChunkData(world.getName(), chunkX, chunkZ);
    }

    @Override
    public List<IChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ, int lightLevel,
                                                 int lightType) {
        List<IChunkData> list = Lists.newArrayList();
        int finalLightLevel = lightLevel;

        if (world == null) {
            return list;
        }

        if (lightLevel < 0) {
            finalLightLevel = 0;
        } else if (lightLevel > 15) {
            finalLightLevel = 15;
        }

        for (int dX = -1; dX <= 1; dX++) {
            int lightLevelX = finalLightLevel - getDeltaLight(blockX & 15, dX);
            if (lightLevelX > 0) {
                for (int dZ = -1; dZ <= 1; dZ++) {
                    int lightLevelZ = lightLevelX - getDeltaLight(blockZ & 15, dZ);
                    if (lightLevelZ > 0) {
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidChunkSection(sectionY)) {
                                    int chunkX = (blockX >> 4) + dX;
                                    int chunkZ = (blockZ >> 4) + dZ;

                                    IChunkData data = searchChunkDataFromList(list, world, chunkX, chunkZ);
                                    if (!list.contains(data)) {
                                        list.add(data);
                                    }
                                    data.markSectionForUpdate(lightType, sectionY);
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    public boolean isValidChunkSection(int sectionY) {
        return sectionY >= -1 && sectionY <= 16;
    }

    @Override
    public int sendChunk(IChunkData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        if (data instanceof BitChunkData) {
            BitChunkData icd = (BitChunkData) data;
            return sendChunk(world, icd.getChunkX(), icd.getChunkZ(), icd.getSkyLightUpdateBits(),
                    icd.getBlockLightUpdateBits());
        }
        return ResultCode.NOT_IMPLEMENTED;
    }

    protected int sendChunk(World world, int chunkX, int chunkZ, BitSet sectionMaskSky, BitSet sectionMaskBlock) {
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        Chunk chunk = worldServer.getChunkAt(chunkX, chunkZ);
        ChunkCoordIntPair chunkCoordIntPair = chunk.getPos();
        Stream<EntityPlayer> stream = worldServer.getChunkProvider().a.a(chunkCoordIntPair, false);
        // https://wiki.vg/index.php?title=Pre-release_protocol&oldid=14804#Update_Light
        // https://github.com/flori-schwa/VarLight/blob/b9349499f9c9fb995c320f95eae9698dd85aad5c/v1_14_R1/src
        // /me/florian/varlight/nms/v1_14_R1/NmsAdapter_1_14_R1.java#L451
        //
        // Two last argument is bit-mask what chunk sections to update. Mask containing
        // 18 bits, with the lowest bit corresponding to chunk section -1 (in the void,
        // y=-16 to y=-1) and the highest bit for chunk section 16 (above the world,
        // y=256 to y=271).
        PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.getWorld().k_(),
                sectionMaskSky, sectionMaskBlock, true);
        stream.forEach(e -> e.b.sendPacket(packet));
        return ResultCode.SUCCESS;
    }

    @Override
    public int sendCmd(int cmdId, Object... args) {
        return 0;
    }
}
