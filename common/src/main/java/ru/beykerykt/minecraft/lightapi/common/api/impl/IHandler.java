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
package ru.beykerykt.minecraft.lightapi.common.api.impl;

import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;
import ru.beykerykt.minecraft.lightapi.common.api.SendMode;

import java.util.List;

/**
 * Common interface for various Minecraft server platform implementations. Must
 * be used for internal and direct use without third-party processing.
 *
 * @author BeYkeRYkt
 */
public interface IHandler {

    /**
     * Placement of a specific type of light with a given level of illumination in
     * the named world in certain coordinates with the return code result.
     */
    int setLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int flags,
                      SendMode mode, List<ChunkData> outputChunks);

    /**
     * Sets "directly" the level of light in given coordinates without additional
     * processing.
     *
     * @param worldName  - World name
     * @param blockX     - Block X coordinate
     * @param blockY     - Block Y coordinate
     * @param blockZ     - Block Z coordinate
     * @param lightLevel - light level. Default range: 0 - 15
     * @param flags      - Light type
     */
    int setRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int lightLevel, int flags);

    /**
     * Gets "directly" the level of light from given coordinates without additional
     * processing.
     *
     * @param worldName - World name
     * @param blockX    - Block X coordinate
     * @param blockY    - Block Y coordinate
     * @param blockZ    - Block Z coordinate
     * @param flags     - Light type
     */
    int getRawLightLevel(String worldName, int blockX, int blockY, int blockZ, int flags);

    /**
     * N/A
     *
     * @return
     */
    boolean isRequireRecalculateLighting();

    /**
     * Performs re-illumination of the light in the given coordinates.
     *
     * @param worldName - World name
     * @param blockX    - Block X coordinate
     * @param blockY    - Block Y coordinate
     * @param blockZ    - Block Z coordinate
     * @param flags     - Light type
     */
    int recalculateLighting(String worldName, int blockX, int blockY, int blockZ, int flags);

    /**
     * Is it required to send changes after changing light levels.
     *
     * @return true - if after changing light levels, the developer needs to
     * manually send the changes. false - if the server automatically sends
     * it after the change.
     */
    boolean isRequireManuallySendingChanges();

    /**
     * Collects modified сhunks with sections around a given coordinate in the
     * radius of the light level. The light level is taken from the arguments.
     *
     * @param worldName  - World name
     * @param blockX     - Block X coordinate
     * @param blockY     - Block Y coordinate
     * @param blockZ     - Block Z coordinate
     * @param lightLevel - Radius in blocks (0 - 15)
     * @return List changed chunk sections around the given coordinate.
     */
    List<ChunkData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ,
                                         int lightLevel);

    /**
     * N/A
     */
    int sendChunk(ChunkData data);

    /**
     * Instant sending a full chunk to players in the world. Sends a single packet.
     *
     * @param worldName - World name
     * @param chunkX    - Chunk X coordinate
     * @param chunkZ    - Chunk Z coordinate
     */
    int sendChunk(String worldName, int chunkX, int chunkZ);

    /**
     * Instant sending a chunk with specific section to players in the world. Sends
     * a single packet.
     *
     * @param worldName     - World name
     * @param chunkX        - Chunk X coordinate
     * @param chunkSectionY - Chunk Y section
     * @param chunkZ        - Chunk Z coordinate
     */
    int sendChunk(String worldName, int chunkX, int chunkZ, int chunkSectionY);

    /**
     * Instant sending a chunk with with known sections mask to players in the
     * world. Sends a single packet.
     *
     * @param worldName        - World name
     * @param chunkX           - Chunk X coordinate
     * @param chunkZ           - Chunk Z coordinate
     * @param sectionMaskSky   - N/A
     * @param sectionMaskBlock - N/A
     */
    int sendChunk(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock);
}
