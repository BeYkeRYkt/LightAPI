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

package ru.beykerykt.minecraft.lightapi.bukkit.internal.storage;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.ILightStorage;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class BukkitPersistentLightStorage implements ILightStorage {

    private final static Object mLock = new Object();
    private final IPlatformImpl mPlatform;
    private final String mWorldName;

    public BukkitPersistentLightStorage(IPlatformImpl platform, String worldName) {
        this.mPlatform = platform;
        this.mWorldName = worldName;
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatform;
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public String getWorldName() {
        return mWorldName;
    }

    /* @hide */
    private int writeLightLevel(String world, long longPos, int lightLevel, int lightFlag) {
        int chunkX = BlockPosition.unpackLongX(longPos) >> 4;
        int chunkZ = BlockPosition.unpackLongZ(longPos) >> 4;

        World bukkitWorld = Bukkit.getWorld(world);
        if (!bukkitWorld.isChunkLoaded(chunkX, chunkZ)) {
            // DO NOT INTERACT WITH NOT LOADED CHUNKS!
            return ResultCode.CHUNK_NOT_LOADED;
        }

        Chunk chunk = bukkitWorld.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer root = chunk.getPersistentDataContainer();
        String rootKey = String.valueOf(longPos);
        /**
         * lightContainer (longPos) -> NamespacedKey(lightFlag) -> lightLevel
         */
        PersistentDataContainer lightContainer;
        if (root.has(new NamespacedKey(BukkitPlugin.getInstance(), rootKey), PersistentDataType.TAG_CONTAINER)) {
            lightContainer = root.get(new NamespacedKey(BukkitPlugin.getInstance(), rootKey),
                    PersistentDataType.TAG_CONTAINER);
            mPlatform.debug("writeLightLevel: Found");
        } else {
            lightContainer = root.getAdapterContext().newPersistentDataContainer();
            mPlatform.debug("writeLightLevel: Create");
        }
        lightContainer.set(new NamespacedKey(BukkitPlugin.getInstance(), String.valueOf(lightFlag)),
                PersistentDataType.INTEGER, lightLevel);
        root.set(new NamespacedKey(BukkitPlugin.getInstance(), rootKey), PersistentDataType.TAG_CONTAINER,
                lightContainer);
        return ResultCode.SUCCESS;
    }

    /* @hide */
    private void deleteLightLevel(String world, long longPos, int lightFlag) {
        if (!checkLightLevel(longPos, lightFlag)) {
            return;
        }
        int chunkX = BlockPosition.unpackLongX(longPos) >> 4;
        int chunkZ = BlockPosition.unpackLongZ(longPos) >> 4;

        World bukkitWorld = Bukkit.getWorld(world);
        if (!bukkitWorld.isChunkLoaded(chunkX, chunkZ)) {
            // DO NOT INTERACT WITH NOT LOADED CHUNKS!
            return;
        }

        Chunk chunk = bukkitWorld.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer root = chunk.getPersistentDataContainer();
        String rootKey = String.valueOf(longPos);
        if (!root.has(new NamespacedKey(BukkitPlugin.getInstance(), rootKey), PersistentDataType.TAG_CONTAINER)) {
            return;
        }
        /**
         * lightContainer (longPos) -> NamespacedKey(lightFlag) -> lightLevel
         */
        PersistentDataContainer lightContainer = root.get(new NamespacedKey(BukkitPlugin.getInstance(), rootKey),
                PersistentDataType.TAG_CONTAINER);
        String lightKey = String.valueOf(lightFlag);
        if (!lightContainer.has(new NamespacedKey(BukkitPlugin.getInstance(), lightKey), PersistentDataType.INTEGER)) {
            return;
        }
        lightContainer.remove(new NamespacedKey(BukkitPlugin.getInstance(), lightKey));
        if (!lightContainer.has(new NamespacedKey(BukkitPlugin.getInstance(), lightKey), PersistentDataType.INTEGER)) {
            mPlatform.debug("deleteLightLevel: REMOVED!");
        }
        root.set(new NamespacedKey(BukkitPlugin.getInstance(), rootKey), PersistentDataType.TAG_CONTAINER,
                lightContainer);

        if (lightContainer.isEmpty()) {
            getPlatformImpl().debug("isEmpty");
            root.remove(new NamespacedKey(BukkitPlugin.getInstance(), rootKey));
        }
    }

    /* @hide */
    private void setLightLevelLocked(long longPos, int lightLevel, int lightFlags) {
        // skylight
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            if (lightLevel > 0) {
                writeLightLevel(getWorldName(), longPos, lightLevel, LightFlag.SKY_LIGHTING);
            } else {
                if (checkLightLevel(longPos, LightFlag.SKY_LIGHTING)) {
                    deleteLightLevel(getWorldName(), longPos, LightFlag.SKY_LIGHTING);
                }
            }
        }

        // blocklight
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            if (lightLevel > 0) {
                writeLightLevel(getWorldName(), longPos, lightLevel, LightFlag.BLOCK_LIGHTING);
            } else {
                if (checkLightLevel(longPos, LightFlag.BLOCK_LIGHTING)) {
                    deleteLightLevel(getWorldName(), longPos, LightFlag.BLOCK_LIGHTING);
                }
            }
        }
    }

    @Override
    public void setLightLevel(long longPos, int lightLevel, int lightFlags) {
        if (!getPlatformImpl().isWorldAvailable(getWorldName())) {
            return;
        }
        if (getPlatformImpl().getBackgroundService().isMainThread()) {
            setLightLevelLocked(longPos, lightLevel, lightFlags);
        } else {
            synchronized (mLock) {
                setLightLevelLocked(longPos, lightLevel, lightFlags);
            }
        }
    }

    /* @hide */
    private int getLightLevelLocked(long longPos, int lightFlag) {
        if (!checkLightLevel(longPos, lightFlag)) {
            return -1;
        }

        int chunkX = BlockPosition.unpackLongX(longPos) >> 4;
        int chunkZ = BlockPosition.unpackLongZ(longPos) >> 4;

        World bukkitWorld = Bukkit.getWorld(getWorldName());
        if (!bukkitWorld.isChunkLoaded(chunkX, chunkZ)) {
            // DO NOT INTERACT WITH NOT LOADED CHUNKS!
            return -1;
        }

        Chunk chunk = bukkitWorld.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer root = chunk.getPersistentDataContainer();
        String rootKey = String.valueOf(longPos);
        /**
         * lightContainer (longPos) -> NamespacedKey(lightFlag) -> lightLevel
         */
        if (!root.has(new NamespacedKey(BukkitPlugin.getInstance(), rootKey), PersistentDataType.TAG_CONTAINER)) {
            return -1;
        }
        PersistentDataContainer longPosRoot = root.get(new NamespacedKey(BukkitPlugin.getInstance(), rootKey),
                PersistentDataType.TAG_CONTAINER);
        String longPosRootKey = null;
        if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
            longPosRootKey = String.valueOf(LightFlag.SKY_LIGHTING);
        } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
            longPosRootKey = String.valueOf(LightFlag.BLOCK_LIGHTING);
        }
        if (!longPosRoot.has(new NamespacedKey(BukkitPlugin.getInstance(), longPosRootKey),
                PersistentDataType.INTEGER)) {
            return -1;
        }
        int lightLevel = longPosRoot.get(new NamespacedKey(BukkitPlugin.getInstance(), longPosRootKey),
                PersistentDataType.INTEGER);
        return lightLevel;
    }

    @Override
    public int getLightLevel(long longPos, int lightFlag) {
        if (!getPlatformImpl().isWorldAvailable(getWorldName())) {
            return ResultCode.WORLD_NOT_AVAILABLE;
        }
        if (getPlatformImpl().getBackgroundService().isMainThread()) {
            return getLightLevelLocked(longPos, lightFlag);
        } else {
            synchronized (mLock) {
                return getLightLevelLocked(longPos, lightFlag);
            }
        }
    }

    /* @hide */
    private boolean checkLightLevelLocked(long longPos, int lightFlag) {
        int chunkX = BlockPosition.unpackLongX(longPos) >> 4;
        int chunkZ = BlockPosition.unpackLongZ(longPos) >> 4;

        World bukkitWorld = Bukkit.getWorld(getWorldName());
        if (!bukkitWorld.isChunkLoaded(chunkX, chunkZ)) {
            // DO NOT INTERACT WITH NOT LOADED CHUNKS!
            return false;
        }
        Chunk chunk = bukkitWorld.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer root = chunk.getPersistentDataContainer();
        String rootKey = String.valueOf(longPos);
        if (!root.has(new NamespacedKey(BukkitPlugin.getInstance(), rootKey), PersistentDataType.TAG_CONTAINER)) {
            return false;
        }
        /**
         * lightContainer (longPos) -> NamespacedKey(lightFlag) -> lightLevel
         */
        PersistentDataContainer longPosRoot = root.get(new NamespacedKey(BukkitPlugin.getInstance(), rootKey),
                PersistentDataType.TAG_CONTAINER);
        String longPosRootkey = null;
        if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
            longPosRootkey = String.valueOf(LightFlag.SKY_LIGHTING);
        } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
            longPosRootkey = String.valueOf(LightFlag.BLOCK_LIGHTING);
        }
        if (!longPosRoot.has(new NamespacedKey(BukkitPlugin.getInstance(), longPosRootkey),
                PersistentDataType.INTEGER)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkLightLevel(long longPos, int lightFlag) {
        if (!getPlatformImpl().isWorldAvailable(getWorldName())) {
            return false;
        }
        if (getPlatformImpl().getBackgroundService().isMainThread()) {
            return checkLightLevelLocked(longPos, lightFlag);
        } else {
            synchronized (mLock) {
                return checkLightLevelLocked(longPos, lightFlag);
            }
        }
    }
}
