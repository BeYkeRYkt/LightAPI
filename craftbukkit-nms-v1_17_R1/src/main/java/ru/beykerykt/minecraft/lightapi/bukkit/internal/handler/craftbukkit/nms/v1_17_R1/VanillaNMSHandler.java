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

import com.google.common.collect.Lists;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.protocol.game.PacketPlayOutLightUpdate;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.LightEngineThreaded;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.lighting.LightEngineBlock;
import net.minecraft.world.level.lighting.LightEngineGraph;
import net.minecraft.world.level.lighting.LightEngineLayer;
import net.minecraft.world.level.lighting.LightEngineSky;
import net.minecraft.world.level.lighting.LightEngineStorage;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.BaseNMSHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.utils.VersionUtil;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.BitChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class VanillaNMSHandler extends BaseNMSHandler {

    protected Field lightEngine_ThreadedMailbox;
    private Field threadedMailbox_State;
    private Method threadedMailbox_DoLoopStep;
    private Field lightEngineLayer_d;
    private Method lightEngineStorage_d;
    private Method lightEngineGraph_a;

    protected static RuntimeException toRuntimeException(Throwable e) {
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

    protected void executeSync(LightEngineThreaded lightEngine, Runnable task) {
        try {
            // ##### STEP 1: Pause light engine mailbox to process its tasks. #####
            ThreadedMailbox<Runnable> threadedMailbox = (ThreadedMailbox<Runnable>) lightEngine_ThreadedMailbox.get(
                    lightEngine);
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
                    // The light engine mailbox can be close due to server shutdown or unloading (closing) the
                    // world.
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
                // Firstly: Clearing busy flag to allow ThreadedMailbox to use it for running light engine
                // tasks.
                while (!stateFlags.compareAndSet(flags = stateFlags.get(), flags & ~2))
                    ;
                // Secondly: IMPORTANT! The main loop of ThreadedMailbox was broken. Not completed tasks may
                // still be
                // in the queue. Therefore, it is important to start the loop again to process tasks from
                // the queue.
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

    private IChunkData createBitChunkData(String worldName, int chunkX, int chunkZ) {
        World world = Bukkit.getWorld(worldName);
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        int bottom = lightEngine.c();
        int top = lightEngine.d();
        return new BitChunkData(worldName, chunkX, chunkZ, top, bottom);
    }

    @Override
    public void onInitialization(BukkitPlatformImpl impl) throws Exception {
        super.onInitialization(impl);
        try {
            threadedMailbox_DoLoopStep = ThreadedMailbox.class.getDeclaredMethod(
                    VersionUtil.compareBukkitVersionTo("1.17.1") >= 0 ? "h" : "g");
            threadedMailbox_DoLoopStep.setAccessible(true);
            threadedMailbox_State = ThreadedMailbox.class.getDeclaredField("d");
            threadedMailbox_State.setAccessible(true);
            lightEngine_ThreadedMailbox = LightEngineThreaded.class.getDeclaredField("e");
            lightEngine_ThreadedMailbox.setAccessible(true);

            lightEngineLayer_d = LightEngineLayer.class.getDeclaredField("d");
            lightEngineLayer_d.setAccessible(true);
            lightEngineStorage_d = LightEngineStorage.class.getDeclaredMethod("d");
            lightEngineStorage_d.setAccessible(true);
            lightEngineGraph_a = LightEngineGraph.class.getDeclaredMethod("a", long.class, long.class, int.class,
                    boolean.class);
            lightEngineGraph_a.setAccessible(true);
            impl.info("Handler initialization is done");
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    @Override
    public void onShutdown(BukkitPlatformImpl impl) {
    }

    @Override
    public LightEngineType getLightEngineType() {
        return LightEngineType.VANILLA;
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
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            return lightEngine.a(EnumSkyBlock.a) instanceof LightEngineSky;
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            return lightEngine.a(EnumSkyBlock.b) instanceof LightEngineBlock;
        }
        return false;
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return LightEngineVersion.V2;
    }

    @Override
    public int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        final int finalLightLevel = lightLevel < 0 ? 0 : Math.min(lightLevel, 15);

        if (!worldServer.getChunkProvider().isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }

        if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
            if (!isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                return ResultCode.BLOCKLIGHT_DATA_NOT_AVAILABLE;
            }
        }

        if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
            if (!isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                return ResultCode.SKYLIGHT_DATA_NOT_AVAILABLE;
            }
        }

        executeSync(lightEngine, () -> {
            // block lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
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
            }

            // sky lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
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
            }
        });
        if (lightEngine.z_()) {
            return ResultCode.SUCCESS;
        }
        return ResultCode.FAILED;
    }

    @Override
    public int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int flags) {
        int lightLevel = -1;
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(flags,
                LightFlag.SKY_LIGHTING)) {
            lightLevel = worldServer.getLightLevel(position);
        } else if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.b, position);
        } else if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.a, position);
        }
        return lightLevel;
    }

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

        if (!worldServer.getChunkProvider().isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }

        // Do not recalculate if no changes!
        if (!lightEngine.z_()) {
            return ResultCode.RECALCULATE_NO_CHANGES;
        }

        executeSync(lightEngine, () -> {
            if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(flags,
                    LightFlag.SKY_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.SKY_LIGHTING) && isLightingSupported(world,
                        LightFlag.BLOCK_LIGHTING)) {
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
                } else {
                    // block lighting
                    if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                        if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                            LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.b);
                            leb.a(Integer.MAX_VALUE, true, true);
                        }
                    }

                    // sky lighting
                    if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                        if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                            LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.a);
                            les.a(Integer.MAX_VALUE, true, true);
                        }
                    }
                }
            } else {
                // block lighting
                if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                    if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                        LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.b);
                        leb.a(Integer.MAX_VALUE, true, true);
                    }
                }

                // sky lighting
                if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                    if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                        LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.a);
                        les.a(Integer.MAX_VALUE, true, true);
                    }
                }
            }
        });
        return ResultCode.SUCCESS;
    }

    @Override
    public IChunkData createChunkData(String worldName, int chunkX, int chunkZ) {
        return createBitChunkData(worldName, chunkX, chunkZ);
    }

    private IChunkData searchChunkDataFromList(List<IChunkData> list, World world, int chunkX, int chunkZ) {
        for (IChunkData data : list) {
            if (data.getWorldName().equals(world.getName()) && data.getChunkX() == chunkX
                    && data.getChunkZ() == chunkZ) {
                return data;
            }
        }
        return createChunkData(world.getName(), chunkX, chunkZ);
    }

    @Override
    public List<IChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        List<IChunkData> list = Lists.newArrayList();
        int finalLightLevel = lightLevel < 0 ? 0 : Math.min(lightLevel, 15);

        if (world == null) {
            return list;
        }

        for (int dX = -1; dX <= 1; dX++) {
            int lightLevelX = finalLightLevel - getDeltaLight(blockX & 15, dX);
            if (lightLevelX > 0) {
                for (int dZ = -1; dZ <= 1; dZ++) {
                    int lightLevelZ = lightLevelX - getDeltaLight(blockZ & 15, dZ);
                    if (lightLevelZ > 0) {
                        int chunkX = (blockX >> 4) + dX;
                        int chunkZ = (blockZ >> 4) + dZ;
                        if (!worldServer.getChunkProvider().isChunkLoaded(chunkX, chunkZ)) {
                            continue;
                        }
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidChunkSection(world, sectionY)) {
                                    IChunkData data = searchChunkDataFromList(list, world, chunkX, chunkZ);
                                    if (!list.contains(data)) {
                                        list.add(data);
                                    }
                                    data.markSectionForUpdate(lightFlags, sectionY);
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
    public boolean isValidChunkSection(World world, int sectionY) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        return (sectionY >= lightEngine.c()) && (sectionY <= lightEngine.d());
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
        if (!worldServer.getChunkProvider().isChunkLoaded(chunkX, chunkZ)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }
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
