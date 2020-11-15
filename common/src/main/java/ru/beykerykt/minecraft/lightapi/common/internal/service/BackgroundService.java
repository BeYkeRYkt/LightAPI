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
package ru.beykerykt.minecraft.lightapi.common.internal.service;

import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class BackgroundService implements Runnable {
    private IPlatformImpl mImpl;
    private List<Runnable> REPEAT_QUEUE = new CopyOnWriteArrayList<>();
    private Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();

    public BackgroundService(IPlatformImpl impl) {
        this.mImpl = impl;
    }

    private IPlatformImpl getPlatformImpl() {
        return mImpl;
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

    public void shutdown() {
        QUEUE.clear();
        REPEAT_QUEUE.clear();
    }

    @Override
    public void run() {
        synchronized (QUEUE) {
            try {
                Runnable request;
                while ((request = QUEUE.poll()) != null) {
                    request.run();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        synchronized (REPEAT_QUEUE) {
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
    }
}