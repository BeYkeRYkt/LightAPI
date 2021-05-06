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
import ru.beykerykt.minecraft.lightapi.bukkit.internal.IBukkitLightAPI;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class BackgroundService implements Runnable {
    private long maxAliveTimePerTick = 50;
    private long maxTimePerTick = 50;
    private int taskId = -1;
    private long lastAliveTime = 0;
    private IBukkitLightAPI mInternal;
    private Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private List<Runnable> REPEAT_QUEUE = new CopyOnWriteArrayList<>();
    private boolean isServerThrottled;
    private int corePoolSize = 1;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> sch;

    public BackgroundService(IBukkitLightAPI internal) {
        this.mInternal = internal;
    }

    private IBukkitLightAPI getInternal() {
        return mInternal;
    }

    public boolean canExecuteSync() {
        return !isServerThrottled;
    }

    public void addToQueue(Runnable runnable) {
        if (runnable != null) {
            QUEUE.add(runnable);
        }
    }

    public void addToRepeat(Runnable runnable) {
        if (runnable != null) {
            REPEAT_QUEUE.add(runnable);
        }
    }

    public void removeRepeat(Runnable runnable) {
        if (runnable != null) {
            REPEAT_QUEUE.remove(runnable);
        }
    }

    public void executeSync(Runnable runnable) {
        runnable.run();
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    private ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, int initialDelay, int delay, TimeUnit unit) {
        return getExecutorService().scheduleWithFixedDelay(runnable, initialDelay, delay, unit);
    }

    public void start() {
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

    public void shutdown() {
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
            Iterator<Runnable> it = REPEAT_QUEUE.iterator();
            while (it.hasNext()) {
                Runnable request = it.next();
                request.run();
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