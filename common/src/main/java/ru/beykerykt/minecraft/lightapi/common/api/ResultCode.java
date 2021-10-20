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
package ru.beykerykt.minecraft.lightapi.common.api;

/**
 * Result codes
 */
public class ResultCode {

    /**
     * N/A
     */
    public static final int MOVED_TO_DEFERRED = 1;

    /**
     * The task has been successfully completed
     */
    public static final int SUCCESS = 0;

    /**
     * The task has been failed
     */
    public static final int FAILED = -1;

    /**
     * Current world is not available
     */
    public static final int WORLD_NOT_AVAILABLE = -2;

    /**
     * (1.14+) No lighting changes in current world
     */
    public static final int RECALCULATE_NO_CHANGES = -3;

    /**
     * (1.14+) SkyLight data is not available in current world
     */
    public static final int SKYLIGHT_DATA_NOT_AVAILABLE = -4;

    /**
     * (1.14+) BlockLight data is not available in current world
     */
    public static final int BLOCKLIGHT_DATA_NOT_AVAILABLE = -5;

    /**
     * Current function is not implemented
     */
    public static final int NOT_IMPLEMENTED = -6;

    /**
     * Chunk is not loaded
     */
    public static final int CHUNK_NOT_LOADED = -7;
}
