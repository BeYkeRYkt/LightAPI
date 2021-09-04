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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;

public abstract class BaseNMSHandler implements IHandler {
    private static final String STARLIGHT_ENGINE_PKG = "ca.spottedleaf.starlight.light.StarLightEngine";
    private IPlatformImpl mPlatformImpl;
    private LightEngineType engineType = LightEngineType.VANILLA;

    @Override
    public void onInitialization(IPlatformImpl impl) throws Exception {
        this.mPlatformImpl = impl;
        try {
            Class<?> starlight = Class.forName(STARLIGHT_ENGINE_PKG);
            if (starlight != null) {
                engineType = LightEngineType.STARLIGHT;
            }
        } catch (ClassNotFoundException e) {
            // nothing
        }
    }

    protected IPlatformImpl getPlatformImpl() {
        return mPlatformImpl;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.CRAFTBUKKIT;
    }

    @Override
    public LightEngineType getLightEngineType() {
        return engineType;
    }
}
