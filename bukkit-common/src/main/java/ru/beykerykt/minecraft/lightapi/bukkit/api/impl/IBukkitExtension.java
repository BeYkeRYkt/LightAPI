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
package ru.beykerykt.minecraft.lightapi.bukkit.api.impl;

import org.bukkit.Location;
import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.SendMode;
import ru.beykerykt.minecraft.lightapi.common.api.impl.IExtension;

import java.util.List;

public interface IBukkitExtension extends IExtension {

    /**
     * Gets the level of light from given coordinates with
     * {@link ru.beykerykt.minecraft.lightapi.common.api.LightFlags#COMBO_LIGHTING}.
     */
    int getLightLevel(Location location);

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    int getLightLevel(Location location, int flags);

    /**
     * Gets the level of light from given coordinates with
     * {@link ru.beykerykt.minecraft.lightapi.common.api.LightFlags#COMBO_LIGHTING}.
     */
    int getLightLevel(World world, int blockX, int blockY, int blockZ);

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    int getLightLevel(World world, int blockX, int blockY, int blockZ, int flags);

    /**
     * Placement of a {@link ru.beykerykt.minecraft.lightapi.common.api.LightFlags#COMBO_LIGHTING} type of light with a
     * given level of illumination in the named world in certain coordinates with the return code result.
     */
    int setLightLevel(Location location, int lightLevel, SendMode mode, List<ChunkData> outputChunks);

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    int setLightLevel(Location location, int lightLevel, int flags, SendMode mode, List<ChunkData> outputChunks);

    /**
     * Placement of a {@link ru.beykerykt.minecraft.lightapi.common.api.LightFlags#COMBO_LIGHTING} type of light with a
     * given level of illumination in the named world in certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, SendMode mode,
                      List<ChunkData> outputChunks);

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags,
                      SendMode mode, List<ChunkData> outputChunks);
}
