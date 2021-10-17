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
package ru.beykerykt.minecraft.lightapi.common.internal.chunks.data;

import java.util.Objects;

public abstract class ChunkData implements IChunkData {

    private final String worldName;
    private int chunkX;
    private int chunkZ;

    public ChunkData(String worldName, int chunkX, int chunkZ) {
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public String getWorldName() {
        return worldName;
    }

    @Override
    public int getChunkX() {
        return chunkX;
    }

    @Override
    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IChunkData chunkData = (IChunkData) o;
        return getChunkX() == chunkData.getChunkX() && getChunkZ() == chunkData.getChunkZ() && Objects.equals(
                getWorldName(), chunkData.getWorldName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWorldName(), getChunkX(), getChunkZ());
    }

    public long toLong() {
        long l = chunkX;
        l = (l << 32) | (chunkZ & 0xFFFFFFFFL);
        return l;
    }

    public void applyLong(long l) {
        this.chunkX = (int) ((l >> 32) & 0xFFFFFFFF);
        this.chunkZ = (int) (l & 0xFFFFFFFFL);
    }
}
