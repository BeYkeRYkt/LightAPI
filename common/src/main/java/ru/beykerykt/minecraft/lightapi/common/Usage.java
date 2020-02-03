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

import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.callback.LStage;
import ru.beykerykt.minecraft.lightapi.common.impl.LightingEngineVersion;

class Usage {

	public void changeLight(boolean create) {
		String worldName = "test_world";
		int flags = LightFlags.COMBO_LIGHTING; // TODO: move to enum
		int blockX = 0;
		int blockY = 0;
		int blockZ = 0;
		int lightlevel = 0;
		if (create) {
			lightlevel = 15;
		}

		if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) { // pre 1.14+
			// Use createLight() only in synchronous threads.
			if (LightAPI.get().createLight(worldName, flags, blockX, blockY, blockZ, lightlevel)) {
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkSectionsData cCoords : LightAPI.get().collectChunkSections(worldName, blockX, blockY,
							blockZ, lightlevel)) {
						LightAPI.get().sendChanges(cCoords);
					}
				}
			}
		} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) { // 1.14+
			if (LightAPI.get().isAsyncLighting()) {
				// Use the callback interface, because with asynchronous flow there is no
				// guarantee that the light will be on time.
				LightAPI.get().setRawLightLevel(worldName, flags, blockX, blockY, blockZ, lightlevel, new LCallback() {

					@Override
					public void onSuccess(String worldName, int flags, int blockX, int blockY, int blockZ,
							int lightlevel, LStage stage) {
						if (LightAPI.get().isRequireRecalculateLighting()) {
							LightAPI.get().recalculateLighting(worldName, flags, blockX, blockY, blockZ,
									new LCallback() {

										@Override
										public void onSuccess(String worldName, int flags, int blockX, int blockY,
												int blockZ, int lightlevel, LStage stage) {
											if (LightAPI.get().isRequireManuallySendingChanges()) {
												for (IChunkSectionsData cCoords : LightAPI.get().collectChunkSections(
														worldName, blockX, blockY, blockZ, lightlevel)) {
													LightAPI.get().sendChanges(cCoords);
												}
											}
										}

										@Override
										public void onFailed(String worldName, int flags, int blockX, int blockY,
												int blockZ, int lightlevel, LStage stage) {
											// ah shit, here we go again
										}
									});
						} else {
							if (LightAPI.get().isRequireManuallySendingChanges()) {
								for (IChunkSectionsData cCoords : LightAPI.get().collectChunkSections(worldName, blockX,
										blockY, blockZ, lightlevel)) {
									LightAPI.get().sendChanges(cCoords);
								}
							}
						}
					}

					@Override
					public void onFailed(String worldName, int flags, int blockX, int blockY, int blockZ,
							int lightlevel, LStage stage) {
						// ah shit, here we go again
					}
				});

				return;
			}

			// Thread is synchronous, callback can be omitted.
			LightAPI.get().setRawLightLevel(worldName, flags, blockX, blockY, blockZ, lightlevel);
			if (LightAPI.get().isRequireRecalculateLighting()) {
				LightAPI.get().recalculateLighting(worldName, flags, blockX, blockY, blockZ);
			}
			if (LightAPI.get().isRequireManuallySendingChanges()) {
				for (IChunkSectionsData cCoords : LightAPI.get().collectChunkSections(worldName, blockX, blockY, blockZ,
						lightlevel)) {
					LightAPI.get().sendChanges(cCoords);
				}
			}
		}
	}

}
