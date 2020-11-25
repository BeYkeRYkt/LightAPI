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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.craftbukkit.v1_14_R1;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.craftbukkit.CommonCraftBukkitHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCodes;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.LightEngineVersion;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftBukkitHandler extends CommonCraftBukkitHandler {
    private Field lightEngine_ThreadedMailbox;
    private Field threadedMailbox_State;
    private Method threadedMailbox_DoLoopStep;
    private Field lightEngineLayer_c;
    private Method lightEngineStorage_c;
    private Method lightEngineGraph_a;

    private static RuntimeException toRuntimeException(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        Class<? extends Throwable> cls = e.getClass();
        return new RuntimeException(String.format("(%s) %s",
                RuntimeException.class.getPackage().equals(cls.getPackage()) ? cls.getSimpleName() : cls.getName(),
                e.getMessage()), e);
    }

    private boolean isLightingSupported(World world, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
            return lightEngine.a(EnumSkyBlock.SKY) instanceof LightEngineSky;
        } else if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
            return lightEngine.a(EnumSkyBlock.BLOCK) instanceof LightEngineBlock;
        }
        return false;
    }

    private int distanceTo(Chunk from, Chunk to) {
        if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
            return 100;
        double var2 = to.getPos().x - from.getPos().x;
        double var4 = to.getPos().z - from.getPos().z;
        return (int) Math.sqrt(var2 * var2 + var4 * var4);
    }

    public int parseViewDistance(EntityPlayer player) {
        int viewDistance = Bukkit.getViewDistance();
        try {
            int playerViewDistance = Integer.valueOf(player.clientViewDistance);
            if (playerViewDistance < viewDistance) {
                viewDistance = playerViewDistance;
            }
        } catch (Exception e) {
            // silent
        }
        return viewDistance;
    }

    private int getDeltaLight(int x, int dx) {
        return (((x ^ ((-dx >> 4) & 15)) + 1) & (-(dx & 1)));
    }

    public boolean isValidSectionY(int sectionY) {
        return sectionY >= -1 && sectionY <= 16;
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
            // This will break the loop in other thread where light engine mailbox processes
            // the taks.
            while (!stateFlags.compareAndSet(flags = stateFlags.get() & ~2, flags | 2)) {
                if ((flags & 1) != 0) {
                    // ThreadedMailbox is closing. The light engine mailbox may also stop processing
                    // tasks.
                    // The light engine mailbox can be close due to server shutdown or unloading
                    // (closing) the world.
                    // I am not sure is it unsafe to process our tasks while the world is closing is
                    // closing,
                    // but will try it (one can throw exception here if it crashes the server).
                    if (timeToWait == -1) {
                        // Try to wait 3 seconds until light engine mailbox is busy.
                        timeToWait = System.currentTimeMillis() + 3 * 1000;
                        getPlatformImpl().log("ThreadedMailbox is closing. Will wait...");
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
                // ##### STEP 2: Safely running the task while the mailbox process is stopped.
                // #####
                task.run();
            } finally {
                // STEP 3: ##### Continue light engine mailbox to process its tasks. #####
                // Firstly: Clearing busy flag to allow ThreadedMailbox to use it for running
                // light engine tasks.
                while (!stateFlags.compareAndSet(flags = stateFlags.get(), flags & ~2))
                    ;
                // Secondly: IMPORTANT! The main loop of ThreadedMailbox was broken. Not
                // completed tasks may still be
                // in the queue. Therefore, it is important to start the loop again to process
                // tasks from the queue.
                // Otherwise, the main server thread may be frozen due to tasks stuck in the
                // queue.
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
            LightEngineStorage<?> ls = (LightEngineStorage<?>) lightEngineLayer_c.get(les);
            lightEngineStorage_c.invoke(ls);
            lightEngineGraph_a.invoke(les, 9223372036854775807L, var0.asLong(), 15 - var1, true);
        } catch (InvocationTargetException e) {
            throw toRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw toRuntimeException(e);
        }
    }

    @Override
    protected int setRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
        final int finalLightLevel = lightLevel < 0 ? 0 : lightLevel > 15 ? 15 : lightLevel;

        executeSync(lightEngine, () -> {
            // block lighting
            if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
                if (isLightingSupported(world, LightFlags.BLOCK_LIGHTING)) {
                    LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
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
            }

            // sky lighting
            if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
                if (isLightingSupported(world, LightFlags.SKY_LIGHTING)) {
                    LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
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
            }
        });
        return ResultCodes.SUCCESS;
    }

    @Override
    protected int getRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int flags) {
        int lightLevel = -1;
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING
                && (flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
            lightLevel = worldServer.getLightLevel(position);
        } else if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.BLOCK, position);
        } else if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.SKY, position);
        }
        return lightLevel;
    }

    @Override
    protected int recalculateLightingLocked(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

        // Do not recalculate if no changes!
        if (!lightEngine.a()) {
            return ResultCodes.RECALCULATE_NO_CHANGES;
        }

        executeSync(lightEngine, () -> {
            if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING
                    && (flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
                if (isLightingSupported(world, LightFlags.SKY_LIGHTING) && isLightingSupported(world,
                        LightFlags.BLOCK_LIGHTING)) {
                    LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
                    LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);

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
                    if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
                        if (isLightingSupported(world, LightFlags.BLOCK_LIGHTING)) {
                            LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
                            leb.a(Integer.MAX_VALUE, true, true);
                        }
                    }

                    // sky lighting
                    if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
                        if (isLightingSupported(world, LightFlags.SKY_LIGHTING)) {
                            LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
                            les.a(Integer.MAX_VALUE, true, true);
                        }
                    }
                }
            } else {
                // block lighting
                if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
                    if (isLightingSupported(world, LightFlags.BLOCK_LIGHTING)) {
                        LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
                        leb.a(Integer.MAX_VALUE, true, true);
                    }
                }

                // sky lighting
                if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
                    if (isLightingSupported(world, LightFlags.SKY_LIGHTING)) {
                        LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
                        les.a(Integer.MAX_VALUE, true, true);
                    }
                }
            }
        });
        return ResultCodes.SUCCESS;
    }

    @Override
    public List<String> getAuthors() {
        return Arrays.asList("BeYkeRYkt");
    }

    @Override
    public void onWorldLoad(WorldLoadEvent event) {

    }

    @Override
    public void onWorldUnload(WorldUnloadEvent event) {

    }

    @Override
    public List<ChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ, int lightLevel) {
        List<ChunkData> list = Lists.newArrayList();
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
                        int chunkX = blockX >> 4;
                        int chunkZ = blockZ >> 4;
                        int sectionMask = 0;
                        boolean isFilled = false;
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidSectionY(sectionY)) {
                                    isFilled = true;
                                    sectionMask |= asSectionMask(sectionY);
                                }
                            }
                        }

                        // don't add null section mask
                        if (isFilled) {
                            ChunkData chunkData = new ChunkData(world.getName(), chunkX + dX, chunkZ + dZ,
                                    sectionMask, sectionMask);
                            if (!list.contains(chunkData)) {
                                list.add(chunkData);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    public int sendChunk(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        for (int i = 0; i < world.getPlayers().size(); i++) {
            Player player = world.getPlayers().get(i);
            Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
            EntityPlayer human = ((CraftPlayer) player).getHandle();
            Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
            int playerViewDistance = parseViewDistance(human);

            if (distanceTo(pChunk, chunk) <= playerViewDistance) {
                PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e());
                human.playerConnection.sendPacket(packet);
            }
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public int sendChunk(World world, int chunkX, int chunkZ, int chunkSectionY) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        for (int i = 0; i < world.getPlayers().size(); i++) {
            Player player = world.getPlayers().get(i);
            Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
            EntityPlayer human = ((CraftPlayer) player).getHandle();
            Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
            int playerViewDistance = parseViewDistance(human);

            if (distanceTo(pChunk, chunk) <= playerViewDistance) {
                // https://wiki.vg/index.php?title=Pre-release_protocol&oldid=14804#Update_Light
                // https://github.com/flori-schwa/VarLight/blob/b9349499f9c9fb995c320f95eae9698dd85aad5c/v1_14_R1/src
                // /me/florian/varlight/nms/v1_14_R1/NmsAdapter_1_14_R1.java#L451
                //
                // Two last argument is bit-mask what chunk sections to update. Mask containing
                // 18 bits, with the lowest bit corresponding to chunk section -1 (in the void,
                // y=-16 to y=-1) and the highest bit for chunk section 16 (above the world,
                // y=256 to y=271).
                //
                // There are 16 sections in chunk. Each section height=16. So, y-coordinate
                // varies from 0 to 255.
                // We know that max light=15 (15 blocks). So, it is enough to update only 3
                // sections: (y\16)-1, y\16, (y\16)+1
                int blockMask = asSectionMask(chunkSectionY);
                int skyMask = blockMask;

                PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e(), skyMask,
                        blockMask);
                human.playerConnection.sendPacket(packet);
            }
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public int sendChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (world == null) {
            return ResultCodes.WORLD_NOT_AVAILABLE;
        }
        for (int i = 0; i < world.getPlayers().size(); i++) {
            Player player = world.getPlayers().get(i);
            Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
            EntityPlayer human = ((CraftPlayer) player).getHandle();
            Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
            int playerViewDistance = parseViewDistance(human);

            if (distanceTo(pChunk, chunk) <= playerViewDistance) {
                // https://wiki.vg/index.php?title=Pre-release_protocol&oldid=14804#Update_Light
                // https://github.com/flori-schwa/VarLight/blob/b9349499f9c9fb995c320f95eae9698dd85aad5c/v1_14_R1/src
                // /me/florian/varlight/nms/v1_14_R1/NmsAdapter_1_14_R1.java#L451
                //
                // Two last argument is bit-mask what chunk sections to update. Mask containing
                // 18 bits, with the lowest bit corresponding to chunk section -1 (in the void,
                // y=-16 to y=-1) and the highest bit for chunk section 16 (above the world,
                // y=256 to y=271).
                PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e(),
                        sectionMaskSky, sectionMaskBlock);
                human.playerConnection.sendPacket(packet);
            }
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public void initialization(IPlatformImpl impl) throws Exception {
        super.initialization(impl);
        try {
            threadedMailbox_DoLoopStep = ThreadedMailbox.class.getDeclaredMethod("f");
            threadedMailbox_DoLoopStep.setAccessible(true);
            threadedMailbox_State = ThreadedMailbox.class.getDeclaredField("c");
            threadedMailbox_State.setAccessible(true);
            lightEngine_ThreadedMailbox = LightEngineThreaded.class.getDeclaredField("b");
            lightEngine_ThreadedMailbox.setAccessible(true);

            lightEngineLayer_c = LightEngineLayer.class.getDeclaredField("c");
            lightEngineLayer_c.setAccessible(true);
            lightEngineStorage_c = LightEngineStorage.class.getDeclaredMethod("c");
            lightEngineStorage_c.setAccessible(true);
            lightEngineGraph_a = LightEngineGraph.class.getDeclaredMethod("a", long.class, long.class, int.class,
                    boolean.class);
            lightEngineGraph_a.setAccessible(true);
            impl.log("adapter initialization: Done");
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    @Override
    public void shutdown(IPlatformImpl impl) {

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
    public boolean isAsyncLighting() {
        return true;
    }

    @Override
    public int asSectionMask(int sectionY) {
        return 1 << sectionY + 1;
    }

    @Override
    public int getSectionFromY(int blockY) {
        return (blockY >> 4) + 1;
    }
}
