/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
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

import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.impl.IPluginImpl;
import ru.beykerykt.minecraft.lightapi.common.impl.ImplementationPlatform;
import ru.beykerykt.minecraft.lightapi.common.impl.LightingEngineVersion;

/**
 * Main class for all platforms. Contains basic methods for all implementations.
 * 
 * @author BeYkeRYkt
 *
 */
public class LightAPI {

	private static LightAPI singleton;

	private IPluginImpl mPluginImpl;

	private LightAPI(IPluginImpl pluginImpl) {
		mPluginImpl = pluginImpl;
	}

	/**
	 * Must be called in onEnable();
	 * 
	 * @param impl
	 */
	public static void prepare(IPluginImpl impl) {
		if (singleton == null && impl != null) {
			impl.log("Preparing LightAPI...");
			synchronized (LightAPI.class) {
				if (impl.getHandlerImpl() == null) {
					throw new IllegalStateException("HandlerImpl not yet initialized");
				}
				singleton = new LightAPI(impl);
				impl.log("Preparing done!");
			}
		}
	}

	/**
	 * N/A
	 */
	public IPluginImpl getPluginImpl() {
		if (get().mPluginImpl == null) {
			throw new IllegalStateException("IPluginImpl not yet initialized! Use prepare() !");
		}
		return get().mPluginImpl;
	}

	/**
	 * N/A
	 */
	protected void log(String msg) {
		getPluginImpl().log(msg);
	}

	/**
	 * The global {@link LightAPI} instance.
	 */
	public static LightAPI get() {
		if (singleton == null) {
			throw new IllegalStateException("Singleton not yet initialized! Use prepare() !");
		}
		return singleton;
	}

	/**
	 * Platform that is being used
	 * 
	 * @return One of the proposed options from {@link ImplementationPlatform}
	 */
	public ImplementationPlatform getImplementationPlatform() {
		if (singleton == null) {
			return ImplementationPlatform.UNKNOWN;
		}
		return getPluginImpl().getImplPlatform();
	}

	/// Handler common methods ///
	/**
	 * Used lighting engine version.
	 * 
	 * @return One of the proposed options from {@link LightingEngineVersion}
	 */
	public LightingEngineVersion getLightingEngineVersion() {
		return getPluginImpl().getLightingEngineVersion();
	}

	/**
	 * N/A
	 * 
	 * @return
	 */
	public boolean isAsyncLighting() {
		return getPluginImpl().isAsyncLighting();
	}

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
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean createLight(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel) {
		return getPluginImpl().createLight(worldName, flags, blockX, blockY, blockZ, lightlevel);
	}

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
			LCallback callback) {
		return getPluginImpl().createLight(worldName, flags, blockX, blockY, blockZ, lightlevel, callback);
	}

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param worldName - World name
	 * @param flags     - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	public boolean deleteLight(String worldName, int flags, int blockX, int blockY, int blockZ) {
		return getPluginImpl().deleteLight(worldName, flags, blockX, blockY, blockZ);
	}

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
	public boolean deleteLight(String worldName, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		return getPluginImpl().deleteLight(worldName, flags, blockX, blockY, blockZ, callback);
	}

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
	 */
	public void setRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel) {
		getPluginImpl().setRawLightLevel(worldName, flags, blockX, blockY, blockZ, lightlevel);
	}

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
	 * @param callback   - ???
	 */
	public void setRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		getPluginImpl().setRawLightLevel(worldName, flags, blockX, blockY, blockZ, lightlevel, callback);
	}

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
	public int getRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ) {
		return getPluginImpl().getRawLightLevel(worldName, flags, blockX, blockY, blockZ);
	}

	/**
	 * N/A
	 * 
	 * @return
	 */
	public boolean isRequireRecalculateLighting() {
		return getPluginImpl().isRequireRecalculateLighting();
	}

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param worldName - World name
	 * @param flags     - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @param callback  - ???
	 */
	public void recalculateLighting(String worldName, int flags, int blockX, int blockY, int blockZ) {
		getPluginImpl().recalculateLighting(worldName, flags, blockX, blockY, blockZ);
	}

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param worldName - World name
	 * @param flags     - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @param callback  - ???
	 */
	public void recalculateLighting(String worldName, int flags, int blockX, int blockY, int blockZ,
			LCallback callback) {
		getPluginImpl().recalculateLighting(worldName, flags, blockX, blockY, blockZ, callback);
	}

	/**
	 * Is it required to send changes after changing light levels.
	 * 
	 * @return true - if after changing light levels, the developer needs to
	 *         manually send the changes. false - if the server automatically sends
	 *         it after the change.
	 */
	public boolean isRequireManuallySendingChanges() {
		return getPluginImpl().isRequireManuallySendingChanges();
	}

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from the arguments.
	 * 
	 * @param worldName  - World name
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - Radius in blocks (lightlevel)
	 * @return List changed chunks around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ,
			int lightlevel) {
		return getPluginImpl().collectChunkSections(worldName, blockX, blockY, blockZ, lightlevel);
	}

	/**
	 * Collects modified сhunks with sections around a given coordinate in the
	 * radius of the light level. The light level is taken from block in the given
	 * coordinates.
	 * 
	 * @param worldName - World name
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @return List changed chunks around the given coordinate.
	 */
	public List<IChunkSectionsData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ) {
		return getPluginImpl().collectChunkSections(worldName, blockX, blockY, blockZ);
	}

	/**
	 * Instant sending a full chunk to players in the world. Sends a single packet.
	 * 
	 * @param worldName - World name
	 * @param chunkX    - Chunk X coordinate
	 * @param chunkZ    - Chunk Z coordinate
	 */
	public void sendChanges(String worldName, int chunkX, int chunkZ) {
		getPluginImpl().sendChanges(worldName, chunkX, chunkZ);
	}

	/**
	 * Instant sending a chunk to players in the world. Sends a single packet.
	 * 
	 * @param chunkData - {@link IChunkSectionsData}
	 */
	public void sendChanges(IChunkSectionsData chunkData) {
		getPluginImpl().sendChanges(chunkData);
	}
}
