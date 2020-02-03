/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.impl;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.impl.IPluginImpl;

/**
 *
 * Extended common interface for Bukkit
 *
 * @author BeYkeRYkt
 *
 */
public interface IBukkitPluginImpl extends IPluginImpl {
	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param world      - World
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean createLight(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel);

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param world      - World
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean createLight(Location location, int flags, int lightlevel);

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param world      - World
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean createLight(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback);

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param world      - World
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean createLight(Location location, int flags, int lightlevel, LCallback callback);

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param world  - World
	 * @param flags  - Light type
	 * @param blockX - Block X coordinate
	 * @param blockY - Block Y coordinate
	 * @param blockZ - Block Z coordinate
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean deleteLight(World world, int flags, int blockX, int blockY, int blockZ);

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param world  - World
	 * @param flags  - Light type
	 * @param blockX - Block X coordinate
	 * @param blockY - Block Y coordinate
	 * @param blockZ - Block Z coordinate
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean deleteLight(Location location, int flags);

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param world    - World
	 * @param flags    - Light type
	 * @param blockX   - Block X coordinate
	 * @param blockY   - Block Y coordinate
	 * @param blockZ   - Block Z coordinate
	 * @param callback - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean deleteLight(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback);

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param world    - World
	 * @param flags    - Light type
	 * @param blockX   - Block X coordinate
	 * @param blockY   - Block Y coordinate
	 * @param blockZ   - Block Z coordinate
	 * @param callback - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean deleteLight(Location location, int flags, LCallback callback);

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param world      - World
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 */
	public void setRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel);

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param world      - World
	 * @param flags      - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - ???
	 */
	public void setRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback);

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param location   - Location
	 * @param flags      - Light type
	 * @param lightlevel - light level. Default range - 0 - 15
	 */
	public void setRawLightLevel(Location location, int flags, int lightlevel);

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param location   - Location
	 * @param flags      - Light type
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - ???
	 */
	public void setRawLightLevel(Location location, int flags, int lightlevel, LCallback callback);

	/**
	 * Gets "directly" the level of light from given coordinates without additional
	 * processing.
	 * 
	 * @param world  - World
	 * @param flags  - Light type
	 * @param blockX - Block X coordinate
	 * @param blockY - Block Y coordinate
	 * @param blockZ - Block Z coordinate
	 */
	public int getRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ);

	/**
	 * Gets "directly" the level of light from given coordinates without additional
	 * processing.
	 * 
	 * @param location - Location
	 * @param flags    - Light type
	 */
	public int getRawLightLevel(Location location, int flags);

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param world  - World
	 * @param type   - Light type
	 * @param blockX - Block X coordinate
	 * @param blockY - Block Y coordinate
	 * @param blockZ - Block Z coordinate
	 */
	public void recalculateLighting(World world, int flags, int blockX, int blockY, int blockZ);

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param world    - World
	 * @param type     - Light type
	 * @param blockX   - Block X coordinate
	 * @param blockY   - Block Y coordinate
	 * @param blockZ   - Block Z coordinate
	 * @param callback - ???
	 */
	public void recalculateLighting(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback);

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param location - Location
	 * @param flags    - Light type
	 */
	public void recalculateLighting(Location location, int flags);

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param location - Location
	 * @param flags    - Light type
	 * @param callback - ???
	 */
	public void recalculateLighting(Location location, int flags, LCallback callback);

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from the arguments.
	 * 
	 * @param world      - World
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - Light level. Default range - 0 - 15
	 * @return List changed chunk sections around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(World world, int blockX, int blockY, int blockZ,
			int lightlevel);

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from block in the given
	 * coordinates.
	 * 
	 * @param world  - World
	 * @param blockX - Block X coordinate
	 * @param blockY - Block Y coordinate
	 * @param blockZ - Block Z coordinate
	 * @return List changed chunk sections around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(World world, int blockX, int blockY, int blockZ);

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from the arguments.
	 * 
	 * @param location   - Location
	 * @param lightlevel - Light level. Default range - 0 - 15
	 * @return List changed chunk sections around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(Location location, int lightlevel);

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from block in the given
	 * coordinates.
	 * 
	 * @param location - Location
	 * @return List changed chunk sections around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(Location location);

	/**
	 * Sending a full chunk to players in the world
	 * 
	 * @param world  - World
	 * @param chunkX - Chunk X coordinate
	 * @param chunkZ - Chunk Z coordinate
	 */
	public void sendChanges(World world, int chunkX, int chunkZ);
}
