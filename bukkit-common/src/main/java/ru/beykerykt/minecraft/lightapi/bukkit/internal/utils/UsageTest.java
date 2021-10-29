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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.utils;

import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.RequestFlag;

public class UsageTest {

    private void setLightLevel(String world, int blockX, int blockY, int blockZ, int lightLevel) {
        int lightTypeFlags = LightFlag.BLOCK_LIGHTING;
        int oldBlockLight = LightAPI.get().getLightLevel(world, blockX, blockY, blockZ, lightTypeFlags);
        EditPolicy editPolicy = EditPolicy.DEFERRED;
        SendPolicy sendPolicy = SendPolicy.DEFERRED;
        LightAPI.get().setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightTypeFlags, editPolicy, sendPolicy,
                new ICallback() {
                    @Override
                    public void onResult(int requestFlag, int resultCode) {
                        switch (requestFlag) {
                            case RequestFlag.EDIT:
                                if (resultCode == ResultCode.SUCCESS) {
                                    System.out.println("Light level was edited.");
                                } else {
                                    System.out.println("request (" + requestFlag + "): result code " + resultCode);
                                }
                                break;
                            case RequestFlag.RECALCULATE:
                                if (resultCode == ResultCode.SUCCESS) {
                                    int newBlockLight = LightAPI.get().getLightLevel(world, blockX, blockY, blockZ,
                                            lightTypeFlags);
                                    if (oldBlockLight != newBlockLight) {
                                        System.out.println("Light level was recalculated.");
                                    } else {
                                        System.out.println("Light level was not recalculated.");
                                    }
                                } else {
                                    System.out.println("request (" + requestFlag + "): result code " + resultCode);
                                }
                                break;
                            case RequestFlag.SEPARATE_SEND:
                                System.out.println("request (" + requestFlag + "): result code " + resultCode);
                                break;
                        }
                    }
                });
    }
}
