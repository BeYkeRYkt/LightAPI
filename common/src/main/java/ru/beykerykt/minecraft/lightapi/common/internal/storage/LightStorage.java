/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.common.internal.storage;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCodes;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.storage.IStorageProvider;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;

import java.util.HashMap;
import java.util.Map;

public class LightStorage {

    private String mWorldName = null;
    private Map<Long, Integer> mLightLevels = new HashMap<Long, Integer>();
    private IStorageProvider mStorageProvider = null;

    public LightStorage(String worldName, IStorageProvider provider) {
        this.mWorldName = worldName;
        this.mStorageProvider = provider != null ? provider : new EmptyStorageProvider();
    }

    public void destroy() {
        mWorldName = null;
        mLightLevels.clear();
        mLightLevels = null;
        mStorageProvider = null;
    }

    /**
     * N/A
     */
    public String getWorldName() {
        return mWorldName;
    }

    /*
     * Light Level
     */

    /**
     * N/A
     */
    public void putLightSource(BlockPosition pos, int lightlevel) {
        long longPos = pos.asLong();
        if (!mLightLevels.containsKey(longPos)) {
            mLightLevels.put(longPos, lightlevel);
        }
    }

    /**
     * N/A
     */
    public void putLightSource(long longPos, int lightlevel) {
        if (!mLightLevels.containsKey(longPos)) {
            mLightLevels.put(longPos, lightlevel);
        }
    }

    /**
     * N/A
     */
    public boolean checkLightSource(BlockPosition pos) {
        long longPos = pos.asLong();
        return mLightLevels.containsKey(longPos);
    }

    public boolean checkLightSource(long longPos) {
        return mLightLevels.containsKey(longPos);
    }

    /**
     * N/A
     */
    public int getLightLevel(long longPos) {
        if (!checkLightSource(longPos)) {
            return -1;
        }
        return mLightLevels.get(longPos);
    }

    /**
     * N/A
     */
    public void removeLightSource(long longPos) {
        if (mLightLevels.containsKey(longPos)) {
            mLightLevels.remove(longPos);
        }
    }

    /*
     * Storage Provider
     */

    /**
     * N/A
     */
    public int restoreLightSources() {
        if (mWorldName == null) {
            return ResultCodes.FAILED;
        }
        if (mStorageProvider == null) {
            return ResultCodes.FAILED;
        }
        mLightLevels.clear();
        mLightLevels.putAll(mStorageProvider.loadLightSources(getWorldName()));
        return ResultCodes.SUCCESS;
    }

    /**
     * N/A
     */
    public int saveLightSources() {
        if (mWorldName == null) {
            return ResultCodes.FAILED;
        }
        if (mStorageProvider == null) {
            return ResultCodes.FAILED;
        }

        int code = mStorageProvider.saveLightSources(getWorldName(), mLightLevels);
        if (code != ResultCodes.SUCCESS) { // something is wrong ?
            return code;
        }
        return ResultCodes.SUCCESS;
    }
}
