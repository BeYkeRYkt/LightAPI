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
package ru.beykerykt.minecraft.lightapi.common.api.engine;

public enum SendPolicy {

    /**
     * Updated chunks are collected in a local pool after the light level changes, but are not
     * automatically sent.
     */
    @Deprecated
    MANUAL(0),

    /**
     * Updated chunks are immediately collected and sent to players after the light level changes.
     */
    IMMEDIATE(1),

    /**
     * Updated chunks are collected in a common pool after the light level changes. Then they are
     * automatically sent to players at regular intervals.
     */
    DEFERRED(2),

    /**
     * Chunks are not collected after the changes are applied.
     */
    IGNORE(3);

    private final int id;

    SendPolicy(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
