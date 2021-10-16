/**
 * The MIT License (MIT)
 *
 * <p>Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.common.internal.engine;

import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.RelightPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;

public interface ILightEngine {

    /**
     * N/A
     */
    void onStart();

    /**
     * N/A
     */
    void onShutdown();

    /**
     * N/A
     */
    LightEngineType getLightEngineType();

    /**
     * Used lighting engine version.
     *
     * @return One of the proposed options from {@link LightEngineVersion}
     */
    LightEngineVersion getLightEngineVersion();

    /**
     * N/A
     */
    RelightPolicy getRelightPolicy();

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    int getLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightFlags);

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback);

    /**
     * Sets "directly" the level of light in given coordinates without additional processing.
     */
    int setRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags);

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    int recalculateLighting(String worldName, int blockX, int blockY, int blockZ, int lightFlags);
}
