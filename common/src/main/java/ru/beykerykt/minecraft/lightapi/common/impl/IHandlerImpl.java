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
package ru.beykerykt.minecraft.lightapi.common.impl;

import java.util.List;

import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;

/**
 * Common interface for various Minecraft server platform implementations. Must
 * be used for internal and direct use without third-party processing.
 *
 * @author BeYkeRYkt
 */
public interface IHandlerImpl {

	/**
	 * Used lighting engine version.
	 * 
	 * @return One of the proposed options from {@link LightingEngineVersion}
	 */
	public LightingEngineVersion getLightingEngineVersion();

	/**
	 * Does the calculation of the lighting in a separate thread.
	 *
	 * @return true - if the lighting calculation occurs in a separate thread, false
	 *         - if in main thread.
	 */
	public boolean isAsyncLighting();

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param worldName  - World name
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean createLight(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback);

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param worldName - World name
	 * @param flags     - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @param callback  - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean deleteLight(String worldName, int flags, int blockX, int blockY, int blockZ, LCallback callback);

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param worldName  - World name
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - Callback interface
	 */
	public void setRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback);

	/**
	 * Gets "directly" the level of light from given coordinates without additional
	 * processing.
	 * 
	 * @param worldName - World name
	 * @param flags     - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @return lightlevel - Light level. Default range - 0 - 15
	 */
	public int getRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ);

	/**
	 * N/A
	 * 
	 * @return
	 */
	public boolean isRequireRecalculateLighting();

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param worldName - World name
	 * @param flags     - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @param callback  - Callback interface
	 */
	public void recalculateLighting(String worldName, int flags, int blockX, int blockY, int blockZ,
			LCallback callback);

	/**
	 * Is it required to send changes after changing light levels.
	 * 
	 * @return true - if after changing light levels, the developer needs to
	 *         manually send the changes. false - if the server automatically sends
	 *         it after the change.
	 */
	public boolean isRequireManuallySendingChanges();

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from the arguments.
	 * 
	 * @param worldName  - World name
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - Radius in blocks (lightlevel)
	 * @return List changed chunk sections around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ,
			int lightlevel);

	/**
	 * Instant sending a full chunk to players in the world. Sends a single packet.
	 * 
	 * @param worldName - World name
	 * @param chunkX    - Chunk X coordinate
	 * @param chunkZ    - Chunk Z coordinate
	 */
	public void sendChanges(String worldName, int chunkX, int chunkZ);

	/**
	 * Instant sending a chunk with specific section to players in the world. Sends
	 * a single packet.
	 * 
	 * @param worldName     - World name
	 * @param chunkX        - Chunk X coordinate
	 * @param chunkSectionY - Chunk Y section
	 * @param chunkZ        - Chunk Z coordinate
	 */
	public void sendChanges(String worldName, int chunkX, int chunkSectionY, int chunkZ);

	/**
	 * Instant sending a chunk with with known sections mask to players in the
	 * world. Sends a single packet.
	 * 
	 * @param worldName        - World name
	 * @param chunkX           - Chunk X coordinate
	 * @param chunkZ           - Chunk Z coordinate
	 * @param sectionMaskSky   - N/A
	 * @param sectionMaskBlock - N/A
	 */
	public void sendChanges(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock);
}
