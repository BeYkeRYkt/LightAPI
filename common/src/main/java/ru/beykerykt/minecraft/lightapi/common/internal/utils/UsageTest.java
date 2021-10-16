/**
 * The MIT License (MIT)
 *
 * <p>Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.common.internal.utils;

import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;

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
    }

    public void onLoad() {
        IPlatformImpl mPluginImpl = null;
        try {
            LightAPI.prepare(mPluginImpl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onEnable() {
        try {
            LightAPI.initialization();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDisable() {
        IPlatformImpl mPluginImpl = null;
        LightAPI.shutdown(mPluginImpl);
    }

    // for clients
    public void test(String world, int blockX, int blockY, int blockZ, int lightLevel) {
        int blockLight = mLightAPI.getLightLevel(world, blockX, blockY, blockZ, lightLevel);

        int lightType = LightType.BLOCK_LIGHTING;
        EditPolicy editPolicy = EditPolicy.DEFERRED;
        SendPolicy sendPolicy = SendPolicy.DEFERRED;
        mLightAPI.setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightType, editPolicy, sendPolicy,
                (requestFlag, resultCode) -> {
                    // test
                });
    }
}
