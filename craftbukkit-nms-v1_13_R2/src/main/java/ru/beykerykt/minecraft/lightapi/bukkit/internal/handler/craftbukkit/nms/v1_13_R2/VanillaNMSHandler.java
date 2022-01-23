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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.v1_13_R2;

import com.google.common.collect.Lists;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EnumSkyBlock;
import net.minecraft.server.v1_13_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_13_R2.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.List;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.BaseNMSHandler;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.LegacyIntChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class VanillaNMSHandler extends BaseNMSHandler {

    private static final BlockFace[] SIDES = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    public static Block getAdjacentAirBlock(Block block) {
        for (BlockFace face : SIDES) {
            if (block.getY() == 0x0 && face == BlockFace.DOWN) // 0
            {
                continue;
            }
            if (block.getY() == 0xFF && face == BlockFace.UP) // 255
            {
                continue;
            }

            Block candidate = block.getRelative(face);

            if (!candidate.getType().isOccluding()) {
                return candidate;
            }
        }
        return block;
    }

    private int distanceTo(Chunk from, Chunk to) {
        if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName())) {
            return 100;
        }
        double var2 = to.locX - from.locX;
        double var4 = to.locZ - from.locZ;
        return (int) Math.sqrt(var2 * var2 + var4 * var4);
    }

    public int parseViewDistance(EntityPlayer player) {
        int viewDistance = Bukkit.getViewDistance();
        try {
            int playerViewDistance = player.clientViewDistance;
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

    private IChunkData createLegacyChunkData(String worldName, int chunkX, int chunkZ, int sectionMaskSky,
            int sectionMaskBlock) {
        return new LegacyIntChunkData(worldName, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
    }

    @Override
    public void onShutdown(BukkitPlatformImpl impl) {
    }

    @Override
    public LightEngineType getLightEngineType() {
        return LightEngineType.VANILLA;
    }

    @Override
    public LightEngineVersion getLightEngineVersion() {
        return LightEngineVersion.V1;
    }

    @Override
    public void onWorldLoad(WorldLoadEvent event) {
    }

    @Override
    public void onWorldUnload(WorldUnloadEvent event) {
    }

    @Override
    public boolean isLightingSupported(World world, int lightFlags) {
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            WorldServer worldServer = ((CraftWorld) world).getHandle();
            return worldServer.worldProvider.g();
        } else {
            return FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING);
        }
    }

    @Override
    public int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        final int finalLightLevel = lightLevel < 0 ? 0 : Math.min(lightLevel, 15);

        if (!worldServer.getChunkProvider().isLoaded(blockX >> 4, blockZ >> 4)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }

        if (finalLightLevel == 0) {
            BlockPosition adjacentPosition = new BlockPosition(blockX, blockY, blockZ);
            if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                    worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
                }
            }
            if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                    worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
                }
            }
            return ResultCode.SUCCESS;
        }
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                worldServer.a(EnumSkyBlock.BLOCK, position, finalLightLevel);
            }
        }
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                worldServer.a(EnumSkyBlock.SKY, position, finalLightLevel);
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightFlags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        int lightlevel = -1;
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(lightFlags,
                LightFlag.SKY_LIGHTING)) {
            lightlevel = worldServer.getLightLevel(position);
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            lightlevel = worldServer.getBrightness(EnumSkyBlock.BLOCK, position);
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            lightlevel = worldServer.getBrightness(EnumSkyBlock.SKY, position);
        }
        return lightlevel;
    }

    @Override
    public int recalculateLighting(World world, int blockX, int blockY, int blockZ, int lightFlags) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();

        if (!worldServer.getChunkProvider().isLoaded(blockX >> 4, blockZ >> 4)) {
            return ResultCode.CHUNK_NOT_LOADED;
        }

        Block adjacent = getAdjacentAirBlock(world.getBlockAt(blockX, blockY, blockZ));
        int ax = adjacent.getX();
        int ay = adjacent.getY();
        int az = adjacent.getZ();

        BlockPosition adjacentPosition = new BlockPosition(ax, ay, az);
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
            }
        }
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
            }
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public IChunkData createChunkData(String worldName, int chunkX, int chunkZ) {
        return createLegacyChunkData(worldName, chunkX, chunkZ, 0, 0);
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
                        for (int dY = -1; dY <= 1; dY++) {
                            if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
                                int sectionY = (blockY >> 4) + dY;
                                if (isValidChunkSection(world, sectionY)) {
                                    int chunkX = (blockX >> 4) + dX;
                                    int chunkZ = (blockZ >> 4) + dZ;

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
        return sectionY >= 0 && sectionY <= 16;
    }

    @Override
    public int sendChunk(IChunkData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        // TODO: support only legacy chunk data (?)
        if (data instanceof LegacyIntChunkData) {
            LegacyIntChunkData icd = (LegacyIntChunkData) data;
            return sendChunk(world, icd.getChunkX(), icd.getChunkZ(), icd.getSkyLightUpdateBits(),
                    icd.getBlockLightUpdateBits());
        }
        return ResultCode.NOT_IMPLEMENTED;
    }

    protected int sendChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
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
        return ResultCode.SUCCESS;
    }

    @Override
    public int sendCmd(int cmdId, Object... args) {
        return 0;
    }
}
