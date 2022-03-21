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
package ru.beykerykt.minecraft.lightapi.bukkit.internal;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.UUID;

import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.api.extension.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.observer.sched.BukkitScheduledChunkObserverImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.engine.sched.BukkitScheduledLightEngineImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.CompatibilityHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.BukkitBackgroundServiceImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.utils.VersionUtil;
import ru.beykerykt.minecraft.lightapi.common.Build;
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
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

public class BukkitPlatformImpl implements IPlatformImpl, IBukkitExtension {

    private static final String DEFAULT_IMPL_NAME = "craftbukkit";
    /**
     * CONFIG
     */
    private final String CONFIG_TITLE = "general";
    private final String CONFIG_DEBUG = CONFIG_TITLE + ".debug";
    private final String CONFIG_ENABLE_METRICS = CONFIG_TITLE + ".enable-metrics";
    private final String CONFIG_ENABLE_COMPATIBILITY_MODE = CONFIG_TITLE + ".enable-compatibility-mode";
    private final String CONFIG_FORCE_ENABLE_LEGACY = CONFIG_TITLE + ".force-enable-legacy";
    private final String CONFIG_SPECIFIC_HANDLER_PATH = CONFIG_TITLE + ".specific-handler-path";
    private final String CONFIG_HANDLERS_TITLE = CONFIG_TITLE + ".handlers";
    private final int BSTATS_ID = 13051;
    private final BukkitPlugin mPlugin;
    private boolean DEBUG = false;
    private boolean isInit = false;
    private boolean forceLegacy = false;
    private boolean compatibilityMode = false;
    private IHandler mHandler;
    private IChunkObserver mChunkObserver;
    private ILightEngine mLightEngine;
    private IBackgroundService mBackgroundService;
    private IExtension mExtension;
    private UUID mUUID;

    public BukkitPlatformImpl(BukkitPlugin plugin) {
        this.mPlugin = plugin;
    }

    public void toggleDebug() {
        DEBUG = !DEBUG;
        log("Debug mode is " + (DEBUG ? "en" : "dis") + "abled");
    }

    private FileConfiguration getConfig() {
        return getPlugin().getConfig();
    }

    private void generateConfig() {
        // create config
        try {
            File file = new File(getPlugin().getDataFolder(), "config.yml");
            if (!file.exists()) {
                getConfig().set(CONFIG_DEBUG, false);
                getConfig().set(CONFIG_ENABLE_METRICS, true);
                getConfig().set(CONFIG_ENABLE_COMPATIBILITY_MODE, false);
                if (Build.API_VERSION == Build.PREVIEW) { // only for PREVIEW build
                    getConfig().set(CONFIG_FORCE_ENABLE_LEGACY, true);
                } else {
                    getConfig().set(CONFIG_FORCE_ENABLE_LEGACY, false);
                }
                getPlugin().saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean upgradeConfig() {
        boolean needSave = false;
        if (getConfig().isSet("general.specific-storage-provider")) {
            getConfig().set("general.specific-storage-provider", null);
            needSave = true;
        }
        if (getConfig().isSet("handler.specific-handler-path")) {
            getConfig().set("handler.specific-handler-path", null);
            needSave = true;
        }
        if (getConfig().isSet("handler.craftbukkit.factory-path")) {
            getConfig().set("handler.craftbukkit.factory-path", null);
            needSave = true;
        }
        if (needSave) {
            getConfig().set("handler", null);
        }
        return needSave;
    }

    private void checkAndSetDefaults() {
        boolean needSave = upgradeConfig();
        if (!getConfig().isSet(CONFIG_ENABLE_COMPATIBILITY_MODE)) {
            getConfig().set(CONFIG_ENABLE_COMPATIBILITY_MODE, false);
            needSave = true;
        }
        if (!getConfig().isSet(CONFIG_SPECIFIC_HANDLER_PATH)) {
            getConfig().set(CONFIG_SPECIFIC_HANDLER_PATH, "none");
            needSave = true;
        }
        if (!getConfig().isSet(CONFIG_HANDLERS_TITLE + "." + DEFAULT_IMPL_NAME + ".factory-path")) {
            getConfig().set(CONFIG_HANDLERS_TITLE + "." + DEFAULT_IMPL_NAME + ".factory-path",
                    "ru.beykerykt.minecraft.lightapi.bukkit.internal.handler." + DEFAULT_IMPL_NAME + ".HandlerFactory");
            needSave = true;
        }
        if (needSave) {
            getPlugin().saveConfig();
        }
    }

    private void initHandler() throws Exception {
        checkAndSetDefaults();
        // load specific handler if available
        String specificPkg = getConfig().getString(CONFIG_SPECIFIC_HANDLER_PATH);
        if (specificPkg != null && !specificPkg.equalsIgnoreCase("none")) {
            info("Initial load specific handler");
            mHandler = (IHandler) Class.forName(specificPkg).getConstructor().newInstance();
            info("Custom handler is loaded: " + mHandler.getClass().getName());
            return;
        }

        // compatibility mode (1.17+)
        compatibilityMode = getConfig().getBoolean(CONFIG_ENABLE_COMPATIBILITY_MODE);
        if (compatibilityMode) {
            if (VersionUtil.compareBukkitVersionTo("1.17") >= 0) {
                info("Compatibility mode is enabled");
                mHandler = new CompatibilityHandler();
                mHandler.onInitialization(this);
                return;
            } else {
                error("Compatibility mode can only work on versions > 1.17");
            }
        }

        // First, check Bukkit server implementation, since Bukkit is only an API, and there
        // may be several implementations (for example: Spigot, Paper, Glowstone and etc)
        String implName = Bukkit.getName().toLowerCase();
        debug("Server implementation name: " + implName);

        String modFactoryPath = getConfig().getString(CONFIG_HANDLERS_TITLE + "." + implName + ".factory-path");
        try {
            Class.forName(modFactoryPath);
        } catch (Exception ex) {
            debug("Specific HandlerFactory for " + implName + " is not detected. Switch to default: "
                    + DEFAULT_IMPL_NAME);
            implName = DEFAULT_IMPL_NAME;
            modFactoryPath = getConfig().getString(CONFIG_HANDLERS_TITLE + "." + implName + ".factory-path");
        }
        IHandlerFactory factory = (IHandlerFactory) Class.forName(modFactoryPath).getConstructor().newInstance();
        mHandler = factory.createHandler(this);
        debug("Handler is loaded: " + mHandler.getClass().getName());
    }

    private void enableMetrics() {
        boolean enableMetrics = getConfig().getBoolean(CONFIG_ENABLE_METRICS);
        if (enableMetrics) {
            Metrics metrics = new Metrics(getPlugin(), BSTATS_ID);
        }
        info("Metrics is " + (enableMetrics ? "en" : "dis") + "abled!");
    }

    @Override
    public int prepare() {
        // general default config
        generateConfig();

        // debug mode
        this.DEBUG = getConfig().getBoolean(CONFIG_DEBUG);
        return ResultCode.SUCCESS;
    }

    @Override
    public int initialization() {
        // enable force legacy
        forceLegacy = getConfig().getBoolean(CONFIG_FORCE_ENABLE_LEGACY);
        if (forceLegacy) {
            info("Force legacy is enabled");
        }

        // init handler
        try {
            initHandler();
            mHandler.onInitialization(this);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultCode.FAILED;
        }

        // init background service
        mBackgroundService = new BukkitBackgroundServiceImpl(this, getHandler());
        mBackgroundService.onStart();

        // init chunk observer
        mChunkObserver = new BukkitScheduledChunkObserverImpl(this, getBackgroundService(), getHandler());
        mChunkObserver.onStart();

        // init light engine
        mLightEngine = new BukkitScheduledLightEngineImpl(this, getBackgroundService(), getHandler());
        mLightEngine.onStart();

        // init extension
        mExtension = this;

        isInit = true;

        // enable metrics
        enableMetrics();

        return ResultCode.SUCCESS;
    }

    @Override
    public void shutdown() {
        if (mLightEngine != null) {
            mLightEngine.onShutdown();
        }
        if (mChunkObserver != null) {
            mChunkObserver.onShutdown();
        }
        if (mBackgroundService != null) {
            mBackgroundService.onShutdown();
        }
        if (mHandler != null) {
            mHandler.onShutdown(this);
        }
        mHandler = null;
        isInit = false;
    }

    @Override
    public boolean isInitialized() {
        return isInit;
    }

    @Override
    public void log(String msg) {
        StringBuilder builder = new StringBuilder(ChatColor.AQUA + "<LightAPI>: ");
        builder.append(ChatColor.WHITE).append(msg);
        mPlugin.getServer().getConsoleSender().sendMessage(builder.toString());
    }

    @Override
    public void info(String msg) {
        log("[INFO] " + msg);
    }

    @Override
    public void debug(String msg) {
        if (DEBUG) {
            log(ChatColor.YELLOW + "[DEBUG] " + msg);
        }
    }

    @Override
    public void error(String msg) {
        log(ChatColor.RED + "[ERROR] " + msg);
    }

    @Override
    public PlatformType getPlatformType() {
        return getHandler().getPlatformType();
    }

    @Override
    public ILightEngine getLightEngine() {
        return mLightEngine;
    }

    @Override
    public IChunkObserver getChunkObserver() {
        return mChunkObserver;
    }

    @Override
    public IBackgroundService getBackgroundService() {
        return mBackgroundService;
    }

    @Override
    public IExtension getExtension() {
        return mExtension;
    }

    @Override
    public boolean isWorldAvailable(String worldName) {
        return getPlugin().getServer().getWorld(worldName) != null;
    }

    /* @hide */
    public int sendCmdLocked(int cmdId, Object... args) {
        int resultCode = ResultCode.SUCCESS;
        // handle internal codes
        switch (cmdId) {
            case InternalCode.UPDATE_UUID:
                mUUID = (UUID) args[0];
                break;
            default:
                resultCode = getHandler().sendCmd(cmdId, args);
                break;
        }
        return resultCode;
    }

    @Override
    public int sendCmd(int cmdId, Object... args) {
        return sendCmdLocked(cmdId, args);
    }

    @Override
    public UUID getUUID() {
        return mUUID;
    }

    public BukkitPlugin getPlugin() {
        return mPlugin;
    }

    @Override
    public IHandler getHandler() {
        return mHandler;
    }

    @Override
    public int getLightLevel(World world, int blockX, int blockY, int blockZ) {
        return getLightLevel(world, blockX, blockY, blockZ, LightFlag.BLOCK_LIGHTING);
    }

    @Override
    public int getLightLevel(World world, int blockX, int blockY, int blockZ, int lightFlags) {
        return getLightEngine().getLightLevel(world.getName(), blockX, blockY, blockZ, lightFlags);
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel) {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, LightFlag.BLOCK_LIGHTING, EditPolicy.DEFERRED,
                SendPolicy.DEFERRED, null);
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags) {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightFlags, EditPolicy.DEFERRED,
                SendPolicy.DEFERRED, null);
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            ICallback callback) {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightFlags, EditPolicy.DEFERRED,
                SendPolicy.DEFERRED, callback);
    }

    @Override
    public int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback) {
        return getLightEngine().setLightLevel(world.getName(), blockX, blockY, blockZ, lightLevel, lightFlags,
                editPolicy, sendPolicy, callback);
    }

    @Override
    public boolean isBackwardAvailable() {
        boolean flag = Build.API_VERSION == Build.PREVIEW;
        try {
            Class.forName("ru.beykerykt.lightapi.LightAPI");
            return forceLegacy ? true : (flag & true);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public boolean isCompatibilityMode() {
        return compatibilityMode;
    }
}
