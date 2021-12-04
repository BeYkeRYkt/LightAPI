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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler;

import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.List;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.BukkitPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.PlatformType;
import ru.beykerykt.minecraft.lightapi.common.internal.chunks.data.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineType;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.LightEngineVersion;

public interface IHandler {

    /**
     * N/A
     */
    void onInitialization(BukkitPlatformImpl impl) throws Exception;

    /**
     * N/A
     */
    void onShutdown(BukkitPlatformImpl impl);

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from {@link PlatformType}
     */
    PlatformType getPlatformType();

    /**
     * N/A
     */
    LightEngineType getLightEngineType();

    /**
     * Used lighting engine version.
     *
     * @return One of the proposed options from {@link LightEngineVersion}
     */
    LightEngineVersion getLightEngineVersion();

    /**
     * N/A
     */
    boolean isMainThread();

    /**
     * N/A
     */
    void onWorldLoad(WorldLoadEvent event);

    /**
     * N/A
     */
    void onWorldUnload(WorldUnloadEvent event);

    /**
     * N/A
     */
    boolean isLightingSupported(World world, int lightFlags);

    /**
     * Sets "directly" the level of light in given coordinates without additional processing.
     */
    int setRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightLevel, int lightFlags);

    /**
     * Gets "directly" the level of light from given coordinates without additional processing.
     */
    int getRawLightLevel(World world, int blockX, int blockY, int blockZ, int lightFlags);

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    int recalculateLighting(World world, int blockX, int blockY, int blockZ, int lightFlags);

    /**
     * N/A
     */
    IChunkData createChunkData(String worldName, int chunkX, int chunkZ);

    /**
     * Collects modified сhunks with sections around a given coordinate in the radius of the light
     * level. The light level is taken from the arguments.
     *
     * @return List changed chunk sections around the given coordinate.
     */
    List<IChunkData> collectChunkSections(World world, int blockX, int blockY, int blockZ, int lightLevel,
            int lightFlags);

    /**
     * N/A
     */
    boolean isValidChunkSection(World world, int sectionY);

    /**
     * N/A
     */
    int sendChunk(IChunkData data);

    /**
     * Can be used for specific commands
     */
    int sendCmd(int cmdId, Object... args);
}
