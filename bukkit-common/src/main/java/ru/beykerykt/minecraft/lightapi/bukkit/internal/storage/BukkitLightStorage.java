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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.ILightStorage;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageFile;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class BukkitLightStorage implements ILightStorage {

    private final IPlatformImpl mPlatform;

    private final String mWorldName;
    private final IStorageFile mStorageFile;

    private final Map<Long, Integer> mSkyLightLevels = new ConcurrentHashMap();
    private final Map<Long, Integer> mBlockLightLevels = new ConcurrentHashMap<>();

    public BukkitLightStorage(IPlatformImpl platform, String worldName, IStorageFile storageFile) {
        this.mPlatform = platform;
        this.mWorldName = worldName;
        this.mStorageFile = storageFile;
    }

    public void destroy() {
        this.mSkyLightLevels.clear();
        this.mBlockLightLevels.clear();
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatform;
    }

    protected IStorageFile getStorageFile() {
        return mStorageFile;
    }

    @Override
    public void onShutdown() {
        destroy();
    }

    @Override
    public String getWorldName() {
        return mWorldName;
    }

    @Override
    public void setLightLevel(long longPos, int lightLevel, int lightFlags) {
        // skylight
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            if (lightLevel > 0) {
                if (!mSkyLightLevels.containsKey(longPos)) {
                    mSkyLightLevels.put(longPos, lightLevel);
                }
            } else {
                mSkyLightLevels.remove(longPos);
                getStorageFile().deleteLightLevel(getWorldName(), longPos, LightFlag.SKY_LIGHTING);
            }
        }

        // blocklight
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            if (lightLevel > 0) {
                if (!mBlockLightLevels.containsKey(longPos)) {
                    mBlockLightLevels.put(longPos, lightLevel);
                }
            } else {
                mBlockLightLevels.remove(longPos);
                getStorageFile().deleteLightLevel(getWorldName(), longPos, LightFlag.BLOCK_LIGHTING);
            }
        }
    }

    private boolean checkLightLevel(long longPos, int lightFlag) {
        if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
            return mSkyLightLevels.containsKey(longPos);
        } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
            return mBlockLightLevels.containsKey(longPos);
        }
        return false;
    }

    @Override
    public int getLightLevel(long longPos, int lightFlag) {
        if (!checkLightLevel(longPos, lightFlag)) {
            return -1;
        }
        if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
            return mSkyLightLevels.get(longPos);
        } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
            return mBlockLightLevels.get(longPos);
        }
        return -1;
    }

    @Override
    public boolean containsChunk(int chunkX, int chunkZ, int lightFlag) {
        if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
            return containsChunk(chunkX, chunkZ, lightFlag, mSkyLightLevels);
        } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
            return containsChunk(chunkX, chunkZ, lightFlag, mBlockLightLevels);
        }
        return getStorageFile().containsChunk(getWorldName(), chunkX, chunkZ, lightFlag);
    }

    private boolean containsChunk(int chunkX, int chunkZ, int lightFlag, Map<Long, Integer> map) {
        for (long longPos : map.keySet()) {
            int posChunkX = BlockPosition.unpackLongX(longPos) >> 4;
            int posChunkZ = BlockPosition.unpackLongZ(longPos) >> 4;
            if (chunkX == posChunkX && chunkZ == posChunkZ) {
                return true;
            }
        }
        return getStorageFile().containsChunk(getWorldName(), chunkX, chunkZ, lightFlag);
    }

    @Override
    public void loadLightDataForChunk(int chunkX, int chunkZ, int lightFlag, boolean restore) {
        getPlatformImpl().debug(">>>>>>");
        getPlatformImpl().debug("loadLightDataForChunk(" + chunkX + ", " + chunkZ + ", " + lightFlag + ")");
        getPlatformImpl().debug("pre mSkyLightLevels: " + mSkyLightLevels.size());
        getPlatformImpl().debug("pre mBlockLightLevels: " + mBlockLightLevels.size());
        Map<Long, Integer> map = getStorageFile().loadLightDataForChunk(getWorldName(), chunkX, chunkZ, lightFlag);
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            long longPos = entry.getKey();

            if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
                if (mSkyLightLevels.containsKey(longPos)) {
                    continue;
                }
            } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
                if (mBlockLightLevels.containsKey(longPos)) {
                    continue;
                }
            }

            int lightLevel = entry.getValue();
            int blockX = BlockPosition.unpackLongX(longPos);
            int blockY = BlockPosition.unpackLongY(longPos);
            int blockZ = BlockPosition.unpackLongZ(longPos);
            if (lightLevel > 0) {
                setLightLevel(longPos, lightLevel, lightFlag);
                if (restore) {
                    LightAPI.get().setLightLevel(getWorldName(), blockX, blockY, blockZ, lightLevel, lightFlag);
                }
            }
        }
        getPlatformImpl().debug("post mSkyLightLevels: " + mSkyLightLevels.size());
        getPlatformImpl().debug("post mBlockLightLevels: " + mBlockLightLevels.size());
    }

    @Override
    public void unloadLightDataFromChunk(int chunkX, int chunkZ, int lightFlag) {
        getPlatformImpl().debug(">>>>>>");
        getPlatformImpl().debug("unloadLightDataFromChunk(" + chunkX + ", " + chunkZ + ", " + lightFlag + ")");
        getPlatformImpl().debug("pre mSkyLightLevels: " + mSkyLightLevels.size());
        getPlatformImpl().debug("pre mBlockLightLevels: " + mBlockLightLevels.size());
        if (FlagUtils.isFlagSet(lightFlag, LightFlag.SKY_LIGHTING)) {
            unloadLightDataFromChunk(chunkX, chunkZ, lightFlag, mSkyLightLevels);
        } else if (FlagUtils.isFlagSet(lightFlag, LightFlag.BLOCK_LIGHTING)) {
            unloadLightDataFromChunk(chunkX, chunkZ, lightFlag, mBlockLightLevels);
        }
        getPlatformImpl().debug("post mSkyLightLevels: " + mSkyLightLevels.size());
        getPlatformImpl().debug("post mBlockLightLevels: " + mBlockLightLevels.size());
    }

    private void unloadLightDataFromChunk(int chunkX, int chunkZ, int lightFlag, Map<Long, Integer> map) {
        for (long longPos : map.keySet()) {
            int posChunkX = BlockPosition.unpackLongX(longPos) >> 4;
            int posChunkZ = BlockPosition.unpackLongZ(longPos) >> 4;
            if (chunkX == posChunkX && chunkZ == posChunkZ) {
                int lightLevel = map.get(longPos);
                getStorageFile().writeLightLevel(getWorldName(), longPos, lightLevel, lightFlag);
                map.remove(longPos);
            }
        }
    }

    @Override
    public void saveLightData() {
        if (!mSkyLightLevels.isEmpty()) {
            saveLightData(mSkyLightLevels, LightFlag.SKY_LIGHTING);
        }

        if (!mBlockLightLevels.isEmpty()) {
            saveLightData(mBlockLightLevels, LightFlag.BLOCK_LIGHTING);
        }
    }

    private void saveLightData(Map<Long, Integer> lightLevels, int lightFlag) {
        for (Map.Entry<Long, Integer> entry : lightLevels.entrySet()) {
            Long longPos = entry.getKey();
            int lightLevel = entry.getValue();
            getStorageFile().writeLightLevel(getWorldName(), longPos, lightLevel, lightFlag);
        }
    }
}
