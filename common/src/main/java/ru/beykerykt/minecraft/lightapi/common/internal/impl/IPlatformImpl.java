/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.common.internal.impl;

import ru.beykerykt.minecraft.lightapi.common.api.impl.IExtension;
import ru.beykerykt.minecraft.lightapi.common.api.impl.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.IChunkObserver;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.handler.IHandlerInternal;
import ru.beykerykt.minecraft.lightapi.common.internal.service.BackgroundService;

public interface IPlatformImpl {

    /**
     * N/A
     */
    void initialization() throws Exception;

    /**
     * N/A
     */
    void shutdown();

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from {@link PlatformType}
     */
    PlatformType getPlatformType();

    /**
     * N/A
     */
    IHandlerInternal getHandler();

    /**
     * N/A
     */
    IExtension getExtension();

    /**
     * N/A
     */
    IChunkObserver getChunkObserver();

    /**
     * N/A
     */
    BackgroundService getBackgroundService();

    /**
     * Log message in console
     *
     * @param msg - message
     */
    void log(String msg);
}
