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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.bukkit.api.extension.IBukkitExtension;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.sched.observer.impl.BukkitScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.engine.sched.impl.BukkitScheduledLightEngine;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.BukkitBackgroundService;
import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;
import ru.beykerykt.minecraft.lightapi.common.internal.InternalCode;
import ru.beykerykt.minecraft.lightapi.common.internal.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.ILightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

public class BukkitPlatformImpl implements IBukkitPlatformImpl, IBukkitExtension {

    private static final String DEFAULT_IMPL_NAME = "craftbukkit";
    private boolean DEBUG = false;
    private BukkitPlugin mPlugin;
    private boolean isInit = false;
    private boolean forceLegacy = false;
    private IHandler mHandler;
    private IChunkObserver mChunkObserver;
    private ILightEngine mLightEngine;
    private IBackgroundService mBackgroundService;
    private IExtension mExtension;
    private UUID mUUID;

    public BukkitPlatformImpl(BukkitPlugin plugin) {
        this.mPlugin = plugin;
    }

    private FileConfiguration getConfig() {
        return getPlugin().getConfig();
    }

    private void checkAndSetDefaults() {
        boolean needSave = false;
        if (getConfig().getString("handler.specific-handler-path") == null) {
            getConfig().set("handler.specific-handler-path", "none");
            needSave = true;
        }
        if (getConfig().getString("handler." + DEFAULT_IMPL_NAME + ".factory-path") == null) {
            getConfig().set("handler." + DEFAULT_IMPL_NAME + ".factory-path",
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
        String specificPkg = getConfig().getString("handler.specific-handler-path");
        if (specificPkg != null && ! specificPkg.equalsIgnoreCase("none")) {
            info("Initial load specific handler");
            mHandler = (IHandler) Class.forName(specificPkg).getConstructor().newInstance();
            info("Custom handler is loaded: " + mHandler.getClass().getName());
            return;
        }

        // First, check Bukkit server implementation, since Bukkit is only an API, and there
        // may be several implementations (for example: Spigot, Paper, Glowstone and etc)
        String implName = Bukkit.getName().toLowerCase();
        debug("Server implementation name: " + implName);

        String modFactoryPath = getConfig().getString("handler." + implName + ".factory-path");
        try {
            Class.forName(modFactoryPath);
        } catch (Exception ex) {
            debug("Specific HandlerFactory for " + implName + " is not detected. Switch to default: "
                    + DEFAULT_IMPL_NAME);
            implName = DEFAULT_IMPL_NAME;
            modFactoryPath = getConfig().getString("handler." + implName + ".factory-path");
        }
        IHandlerFactory factory = (IHandlerFactory) Class.forName(modFactoryPath).getConstructor().newInstance();
        mHandler = factory.createHandler(this);
        debug("Handler is loaded: " + mHandler.getClass().getName());
    }

    @Override
    public int prepare() {
        // debug mode
        this.DEBUG = getConfig().getBoolean(ConfigurationPath.GENERAL_DEBUG);
        return ResultCode.SUCCESS;
    }

    @Override
    public int initialization() {
        // enable force legacy
        forceLegacy = getConfig().getBoolean(ConfigurationPath.GENERAL_FORCE_ENABLE_LEGACY);
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
        mBackgroundService = new BukkitBackgroundService(this, getHandler());
        mBackgroundService.onStart();

        // init chunk observer
        mChunkObserver = new BukkitScheduledChunkObserver(this, getBackgroundService(), getHandler());
        mChunkObserver.onStart();

        // init light engine
        mLightEngine = new BukkitScheduledLightEngine(this, getBackgroundService(), getHandler());
        mLightEngine.onStart();

        // init extension
        mExtension = this;

        isInit = true;
        return ResultCode.SUCCESS;
    }

    @Override
    public void shutdown() {
        mLightEngine.onShutdown();
        mChunkObserver.onShutdown();
        mBackgroundService.onShutdown();
        mHandler.onShutdown(this);
        mHandler = null;
        isInit = false;
    }

    @Override
    public boolean isInitialized() {
        return isInit;
    }

    @Override
    public void log(String msg) {
        mPlugin.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + msg);
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

    @Override
    public BukkitPlugin getPlugin() {
        return mPlugin;
    }

    @Override
    public IHandler getHandler() {
        return mHandler;
    }

    @Override
    public boolean isMainThread() {
        return getHandler().isMainThread();
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
}
