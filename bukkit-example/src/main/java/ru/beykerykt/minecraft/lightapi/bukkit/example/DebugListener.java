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
package ru.beykerykt.minecraft.lightapi.bukkit.example;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;

import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.RequestFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;

public class DebugListener implements Listener {

    private final Map<Long, Integer> mLightTasks = new HashMap<>();

    // testing
    private final LightAPI mAPI;
    private final BukkitPlugin mPlugin;

    public DebugListener(BukkitPlugin plugin) {
        mPlugin = plugin;
        mAPI = LightAPI.get();
    }

    private void setLightLevel(Block block, int lightLevel) {
        Location location = block.getLocation();
        switch (block.getType()) {
            case BEDROCK: {
                int flags = LightType.BLOCK_LIGHTING;
                EditPolicy editPolicy = EditPolicy.IMMEDIATE;
                SendPolicy sendPolicy = SendPolicy.IMMEDIATE;
                mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY() + mPlugin.getOffsetY(), location.getBlockZ(), lightLevel, flags,
                        editPolicy, sendPolicy, (requestFlag, resultCode) -> mPlugin.getServer().getLogger().info(
                                block.getType().name() + ": requestFlag: " + requestFlag + " resultCode: "
                                        + resultCode));
                break;
            }
            case REDSTONE_BLOCK: {
                int flags = LightType.BLOCK_LIGHTING;
                int oldBlockLight = LightAPI.get().getLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), flags);
                EditPolicy editPolicy = EditPolicy.DEFERRED;
                SendPolicy sendPolicy = SendPolicy.DEFERRED;
                int result = mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY() + mPlugin.getOffsetY(), location.getBlockZ(), lightLevel, flags,
                        editPolicy, sendPolicy, (requestFlag, resultCode) -> {
                            switch (requestFlag) {
                                case RequestFlag.EDIT:
                                    if (resultCode == ResultCode.SUCCESS) {
                                        mPlugin.getServer().getLogger().info(
                                                block.getType().name() + "Light level was edited.");
                                    } else {
                                        mPlugin.getServer().getLogger().info(
                                                block.getType().name() + "request (" + requestFlag + "): result code "
                                                        + resultCode);
                                    }
                                    break;
                                case RequestFlag.RECALCULATE:
                                    if (resultCode == ResultCode.SUCCESS) {
                                        int newBlockLight = LightAPI.get().getLightLevel(location.getWorld().getName(),
                                                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                                                flags);
                                        if (oldBlockLight != newBlockLight) {
                                            mPlugin.getServer().getLogger().info(
                                                    block.getType().name() + "Light level was recalculated.");
                                        } else {
                                            mPlugin.getServer().getLogger().info(
                                                    block.getType().name() + "Light level was not recalculated. (Old:"
                                                            + oldBlockLight + " new:" + newBlockLight + ")");
                                        }
                                    } else {
                                        mPlugin.getServer().getLogger().info(
                                                block.getType().name() + "request (" + requestFlag + "): result code "
                                                        + resultCode);
                                    }
                                    break;
                                case RequestFlag.SEPARATE_SEND:
                                case RequestFlag.COMBINED_SEND:
                                    mPlugin.getServer().getLogger().info(
                                            block.getType().name() + "request (" + requestFlag + "): result code "
                                                    + resultCode);
                                    break;
                            }
                        });
                int blockLight = mAPI.getLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ(), flags);
                mPlugin.getServer().getLogger().info("blockLight: " + blockLight + " result: " + result);
                break;
            }
            case OBSIDIAN: {
                int flags = LightType.BLOCK_LIGHTING;
                EditPolicy editPolicy = EditPolicy.FORCE_IMMEDIATE;
                SendPolicy sendPolicy = SendPolicy.IMMEDIATE;
                mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY() + mPlugin.getOffsetY(), location.getBlockZ(), lightLevel, flags,
                        editPolicy, sendPolicy, (requestFlag, resultCode) -> mPlugin.getServer().getLogger().info(
                                block.getType().name() + ": requestFlag: " + requestFlag + " resultCode: "
                                        + resultCode));
                break;
            }
            case GLASS: {
                int flags = LightType.SKY_LIGHTING;
                EditPolicy editPolicy = EditPolicy.IMMEDIATE;
                SendPolicy sendPolicy = SendPolicy.IMMEDIATE;
                mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                        location.getBlockY() + mPlugin.getOffsetY(), location.getBlockZ(), lightLevel, flags,
                        editPolicy, sendPolicy, (requestFlag, resultCode) -> mPlugin.getServer().getLogger().info(
                                block.getType().name() + ": requestFlag: " + requestFlag + " resultCode: "
                                        + resultCode));
                break;
            }
            case REDSTONE_LAMP: {
                int flags = LightType.BLOCK_LIGHTING;
                long longPos = BlockPosition.asLong(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                if (mLightTasks.containsKey(longPos)) {
                    int taskId = mLightTasks.get(longPos);
                    mPlugin.getServer().getScheduler().cancelTask(taskId);
                    mLightTasks.remove(longPos);
                    mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(), location.getBlockY(),
                            location.getBlockZ(), 0, flags, null);
                    return;
                }

                if (lightLevel > 0) {
                    int taskId = mPlugin.getServer().getScheduler().runTaskTimer(mPlugin, () -> {
                        int currentLightLevel = mAPI.getLightLevel(location.getWorld().getName(), location.getBlockX(),
                                location.getBlockY(), location.getBlockZ(), flags);
                        if (currentLightLevel == 0) {
                            mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                                    location.getBlockY(), location.getBlockZ(), lightLevel, flags, null);
                        } else if (currentLightLevel > 0) {
                            mAPI.setLightLevel(location.getWorld().getName(), location.getBlockX(),
                                    location.getBlockY(), location.getBlockZ(), 0, flags, null);
                        }
                    }, 20L, 20L).getTaskId();
                    mLightTasks.put(longPos, taskId);
                }
            }
            default:
                break;
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock() == null) {
            return;
        }

        int lightLevel = 15;
        setLightLevel(event.getBlock(), lightLevel);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock() == null) {
            return;
        }

        int lightLevel = 0;
        setLightLevel(event.getBlock(), lightLevel);
    }
}
