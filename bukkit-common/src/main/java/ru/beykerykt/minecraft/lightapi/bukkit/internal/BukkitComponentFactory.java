/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.internal;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks.sched.observer.impl.BukkitScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.engine.sched.impl.BukkitScheduledLightEngine;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.HandlerFactory;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.service.BukkitBackgroundService;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;
import ru.beykerykt.minecraft.lightapi.common.internal.IComponentFactory;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.sched.IScheduledChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.ILightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.IScheduledLightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.impl.PriorityScheduler;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

public class BukkitComponentFactory implements IComponentFactory {

    private IBukkitPlatformImpl mImpl;
    private IHandler mHandler;

    public BukkitComponentFactory(IBukkitPlatformImpl impl) {
        this.mImpl = impl;
    }

    protected IBukkitPlatformImpl getImpl() {
        return mImpl;
    }

    protected IHandler getHandler() {
        return mHandler;
    }

    public void init() throws Exception {
        // load adapter
        HandlerFactory factory = new HandlerFactory(getImpl().getPlugin(), getImpl());
        this.mHandler = factory.createHandler();

        // start handler initialization
        this.mHandler.onInitialization(getImpl());
    }

    public void shutdown() {
        this.mHandler.onShutdown(getImpl());
        this.mHandler = null;
    }

    @Override
    public ILightEngine createLightEngine() {
        IScheduledLightEngine engine = new BukkitScheduledLightEngine(getImpl(), getImpl().getBackgroundService(), RelightStrategy.DEFERRED, getHandler(), 250, 250);
        engine.setScheduler(new PriorityScheduler(engine, (IScheduledChunkObserver) getImpl().getChunkObserver(), getImpl().getBackgroundService()));
        return engine;
    }

    @Override
    public IChunkObserver createChunkObserver() {
        IChunkObserver observer = new BukkitScheduledChunkObserver(getImpl(), getImpl().getBackgroundService(), getHandler());
        return observer;
    }

    @Override
    public IBackgroundService createBackgroundService() {
        IBackgroundService service = new BukkitBackgroundService(getImpl(), getHandler());
        return service;
    }

    @Override
    public IExtension createExtension() {
        return (IExtension) mImpl;
    }
}
