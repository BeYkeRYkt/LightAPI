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
package ru.beykerykt.minecraft.lightapi.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.InternalCode;
import ru.beykerykt.minecraft.lightapi.common.internal.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.ILightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.RequestFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

/**
 * Main class for all platforms. Contains basic methods for all implementations.
 *
 * @author BeYkeRYkt
 */
public final class LightAPI {

    private static volatile LightAPI singleton;
    private final IPlatformImpl mInternal;

    private LightAPI(IPlatformImpl internal) {
        if (singleton != null) {
            throw new RuntimeException("Use get() method to get the single instance of this class.");
        }
        mInternal = internal;
    }

    /**
     * Must be called in onLoad();
     */
    public static void prepare(IPlatformImpl impl) throws Exception {
        if (singleton == null && impl != null || !get().isInitialized()) {
            impl.info("Preparing LightAPI...");
            synchronized (LightAPI.class) {
                if (singleton == null) {
                    int initCode = impl.prepare();
                    if (initCode == ResultCode.SUCCESS) {
                        singleton = new LightAPI(impl);
                        impl.info("Preparing done!");
                    } else {
                        throw new IllegalStateException("Preparing failed! Code: " + initCode);
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
                int initCode = get().getPluginImpl().initialization();
                if (initCode == ResultCode.SUCCESS) {
                    // send random generated uuid
                    UUID uuid = UUID.randomUUID();
                    get().getPluginImpl().sendCmd(InternalCode.UPDATE_UUID, uuid);
                    get().log("LightAPI initialized!");
                } else {
                    // Initialization failed
                    throw new IllegalStateException("Initialization failed! Code: " + initCode);
                }
            }
        }
    }

    /**
     * Must be called in onDisable();
     */
    public static void shutdown(IPlatformImpl impl) {
        if (get().isInitialized() && get().getPluginImpl().getUUID().equals(impl.getUUID())) {
            get().log("Shutdown LightAPI...");
            synchronized (LightAPI.class) {
                get().getPluginImpl().shutdown();
            }
        } else {
            get().log("Disabling the plugin is allowed only to the implementation class");
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

    @Deprecated
    public static boolean createLight(String worldName, ru.beykerykt.minecraft.lightapi.common.LightType type,
            int blockX, int blockY, int blockZ, int lightlevel) {
        return createLight(worldName, type, blockX, blockY, blockZ, lightlevel, null);
    }

    @Deprecated
    public static boolean createLight(String worldName, ru.beykerykt.minecraft.lightapi.common.LightType type,
            int blockX, int blockY, int blockZ, int lightlevel, LCallback callback) {
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        if (type == ru.beykerykt.minecraft.lightapi.common.LightType.SKY) {
            lightFlags = LightFlag.SKY_LIGHTING;
        }
        int resultCode = get().setLightLevel(worldName, blockX, blockY, blockZ, lightlevel, lightFlags,
                EditPolicy.IMMEDIATE, SendPolicy.DEFERRED, (requestFlag, resultCode1) -> {
                    if (callback != null) {
                        LStage stage = LStage.CREATING;
                        switch (requestFlag) {
                            case RequestFlag.EDIT:
                                stage = LStage.WRITTING;
                                break;
                            case RequestFlag.RECALCULATE:
                                stage = LStage.RECALCULATING;
                                break;
                        }
                        if (resultCode1 == ResultCode.SUCCESS || resultCode1 == ResultCode.MOVED_TO_DEFERRED) {
                            callback.onSuccess(worldName, type, blockX, blockY, blockZ, lightlevel, stage);
                        } else {
                            LReason reason = LReason.UNKNOWN;
                            switch (resultCode1) {
                                case ResultCode.RECALCULATE_NO_CHANGES:
                                    reason = LReason.NO_LIGHT_CHANGES;
                                    break;
                            }
                            callback.onFailed(worldName, type, blockX, blockY, blockZ, lightlevel, stage, reason);
                        }
                    }
                });
        return resultCode == ResultCode.SUCCESS;
    }

    @Deprecated
    public static boolean deleteLight(String worldName, ru.beykerykt.minecraft.lightapi.common.LightType type,
            int blockX, int blockY, int blockZ) {
        return deleteLight(worldName, type, blockX, blockY, blockZ, null);
    }

    @Deprecated
    public static boolean deleteLight(String worldName, ru.beykerykt.minecraft.lightapi.common.LightType type,
            int blockX, int blockY, int blockZ, LCallback callback) {
        int lightFlags = LightFlag.BLOCK_LIGHTING;
        if (type == ru.beykerykt.minecraft.lightapi.common.LightType.SKY) {
            lightFlags = LightFlag.SKY_LIGHTING;
        }
        int resultCode = get().setLightLevel(worldName, blockX, blockY, blockZ, 0, lightFlags, EditPolicy.IMMEDIATE,
                SendPolicy.DEFERRED, (requestFlag, resultCode1) -> {
                    if (callback != null) {
                        LStage stage = LStage.DELETING;
                        switch (requestFlag) {
                            case RequestFlag.EDIT:
                                stage = LStage.WRITTING;
                                break;
                            case RequestFlag.RECALCULATE:
                                stage = LStage.RECALCULATING;
                                break;
                        }
                        if (resultCode1 == ResultCode.SUCCESS || resultCode1 == ResultCode.MOVED_TO_DEFERRED) {
                            callback.onSuccess(worldName, type, blockX, blockY, blockZ, 0, stage);
                        } else {
                            LReason reason = LReason.UNKNOWN;
                            switch (resultCode1) {
                                case ResultCode.RECALCULATE_NO_CHANGES:
                                    reason = LReason.NO_LIGHT_CHANGES;
                                    break;
                            }
                            callback.onFailed(worldName, type, blockX, blockY, blockZ, 0, stage, reason);
                        }
                    }
                });
        return resultCode == ResultCode.SUCCESS;
    }

    @Deprecated
    public static List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ, int lightlevel) {
        List<IChunkData> list = new ArrayList<>();
        // Let's not do this , all right?
        return list;
    }

    @Deprecated
    public static List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ) {
        return collectChunks(worldName, blockX, blockY, blockZ, 15);
    }

    @Deprecated
    public static void sendChanges(String worldName, int chunkX, int chunkZ, String playerName) {
    }

    @Deprecated
    public static void sendChanges(String worldName, int chunkX, int blockY, int chunkZ, String playerName) {
    }

    @Deprecated
    public static void sendChanges(IChunkData chunkData, String playerName) {
    }

    @Deprecated
    public static void sendChanges(String worldName, int chunkX, int chunkZ) {
    }

    @Deprecated
    public static void sendChanges(String worldName, int chunkX, int blockY, int chunkZ) {
    }

    @Deprecated
    public static void sendChanges(IChunkData chunkData) {
    }

    public boolean isInitialized() {
        return getPluginImpl().isInitialized();
    }

    /**
     * N/A
     */
    private IPlatformImpl getPluginImpl() {
        if (get().mInternal == null) {
            throw new IllegalStateException("LightAPI not yet initialized! Use prepare() !");
        }
        return get().mInternal;
    }

    /**
     * N/A
     */
    private ILightEngine getLightEngine() {
        if (getPluginImpl().getLightEngine() == null) {
            throw new IllegalStateException("LightEngine not yet initialized!");
        }
        return getPluginImpl().getLightEngine();
    }

    /**
     * N/A
     */
    private IChunkObserver getChunkObserver() {
        if (getPluginImpl().getChunkObserver() == null) {
            throw new IllegalStateException("ChunkObserver not yet initialized!");
        }
        return getPluginImpl().getChunkObserver();
    }

    /**
     * N/A
     */
    private IBackgroundService getBackgroundService() {
        if (getPluginImpl().getBackgroundService() == null) {
            throw new IllegalStateException("BackgroundService not yet initialized!");
        }
        return getPluginImpl().getBackgroundService();
    }

    /**
     * N/A
     */
    public IExtension getExtension() {
        return getPluginImpl().getExtension();
    }

    /**
     * N/A
     */
    protected void log(String msg) {
        getPluginImpl().info(msg);
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
        return getPluginImpl().getPlatformType();
    }

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ) {
        return getLightLevel(worldName, blockX, blockY, blockZ, LightFlag.BLOCK_LIGHTING);
    }

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    public int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        return getLightEngine().getLightLevel(worldName, blockX, blockY, blockZ, lightFlags);
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel) {
        return setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, LightFlag.BLOCK_LIGHTING,
                EditPolicy.DEFERRED, SendPolicy.DEFERRED, null);
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags) {
        return setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, lightFlags, EditPolicy.DEFERRED,
                SendPolicy.DEFERRED, null);
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            ICallback callback) {
        return setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, lightFlags, EditPolicy.DEFERRED,
                SendPolicy.DEFERRED, callback);
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    public int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback) {
        return getLightEngine().setLightLevel(worldName, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy,
                sendPolicy, callback);
    }

    /**
     * Checks the light level and restores it if available.
     */
    public int checkLight(String worldName, int blockX, int blockY, int blockZ) {
        return checkLight(worldName, blockX, blockY, blockZ, LightFlag.BLOCK_LIGHTING);
    }

    /**
     * Checks the light level and restores it if available.
     */
    public int checkLight(String worldName, int blockX, int blockY, int blockZ, int lightFlags) {
        return getLightEngine().checkLight(worldName, blockX, blockY, blockZ, lightFlags);
    }

    /**
     * Send specific commands for implementation
     */
    public int sendCmd(int cmdId, Object... args) {
        if (cmdId <= InternalCode.RESERVED_LENGTH) {
            getPluginImpl().error(cmdId + " is reserved for internal use.");
            return ResultCode.FAILED;
        }
        return getPluginImpl().sendCmd(cmdId, args);
    }
}
