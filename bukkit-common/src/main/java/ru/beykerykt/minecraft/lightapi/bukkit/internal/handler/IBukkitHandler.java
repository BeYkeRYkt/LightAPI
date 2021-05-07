package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.chunks.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.handler.IHandler;

import java.util.ArrayList;
import java.util.List;

public interface IBukkitHandler extends IHandler {

    /**
     * N/A
     */
    void onWorldLoad(WorldLoadEvent event);

    /**
     * N/A
     */
    void onWorldUnload(WorldUnloadEvent event);

    /**
     * Sets "directly" the level of light in given coordinates without additional
     * processing.
     */
    int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightType);

    @Override
    default int setRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return setRawLightLevel(world, blockX, blockY, blockZ, lightLevel, lightType);
    }

    /**
     * Gets "directly" the level of light from given coordinates without additional
     * processing.
     */
    int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightType);

    @Override
    default int getRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return getRawLightLevel(world, blockX, blockY, blockZ, lightType);
    }

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    int recalculateLighting(World world, int blockX, int blockY, int blockZ, int lightType);

    @Override
    default int recalculateLighting(String worldName, int blockX, int blockY, int blockZ, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return recalculateLighting(world, blockX, blockY, blockZ, lightType);
    }

    /**
     * Collects modified сhunks with sections around a given coordinate in the
     * radius of the light level. The light level is taken from the arguments.
     *
     * @return List changed chunk sections around the given coordinate.
     */
    List<ChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ,
                                         int lightLevel, int lightType);

    @Override
    default List<ChunkData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ,
                                         int lightLevel, int lightType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return new ArrayList<>();
        }
        return collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightType);
    }

    @Override
    default int sendChunk(ChunkData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, data.getChunkX(), data.getChunkZ(), data.getSectionMaskSky(),
                data.getSectionMaskBlock());
    }

    /**
     * Instant sending a full chunk to players in the world. Sends a single packet.
     */
    int sendChunk(World world, int chunkX, int chunkZ);

    @Override
    default int sendChunk(String worldName, int chunkX, int chunkZ) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, chunkX, chunkZ);
    }

    /**
     * Instant sending a chunk with specific section to players in the world. Sends
     * a single packet.
     */
    int sendChunk(World world, int chunkX, int chunkZ, int chunkSectionY);

    @Override
    default int sendChunk(String worldName, int chunkX, int chunkZ, int chunkSectionY) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, chunkX, chunkZ, chunkSectionY);
    }

    /**
     * Instant sending a chunk with with known sections mask to players in the
     * world. Sends a single packet.
     */
    int sendChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock);

    default int sendChunk(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        return sendChunk(world, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
    }
}
