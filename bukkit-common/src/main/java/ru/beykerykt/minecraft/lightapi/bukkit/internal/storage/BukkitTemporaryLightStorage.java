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

import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.ILightStorage;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

/**
 * Use it if we don't have a persistent storage
 */
public class BukkitTemporaryLightStorage implements ILightStorage {

    private final IPlatformImpl mPlatform;

    private final String mWorldName;
    private final Map<Long, Integer> mSkyLightLevels = new ConcurrentHashMap();
    private final Map<Long, Integer> mBlockLightLevels = new ConcurrentHashMap<>();

    public BukkitTemporaryLightStorage(IPlatformImpl platform, String worldName) {
        this.mPlatform = platform;
        this.mWorldName = worldName;
    }

    private void destroy() {
        this.mSkyLightLevels.clear();
        this.mBlockLightLevels.clear();
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatform;
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
                } else {
                    mSkyLightLevels.replace(longPos, lightLevel);
                }
            } else {
                if (mSkyLightLevels.containsKey(longPos)) {
                    mSkyLightLevels.remove(longPos);
                }
            }
        }

        // blocklight
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            if (lightLevel > 0) {
                if (!mBlockLightLevels.containsKey(longPos)) {
                    mBlockLightLevels.put(longPos, lightLevel);
                } else {
                    mBlockLightLevels.replace(longPos, lightLevel);
                }
            } else {
                if (mBlockLightLevels.containsKey(longPos)) {
                    mBlockLightLevels.remove(longPos);
                }
            }
        }
    }

    @Override
    public boolean checkLightLevel(long longPos, int lightFlag) {
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
}
