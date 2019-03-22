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
package ru.beykerykt.minecraft.lightapi.common;

import java.util.List;

public class LightAPI {

	private static ILightHandler si;

	public static boolean setLightHandler(ILightHandler impl) {
		if (si != null) {
			return false;
		}
		si = impl;
		return true;
	}

	public static ILightHandler getLightHandler() {
		return si;
	}

	public static boolean isInitialized() {
		return getLightHandler() != null;
	}

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates.
	 * 
	 * @param worldName  - World name
	 * @param type       - Lighting type
	 * @param x          - Block X coordinate
	 * @param y          - Block Y coordinate
	 * @param z          - Block Z coordinate
	 * @param lightlevel - Lighting level. Default range - 0 - 15
	 * @return true - if the task is completed, false - if not
	 */
	public static boolean createLight(String worldName, LightType type, int x, int y, int z, int lightlevel) {
		if (!isInitialized()) {
			return false;
		}
		return getLightHandler().createLight(worldName, type, x, y, z, lightlevel);
	}

	/**
	 * Removing a certain type of light in the named world in certain coordinates.
	 * 
	 * @param worldName - World name
	 * @param type      - Lighting type
	 * @param x         - Block X coordinate
	 * @param y         - Block Y coordinate
	 * @param z         - Block Z coordinate
	 * @return true - if the task is completed, false - if not
	 */
	public static boolean deleteLight(String worldName, LightType type, int x, int y, int z) {
		if (!isInitialized()) {
			return false;
		}
		return getLightHandler().deleteLight(worldName, type, x, y, z);
	}

	/**
	 * Platform that is being used
	 * 
	 * @return One of the proposed options from {@link ImplementationPlatform}
	 */
	public static ImplementationPlatform getImplementationPlatform() {
		if (!isInitialized()) {
			return ImplementationPlatform.UNKNOWN;
		}
		return getLightHandler().getImplementationPlatform();
	}

	/**
	 * Is it required to send a chunk after placing / removing light.
	 * 
	 * @return true - if after changing the chunk, the developer needs to manually
	 *         send the chunks. false - if the server automatically sends it after
	 *         the change.
	 */
	public static boolean isRequireManuallySendingChunks() {
		if (!isInitialized()) {
			return false;
		}
		return getLightHandler().isRequireManuallySendingChunks();
	}

	/**
	 * 
	 * Collects in the list СhunkData around the given coordinate. The radius is
	 * specified in blocks.
	 * 
	 * @param worldName    - World name
	 * @param x            - Block X coordinate
	 * @param y            - Block Y coordinate
	 * @param z            - Block Z coordinate
	 * @param radiusBlocks - radius
	 * @return List СhunkData around the given coordinate.
	 */
	public static List<IChunkData> collectChunks(String worldName, int x, int y, int z, int radiusBlocks) {
		if (!isInitialized()) {
			return null;
		}
		return getLightHandler().collectChunks(worldName, x, y, z, radiusBlocks);
	}

	/**
	 * Sending a chunk to a player by name
	 * 
	 * @param worldName  - World name
	 * @param chunkX     - Chunk X coordinate
	 * @param chunkZ     - Chunk Z coordinate
	 * @param playerName - Player name
	 */
	public static void sendChunk(String worldName, int chunkX, int chunkZ, String playerName) {
		if (!isInitialized()) {
			return;
		}
		getLightHandler().sendChunk(worldName, chunkX, chunkZ, playerName);
	}

	/**
	 * Sending a chunk to a player by name
	 * 
	 * @param worldName  - World name
	 * @param chunkX     - Chunk X coordinate
	 * @param y          - Block Y coordinate
	 * @param chunkZ     - Chunk Z coordinate
	 * @param playerName - Player name
	 */
	public static void sendChunk(String worldName, int chunkX, int y, int chunkZ, String playerName) {
		if (!isInitialized()) {
			return;
		}
		getLightHandler().sendChunk(worldName, chunkX, y, chunkZ, playerName);
	}

	/**
	 * Sending a chunk to a player by name
	 * 
	 * @param worldName  - World name
	 * @param chunkData  - {@link IChunkData}
	 * @param playerName - Player name
	 */
	public static void sendChunk(String worldName, IChunkData chunkData, String playerName) {
		if (!isInitialized()) {
			return;
		}
		getLightHandler().sendChunk(worldName, chunkData, playerName);
	}

	/**
	 * Sending a chunk to a world
	 * 
	 * @param worldName - World name
	 * @param chunkX    - Chunk X coordinate
	 * @param chunkZ    - Chunk Z coordinate
	 */
	public static void sendChunk(String worldName, int chunkX, int chunkZ) {
		if (!isInitialized()) {
			return;
		}
		getLightHandler().sendChunk(worldName, chunkX, chunkZ);
	}

	/**
	 * Sending a chunk to a world
	 * 
	 * @param worldName - World name
	 * @param chunkX    - Chunk X coordinate
	 * @param y         - Block Y coordinate
	 * @param chunkZ    - Chunk Z coordinate
	 */
	public static void sendChunk(String worldName, int chunkX, int y, int chunkZ) {
		if (!isInitialized()) {
			return;
		}
		getLightHandler().sendChunk(worldName, chunkX, y, chunkZ);
	}

	/**
	 * Sending a chunk to world
	 * 
	 * @param worldName - World name
	 * @param chunkData - {@link IChunkData}
	 */
	public static void sendChunk(String worldName, IChunkData chunkData) {
		if (!isInitialized()) {
			return;
		}
		getLightHandler().sendChunk(worldName, chunkData);
	}
}
