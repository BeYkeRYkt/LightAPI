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

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.ILightEngine;

public class BukkitListener implements Listener {

    private static final BlockFace[] SIDES = {
            BlockFace.SELF,
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };
    private final BukkitPlugin mPlugin;

    public BukkitListener(BukkitPlugin plugin) {
        this.mPlugin = plugin;
    }

    private BukkitPlatformImpl getPlatformImpl() {
        return mPlugin.getPlatformImpl();
    }

    private boolean checkBlock(Location loc, int lightFlags) {
        ILightEngine engine = getPlatformImpl().getLightEngine();
        int resultCode = engine.checkLight(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                lightFlags);
        switch (resultCode) {
            case ResultCode.SUCCESS:
                return true;
        }
        return false;
    }

    private void checkSides(Location loc, int lightFlags) {
        for (BlockFace face : SIDES) {
            Location sideLocation = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
            if (checkBlock(sideLocation, lightFlags)) {
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        getPlatformImpl().getPlugin().getServer().getScheduler().runTaskLater(getPlatformImpl().getPlugin(), () -> {
            // check all sides
            checkSides(loc, lightFlags);
        }, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        getPlatformImpl().getPlugin().getServer().getScheduler().runTaskLater(getPlatformImpl().getPlugin(), () -> {
            // check all sides
            checkSides(loc, lightFlags);
        }, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        if (block.getType().isSolid()) {
            getPlatformImpl().getPlugin().getServer().getScheduler().runTaskLater(getPlatformImpl().getPlugin(), () -> {
                // check all sides
                checkSides(loc, lightFlags);
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        if (block.getType().isSolid()) {
            getPlatformImpl().getPlugin().getServer().getScheduler().runTaskLater(getPlatformImpl().getPlugin(), () -> {
                // check all sides
                checkSides(loc, lightFlags);
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        getPlatformImpl().getPlugin().getServer().getScheduler().runTaskLater(getPlatformImpl().getPlugin(), () -> {
            if (event.getEntityType() == EntityType.FALLING_BLOCK) {
                // check all sides
                checkSides(block.getLocation(), lightFlags);
            }
        }, 1);
    }
}
