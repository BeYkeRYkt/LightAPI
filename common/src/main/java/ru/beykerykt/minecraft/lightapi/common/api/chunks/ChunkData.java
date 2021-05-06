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
package ru.beykerykt.minecraft.lightapi.common.api.chunks;

import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

import java.util.Objects;

/**
 * https://wiki.vg/Chunk_Format#Packet_structure
 *
 * @author BeYkeRYkt
 */
public class ChunkData {
    public static final int FULL_MASK = 0x1ffff;

    private String worldName;
    private int chunkX;
    private int chunkZ;
    private int sectionMaskSky;
    private int sectionMaskBlock;

    public ChunkData(String worldName, int chunkX, int chunkZ) {
        this(worldName, chunkX, chunkZ, FULL_MASK, FULL_MASK);
    }

    public ChunkData(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.sectionMaskSky = sectionMaskSky;
        this.sectionMaskBlock = sectionMaskBlock;
    }

    /**
     * @return World name
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * @return Chunk X Coordinate
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * @return Chunk Z Coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }

    public int getSectionMaskSky() {
        return sectionMaskSky;
    }

    public void setSectionMaskSky(int sectionMask) {
        this.sectionMaskSky = sectionMask;
    }

    public void addSectionMaskSky(int sectionMaskSky) {
        this.sectionMaskSky = FlagUtils.addFlag(this.sectionMaskSky, sectionMaskSky);
    }

    public void removeSectionMaskSky(int sectionMaskSky) {
        this.sectionMaskSky = FlagUtils.removeFlag(this.sectionMaskSky, sectionMaskSky);
    }

    public boolean checkSectionMaskSky(int sectionMaskSky) {
        return FlagUtils.isFlagSet(this.sectionMaskSky, sectionMaskSky);
    }

    public int getSectionMaskBlock() {
        return sectionMaskBlock;
    }

    public void setSectionMaskBlock(int sectionMask) {
        this.sectionMaskBlock = sectionMask;
    }

    public void addSectionMaskBlock(int sectionMaskBlock) {
        this.sectionMaskBlock = FlagUtils.addFlag(this.sectionMaskBlock, sectionMaskBlock);
    }

    public void removeSectionMaskBlock(int sectionMaskBlock) {
        this.sectionMaskBlock = FlagUtils.removeFlag(this.sectionMaskBlock, sectionMaskBlock);
    }

    public boolean checkSectionMaskBlock(int sectionMaskBlock) {
        return FlagUtils.isFlagSet(this.sectionMaskBlock, sectionMaskBlock);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkData chunkData = (ChunkData) o;
        return chunkX == chunkData.chunkX &&
                chunkZ == chunkData.chunkZ &&
                sectionMaskSky == chunkData.sectionMaskSky &&
                sectionMaskBlock == chunkData.sectionMaskBlock &&
                Objects.equals(worldName, chunkData.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
    }

    @Override
    public String toString() {
        return "ChunkData{" +
                "worldName='" + worldName + '\'' +
                ", x=" + chunkX +
                ", z=" + chunkZ +
                ", sectionMaskSky=" + sectionMaskSky +
                ", sectionMaskBlock=" + sectionMaskBlock +
                '}';
    }
}
