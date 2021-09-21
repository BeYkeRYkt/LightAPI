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
package ru.beykerykt.minecraft.lightapi.common.internal;

import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.ILightEngine;
import ru.beykerykt.minecraft.lightapi.common.internal.service.IBackgroundService;

import java.util.UUID;

public interface IPlatformImpl {

    /**
     * N/A
     */
    int prepare();

    /**
     * N/A
     */
    int initialization();

    /**
     * N/A
     */
    void shutdown();

    /**
     * N/A
     */
    boolean isInitialized();

    /**
     * Log message in console
     *
     * @param msg - message
     */
    void log(String msg);

    /**
     * Debug message in console
     *
     * @param msg - message
     */
    void debug(String msg);

    /**
     * Error message in console
     *
     * @param msg - message
     */
    void error(String msg);

    /**
     * N/A
     */
    UUID getUUID();

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from {@link PlatformType}
     */
    PlatformType getPlatformType();

    /**
     * N/A
     */
    IComponentFactory getFactory();

    /**
     * N/A
     */
    ILightEngine getLightEngine();

    /**
     * N/A
     */
    IChunkObserver getChunkObserver();

    /**
     * N/A
     */
    IBackgroundService getBackgroundService();

    /**
     * N/A
     */
    IExtension getExtension();

    /**
     * N/A
     */
    boolean isWorldAvailable(String worldName);

    /**
     * Can be used for specific commands
     */
    int sendCmd(int cmdId, Object... args);
}
