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
package ru.beykerykt.lightapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.events.DeleteLightEvent;
import ru.beykerykt.lightapi.events.SetLightEvent;
import ru.beykerykt.lightapi.events.UpdateChunkEvent;
import ru.beykerykt.minecraft.lightapi.bukkit.api.extension.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;

@Deprecated
public class LightAPI extends JavaPlugin {

    private static final String DEPRECATED_MSG =
            "The package \"ru.beykerykt.lightapi\" is outdated! Switch to the new package \"ru.beykerykt.minecraft.lightapi.*\" or turn on the \"force-enable-legacy\" mode.";

    @Deprecated
    private static final BlockFace[] SIDES = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    private static boolean isBackwardEnabled() {
        return ((IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension()).isBackwardAvailable();
    }

    @Deprecated
    private static void log(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + "<LightAPI-Backward>: " + message);
    }

    @Deprecated
    public static LightAPI getInstance() {
        return null;
    }

    @Deprecated
    public static boolean createLight(Location location, int lightlevel, boolean async) {
        return createLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                lightlevel, async);
    }

    @Deprecated
    public static boolean createLight(final World world, final int x, final int y, final int z, final int lightlevel,
            boolean async) {
        return createLight(world, x, y, z, ru.beykerykt.lightapi.LightType.BLOCK, lightlevel, async);
    }

    @Deprecated
    public static boolean deleteLight(Location location, boolean async) {
        return deleteLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                async);
    }

    @Deprecated
    public static boolean deleteLight(final World world, final int x, final int y, final int z, boolean async) {
        return deleteLight(world, x, y, z, ru.beykerykt.lightapi.LightType.BLOCK, async);
    }

    @Deprecated
    public static List<ChunkInfo> collectChunks(Location location) {
        return collectChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Deprecated
    public static List<ChunkInfo> collectChunks(final World world, final int x, final int y, final int z) {
        return collectChunks(world, x, y, z, ru.beykerykt.lightapi.LightType.BLOCK, 15);
    }

    @Deprecated
    public static boolean updateChunks(ChunkInfo info) {
        return updateChunk(info);
    }

    @Deprecated
    public static boolean updateChunk(ChunkInfo info) {
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return false;
        }
        UpdateChunkEvent event = new UpdateChunkEvent(info);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            IBukkitExtension ext =
                    (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
            IHandler handler = ext.getHandler();
            // TODO: Say to handler
            IChunkData newData = handler.createChunkData(event.getChunkInfo().getWorld().getName(),
                    event.getChunkInfo().getChunkX(), event.getChunkInfo().getChunkZ());
            newData.setFullSections();
            handler.sendChunk(newData);
            return true;
        }
        return false;
    }

    @Deprecated
    public static boolean updateChunks(Location location, Collection<? extends Player> players) {
        return updateChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                players);
    }

    @Deprecated
    public static boolean updateChunks(World world, int x, int y, int z, Collection<? extends Player> players) {
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return false;
        }
        IBukkitExtension ext = (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
        IHandler handler = ext.getHandler();
        for (IChunkData newData : handler.collectChunkSections(world, x, y, z, 15,
                LightFlag.BLOCK_LIGHTING | LightFlag.SKY_LIGHTING)) {
            handler.sendChunk(newData);
        }
        return true;
    }

    @Deprecated
    public static boolean updateChunk(Location location, Collection<? extends Player> players) {
        return updateChunk(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                players);
    }

    @Deprecated
    public static boolean updateChunk(World world, int x, int y, int z, Collection<? extends Player> players) {
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return false;
        }
        IBukkitExtension ext = (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
        IHandler handler = ext.getHandler();
        IChunkData data = ext.getHandler().createChunkData(world.getName(), x >> 4, z >> 4);
        data.setFullSections();
        handler.sendChunk(data);
        return true;
    }

    @Deprecated
    public static Block getAdjacentAirBlock(Block block) {
        for (BlockFace face : SIDES) {
            if (block.getY() == 0x0 && face == BlockFace.DOWN) {
                continue;
            }
            if (block.getY() == 0xFF && face == BlockFace.UP) {
                continue;
            }

            Block candidate = block.getRelative(face);

            if (candidate.getType().isTransparent()) {
                return candidate;
            }
        }
        return block;
    }

    // Qvesh's fork - start
    @Deprecated
    public static boolean createLight(Location location, ru.beykerykt.lightapi.LightType lightType, int lightlevel,
            boolean async) {
        return createLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                lightType, lightlevel, async);
    }

    @Deprecated
    public static boolean createLight(World world, int x, final int y, final int z,
            ru.beykerykt.lightapi.LightType lightType, final int lightlevel, boolean async) {
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return false;
        }
        int flags = LightFlag.NONE;
        if (lightType == ru.beykerykt.lightapi.LightType.BLOCK) {
            flags |= LightFlag.BLOCK_LIGHTING;
        } else if (lightType == ru.beykerykt.lightapi.LightType.SKY) {
            flags |= LightFlag.SKY_LIGHTING;
        }
        final SetLightEvent event = new SetLightEvent(world, x, y, z, lightlevel, async);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
      /*
      if (ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getImplHandler().isAsyncLighting()) {
      	// not supported
      	return false;
      } */

            IBukkitExtension ext =
                    (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
            IHandler handler = ext.getHandler();

            int oldlightlevel = handler.getRawLightLevel(event.getWorld(), event.getX(), event.getY(), event.getZ(),
                    flags);

            handler.setRawLightLevel(event.getWorld(), event.getX(), event.getY(), event.getZ(), event.getLightLevel(),
                    flags);
            handler.recalculateLighting(event.getWorld(), event.getX(), event.getY(), event.getZ(), flags);

            // check light
            int newLightLevel = handler.getRawLightLevel(event.getWorld(), event.getX(), event.getY(), event.getZ(),
                    flags);
            return newLightLevel >= oldlightlevel;
        }
        return false;
    }

    @Deprecated
    public static boolean deleteLight(Location location, ru.beykerykt.lightapi.LightType lightType, boolean async) {
        return deleteLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                lightType, async);
    }

    @Deprecated
    public static boolean deleteLight(final World world, final int x, final int y, final int z,
            ru.beykerykt.lightapi.LightType lightType, boolean async) {
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return false;
        }
        int flags = LightFlag.NONE;
        if (lightType == ru.beykerykt.lightapi.LightType.BLOCK) {
            flags |= LightFlag.BLOCK_LIGHTING;
        } else if (lightType == ru.beykerykt.lightapi.LightType.SKY) {
            flags |= LightFlag.SKY_LIGHTING;
        }
        final DeleteLightEvent event = new DeleteLightEvent(world, x, y, z, async);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
      /*
      if (ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getAdapterImpl().isAsyncLighting()) {
      	// not supported
      	return false;
      }
      */

            IBukkitExtension ext =
                    (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
            IHandler handler = ext.getHandler();

            int oldlightlevel = handler.getRawLightLevel(event.getWorld(), event.getX(), event.getY(), event.getZ(),
                    flags);
            handler.setRawLightLevel(event.getWorld(), event.getX(), event.getY(), event.getZ(), 0, flags);
            handler.recalculateLighting(event.getWorld(), event.getX(), event.getY(), event.getZ(), flags);

            // check light
            int newLightLevel = handler.getRawLightLevel(event.getWorld(), event.getX(), event.getY(), event.getZ(),
                    flags);
            return newLightLevel != oldlightlevel;
        }
        return false;
    }

    public static List<ChunkInfo> collectChunks(Location location, ru.beykerykt.lightapi.LightType lightType,
            int lightLevel) {
        return collectChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                lightType, lightLevel);
    }

    public static List<ChunkInfo> collectChunks(World world, int x, int y, int z,
            ru.beykerykt.lightapi.LightType lightType, int lightLevel) {
        List<ChunkInfo> list = new CopyOnWriteArrayList<>();
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return list;
        }
        IBukkitExtension ext = (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
        IHandler handler = ext.getHandler();
        int lightTypeNew = LightFlag.BLOCK_LIGHTING;
        if (lightType == ru.beykerykt.lightapi.LightType.SKY) {
            lightTypeNew = LightFlag.SKY_LIGHTING;
        }
        for (IChunkData newData : handler.collectChunkSections(world, x, y, z, lightLevel, lightTypeNew)) {
            ChunkInfo info = new ChunkInfo(world, newData.getChunkX(), newData.getChunkZ(), world.getPlayers());
            if (!list.contains(info)) {
                list.add(info);
            }
        }
        return list;
    }

    public static boolean updateChunk(ChunkInfo info, ru.beykerykt.lightapi.LightType lightType) {
        return updateChunk(info, lightType, null);
    }

    public static boolean updateChunk(ChunkInfo info, ru.beykerykt.lightapi.LightType lightType,
            Collection<? extends Player> players) {
        if (!isBackwardEnabled()) {
            log(Bukkit.getServer().getConsoleSender(), DEPRECATED_MSG);
            return false;
        }
        UpdateChunkEvent event = new UpdateChunkEvent(info);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            IBukkitExtension ext =
                    (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
            IHandler handler = ext.getHandler();
            // TODO: Say to handler
            IChunkData newData = handler.createChunkData(event.getChunkInfo().getWorld().getName(),
                    event.getChunkInfo().getChunkX(), event.getChunkInfo().getChunkZ());
            newData.setFullSections();
            handler.sendChunk(newData);
            return true;
        }
        return false;
    }

    @Deprecated
    public static boolean isSupported(World world, ru.beykerykt.lightapi.LightType type) {
        IBukkitExtension ext = (IBukkitExtension) ru.beykerykt.minecraft.lightapi.common.LightAPI.get().getExtension();
        IHandler handler = ext.getHandler();
        int flag = LightFlag.BLOCK_LIGHTING;
        if (type == ru.beykerykt.lightapi.LightType.SKY) {
            flag = LightFlag.SKY_LIGHTING;
        }
        return handler.isLightingSupported(world, flag);
    }

    @Deprecated
    public int getUpdateDelayTicks() {
        return 0;
    }

    @Deprecated
    public void setUpdateDelayTicks(int update_delay_ticks) {
    }

    @Deprecated
    public int getMaxIterationsPerTick() {
        return 0;
    }

    @Deprecated
    public void setMaxIterationsPerTick(int max_iterations_per_tick) {
    }
    // Qvesh's fork - end
}
