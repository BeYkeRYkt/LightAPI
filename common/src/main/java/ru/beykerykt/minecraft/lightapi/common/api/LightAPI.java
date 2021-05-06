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

import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;
import ru.beykerykt.minecraft.lightapi.common.api.service.ICallback;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.EditStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.RelightStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.SendStrategy;
import ru.beykerykt.minecraft.lightapi.common.internal.ILightAPI;

/**
 * Main class for all platforms. Contains basic methods for all implementations.
 *
 * @author BeYkeRYkt
 */
public final class LightAPI {
    private static volatile LightAPI singleton;
    private final ILightAPI mInternal;

    private LightAPI(ILightAPI internal) {
        if (singleton != null) {
            throw new RuntimeException("Use get() method to get the single instance of this class.");
        }
        mInternal = internal;
    }

    /**
     * Must be called in onLoad();
     */
    public static void prepare(ILightAPI impl) {
        if (singleton == null && impl != null || !get().isInitialized()) {
            impl.log("Preparing LightAPI...");
            synchronized (LightAPI.class) {
                if (singleton == null) {
                    singleton = new LightAPI(impl);
                    int initCode = get().getInternal().prepare();
                    if (initCode == ResultCode.SUCCESS) {
                        impl.log("Preparing done!");
                    } else {
                        //throw new IllegalStateException("Preparing failed! Code: " + initCode);
                    }
                }
            }
        }
    }

    /**
     * Must be called in onEnable();
     */
    public static void initialization() throws Exception {
        if (!get().isInitialized()) {
            get().log("Initializing LightAPI...");
            synchronized (LightAPI.class) {
                int initCode = get().getInternal().initialization();
                if (initCode == ResultCode.SUCCESS) {
                    get().log("LightAPI initialized!");
                } else {
                    // Initialization failed
                    throw new IllegalStateException("Initialization failed! Code: " + initCode);
                }
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
        return getInternal().isInitialized();
    }

    /**
     * N/A
     */
    private ILightAPI getInternal() {
        if (get().mInternal == null) {
            throw new IllegalStateException("ILightAPI not yet initialized! Use prepare() !");
        }
        return get().mInternal;
    }

    /**
     * N/A
     */
    public IExtension getExtension() {
        if (getInternal().getExtension() == null) {
            throw new IllegalStateException("Extension not yet initialized!");
        }
        return getInternal().getExtension();
    }

    /**
     * N/A
     */
    protected void log(String msg) {
        getInternal().log(msg);
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
        return getInternal().getPlatformType();
    }

    /**
     * N/A
     */
    public RelightStrategy getRelightStrategy() {
        return getInternal().getRelightStrategy();
    }

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int flags) {
        return getInternal().getLightLevel(worldName, blockX, blockY, blockZ, flags);
    }

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
                             EditStrategy editStrategy, SendStrategy sendStrategy, ICallback callback) {
        return getInternal().setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, lightType,
                editStrategy, sendStrategy, callback);
    }

    /**
     * Send specific commands for implementation
     */
    public int sendCmd(int cmdId, Object... args) {
        return getInternal().sendCmd(cmdId, args);
    }
}
