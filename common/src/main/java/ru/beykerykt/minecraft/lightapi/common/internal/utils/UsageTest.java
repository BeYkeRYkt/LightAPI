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
import ru.beykerykt.minecraft.lightapi.common.api.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.LightType;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.EditStrategy;
import ru.beykerykt.minecraft.lightapi.common.api.strategy.SendStrategy;

public class UsageTest {

    private LightAPI mLightAPI;

    private void initClient() {
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

    public void test(String world, int blockX, int blockY, int blockZ, int lightLevel) {
        int blockLight = mLightAPI.getLightLevel(world, blockX, blockY, blockZ, lightLevel);

        int lightType = LightType.BLOCK_LIGHTING;
        EditStrategy editStrategy = EditStrategy.DEFERRED;
        SendStrategy sendStrategy = SendStrategy.DEFERRED;
        mLightAPI.setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightType, editStrategy, sendStrategy,
                (requestFlag, resultCode) -> {
                    // test
                });
    }
}
