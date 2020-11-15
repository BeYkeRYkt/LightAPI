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

import org.bukkit.World;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.SendMode;
import ru.beykerykt.minecraft.lightapi.common.api.impl.IHandler;

import java.util.List;

public interface IBukkitHandler extends IHandler {

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    int setLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags,
                      SendMode mode, List<ChunkData> outputChunks);

    /**
     * Sets "directly" the level of light in given coordinates without additional
     * processing.
     */
    int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int flags);

    /**
     * Gets "directly" the level of light from given coordinates without additional
     * processing.
     */
    int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int flags);

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    int recalculateLighting(World world, int blockX, int blockY, int blockZ, int flags);

    /**
     * Collects modified сhunks with sections around a given coordinate in the
     * radius of the light level. The light level is taken from the arguments.
     */
    List<ChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ,
                                         int lightLevel);

    /**
     * Instant sending a full chunk to players in the world. Sends a single packet.
     */
    int sendChunk(World world, int chunkX, int chunkZ);

    /**
     * Instant sending a chunk with specific section to players in the world. Sends
     * a single packet.
     */
    int sendChunk(World world, int chunkX, int chunkZ, int chunkSectionY);

    /**
     * Instant sending a chunk with with known sections mask to players in the
     * world. Sends a single packet.
     */
    int sendChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock);
}
