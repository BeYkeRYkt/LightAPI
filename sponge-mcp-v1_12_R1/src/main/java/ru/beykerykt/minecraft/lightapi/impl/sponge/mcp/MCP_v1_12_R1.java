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
package ru.beykerykt.minecraft.lightapi.impl.sponge.mcp;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.property.block.GroundLuminanceProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LightType;
import ru.beykerykt.minecraft.lightapi.common.LightingEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.MappingType;
import ru.beykerykt.minecraft.lightapi.impl.sponge.SpongeChunkData;
import ru.beykerykt.minecraft.lightapi.impl.sponge.SpongeLightHandler;

/**
 * Interface implementation for MCP NMS (Net Minecraft Server) version 1.12.2
 *
 * @author BeYkeRYkt
 */
public class MCP_v1_12_R1 extends SpongeLightHandler {

    private static Direction[] SIDES = {
            Direction.UP,
            Direction.DOWN,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private static Location<World> getAdjacentAirBlock(Location<World> blockLoc) {
        for (Direction face : SIDES) {
            if (blockLoc.getBlockY() == 0x0 && face == Direction.DOWN) // 0
            {
                continue;
            }
            if (blockLoc.getBlockY() == 0xFF && face == Direction.UP) // 255
            {
                continue;
            }

            Location<World> candidate = blockLoc.getRelative(face);
            WorldServer world = (WorldServer) candidate.getExtent();
            if (!world.getBlockState(
                    new BlockPos(candidate.getBlockX(), candidate.getBlockY(), candidate.getBlockZ())).isOpaqueCube()) {
                return candidate;
            }
            // if (!candidate.getType().isOccluding()) {
            // return candidate;
            // }
        }
        return blockLoc;
    }

    private int distanceToSquared(Chunk from, Chunk to) {
        if (!from.getWorld().getWorldInfo().getWorldName().equals(to.getWorld().getWorldInfo().getWorldName())) {
            return 100;
        }
        double var2 = to.x - from.x;
        double var4 = to.z - from.z;
        return (int) (var2 * var2 + var4 * var4);
    }

    /***********************************************************************************************************************/
    @Override
    public boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        return createLight(world.get(), type, blockX, blockY, blockZ, lightlevel);
    }

    @Override
    public boolean createLight(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
        if (world == null || type == null) {
            return false;
        }

        setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel);

        Location<World> blockLoc = new Location<World>(world, blockX, blockY, blockZ);
        Location<World> adjacent = getAdjacentAirBlock(blockLoc);
        int ax = adjacent.getBlockX();
        int ay = adjacent.getBlockY();
        int az = adjacent.getBlockZ();
        recalculateLighting(world, type, ax, ay, az);

        // check light
        Optional<GroundLuminanceProperty> groundLumOpt = blockLoc.getProperty(GroundLuminanceProperty.class);
        if (groundLumOpt.isPresent()) {
            if (groundLumOpt.get().getValue() >= lightlevel) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteLight(String worldName, LightType type, int x, int y, int z) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        return deleteLight(world.get(), type, x, y, z);
    }

    @Override
    public boolean deleteLight(World world, LightType type, int x, int y, int z) {
        if (world == null || type == null) {
            return false;
        }

        Location<World> blockLoc = new Location<World>(world, x, y, z);

        // check light
        Optional<GroundLuminanceProperty> groundLumOpt = blockLoc.getProperty(GroundLuminanceProperty.class);
        if (groundLumOpt.isPresent()) {
            Double oldlightlevel = groundLumOpt.get().getValue();
            recalculateLighting(world, type, x, y, z);

            groundLumOpt = blockLoc.getProperty(GroundLuminanceProperty.class);
            if (groundLumOpt.isPresent()) {
                if (groundLumOpt.get().getValue() != oldlightlevel) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        setRawLightLevel(world.get(), type, blockX, blockY, blockZ, lightlevel);
    }

    @Override
    public void setRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
        if (world == null || type == null) {
            return;
        }
        if (lightlevel == 0) {
            recalculateLighting(world, type, blockX, blockY, blockZ);
            return;
        }
        WorldServer mcpWorld = (WorldServer) world;
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        EnumSkyBlock esb = EnumSkyBlock.BLOCK;
        if (type == LightType.SKY) {
            esb = EnumSkyBlock.SKY;
        }

        /**
         * With asynchronous lighting, all lighting calculations occur in a separate thread. This causes
         * problems with defining changed chunks when calling a function from another thread, since the
         * list of changed chunks can be created earlier than changes occur that need to be sent to
         * players after placing the light source. Use the CompletableFuture features from JRE_1.8 to
         * solve the problem.
         */
        if (isAsyncLighting()) {
            // We hope that the user uses the latest version of Java
            CompletableFuture<Void> future = null;
            if (type == LightType.BLOCK) {
                future = CompletableFuture.runAsync(() -> mcpWorld.setLightFor(EnumSkyBlock.BLOCK, pos, lightlevel));
            } else if (type == LightType.SKY) {
                future = CompletableFuture.runAsync(() -> mcpWorld.setLightFor(EnumSkyBlock.SKY, pos, lightlevel));
            }
            future.join(); // wait
        } else {
            mcpWorld.setLightFor(esb, pos, lightlevel);
        }
    }

    @Override
    public int getRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        return getRawLightLevel(world.get(), type, blockX, blockY, blockZ);
    }

    @Override
    public int getRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ) {
        if (world == null || type == null) {
            return 0;
        }
        WorldServer mixWorld = (WorldServer) world;
        BlockPos adjacentPosition = new BlockPos(blockX, blockY, blockZ);
        EnumSkyBlock esb = EnumSkyBlock.BLOCK;
        if (type == LightType.SKY) {
            esb = EnumSkyBlock.SKY;
        }
        return mixWorld.getLightFor(esb, adjacentPosition);
    }

    @Override
    public void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        recalculateLighting(world.get(), type, blockX, blockY, blockZ);
    }

    @Override
    public void recalculateLighting(World world, LightType type, int blockX, int blockY, int blockZ) {
        WorldServer mixWorld = (WorldServer) world;
        BlockPos adjacentPosition = new BlockPos(blockX, blockY, blockZ);
        /**
         * With asynchronous lighting, all lighting calculations occur in a separate thread. This causes
         * problems with defining changed chunks when calling a function from another thread, since the
         * list of changed chunks can be created earlier than changes occur that need to be sent to
         * players after placing the light source. Use the CompletableFuture features from JRE_1.8 to
         * solve the problem.
         *
         * <p>Unfortunately, performance tests were not performed.
         */
        if (isAsyncLighting()) {
            // We hope that the user uses the latest version of Java
            CompletableFuture<Boolean> future = null;
            if (type == LightType.BLOCK) {
                future = CompletableFuture.supplyAsync(
                        () -> mixWorld.checkLightFor(EnumSkyBlock.BLOCK, adjacentPosition));
            } else if (type == LightType.SKY) {
                future = CompletableFuture.supplyAsync(
                        () -> mixWorld.checkLightFor(EnumSkyBlock.SKY, adjacentPosition));
            }
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return;
        }
        EnumSkyBlock esb = EnumSkyBlock.BLOCK;
        if (type == LightType.SKY) {
            esb = EnumSkyBlock.SKY;
        }
        mixWorld.checkLightFor(esb, adjacentPosition);
    }

    @Override
    public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ, int radiusBlocks) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        return collectChunks(world.get(), blockX, blockY, blockZ, radiusBlocks);
    }

    @Override
    public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ) {
        return collectChunks(worldName, blockX, blockY, blockZ, 15);
    }

    @Override
    public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ) {
        return collectChunks(world, blockX, blockY, blockZ, 15);
    }

    @Override
    public List<IChunkData> collectChunks(World world, int x, int y, int z, int lightlevel) {
        if (world == null) {
            return null;
        }
        int radiusBlocks = lightlevel / 2;
        if (radiusBlocks > 8 || radiusBlocks <= 0) {
            radiusBlocks = 8;
        }
        List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();
        WorldServer mcpWorld = (WorldServer) world;
        for (int dX = -radiusBlocks; dX <= radiusBlocks; dX += radiusBlocks) {
            for (int dZ = -radiusBlocks; dZ <= radiusBlocks; dZ += radiusBlocks) {
                int chunkX = (x + dX) >> 4;
                int chunkZ = (z + dZ) >> 4;
                if (mcpWorld.getChunkProvider().chunkExists(chunkX, chunkZ)) {
                    Chunk chunk = mcpWorld.getChunkFromChunkCoords(chunkX, chunkZ);
                    if (chunk.needsSaving(false)) {
                        IChunkData cCoord = new SpongeChunkData(world, chunk.x, y, chunk.z, world.getPlayers());
                        if (!list.contains(cCoord)) {
                            list.add(cCoord);
                        }
                        chunk.setModified(false);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void sendChanges(World world, int chunkX, int chunkZ, Player player) {
        if (world == null || player == null) {
            return;
        }
        WorldServer mcpWorld = (WorldServer) world;
        Chunk chunk = mcpWorld.getChunkFromChunkCoords(chunkX, chunkZ);
        EntityPlayerMP human = (EntityPlayerMP) player;
        Chunk pChunk = human.getServerWorld().getChunkFromBlockCoords(human.getPosition());
        if (distanceToSquared(pChunk, chunk) < 5 * 5) {
            // Last argument is bit-mask what chunk sections to update. Only lower 16 bits
            // are used.
            // There are 16 sections in chunk. Each section height=16. So, y-coordinate
            // varies from 0 to 255.
            // Use 0x1ffff instead 0xffff because of little bug in PacketPlayOutMapChunk
            // constructor.
            SPacketChunkData packet = new SPacketChunkData(chunk, 0x1ffff);
            human.connection.sendPacket(packet);
        }
    }

    @Override
    public void sendChanges(World world, int chunkX, int y, int chunkZ, Player player) {
        if (world == null || player == null) {
            return;
        }
        WorldServer mcpWorld = (WorldServer) world;
        Chunk chunk = mcpWorld.getChunkFromChunkCoords(chunkX, chunkZ);
        EntityPlayerMP human = (EntityPlayerMP) player;
        Chunk pChunk = human.getServerWorld().getChunkFromBlockCoords(human.getPosition());
        if (distanceToSquared(pChunk, chunk) < 5 * 5) {
            // Last argument is bit-mask what chunk sections to update. Only lower 16 bits
            // are used.
            // There are 16 sections in chunk. Each section height=16. So, y-coordinate
            // varies from 0 to 255.
            // Use 0x1ffff instead 0xffff because of little bug in PacketPlayOutMapChunk
            // constructor.
            SPacketChunkData packet = new SPacketChunkData(chunk, (7 << (y >> 4)) >> 1);
            human.connection.sendPacket(packet);
        }
    }

    @Override
    public LightingEngineVersion getLightingEngineVersion() {
        return LightingEngineVersion.V1;
    }

    @Override
    public boolean isRequireManuallySendingChanges() {
        return true;
    }

    @Override
    public boolean isAsyncLighting() {
        return SpongeImpl.getGlobalConfig().getConfig().getOptimizations().useAsyncLighting();
    }

    @Override
    public MappingType getMappingType() {
        return MappingType.SRG; // Because it works so SpongeVanilla
    }

    @Override
    public void sendChanges(IChunkData chunkData, Player player) {
        if (chunkData == null || player == null) {
            return;
        }
        if (chunkData instanceof SpongeChunkData) {
            SpongeChunkData bcd = (SpongeChunkData) chunkData;
            sendChanges(bcd.getWorld(), bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ(), player);
        }
    }

    @Override
    public void sendChanges(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            sendChanges(world, chunkX, chunkZ, player);
        }
    }

    @Override
    public void sendChanges(World world, int chunkX, int blockY, int chunkZ) {
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            sendChanges(world, chunkX, blockY, chunkZ, player);
        }
    }

    @Override
    public void sendChanges(String worldName, int chunkX, int chunkZ, String playerName) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        Optional<Player> player = Sponge.getServer().getPlayer(playerName);
        sendChanges(world.get(), chunkX, chunkZ, player.get());
    }

    @Override
    public void sendChanges(String worldName, int chunkX, int blockY, int chunkZ, String playerName) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        Optional<Player> player = Sponge.getServer().getPlayer(playerName);
        sendChanges(world.get(), chunkX, blockY, chunkZ, player.get());
    }

    @Override
    public void sendChanges(IChunkData chunkData, String playerName) {
        Optional<Player> player = Sponge.getServer().getPlayer(playerName);
        sendChanges(chunkData, player.get());
    }

    @Override
    public void sendChanges(String worldName, int chunkX, int chunkZ) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        sendChanges(world.get(), chunkX, chunkZ);
    }

    @Override
    public void sendChanges(String worldName, int chunkX, int blockY, int chunkZ) {
        Optional<World> world = Sponge.getServer().getWorld(worldName);
        sendChanges(world.get(), chunkX, blockY, chunkZ);
    }

    @Override
    public void sendChanges(IChunkData chunkData) {
        if (chunkData == null) {
            return;
        }
        if (chunkData instanceof SpongeChunkData) {
            SpongeChunkData bcd = (SpongeChunkData) chunkData;
            for (Player player : bcd.getReceivers()) {
                sendChanges(bcd.getWorld(), bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ(), player);
            }
        }
    }
}
