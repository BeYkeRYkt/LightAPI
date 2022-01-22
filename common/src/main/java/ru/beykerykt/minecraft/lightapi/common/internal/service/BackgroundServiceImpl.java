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

package ru.beykerykt.minecraft.lightapi.common.internal.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;

public abstract class BackgroundServiceImpl implements IBackgroundService {
    private ScheduledExecutorService executorService;
    private IPlatformImpl mPlatform;

    public BackgroundServiceImpl(IPlatformImpl platform) {
        this.mPlatform = platform;
    }

    protected IPlatformImpl getPlatformImpl() {
        return this.mPlatform;
    }

    protected void configureExecutorService(int corePoolSize, ThreadFactory namedThreadFactory) {
        if (this.executorService == null) {
            this.executorService = Executors.newScheduledThreadPool(corePoolSize, namedThreadFactory);
        }
    }

    protected ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    @Override
    public void onShutdown() {
        if (getExecutorService() != null) {
            getExecutorService().shutdown();
            try {
                if (!getExecutorService().awaitTermination(10, TimeUnit.SECONDS)) {
                    getPlatformImpl().info("Still waiting after 10 seconds: Shutdown now.");
                    getExecutorService().shutdownNow();
                }
            } catch (InterruptedException e) {
                // (Re-)Cancel if current thread also interrupted
                getExecutorService().shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, int initialDelay, int delay, TimeUnit unit) {
        return getExecutorService().scheduleWithFixedDelay(runnable, initialDelay, delay, unit);
    }
}
