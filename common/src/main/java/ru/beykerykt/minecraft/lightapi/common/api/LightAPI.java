/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.common.api;

import ru.beykerykt.minecraft.lightapi.common.api.impl.IExtension;
import ru.beykerykt.minecraft.lightapi.common.api.impl.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.impl.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;

import java.util.List;

/**
 * Main class for all platforms. Contains basic methods for all implementations.
 *
 * @author BeYkeRYkt
 */
public final class LightAPI {

    private static final int DEFAULT_FLAG = LightFlags.COMBO_LIGHTING;
    private static volatile LightAPI singleton;
    private boolean isInit = false;
    private IPlatformImpl mPlatformImpl;

    private LightAPI(IPlatformImpl platformImpl) {
        if (singleton != null) {
            throw new RuntimeException("Use get() method to get the single instance of this class.");
        }
        mPlatformImpl = platformImpl;
    }

    /**
     * Must be called in onLoad();
     *
     * @param impl
     */
    public static void prepare(IPlatformImpl impl) {
        if (singleton == null && impl != null || !get().isInit) {
            impl.log("Preparing LightAPI...");
            synchronized (LightAPI.class) {
                if (singleton == null) {
                    singleton = new LightAPI(impl);
                    impl.log("Preparing done!");
                }
            }
        }
    }

    /**
     * Must be called in onEnable();
     */
    public static void initialization() throws Exception {
        if (!get().isInit) {
            get().log("Initializing LightAPI...");
            synchronized (LightAPI.class) {
                get().getPlatformImpl().initialization();
                get().isInit = true;
            }
        }
    }

    /**
     * The global {@link LightAPI} instance.
     */
    public static LightAPI get() {
        if (singleton == null) {
            throw new IllegalStateException("Singleton not yet initialized! Use prepare() !");
        }
        return singleton;
    }

    public boolean isInitialized() {
        return get().isInit;
    }

    /**
     * N/A
     */
    private IPlatformImpl getPlatformImpl() {
        if (get().mPlatformImpl == null) {
            throw new IllegalStateException("IPlatformImpl not yet initialized! Use prepare() !");
        }
        return get().mPlatformImpl;
    }

    /**
     * N/A
     */
    public IHandler getImplHandler() {
        if (getPlatformImpl().getHandler() == null) {
            throw new IllegalStateException("IPlatformHandler not yet initialized! Use prepare() !");
        }
        return getPlatformImpl().getHandler();
    }

    /**
     * N/A
     */
    public IExtension getImplExtension() {
        return getPlatformImpl().getExtension();
    }

    /**
     * N/A
     */
    protected void log(String msg) {
        getPlatformImpl().log(msg);
    }

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from {@link PlatformType}
     */
    public PlatformType getPlatformType() {
        if (singleton == null) {
            return PlatformType.UNKNOWN;
        }
        return getPlatformImpl().getPlatformType();
    }

    /**
     * Gets the level of light from given coordinates with
     * {@link ru.beykerykt.minecraft.lightapi.common.api.LightFlags#COMBO_LIGHTING}.
     */
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ) {
        return getLightLevel(worldName, blockX, blockY, blockZ, DEFAULT_FLAG);
    }

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int flags) {
        return getImplHandler().getRawLightLevel(worldName, blockX, blockY, blockZ, flags);
    }

    /**
     * Placement of a {@link ru.beykerykt.minecraft.lightapi.common.api.LightFlags#COMBO_LIGHTING} type of light with a
     * given level of illumination in the named world in certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, SendMode mode,
                             List<ChunkData> outputChunks) {
        return setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, DEFAULT_FLAG, mode, outputChunks);
    }

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int flags,
                             SendMode mode, List<ChunkData> outputChunks) {
        return getImplHandler().setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, flags, mode, outputChunks);
    }
}
