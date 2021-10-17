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
package ru.beykerykt.minecraft.lightapi.common.internal.utils;

public class BlockPosition {

    // from NMS 1.15.2
    private static final int SIZE_BITS_X = 26;
    private static final int SIZE_BITS_Z = 26;
    private static final int SIZE_BITS_Y = 12;
    private static final long BITS_X = 0x3FFFFFF; // hex to dec: 67108863
    private static final long BITS_Y = 0xFFF; // hex to dec: 4095
    private static final long BITS_Z = 0x3FFFFFF; // hex to dec: 67108863
    private static final int BIT_SHIFT_Z = 12;
    private static final int BIT_SHIFT_X = 38;

    private int blockX;
    private int blockY;
    private int blockZ;

    public BlockPosition(int blockX, int blockY, int blockZ) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public static long asLong(int blockX, int blockY, int blockZ) {
        long long4 = 0L;
        long4 |= ((long) blockX & BITS_X) << BIT_SHIFT_X;
        long4 |= ((long) blockY & BITS_Y) << 0;
        long4 |= ((long) blockZ & BITS_Z) << BIT_SHIFT_Z;
        return long4;
    }

    public static int unpackLongX(final long long1) {
        return (int) (long1 << 64 - BIT_SHIFT_X - SIZE_BITS_X >> 64 - SIZE_BITS_X);
    }

    public static int unpackLongY(final long long1) {
        return (int) (long1 << 64 - SIZE_BITS_Y >> 64 - SIZE_BITS_Y);
    }

    public static int unpackLongZ(final long long1) {
        return (int) (long1 << 64 - BIT_SHIFT_Z - SIZE_BITS_Z >> 64 - SIZE_BITS_Z);
    }

    public static BlockPosition fromLong(final long value) {
        return new BlockPosition(unpackLongX(value), unpackLongY(value), unpackLongZ(value));
    }

    public int getBlockX() {
        return blockX;
    }

    public void setBlockX(int blockX) {
        this.blockX = blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public void setBlockY(int blockY) {
        this.blockY = blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public void setBlockZ(int blockZ) {
        this.blockZ = blockZ;
    }

    public long asLong() {
        return asLong(getBlockX(), getBlockY(), getBlockZ());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + blockX;
        result = prime * result + blockY;
        result = prime * result + blockZ;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BlockPosition other = (BlockPosition) obj;
        if (blockX != other.blockX) {
            return false;
        }
        if (blockY != other.blockY) {
            return false;
        }
        return blockZ == other.blockZ;
    }

    @Override
    public String toString() {
        return "BlockPosition [blockX=" + blockX + ", blockY=" + blockY + ", blockZ=" + blockZ + "]";
    }
}
