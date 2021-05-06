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

import ru.beykerykt.minecraft.lightapi.common.api.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;
import ru.beykerykt.minecraft.lightapi.common.api.service.ICallback;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.EditStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.RelightStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.SendStrategy;

public interface ILightAPI {

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
     * N/A
     */
    IExtension getExtension();

    /**
     * N/A
     */
    RelightStrategy getRelightStrategy();

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from {@link PlatformType}
     */
    PlatformType getPlatformType();

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightType);

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightType,
                      EditStrategy editStrategy, SendStrategy sendStrategy, ICallback callback);

    /**
     * Can be used for specific commands
     */
    int sendCmd(int cmdId, Object... args);
}
