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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.concurrent.ThreadFactory;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.internal.service.BackgroundServiceImpl;

public class BukkitBackgroundServiceImpl extends BackgroundServiceImpl {

    /**
     * CONFIG
     */
    private final String CONFIG_TITLE = getClass().getSimpleName();

    @Deprecated
    private final String CONFIG_TICK_PERIOD = CONFIG_TITLE + ".tick-period";
    private final String CONFIG_CORE_POOL_SIZE = CONFIG_TITLE + ".corePoolSize";

    private final IHandler mHandler;
    private int taskId = -1;
    private long lastAliveTime = 0;

    public BukkitBackgroundServiceImpl(BukkitPlatformImpl platform, IHandler handler) {
        super(platform);
        mHandler = handler;
    }

    @Override
    protected BukkitPlatformImpl getPlatformImpl() {
        return (BukkitPlatformImpl) super.getPlatformImpl();
    }

    private IHandler getHandler() {
        return mHandler;
    }

    private boolean upgradeConfig() {
        boolean needSave = false;
        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();
        if (fc.isSet(CONFIG_TICK_PERIOD)) {
            fc.set(CONFIG_TICK_PERIOD, null);
            needSave = true;
        }
        return needSave;
    }

    private void checkAndSetDefaults() {
        boolean needSave = upgradeConfig();
        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();

        if (!fc.isSet(CONFIG_CORE_POOL_SIZE)) {
            fc.set(CONFIG_CORE_POOL_SIZE, 1);
            needSave = true;
        }

        if (needSave) {
            getPlatformImpl().getPlugin().saveConfig();
        }
    }

    @Override
    public void onStart() {
        checkAndSetDefaults();

        // executor service
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                "lightapi-background-thread-%d").build();
        FileConfiguration fc = getPlatformImpl().getPlugin().getConfig();
        int corePoolSize = fc.getInt(CONFIG_CORE_POOL_SIZE);
        configureExecutorService(corePoolSize, namedThreadFactory);

        // heartbeat
        taskId = getPlatformImpl().getPlugin().getServer().getScheduler().runTaskTimer(getPlatformImpl().getPlugin(),
                () -> lastAliveTime = System.currentTimeMillis(), 0, 1).getTaskId();
    }

    @Override
    public void onShutdown() {
        if (taskId != -1) {
            getPlatformImpl().getPlugin().getServer().getScheduler().cancelTask(taskId);
        }
        super.onShutdown();
    }

    @Override
    public boolean canExecuteSync(long maxTime) {
        return ((System.currentTimeMillis() - lastAliveTime) < maxTime);
    }

    @Override
    public boolean isMainThread() {
        return getHandler().isMainThread();
    }
}
