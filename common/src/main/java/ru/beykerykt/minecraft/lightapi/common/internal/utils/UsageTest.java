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
package ru.beykerykt.minecraft.lightapi.common.internal.utils;

import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.api.*;
import ru.beykerykt.minecraft.lightapi.common.api.impl.IHandler;

import java.util.ArrayList;
import java.util.List;

public class UsageTest {

    private LightAPI mLightAPI;

    private void init() {
        mLightAPI = LightAPI.get();

        // internal use
        switch (Build.CURRENT_IMPLEMENTATION) {
            case UNKNOWN:
                // unknown implementation
                break;
            case BUKKIT:
                // bukkit implementation
                break;
            case CRAFTBUKKIT:
                // craftbukkit implementation
                break;
            case SPONGE:
                // sponge implementation
                break;
        }

        switch (Build.CURRENT_VERSION) {
            case Build.VERSION_CODES.ONE:
                // version one
                break;
            case Build.VERSION_CODES.TWO:
                // version two
                break;
            case Build.VERSION_CODES.THREE:
                // version three
                break;
            case Build.VERSION_CODES.FOUR:
                // version four
                break;
        }
    }

    /* Sync call method */
    private void setLightLevel(String world, int blockX, int blockY, int blockZ) {
        int flag = LightFlags.BLOCK_LIGHTING;
        int lightLevel = 15;
        SendMode mode = SendMode.MANUAL;
        List<ChunkData> chunks = new ArrayList<>();
        int code = mLightAPI.setLightLevel(world, blockX, blockY, blockZ, lightLevel, flag, mode, chunks);
        if (code == ResultCodes.SUCCESS) {
            switch (mode) {
                case INSTANT:
                    // Nothing. Chunks will be sent immediately after completion.
                    break;
                case DELAYED:
                    // Nothing. Chunks will be sent after a certain number of ticks.
                    break;
                case MANUAL:
                    // You need to manually send chunks.
                    for (int i = 0; i < chunks.size(); i++) {
                        ChunkData data = chunks.get(i);
                        mLightAPI.getImplHandler().sendChunk(data);
                    }
                    break;
            }
        }
    }

    /* Async call method */
    private void setLightLevelAsync(String world, int blockX, int blockY, int blockZ) {
        int lightLevel = 15;
        SendMode mode = SendMode.DELAYED;
        List<ChunkData> chunks = null;
        int code = mLightAPI.setLightLevel(world, blockX, blockY, blockZ, lightLevel, mode, chunks);
        // Be careful, asynchronous thread can be blocked
        if (code == ResultCodes.SUCCESS) {
            switch (mode) {
                case INSTANT:
                    // Nothing. Chunks will be sent immediately after completion.
                    break;
                case DELAYED:
                    // Nothing. Chunks will be sent after a certain number of ticks.
                    break;
                case MANUAL:
                    // You need to manually send chunks.
                    if (chunks != null) {
                        for (int i = 0; i < chunks.size(); i++) {
                            ChunkData data = chunks.get(i);
                            mLightAPI.getImplHandler().sendChunk(data);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Old style code
     **/
    private void setLightLevelAdvanced(String world, int blockX, int blockY, int blockZ) {
        int flag = LightFlags.BLOCK_LIGHTING;
        int lightLevel = 15;

        IHandler handler = LightAPI.get().getImplHandler();
        // Be careful, asynchronous thread can be blocked
        handler.setRawLightLevel(world, blockX, blockY, blockZ, lightLevel, flag);
        if (handler.isRequireRecalculateLighting()) {
            handler.recalculateLighting(world, blockX, blockY, blockZ, flag);
        }
        if (handler.isRequireManuallySendingChanges()) {
            List<ChunkData> chunkList = handler.collectChunkSections(world, blockX, blockY, blockZ, lightLevel);
            for (int i = 0; i < chunkList.size(); i++) {
                ChunkData data = chunkList.get(i);
                handler.sendChunk(data);
            }
        }
    }
}
