/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.bukkit.example;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.beykerykt.minecraft.lightapi.common.api.LightFlags;

public class DebugListener implements Listener {

    // testing
    private BukkitPlugin mPlugin;
    private Location prevLoc;

    public DebugListener(BukkitPlugin plugin) {
        mPlugin = plugin;
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        int lightlevel = 15;
        int flag = LightFlags.BLOCK_LIGHTING;

        if (event.getItem() == null)
            return;

        if (event.getItem().getType() == Material.STICK) {

        }
    }

    /*
	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		int lightlevel = 15;
		int flag = LightFlags.BLOCK_LIGHTING;

		if (event.getItem() == null)
			return;

		if (event.getItem().getType() == Material.STICK) {
			event.setCancelled(true);
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				List<ChunkData> list = new ArrayList<ChunkData>();
				if (mPluginImpl.getLightEngineVersion() == LightEngineVersion.V1) {
					if (prevLoc != null) {
						// remove and collect changed chunks
						int oldBlockLight = mPluginImpl.getRawLightLevel(prevLoc, flag);
						if (mPluginImpl.setLightLevel(prevLoc, flag, 0) == ResultCodes.SUCCESS) {
							List<IChunkSectionsData> chunkList = mPluginImpl.collectChunkSections(
									prevLoc.getWorld().getName(), prevLoc.getBlockX(), prevLoc.getBlockY(),
									prevLoc.getBlockZ(), oldBlockLight);
							for (int j = 0; j < chunkList.size(); j++) {
								IChunkSectionsData data = chunkList.get(j);
								if (!list.contains(data)) {
									list.add(data);
								}
							}
						}
					}
					prevLoc = event.getClickedBlock().getLocation();
					if (mPluginImpl.setLightLevel(prevLoc, flag, lightlevel) == ResultCodes.SUCCESS) {
						List<IChunkSectionsData> chunkList = mPluginImpl.collectChunkSections(
								prevLoc.getWorld().getName(), prevLoc.getBlockX(), prevLoc.getBlockY(),
								prevLoc.getBlockZ(), lightlevel);
						for (int j = 0; j < chunkList.size(); j++) {
							IChunkSectionsData data = chunkList.get(j);
							if (!list.contains(data)) {
								list.add(data);
							}
						}
					}
				} else if (mPluginImpl.getLightingEngineVersion() == LightingEngineVersion.V2) {
					if (prevLoc != null) {
						// remove and collect changed chunks
						int oldBlockLight = mPluginImpl.getRawLightLevel(prevLoc, flag);
						mPluginImpl.setRawLightLevel(prevLoc, flag, 0);
						mPluginImpl.recalculateLighting(prevLoc, flag);
						List<IChunkSectionsData> chunkList = mPluginImpl.collectChunkSections(
								prevLoc.getWorld().getName(), prevLoc.getBlockX(), prevLoc.getBlockY(),
								prevLoc.getBlockZ(), oldBlockLight);
						for (int j = 0; j < chunkList.size(); j++) {
							IChunkSectionsData data = chunkList.get(j);
							if (!list.contains(data)) {
								list.add(data);
							}
						}
					}
					prevLoc = event.getClickedBlock().getLocation();
					mPluginImpl.setRawLightLevel(prevLoc.getWorld(), prevLoc.getBlockX(), prevLoc.getBlockY(),
							prevLoc.getBlockZ(), flag, lightlevel, new LCallback() {

								@Override
								public void onSuccess(String worldName, int blockX, int blockY, int blockZ, int type,
										int lightlevel, LStage stage) {
									if (stage == LStage.WRITTING) {
										mPluginImpl.recalculateLighting(worldName, blockX, blockY, blockZ, flag,
												new LCallback() {

													@Override
													public void onSuccess(String worldName, int blockX, int blockY,
															int blockZ, int type, int lightlevel, LStage stage) {
														List<IChunkSectionsData> chunkList = mPluginImpl
																.collectChunkSections(prevLoc.getWorld().getName(),
																		prevLoc.getBlockX(), prevLoc.getBlockY(),
																		prevLoc.getBlockZ(), lightlevel);
														for (int j = 0; j < chunkList.size(); j++) {
															IChunkSectionsData data = chunkList.get(j);
															if (!list.contains(data)) {
																list.add(data);
															}
														}
													}

													@Override
													public void onFailed(String worldName, int blockX, int blockY,
															int blockZ, int type, int lightlevel, LStage stage) {
														// TODO Auto-generated method stub

													}
												});
									}
								}

								@Override
								public void onFailed(String worldName, int blockX, int blockY, int blockZ, int type,
										int lightlevel, LStage stage) {
									// TODO Auto-generated method stub

								}
							});
				}
				for (int j = 0; j < list.size(); j++) {
					IChunkSectionsData coords = list.get(j);
					mPluginImpl.sendChanges(coords);
				}
			} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				List<IChunkSectionsData> list = new ArrayList<IChunkSectionsData>();
				if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
					if (prevLoc != null) {
						// remove and collect changed chunks
						int oldBlockLight = mPluginImpl.getRawLightLevel(prevLoc, flag);
						mPluginImpl.setRawLightLevel(prevLoc, flag, 0);
						mPluginImpl.recalculateLighting(prevLoc, flag);
						List<IChunkSectionsData> chunkList = mPluginImpl.collectChunkSections(
								prevLoc.getWorld().getName(), prevLoc.getBlockX(), prevLoc.getBlockY(),
								prevLoc.getBlockZ(), oldBlockLight);
						for (int j = 0; j < chunkList.size(); j++) {
							IChunkSectionsData data = chunkList.get(j);
							if (!list.contains(data)) {
								list.add(data);
							}
						}
					}
				}
				for (int j = 0; j < list.size(); j++) {
					IChunkSectionsData coords = list.get(j);
					mPluginImpl.sendChanges(coords);
				}
			}
		}
	}
     */
}