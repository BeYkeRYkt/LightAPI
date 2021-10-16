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
package ru.beykerykt.minecraft.lightapi.common.internal.chunks.observer;

import java.util.List;

import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;

public interface IChunkObserver {

    /**
     * N/A
     */
    void onStart();

    /**
     * N/A
     */
    void onShutdown();

    /**
     * Collects modified сhunks with sections around a given coordinate in the radius of the light
     * level. The light level is taken from the arguments.
     *
     * @return List changed chunk sections around the given coordinate.
     */
    List<IChunkData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags);

    /**
     * N/A
     */
    int sendChunk(IChunkData data);
}
