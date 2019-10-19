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

	private static LightAPI singleton;
	private static IPluginImpl pluginImpl;
	private IHandlerImpl mHandler;

	private LightAPI(IPluginImpl pluginImpl, IHandlerImpl handler) {
		LightAPI.pluginImpl = pluginImpl;
		mHandler = handler;
	}

	/**
	 * Must be called in onLoad();
	 * 
	 * @param impl
	 */
	public static void prepare(IPluginImpl impl) {
		if (singleton == null && impl != null) {
			impl.log("Preparing LightAPI...");
			synchronized (LightAPI.class) {
				if (impl.getHandlerFactory() == null) {
					throw new IllegalStateException("HandlerFactory not yet initialized");
				}
				IHandlerImpl implHandler = impl.getHandlerFactory().createHandler();
				if (implHandler == null) {
					throw new IllegalStateException("ILightHandler not yet initialized");
				}
				singleton = new LightAPI(impl, implHandler);
				impl.log("Done!");
			}
		}
	}

	/**
	 * N/A
	 */
	protected static IPluginImpl getPluginImpl() {
		if (pluginImpl == null) {
			throw new IllegalStateException("IPluginImpl not yet initialized! Use prepare() !");
		}
		return pluginImpl;
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
			prepare(getPluginImpl());
		}
		return singleton;
	}

	public IHandlerImpl getHandlerImpl() {
		return get().mHandler;
	}

	public boolean isInitialized() {
		return singleton != null;
	}

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param worldName  - World name
	 * @param type       - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	@Deprecated
	public boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		if (!isInitialized()) {
			return false;
		}
		return getHandlerImpl().createLight(worldName, type, blockX, blockY, blockZ, lightlevel);
	}

	/**
	 * Placement of a certain type of light with a given level of illumination in
	 * the named world in certain coordinates with the return result.
	 * 
	 * @param worldName  - World name
	 * @param type       - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	@Deprecated
	public boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (!isInitialized()) {
			return false;
		}
		return getHandlerImpl().createLight(worldName, type, blockX, blockY, blockZ, lightlevel, callback);
	}

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param worldName - World name
	 * @param type      - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	@Deprecated
	public boolean deleteLight(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		if (!isInitialized()) {
			return false;
		}
		return getHandlerImpl().deleteLight(worldName, type, blockX, blockY, blockZ);
	}

	/**
	 * Removing a certain type of light in the named world in certain coordinates
	 * with the return result.
	 * 
	 * @param worldName - World name
	 * @param type      - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @param callback  - Callback interface
	 * @return true - if the light in the given coordinates has changed, false - if
	 *         not
	 */
	@Deprecated
	public boolean deleteLight(String worldName, LightType type, int blockX, int blockY, int blockZ,
			LCallback callback) {
		if (!isInitialized()) {
			return false;
		}
		return getHandlerImpl().deleteLight(worldName, type, blockX, blockY, blockZ, callback);
	}

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param worldName  - World name
	 * @param type       - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 */
	public void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().setRawLightLevel(worldName, type, blockX, blockY, blockZ, lightlevel);
	}

	/**
	 * Sets "directly" the level of light in given coordinates without additional
	 * processing.
	 * 
	 * @param worldName  - World name
	 * @param type       - Light type
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - light level. Default range - 0 - 15
	 * @param callback   - ???
	 */
	public void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().setRawLightLevel(worldName, type, blockX, blockY, blockZ, lightlevel, callback);
	}

	/**
	 * Gets "directly" the level of light from given coordinates without additional
	 * processing.
	 * 
	 * @param worldName - World name
	 * @param type      - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @return lightlevel - Light level. Default range - 0 - 15
	 */
	public int getRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		if (!isInitialized()) {
			return 0;
		}
		return getHandlerImpl().getRawLightLevel(worldName, type, blockX, blockY, blockZ);
	}

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param worldName - World name
	 * @param type      - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 */
	public void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().recalculateLighting(worldName, type, blockX, blockY, blockZ);
	}

	/**
	 * Performs re-illumination of the light in the given coordinates.
	 * 
	 * @param worldName - World name
	 * @param type      - Light type
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @param callback  - ???
	 */
	public void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ,
			LCallback callback) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().recalculateLighting(worldName, type, blockX, blockY, blockZ, callback);
	}

	/**
	 * Platform that is being used
	 * 
	 * @return One of the proposed options from {@link ImplementationPlatform}
	 */
	public ImplementationPlatform getImplementationPlatform() {
		if (!isInitialized()) {
			return ImplementationPlatform.UNKNOWN;
		}
		return getPluginImpl().getImplPlatform();
	}

	/**
	 * Used lighting engine version.
	 * 
	 * @return One of the proposed options from {@link LightingEngineVersion}
	 */
	public LightingEngineVersion getLightingEngineVersion() {
		if (!isInitialized()) {
			return LightingEngineVersion.UNKNOWN;
		}
		return getHandlerImpl().getLightingEngineVersion();
	}

	/**
	 * Does the calculation of the lighting in a separate thread.
	 *
	 * @return true - if the lighting calculation occurs in a separate thread, false
	 *         - if in main thread.
	 */
	public boolean isAsyncLighting() {
		if (!isInitialized()) {
			return false;
		}
		return getHandlerImpl().isAsyncLighting();
	}

	/**
	 * Is it required to send changes after changing light levels.
	 * 
	 * @return true - if after changing light levels, the developer needs to
	 *         manually send the changes. false - if the server automatically sends
	 *         it after the change.
	 */
	public boolean isRequireManuallySendingChanges() {
		if (!isInitialized()) {
			return false;
		}
		return getHandlerImpl().isRequireManuallySendingChanges();
	}

	/**
	 * Collects modified сhunks around a given coordinate in the radius of the light
	 * level. The light level is taken from the arguments.
	 * 
	 * @param worldName  - World name
	 * @param blockX     - Block X coordinate
	 * @param blockY     - Block Y coordinate
	 * @param blockZ     - Block Z coordinate
	 * @param lightlevel - Light level. Default range - 0 - 15
	 * @return List changed chunks around the given coordinate.
	 */
	public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ, int lightlevel) {
		if (!isInitialized()) {
			return null;
		}
		return getHandlerImpl().collectChunks(worldName, blockX, blockY, blockZ, lightlevel);
	}

	/**
	 * Collects modified сhunks around a given coordinate in the radius of the light
	 * level. The light level is taken from block in the given coordinates.
	 * 
	 * @param worldName - World name
	 * @param blockX    - Block X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param blockZ    - Block Z coordinate
	 * @return List changed chunks around the given coordinate.
	 */
	public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ) {
		if (!isInitialized()) {
			return null;
		}
		return getHandlerImpl().collectChunks(worldName, blockX, blockY, blockZ);
	}

	/**
	 * Sending changes to world
	 * 
	 * @param worldName - World name
	 * @param chunkX    - Chunk X coordinate
	 * @param chunkZ    - Chunk Z coordinate
	 */
	public void sendChanges(String worldName, int chunkX, int chunkZ) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().sendChanges(worldName, chunkX, chunkZ);
	}

	/**
	 * Sending changes to world
	 * 
	 * @param worldName - World name
	 * @param chunkX    - Chunk X coordinate
	 * @param blockY    - Block Y coordinate
	 * @param chunkZ    - Chunk Z coordinate
	 */
	public void sendChanges(String worldName, int chunkX, int blockY, int chunkZ) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().sendChanges(worldName, chunkX, blockY, chunkZ);
	}

	/**
	 * Sending changes to world
	 * 
	 * @param chunkData - {@link IChunkData}
	 */
	public void sendChanges(IChunkData chunkData) {
		if (!isInitialized()) {
			return;
		}
		getHandlerImpl().sendChanges(chunkData);
	}
}
