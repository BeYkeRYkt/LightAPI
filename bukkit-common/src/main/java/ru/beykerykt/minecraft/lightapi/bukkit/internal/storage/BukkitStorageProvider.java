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

import java.util.HashMap;
import java.util.Map;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.ILightStorage;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageFile;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageProvider;

public class BukkitStorageProvider implements IStorageProvider {

    private final BukkitPlatformImpl mPlatform;
    private final IStorageFile mStorageFile;
    private final Map<String, ILightStorage> mLightStorageMap = new HashMap<>();

    public BukkitStorageProvider(BukkitPlatformImpl platform, IStorageFile storageFile) {
        this.mPlatform = platform;
        this.mStorageFile = storageFile;
    }

    protected BukkitPlatformImpl getPlatformImpl() {
        return mPlatform;
    }

    protected IStorageFile getStorageFile() {
        return mStorageFile;
    }

    @Override
    public void onStart() {
        mStorageFile.onStart();
    }

    @Override
    public void onShutdown() {
        for (ILightStorage storage : mLightStorageMap.values()) {
            storage.saveLightData();
        }
        mLightStorageMap.clear();
        mStorageFile.onShutdown();
    }

    @Override
    public ILightStorage getLightStorage(String worldName) {
        // currently, we only support available worlds
        if (!getPlatformImpl().isWorldAvailable(worldName)) {
            return null;
        }
        if (!mLightStorageMap.containsKey(worldName)) {
            prepareStorage(worldName);
        }
        return mLightStorageMap.get(worldName);
    }

    private void prepareStorage(String worldName) {
        ILightStorage storage = new BukkitLightStorage(getPlatformImpl(), worldName, getStorageFile());
        mLightStorageMap.put(worldName, storage);
    }
}
