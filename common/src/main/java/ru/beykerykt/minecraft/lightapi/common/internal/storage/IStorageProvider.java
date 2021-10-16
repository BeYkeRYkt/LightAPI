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
package ru.beykerykt.minecraft.lightapi.common.internal.storage;

import java.util.Map;

import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;

public interface IStorageProvider {

    /**
     * N/A
     */
    void initialization(IPlatformImpl impl) throws Exception;

    /**
     * N/A
     */
    void shutdown();

    /*
     * N/A
     */
    int saveLightLevel(String world, int blockX, int blockY, int blockZ, int lightLevel);

    /*
     * N/A
     */
    int saveLightLevel(String world, long longPos, int lightLevel);

    /*
     * N/A
     */
    int saveLightLevels(String world, Map<Long, Integer> map);

    /*
     * N/A
     */
    void loadLightLevel(String world, long longPos, Map<Long, Integer> map);

    /*
     * N/A
     */
    void loadLightLevel(String world, int blockX, int blockY, int blockZ, Map<Long, Integer> map);

    /*
     * N/A
     */
    Map<Long, Integer> loadLightLevels(String world);
}
