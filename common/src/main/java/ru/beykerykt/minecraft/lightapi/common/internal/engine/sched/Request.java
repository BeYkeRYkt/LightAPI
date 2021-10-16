/**
 * The MIT License (MIT)
 *
 * <p>Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.common.internal.engine.sched;

import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.FlagUtils;

public class Request implements Comparable<Request> {

    public static final int HIGH_PRIORITY = 10;
    public static final int DEFAULT_PRIORITY = 5;
    public static final int LOW_PRIORITY = 0;

    private final String mWorldName;
    private final int mBlockX;
    private final int mBlockY;
    private final int mBlockZ;
    private final int mLightLevel;
    private final int mOldLightLevel;
    private final int mLightFlags;
    private int mRequestFlags;
    private int mPriority;
    private ICallback mCallback;

    public Request(int priority, int requestFlags, String worldName, int blockX, int blockY, int blockZ,
            int oldLightLevel, int lightLevel, int lightFlags, ICallback callback) {
        this.mPriority = priority;
        this.mRequestFlags = requestFlags;
        this.mWorldName = worldName;
        this.mBlockX = blockX;
        this.mBlockY = blockY;
        this.mBlockZ = blockZ;
        this.mOldLightLevel = oldLightLevel;
        this.mLightLevel = lightLevel;
        this.mLightFlags = lightFlags;
        this.mCallback = callback;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        this.mPriority = priority;
    }

    public int getRequestFlags() {
        return mRequestFlags;
    }

    public void setRequestFlags(int requestFlags) {
        this.mRequestFlags = requestFlags;
    }

    public void addRequestFlag(int targetFlag) {
        this.mRequestFlags = FlagUtils.addFlag(mRequestFlags, targetFlag);
    }

    public void removeRequestFlag(int targetFlag) {
        this.mRequestFlags = FlagUtils.removeFlag(mRequestFlags, targetFlag);
    }

    public String getWorldName() {
        return mWorldName;
    }

    public int getBlockX() {
        return mBlockX;
    }

    public int getBlockY() {
        return mBlockY;
    }

    public int getBlockZ() {
        return mBlockZ;
    }

    public int getOldLightLevel() {
        return mOldLightLevel;
    }

    public int getLightLevel() {
        return mLightLevel;
    }

    public int getLightFlags() {
        return mLightFlags;
    }

    public ICallback getCallback() {
        return mCallback;
    }

    @Override
    public int compareTo(Request o) {
        return this.getPriority() - o.getPriority();
    }
}
