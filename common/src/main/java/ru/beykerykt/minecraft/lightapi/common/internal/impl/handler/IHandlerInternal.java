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
package ru.beykerykt.minecraft.lightapi.common.internal.impl.handler;

import ru.beykerykt.minecraft.lightapi.common.api.impl.IHandler;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.LightEngineVersion;

public interface IHandlerInternal extends IHandler {
    /**
     * N/A
     */
    void initialization(IPlatformImpl impl) throws Exception;

    /**
     * N/A
     */
    void shutdown(IPlatformImpl impl);

    /**
     * Used lighting engine version.
     *
     * @return One of the proposed options from {@link LightEngineVersion}
     */
    LightEngineVersion getLightEngineVersion();

    /**
     * N/A
     */
    boolean isMainThread();

    /**
     * Does the calculation of the lighting in a separate thread.
     *
     * @return true - if the lighting calculation occurs in a separate thread, false
     * - if in main thread.
     */
    boolean isAsyncLighting();

    /**
     * N/A
     */
    int asSectionMask(int sectionY);

    /**
     * N/A
     */
    int getSectionFromY(int blockY);
}
