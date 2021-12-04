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
package ru.beykerykt.minecraft.lightapi.bukkit.api.extension;

import org.bukkit.World;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.api.extension.IExtension;

public interface IBukkitExtension extends IExtension {

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    int getLightLevel(World world, int blockX, int blockY, int blockZ);

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    int getLightLevel(World world, int blockX, int blockY, int blockZ, int lightFlags);

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel);

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags);

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            ICallback callback);

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags,
            EditPolicy editPolicy, SendPolicy sendPolicy, ICallback callback);

    /**
     * N/A
     */
    boolean isBackwardAvailable();

    /**
     * N/A
     */
    boolean isCompatibilityMode();

    /**
     * N/A
     */
    IHandler getHandler();
}
