/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.common.internal.chunks.data;

import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

import java.util.BitSet;

/**
 * ChunkData with BitSet data
 */
public class BitChunkData extends ChunkData {
    private static final int DEFAULT_SIZE = 2048;
    private BitSet skyLightUpdateBits;
    private BitSet blockLightUpdateBits;

    private int topSection;
    private int bottomSection;

    public BitChunkData(String worldName, int chunkX, int chunkZ, int topSection, int bottomSection) {
        super(worldName, chunkX, chunkZ);
        this.skyLightUpdateBits = new BitSet(DEFAULT_SIZE);
        this.blockLightUpdateBits = new BitSet(DEFAULT_SIZE);
        this.topSection = topSection;
        this.bottomSection = bottomSection;
    }

    public BitSet getSkyLightUpdateBits() {
        return skyLightUpdateBits;
    }

    public BitSet getBlockLightUpdateBits() {
        return blockLightUpdateBits;
    }

    /**
     * Max chunk section
     */
    // getHeight() - this.world.countVerticalSections() + 2;
    // getTopY() - this.getBottomY() + this.getHeight();
    public int getTopSection() {
        return topSection;
    }

    /**
     * Min chunk section
     */
    // getBottomY() - this.world.getBottomSectionCoord() - 1
    public int getBottomSection() {
        return bottomSection;
    }

    @Override
    public void markSectionForUpdate(int lightFlags, int sectionY) {
        int minY = getBottomSection();
        int maxY = getTopSection();
        if (sectionY < minY || sectionY > maxY) {
            return;
        }
        int l = sectionY - minY;

        if (FlagUtils.isFlagSet(lightFlags, LightType.SKY_LIGHTING)) {
            skyLightUpdateBits.set(l);
        }

        if (FlagUtils.isFlagSet(lightFlags, LightType.BLOCK_LIGHTING)) {
            blockLightUpdateBits.set(l);
        }
    }

    @Override
    public void clearUpdate() {
        this.skyLightUpdateBits.clear();
        this.blockLightUpdateBits.clear();
    }

    @Override
    public void setFullSections() {
        // TODO: Mark full sections
        for (int i = getBottomSection(); i < getTopSection(); i++) {
            markSectionForUpdate(LightType.SKY_LIGHTING | LightType.BLOCK_LIGHTING, i);
        }
    }
}
