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

package ru.beykerykt.minecraft.lightapi.bukkit;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.IScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.ILightStorage;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageProvider;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;

public class BukkitListener implements Listener {

    private static final BlockFace[] SIDES =
            {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private BukkitPlugin mPlugin;

    public BukkitListener(BukkitPlugin plugin) {
        this.mPlugin = plugin;
    }

    private BukkitPlatformImpl getPlatformImpl() {
        return mPlugin.getPlatformImpl();
    }

    private boolean checkBlock(Location loc, int lightFlags, boolean lightInBlock) {
        IStorageProvider storageProvider = getPlatformImpl().getStorageProvider();
        ILightStorage lightStorage = storageProvider.getLightStorage(loc.getWorld().getName());
        if (lightStorage == null) {
            return false;
        }
        long longPos = BlockPosition.asLong(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        int blockLight = lightStorage.getLightLevel(longPos, lightFlags);
        getPlatformImpl().debug(
                "x=" + loc.getBlockX() + " y=" + loc.getBlockY() + " z=" + loc.getBlockZ() + " lightLevel="
                        + blockLight);
        if (blockLight != -1) {
            int serverBlockLight = getPlatformImpl().getLightLevel(loc.getWorld(), loc.getBlockX(), loc.getBlockY(),
                    loc.getBlockZ(), lightFlags);
            // Restore the light if the current light level does not match the declared one, or if a block that will
            // block the light will be placed or broken at the current location in the future.
            if (blockLight != serverBlockLight || lightInBlock) {
                getPlatformImpl().setLightLevel(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                        blockLight);
            } else {
                // notify send chunks
                IScheduledChunkObserver chunkObserver = (IScheduledChunkObserver) getPlatformImpl().getChunkObserver();
                chunkObserver.notifyUpdateChunks(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(),
                        loc.getBlockZ(), blockLight, lightFlags);
            }
            return true;
        }
        return false;
    }

    private void checkSides(Location loc, int lightFlags, boolean lightInBlock) {
        for (BlockFace face : SIDES) {
            Location sideLocation = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
            if (checkBlock(sideLocation, lightFlags, lightInBlock)) {
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        // check all sides
        if (block.getType().isSolid()) {
            int lightFlags = LightType.BLOCK_LIGHTING;
            if (!checkBlock(block.getLocation(), lightFlags, true)) {
                checkSides(block.getLocation(), lightFlags, false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        // check all sides
        int lightFlags = LightType.BLOCK_LIGHTING;
        if (!checkBlock(event.getBlock().getLocation(), lightFlags, true)) {
            checkSides(block.getLocation(), lightFlags, false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            // check all sides
            int lightFlags = LightType.BLOCK_LIGHTING;
            if (!checkBlock(event.getBlock().getLocation(), lightFlags, true)) {
                checkSides(event.getBlock().getLocation(), lightFlags, false);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        IStorageProvider storageProvider = getPlatformImpl().getStorageProvider();
        ILightStorage lightStorage = storageProvider.getLightStorage(chunk.getWorld().getName());
        if (lightStorage == null) {
            return;
        }

        // try to restore light levels
        if (lightStorage.containsChunk(chunk.getX(), chunk.getZ(), LightType.SKY_LIGHTING)) {
            lightStorage.loadLightDataForChunk(chunk.getX(), chunk.getZ(), LightType.SKY_LIGHTING, true);
        }
        if (lightStorage.containsChunk(chunk.getX(), chunk.getZ(), LightType.BLOCK_LIGHTING)) {
            lightStorage.loadLightDataForChunk(chunk.getX(), chunk.getZ(), LightType.BLOCK_LIGHTING, true);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // save light levels and unload chunk data
        Chunk chunk = event.getChunk();
        IStorageProvider storageProvider = getPlatformImpl().getStorageProvider();
        ILightStorage lightStorage = storageProvider.getLightStorage(chunk.getWorld().getName());
        if (lightStorage == null) {
            return;
        }

        if (lightStorage.containsChunk(chunk.getX(), chunk.getZ(), LightType.SKY_LIGHTING)) {
            lightStorage.unloadLightDataFromChunk(chunk.getX(), chunk.getZ(), LightType.SKY_LIGHTING);
        }
        if (lightStorage.containsChunk(chunk.getX(), chunk.getZ(), LightType.BLOCK_LIGHTING)) {
            lightStorage.unloadLightDataFromChunk(chunk.getX(), chunk.getZ(), LightType.BLOCK_LIGHTING);
        }
    }
}
