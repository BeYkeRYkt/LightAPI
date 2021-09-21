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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.v1_15_R1;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.BaseNMSHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IntChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class NMSHandler extends BaseNMSHandler {

    private Field lightEngine_ThreadedMailbox;
    private Field lightEngineLayer_c;
    private Method lightEngineStorage_d;
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

    private int getDeltaLight(int x, int dx) {
        return (((x ^ ((-dx >> 4) & 15)) + 1) & (-(dx & 1)));
    }

    private void executeSync(LightEngineThreaded lightEngine, Runnable task) {
        try {
            ThreadedMailbox<Runnable> threadedMailbox = (ThreadedMailbox<Runnable>) lightEngine_ThreadedMailbox
                    .get(lightEngine);
            CompletableFuture<Void> future = new CompletableFuture<>();
            threadedMailbox.a(() -> {
                task.run();
                future.complete(null);
            });
            future.join();
        } catch (IllegalAccessException e) {
            throw toRuntimeException(e);
        }
    }

    private void lightEngineLayer_a(LightEngineLayer<?, ?> les, BlockPosition var0, int var1) {
        try {
            LightEngineStorage<?> ls = (LightEngineStorage<?>) lightEngineLayer_c.get(les);
            lightEngineStorage_d.invoke(ls);
            lightEngineGraph_a.invoke(les, 9223372036854775807L, var0.asLong(), 15 - var1, true);
        } catch (InvocationTargetException e) {
            throw toRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw toRuntimeException(e);
        }
    }

    private IChunkData createChunkData(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        return new IntChunkData(worldName, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
    }

    @Override
    public void onInitialization(IPlatformImpl impl) throws Exception {
        super.onInitialization(impl);
        try {
            lightEngine_ThreadedMailbox = LightEngineThreaded.class.getDeclaredField("b");
            lightEngine_ThreadedMailbox.setAccessible(true);

            lightEngineLayer_c = LightEngineLayer.class.getDeclaredField("c");
            lightEngineLayer_c.setAccessible(true);
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
            return lightEngine.a(EnumSkyBlock.SKY) instanceof LightEngineSky;
        } else if ((lightFlags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
            return lightEngine.a(EnumSkyBlock.BLOCK) instanceof LightEngineBlock;
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
            if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                if (isLightingSupported(world, LightType.SKY_LIGHTING)) {
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
            lightLevel = worldServer.getBrightness(EnumSkyBlock.BLOCK, position);
        } else if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
            lightLevel = worldServer.getBrightness(EnumSkyBlock.SKY, position);
        }
        return lightLevel;
    }

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

        // Do not recalculate if no changes!
        if (!lightEngine.a()) {
            return ResultCode.RECALCULATE_NO_CHANGES;
        }

        executeSync(lightEngine, () -> {
            if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING
                    && (flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                if (isLightingSupported(world, LightType.SKY_LIGHTING) && isLightingSupported(world,
                        LightType.BLOCK_LIGHTING)) {
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
                    if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
                        if (isLightingSupported(world, LightType.BLOCK_LIGHTING)) {
                            LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
                            leb.a(Integer.MAX_VALUE, true, true);
                        }
                    }

                    // sky lighting
                    if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                        if (isLightingSupported(world, LightType.SKY_LIGHTING)) {
                            LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
                            les.a(Integer.MAX_VALUE, true, true);
                        }
                    }
                }
            } else {
                // block lighting
                if ((flags & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
                    if (isLightingSupported(world, LightType.BLOCK_LIGHTING)) {
                        LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
                        leb.a(Integer.MAX_VALUE, true, true);
                    }
                }

                // sky lighting
                if ((flags & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                    if (isLightingSupported(world, LightType.SKY_LIGHTING)) {
                        LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
                        les.a(Integer.MAX_VALUE, true, true);
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
                        int chunkX = blockX >> 4;
                        int chunkZ = blockZ >> 4;
                        int sectionMaskSky = 0;
                        int sectionMaskBlock = 0;
                        boolean isFilled = false;
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidChunkSection(sectionY)) {
                                    isFilled = true;
                                    // block lighting
                                    if ((lightType & LightType.BLOCK_LIGHTING) == LightType.BLOCK_LIGHTING) {
                                        sectionMaskBlock |= asSectionMask(sectionY);
                                    }

                                    // sky lighting
                                    if ((lightType & LightType.SKY_LIGHTING) == LightType.SKY_LIGHTING) {
                                        sectionMaskSky |= asSectionMask(sectionY);
                                    }
                                }
                            }
                        }

                        // don't add null section mask
                        if (isFilled) {
                            IChunkData chunkData = createChunkData(world.getName(), chunkX + dX, chunkZ + dZ,
                                    sectionMaskSky, sectionMaskBlock);
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
    public boolean isValidChunkSection(int sectionY) {
        return sectionY >= -1 && sectionY <= 16;
    }

    @Override
    public int sendChunk(IChunkData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        if (data instanceof IntChunkData) {
            IntChunkData icd = (IntChunkData) data;
            return sendChunk(world, icd.getChunkX(), icd.getChunkZ(), icd.getSkyLightUpdateBits(),
                    icd.getBlockLightUpdateBits());
        }
        return ResultCode.NOT_IMPLEMENTED;
    }

    protected int sendChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        Chunk chunk = worldServer.getChunkAt(chunkX, chunkZ);
        ChunkCoordIntPair chunkCoordIntPair = chunk.getPos();
        Stream<EntityPlayer> stream = worldServer.getChunkProvider().playerChunkMap.a(chunkCoordIntPair, false);
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
        stream.forEach(e -> e.playerConnection.sendPacket(packet));
        return ResultCode.SUCCESS;
    }

    @Override
    public int sendCmd(int cmdId, Object... args) {
        return 0;
    }
}
