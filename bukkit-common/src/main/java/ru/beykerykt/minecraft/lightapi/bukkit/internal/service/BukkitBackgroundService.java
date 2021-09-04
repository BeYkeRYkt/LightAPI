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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;
import ru.beykerykt.minecraft.lightapi.common.internal.service.ITickable;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class BukkitBackgroundService implements IBackgroundService, Runnable {
    private long maxAliveTimePerTick = 50;
    private long maxTimePerTick = 50;
    private int taskId = -1;
    private long lastAliveTime = 0;
    private IBukkitPlatformImpl mInternal;
    private IHandler mHandler;
    private Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private List<ITickable> REPEAT_QUEUE = new CopyOnWriteArrayList<>();
    private boolean isServerThrottled;
    private int corePoolSize = 1;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> sch;

    public BukkitBackgroundService(IBukkitPlatformImpl internal, IHandler handler) {
        this.mInternal = internal;
        this.mHandler = handler;
    }

    private IBukkitPlatformImpl getInternal() {
        return mInternal;
    }

    private IHandler getHandler() {
        return mHandler;
    }

    @Override
    public void onStart() {
        // executor service
        ThreadFactory namedThreadFactory =
                new ThreadFactoryBuilder().setNameFormat("lightapi-background-thread-%d").build();
        this.corePoolSize =
                getInternal().getPlugin().getConfig().getInt(ConfigurationPath.BACKGROUND_SERVICE_CORE_POOL_SIZE);
        this.executorService = Executors.newScheduledThreadPool(this.corePoolSize, namedThreadFactory);

        int period = getInternal().getPlugin().getConfig().getInt(ConfigurationPath.BACKGROUND_SERVICE_TICK_DELAY);
        sch = scheduleWithFixedDelay(this, 0, 50 * period, TimeUnit.MILLISECONDS);

        maxAliveTimePerTick = 50 * period;
        maxTimePerTick = 50 * period;

        taskId =
                getInternal().getPlugin().getServer().getScheduler().runTaskTimer(getInternal().getPlugin(), () -> {
                    lastAliveTime = System.currentTimeMillis();
                    isServerThrottled = false;
                }, 0, 1).getTaskId();
    }

    @Override
    public void onShutdown() {
        if (taskId != -1) {
            getInternal().getPlugin().getServer().getScheduler().cancelTask(taskId);
        }
        if (sch != null) {
            this.sch.cancel(false);
        }
        this.executorService.shutdown();
        this.QUEUE.clear();
        this.REPEAT_QUEUE.clear();
    }

    @Override
    public boolean canExecuteSync() {
        return !isServerThrottled;
    }

    @Override
    public boolean isMainThread() {
        return getHandler().isMainThread();
    }

    @Override
    public void executeSync(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void executeAsync(Runnable runnable) {
        // TODO: ???
        getInternal().getPlugin().getServer().getScheduler().runTaskAsynchronously(getInternal().getPlugin(), runnable);
    }

    @Override
    public void addToQueue(Runnable runnable) {
        if (runnable != null) {
            QUEUE.add(runnable);
        }
    }

    @Override
    public void addToRepeat(ITickable tickable) {
        if (tickable != null) {
            REPEAT_QUEUE.add(tickable);
        }
    }

    @Override
    public void removeRepeat(ITickable tickable) {
        if (tickable != null) {
            REPEAT_QUEUE.remove(tickable);
        }
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    private ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, int initialDelay, int delay, TimeUnit unit) {
        return getExecutorService().scheduleWithFixedDelay(runnable, initialDelay, delay, unit);
    }

    private void handleQueueLocked() {
        long startTime = System.currentTimeMillis();
        try {
            Runnable request;
            while ((request = QUEUE.poll()) != null) {
                long time = System.currentTimeMillis() - startTime;
                if (time > maxTimePerTick) {
                    getInternal().debug("handleQueueLocked: maxTimePerTick (" + time + " ms)");
                    isServerThrottled = true;
                    break;
                }
                request.run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleRepeatQueueLocked() {
        try {
            Iterator<ITickable> it = REPEAT_QUEUE.iterator();
            while (it.hasNext()) {
                ITickable request = it.next();
                request.onTick();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() - lastAliveTime > maxAliveTimePerTick) {
            isServerThrottled = true;
        }

        synchronized (QUEUE) {
            handleQueueLocked();
        }

        synchronized (REPEAT_QUEUE) {
            handleRepeatQueueLocked();
        }
    }
}