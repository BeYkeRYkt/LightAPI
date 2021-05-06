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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks;

import org.bukkit.World;

public interface IChunkObserver {

    /*
     * N/A
     */
    void start();

    /*
     * N/A
     */
    void shutdown();

    /*
     * N/A
     */
    void onTick();

    /*
     * N/A
     */
    boolean isMergeChunksEnabled();

    /*
     * N/A
     */
    void setMergeChunksEnabled(boolean enabled);

    /*
     * N/A
     */
    int notifyUpdateChunks(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightType);

    /*
     * N/A
     */
    int notifyUpdateChunk(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock);

    /*
     * N/A
     */
    void sendUpdateChunks(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightType);
}
