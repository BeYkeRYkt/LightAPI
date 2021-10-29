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

import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class IntChunkData extends ChunkData {

    public static final int FULL_MASK = 0x1ffff;

    protected int skyLightUpdateBits;
    protected int blockLightUpdateBits;

    public IntChunkData(String worldName, int chunkX, int chunkZ, int skyLightUpdateBits, int blockLightUpdateBits) {
        super(worldName, chunkX, chunkZ);
        this.skyLightUpdateBits = skyLightUpdateBits;
        this.blockLightUpdateBits = blockLightUpdateBits;
    }

    public int getSkyLightUpdateBits() {
        return skyLightUpdateBits;
    }

    public int getBlockLightUpdateBits() {
        return blockLightUpdateBits;
    }

    @Override
    public void markSectionForUpdate(int lightFlags, int sectionY) {
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            skyLightUpdateBits |= 1 << sectionY + 1;
        }

        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            blockLightUpdateBits |= 1 << sectionY + 1;
        }
    }

    @Override
    public void clearUpdate() {
        this.skyLightUpdateBits = 0;
        this.blockLightUpdateBits = 0;
    }

    @Override
    public void setFullSections() {
        this.skyLightUpdateBits = FULL_MASK;
        this.blockLightUpdateBits = FULL_MASK;
    }

    @Override
    public String toString() {
        return "IntChunkData{" + "worldName=" + getWorldName() + ", chunkX=" + getChunkX() + ", chunkZ=" + getChunkZ()
                + ", skyLightUpdateBits=" + skyLightUpdateBits + ", blockLightUpdateBits=" + blockLightUpdateBits + '}';
    }
}
