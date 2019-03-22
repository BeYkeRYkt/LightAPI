/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Vladimir Mikhailov <beykerykt@gmail.com>
 * Copyright (c) 2016-2017 The ImplexDevOne Project
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

import java.util.Collection;
import java.util.List;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.ILightHandler;
import ru.beykerykt.minecraft.lightapi.common.LightType;

/**
 *
 * Extended common interface for Sponge
 *
 * @author BeYkeRYkt
 *
 */
public interface ISpongeLightHandler extends ILightHandler {

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates.
	 * 
	 * @param world      - World
	 * @param type       - Lighting type
	 * @param x          - Block X coordinate
	 * @param y          - Block Y coordinate
	 * @param z          - Block Z coordinate
	 * @param lightlevel - Lighting level. Default range - 0 - 15
	 * @return true - if the task is completed, false - if not
	 */
	public boolean createLight(World world, LightType type, int x, int y, int z, int lightlevel);

	/**
	 * Removing a certain type of light in the named world in certain coordinates.
	 * 
	 * @param world - World
	 * @param type  - Lighting type
	 * @param x     - Block X coordinate
	 * @param y     - Block Y coordinate
	 * @param z     - Block Z coordinate
	 * @return true - if the task is completed, false - if not
	 */
	public boolean deleteLight(World world, LightType type, int x, int y, int z);

	/**
	 * 
	 * Collects in the list СhunkData around the given coordinate. The radius is
	 * specified in blocks.
	 * 
	 * @param world        - World
	 * @param x            - Block X coordinate
	 * @param y            - Block Y coordinate
	 * @param z            - Block Z coordinate
	 * @param radiusBlocks - radius
	 * @return List СhunkData around the given coordinate.
	 */
	public List<IChunkData> collectChunks(World world, int x, int y, int z, int radiusBlocks);

	/**
	 * Sending a chunk to a player
	 * 
	 * @param world  - World
	 * @param chunkX - Chunk X coordinate
	 * @param chunkZ - Chunk Z coordinate
	 * @param player - Player
	 */
	public void sendChunk(World world, int chunkX, int chunkZ, Player player);

	/**
	 * Sending a chunk to a player list
	 * 
	 * @param world   - World
	 * @param chunkX  - Chunk X coordinate
	 * @param chunkZ  - Chunk Z coordinate
	 * @param players - Players list
	 */
	public void sendChunk(World world, int chunkX, int chunkZ, Collection<? extends Player> players);

	/**
	 * Sending a chunk to a player
	 * 
	 * @param world  - World
	 * @param chunkX - Chunk X coordinate
	 * @param y      - Block Y coordinate
	 * @param chunkZ - Chunk Z coordinate
	 * @param player - Player
	 */
	public void sendChunk(World world, int chunkX, int y, int chunkZ, Player player);

	/**
	 * Sending a chunk to a player list
	 * 
	 * @param world   - World
	 * @param chunkX  - Chunk X coordinate
	 * @param y       - Block Y coordinate
	 * @param chunkZ  - Chunk Z coordinate
	 * @param players - Players list
	 */
	public void sendChunk(World world, int chunkX, int y, int chunkZ, Collection<? extends Player> players);

	/**
	 * Sending a chunk to a receivers
	 *
	 * @param chunkData - {@link SpongeChunkData}
	 */
	public void sendChunk(SpongeChunkData chunkData);

	/**
	 * Sending a chunk to a player
	 *
	 * @param chunkData - {@link SpongeChunkData}
	 * @param player    - Player
	 */
	public void sendChunk(SpongeChunkData chunkData, Player player);

	/**
	 * Checking the inclusion of asynchronous light calculation in the server
	 * settings
	 *
	 * @return true - if asynchronous light calculation is enabled, false - if not.
	 */
	public boolean isAsyncLighting();
}
