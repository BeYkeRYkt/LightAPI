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

import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;

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
        int apiVersion = Build.API_VERSION;
        int internalVersion = Build.INTERNAL_VERSION;
    }

    // for clients
    public void test(String world, int blockX, int blockY, int blockZ, int lightLevel) {
        int lightType = LightFlag.BLOCK_LIGHTING;
        EditPolicy editPolicy = EditPolicy.DEFERRED;
        SendPolicy sendPolicy = SendPolicy.DEFERRED;
        mLightAPI.setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightType, editPolicy, sendPolicy,
                (requestFlag, resultCode) -> {
                    // test
                });
    }
}
