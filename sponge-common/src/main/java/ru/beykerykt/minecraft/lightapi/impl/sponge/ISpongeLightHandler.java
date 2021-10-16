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
package ru.beykerykt.minecraft.lightapi.impl.sponge;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import java.util.List;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.ILightHandler;
import ru.beykerykt.minecraft.lightapi.common.LightType;

/**
 * Extended common interface for Sponge
 *
 * @author BeYkeRYkt
 */
public interface ISpongeLightHandler extends ILightHandler {

    /**
     * Placement of a certain type of light with a given level of illumination in the named world in
     * certain coordinates with the return result.
     *
     * @param world      - World
     * @param type       - Light type
     * @param blockX     - Block X coordinate
     * @param blockY     - Block Y coordinate
     * @param blockZ     - Block Z coordinate
     * @param lightlevel - light level. Default range - 0 - 15
     * @return true - if the light in the given coordinates has changed, false - if not
     */
    public boolean createLight(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel);

    /**
     * Removing a certain type of light in the named world in certain coordinates with the return
     * result.
     *
     * @param world  - World
     * @param type   - Light type
     * @param blockX - Block X coordinate
     * @param blockY - Block Y coordinate
     * @param blockZ - Block Z coordinate
     * @return true - if the light in the given coordinates has changed, false - if not
     */
    public boolean deleteLight(World world, LightType type, int blockX, int blockY, int blockZ);

    /**
     * Sets "directly" the level of light in given coordinates without additional processing.
     *
     * @param world      - World
     * @param type       - Light type
     * @param blockX     - Block X coordinate
     * @param blockY     - Block Y coordinate
     * @param blockZ     - Block Z coordinate
     * @param lightlevel - light level. Default range - 0 - 15
     */
    public void setRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel);

    /**
     * Gets "directly" the level of light from given coordinates without additional processing.
     *
     * @param world  - World
     * @param type   - Light type
     * @param blockX - Block X coordinate
     * @param blockY - Block Y coordinate
     * @param blockZ - Block Z coordinate
     * @return lightlevel - Light level. Default range - 0 - 15
     */
    public int getRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ);

    /**
     * Performs re-illumination of the light in the given coordinates.
     *
     * @param world  - World
     * @param type   - Light type
     * @param blockX - Block X coordinate
     * @param blockY - Block Y coordinate
     * @param blockZ - Block Z coordinate
     */
    public void recalculateLighting(World world, LightType type, int blockX, int blockY, int blockZ);

    /**
     * Collects changed chunks in the list around the given coordinate.
     *
     * @param world      - World
     * @param blockX     - Block X coordinate
     * @param blockY     - Block Y coordinate
     * @param blockZ     - Block Z coordinate
     * @param lightlevel - Light level. Default range - 0 - 15
     * @return List changed chunks around the given coordinate.
     */
    public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ, int lightlevel);

    /**
     * Collects changed chunks in the list around the given coordinate.
     *
     * @param world  - World
     * @param blockX - Block X coordinate
     * @param blockY - Block Y coordinate
     * @param blockZ - Block Z coordinate
     * @return List changed chunks around the given coordinate.
     */
    public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ);

    /**
     * Sending changes to a player by name
     *
     * @param world  - World
     * @param chunkX - Chunk X coordinate
     * @param chunkZ - Chunk Z coordinate
     * @param player - Player
     */
    public void sendChanges(World world, int chunkX, int chunkZ, Player player);

    /**
     * Sending changes to a player by name
     *
     * @param world  - World
     * @param chunkX - Chunk X coordinate
     * @param blockY - Block Y coordinate
     * @param chunkZ - Chunk Z coordinate
     * @param player - Player
     */
    public void sendChanges(World world, int chunkX, int blockY, int chunkZ, Player player);

    /**
     * Sending changes to a player by name
     *
     * @param chunkData - {@link IChunkData}
     * @param player    - Player
     */
    public void sendChanges(IChunkData chunkData, Player player);

    /**
     * Sending changes to world
     *
     * @param world  - World
     * @param chunkX - Chunk X coordinate
     * @param chunkZ - Chunk Z coordinate
     */
    public void sendChanges(World world, int chunkX, int chunkZ);

    /**
     * Sending changes to world
     *
     * @param world  - World
     * @param chunkX - Chunk X coordinate
     * @param blockY - Block Y coordinate
     * @param chunkZ - Chunk Z coordinate
     */
    public void sendChanges(World world, int chunkX, int blockY, int chunkZ);
}
