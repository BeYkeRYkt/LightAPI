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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.craftbukkit.v1_13_R2;

import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.BukkitHandlerInternal;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCodes;
import ru.beykerykt.minecraft.lightapi.common.api.SendMode;
import ru.beykerykt.minecraft.lightapi.common.api.impl.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.LightEngineVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CraftBukkitHandler extends BukkitHandlerInternal {

    private static BlockFace[] SIDES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
            BlockFace.WEST};
    private IChunkObserver mChunkObserver;

    public static Block getAdjacentAirBlock(Block block) {
        for (BlockFace face : SIDES) {
            if (block.getY() == 0x0 && face == BlockFace.DOWN) // 0
                continue;
            if (block.getY() == 0xFF && face == BlockFace.UP) // 255
                continue;

            Block candidate = block.getRelative(face);

            if (!candidate.getType().isOccluding()) {
                return candidate;
            }
        }
        return block;
    }

    private IChunkObserver getChunkObserver() {
        return mChunkObserver;
    }

    private int distanceTo(Chunk from, Chunk to) {
        if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
            return 100;
        double var2 = to.locX - from.locX;
        double var4 = to.locZ - from.locZ;
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

    @Override
    public int asSectionMask(int sectionY) {
        return 1 << sectionY;
    }

    @Override
    public int getSectionFromY(int blockY) {
        return blockY >> 4;
    }

    public boolean isValidSectionY(int sectionY) {
        return sectionY >= 0 && sectionY <= 16;
    }

    private int getThreeSectionsMask(int y) {
        return (isValidSectionY(y) ? asSectionMask(y) : 0) | (isValidSectionY(y - 1) ? asSectionMask(y - 1) : 0)
                | (isValidSectionY(y + 1) ? asSectionMask(y + 1) : 0);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.CRAFTBUKKIT;
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
    private int setRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final int finalLightLevel = lightLevel < 0 ? 0 : lightLevel > 15 ? 15 : lightLevel;
        if (finalLightLevel == 0) {
            BlockPosition adjacentPosition = new BlockPosition(blockX, blockY, blockZ);
            if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
                worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
            }
            if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
                worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
            }
            if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
                worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
                //worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
            }
            return ResultCodes.SUCCESS;
        }
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
            worldServer.a(EnumSkyBlock.BLOCK, position, finalLightLevel);
        }
        if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
            worldServer.a(EnumSkyBlock.SKY, position, finalLightLevel);
        }
        if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
            worldServer.a(EnumSkyBlock.BLOCK, position, finalLightLevel);
            worldServer.a(EnumSkyBlock.SKY, position, finalLightLevel);
        }
        return ResultCodes.SUCCESS;
    }

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
    private int getRawLightLevelLocked(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        int lightlevel = -1;
        if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
            lightlevel = worldServer.getBrightness(EnumSkyBlock.BLOCK, position);
        } else if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
            lightlevel = worldServer.getBrightness(EnumSkyBlock.SKY, position);
        } else if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
            lightlevel = worldServer.getLightLevel(position);
        }
        return lightlevel;
    }

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

    private int recalculateLightingLocked(World world, int blockX, int blockY, int blockZ, int flags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();

        Block adjacent = getAdjacentAirBlock(world.getBlockAt(blockX, blockY, blockZ));
        int ax = adjacent.getX();
        int ay = adjacent.getY();
        int az = adjacent.getZ();

        BlockPosition adjacentPosition = new BlockPosition(ax, ay, az);
        if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
            worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
        }
        if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
            worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
        }
        if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
            // i don't known
            worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
            worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
        }
        return ResultCodes.SUCCESS;
    }

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
    public List<ChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ, int lightLevel) {
        List<ChunkData> list = new ArrayList<ChunkData>();
        if (world == null) {
            return list;
        }

        final int finalLightLevel = lightLevel < 0 ? 0 : lightLevel > 15 ? 15 : lightLevel;
        if (finalLightLevel > 0) {
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
                                ChunkData chunkData = new ChunkData(world.getName(), chunkX + dX,
                                        chunkZ + dZ, sectionMask, sectionMask);
                                if (!list.contains(chunkData)) {
                                    list.add(chunkData);
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
                // Last argument is bit-mask what chunk sections to update. Only lower 16 bits
                // are used.
                // There are 16 sections in chunk. Each section height=16. So, y-coordinate
                // varies from 0 to 255.
                // Use 0x1ffff instead 0xffff because of little bug in PacketPlayOutMapChunk
                // constructor.
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, 0x1ffff);
                human.playerConnection.sendPacket(packet);
            }
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public int sendChunk(World world, int chunkX, int chunkSectionY, int chunkZ) {
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
                // Last argument is bit-mask what chunk sections to update. Only lower 16 bits
                // are used.
                // There are 16 sections in chunk. Each section height=16. So, y-coordinate
                // varies from 0 to 255.
                int mask = getThreeSectionsMask(chunkSectionY);
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, mask);
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
                // Last argument is bit-mask what chunk sections to update. Only lower 16 bits
                // are used.
                // There are 16 sections in chunk. Each section height=16. So, y-coordinate
                // varies from 0 to 255.
                // We know that max light=15 (15 blocks). So, it is enough to update only 3
                // sections: y\16-1, y\16, y\16+1
                int mask = sectionMaskSky | sectionMaskBlock;
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, mask);
                human.playerConnection.sendPacket(packet);
            }
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public void initialization(IPlatformImpl impl) throws Exception {
        mChunkObserver = impl.getChunkObserver();
    }

    @Override
    public void shutdown(IPlatformImpl impl) {
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return LightEngineVersion.V1;
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == MinecraftServer.getServer().primaryThread;
    }

    @Override
    public boolean isAsyncLighting() {
        return false;
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
