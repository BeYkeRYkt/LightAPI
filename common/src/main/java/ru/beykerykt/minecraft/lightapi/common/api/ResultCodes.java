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
package ru.beykerykt.minecraft.lightapi.common.api;

public class ResultCodes {

    /**
     * N/A
     */
    public static final int MOVED_TO_ASYNC = 1;

    /**
     * N/A
     */
    public static final int SUCCESS = 0;

    /**
     * N/A
     */
    public static final int FAILED = -1;

    /**
     * N/A
     */
    public static final int WORLD_NOT_AVAILABLE = -2;

    /**
     * N/A
     */
    public static final int MANUAL_MODE_CHUNK_LIST_IS_NULL = -3;

    /**
     * N/A
     */
    public static final int RECALCULATE_NO_CHANGES = -4;

    /**
     * N/A
     */
    public static final int SKYLIGHT_DATA_NOT_AVAILABLE = -5;

    /**
     * N/A
     */
    public static final int BLOCKLIGHT_DATA_NOT_AVAILABLE = -6;
}
