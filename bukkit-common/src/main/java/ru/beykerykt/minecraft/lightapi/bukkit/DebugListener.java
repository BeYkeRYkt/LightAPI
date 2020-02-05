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
package ru.beykerykt.minecraft.lightapi.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import ru.beykerykt.minecraft.lightapi.bukkit.impl.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.impl.IBukkitPluginImpl;
import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.callback.LStage;
import ru.beykerykt.minecraft.lightapi.common.impl.ImplementationPlatform;
import ru.beykerykt.minecraft.lightapi.common.impl.LightingEngineVersion;

public class DebugListener implements Listener {

	// testing
	private Location prevLoc;

	// lightapi_extented
	private IBukkitPluginImpl mPluginImpl;

	private boolean disableHardBench;

	public DebugListener(BukkitPlugin plugin) {
		if (LightAPI.get().getImplementationPlatform() == ImplementationPlatform.CRAFTBUKKIT
				|| LightAPI.get().getImplementationPlatform() == ImplementationPlatform.BUKKIT) {
			mPluginImpl = (IBukkitPluginImpl) LightAPI.get().getPluginImpl();
		}
	}

	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		int lightlevel = 15;
		int flag = LightFlags.BLOCK_LIGHTING;

		if (event.getItem() == null)
			return;

		if (event.getItem().getType() == Material.STICK) {
			event.setCancelled(true);
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				List<IChunkSectionsData> list = new ArrayList<IChunkSectionsData>();
				if (mPluginImpl.getLightingEngineVersion() == LightingEngineVersion.V1) {
					if (prevLoc != null) {
						// remove and collect changed chunks
						int oldBlockLight = mPluginImpl.getRawLightLevel(prevLoc, flag);
						if (mPluginImpl.deleteLight(prevLoc, flag)) {
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
					if (mPluginImpl.createLight(prevLoc, flag, lightlevel)) {
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
					mPluginImpl.setRawLightLevel(prevLoc.getWorld(), flag, prevLoc.getBlockX(), prevLoc.getBlockY(),
							prevLoc.getBlockZ(), lightlevel, new LCallback() {

								@Override
								public void onSuccess(String worldName, int type, int blockX, int blockY, int blockZ,
										int lightlevel, LStage stage) {
									if (stage == LStage.WRITTING) {
										mPluginImpl.recalculateLighting(worldName, flag, blockX, blockY, blockZ,
												new LCallback() {

													@Override
													public void onSuccess(String worldName, int type, int blockX,
															int blockY, int blockZ, int lightlevel, LStage stage) {
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
													public void onFailed(String worldName, int type, int blockX,
															int blockY, int blockZ, int lightlevel, LStage stage) {
														// TODO Auto-generated method stub

													}
												});
									}
								}

								@Override
								public void onFailed(String worldName, int type, int blockX, int blockY, int blockZ,
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
				if (mPluginImpl.getLightingEngineVersion() == LightingEngineVersion.V2) {
					if (prevLoc == null) {
						prevLoc = event.getClickedBlock().getLocation();
						// LightBulb bulb = new LightBulb(id, world, blockX, blockY, blockZ);
						// bulb.setTickable(true);
						// bulb.setLightLevel(15);
						// bulb.addFlag(LightFlags.COMBO_LIGHTING);
						// bulb.createLight();
						// bulb.markForRecalculate();
						// Bukkit.broadcastMessage("flags: " + bulb.getFlags());
					} else {
						// LightBulb bulb =
						// LightAPI.get().getLightManager().getLightBulbFromCoods(prevLoc.getBlockX(),
						// prevLoc.getBlockY(), prevLoc.getBlockZ());
						// bulb.deleteLight();
						// prevLoc = event.getClickedBlock().getLocation();
						// bulb.moveToNewPosition(prevLoc.getBlockX(), prevLoc.getBlockY(),
						// prevLoc.getBlockZ());
						// bulb.createLight();
						// bulb.markForRecalculate();
					}
				}
			}
		}
	}

	/**
	 * Benchmark
	 * 
	 * @param loc - world location
	 */
	private void cycleRecreate(Location loc) {
		int oldBlockLight = 15;
		int flag = LightFlags.COMBO_LIGHTING;

		///////////////////////////////////////////
		long time_start = System.currentTimeMillis();
		for (int i = 0; i < 99; i++) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0);
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
		}
		if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
			LightAPI.get().createLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
		} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
			LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ());
		}
		long time_end = System.currentTimeMillis() - time_start;
		mPluginImpl.log("< #1 cycle raw + once recalc: " + time_end);
		LightAPI.get().deleteLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
				loc.getBlockZ());

		///////////////////////////////////////////
		time_start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0);
			if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
				LightAPI.get().createLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ(), oldBlockLight);
			} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
				// LightAPI.get().createLight(loc.getWorld().getName(), flag, loc.getBlockX(),
				// loc.getBlockY(),
				// loc.getBlockZ(), oldBlockLight);
				LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ(), oldBlockLight);
				LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ());
			}
		}
		time_end = System.currentTimeMillis() - time_start;
		mPluginImpl.log("< #2 cycle raw + cycle recalc: " + time_end);
		LightAPI.get().deleteLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
				loc.getBlockZ());

		///////////////////////////////////////////
		if (!disableHardBench) {
			try {
				time_start = System.currentTimeMillis();
				for (int i = 0; i < 100; i++) {
					if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
						LightAPI.get().deleteLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
								loc.getBlockZ());
						LightAPI.get().createLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
								loc.getBlockZ(), oldBlockLight);
					} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
						LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ(), 0);
						LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ());
						LightAPI.get().setRawLightLevel(loc.getWorld().getName(), flag, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ(), oldBlockLight);
						LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ());
					}
				}
				time_end = System.currentTimeMillis() - time_start;
				mPluginImpl.log("< #3 cycle raw + cycle recalc: " + time_end);
				LightAPI.get().deleteLight(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ());
				LightAPI.get().recalculateLighting(loc.getWorld().getName(), flag, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ());
			} catch (Exception ex) {
				ex.printStackTrace();
				disableHardBench = true;
			}
		}
	}

	private boolean removeLight(Location loc) {
		LightAPI api = LightAPI.get();
		List<IChunkSectionsData> moddedChunks = new ArrayList<IChunkSectionsData>();
		int oldBlockLight = api.getRawLightLevel(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(),
				loc.getBlockY(), loc.getBlockZ());

		if (api.getLightingEngineVersion() == LightingEngineVersion.V2) {
			if (!api.isAsyncLighting()) { // sync
				api.setRawLightLevel(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), 0);

				if (api.isRequireRecalculateLighting()) {
					api.recalculateLighting(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ());
				}

				if (api.isRequireManuallySendingChanges()) {
					List<IChunkSectionsData> datas = api.collectChunkSections(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), oldBlockLight);
					for (int i = 0; i < datas.size(); i++) {
						IChunkSectionsData chunkData = datas.get(i);
						if (!moddedChunks.contains(chunkData)) {
							moddedChunks.add(chunkData);
						}
					}
				}

				for (int i = 0; i < moddedChunks.size(); i++) {
					IChunkSectionsData chunkData = moddedChunks.get(i);
					api.sendChanges(chunkData);
				}
				moddedChunks.clear();
				return true;
			}

			api.setRawLightLevel(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0, new LCallback() {

						@Override
						public void onSuccess(String worldName, int flags, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage) {
							api.recalculateLighting(worldName, flags, blockX, blockY, blockZ, new LCallback() {

								@Override
								public void onSuccess(String worldName, int flags, int blockX, int blockY, int blockZ,
										int lightlevel, LStage stage) {
									if (api.isRequireManuallySendingChanges()) {
										for (IChunkSectionsData data : api.collectChunkSections(
												loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(),
												loc.getBlockZ(), oldBlockLight)) {
											if (!moddedChunks.contains(data)) {
												moddedChunks.add(data);
											}
										}

										// send changes
										for (IChunkSectionsData data : moddedChunks) {
											api.sendChanges(data);
										}
									} else {
										System.out.println(
												"Seems with sending changes to take care of the server itself. Do not send anything.");
									}
								}

								@Override
								public void onFailed(String worldName, int flags, int blockX, int blockY, int blockZ,
										int lightlevel, LStage stage) {
									System.out.println("remove-" + stage.name() + ": Ah shit, here we go again");
								}
							});
						}

						@Override
						public void onFailed(String worldName, int flags, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage) {
							System.out.println("remove-" + stage.name() + ": Ah shit, here we go again");
						}
					});
			moddedChunks.clear();
			return true;
		} else if (api.getLightingEngineVersion() == LightingEngineVersion.V1) {
			// delete and collect changed chunks
			if (api.deleteLight(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ())) {
				if (api.isRequireManuallySendingChanges()) {
					for (IChunkSectionsData data : api.collectChunkSections(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), oldBlockLight)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}

					// send changes
					for (IChunkSectionsData data : moddedChunks) {
						api.sendChanges(data);
					}
				} else {
					System.out.println(
							"Seems with sending changes to take care of the server itself. Do not send anything.");
				}
			} else {
				System.out.println("The light level has not changed. Check your arguments.");
				return false;
			}
			moddedChunks.clear();
			return true;
		}
		return false;
	}

	private boolean createLight(Location loc, int lightlevel) {
		LightAPI api = LightAPI.get();
		List<IChunkSectionsData> moddedChunks = new ArrayList<IChunkSectionsData>();

		if (api.getLightingEngineVersion() == LightingEngineVersion.V2) {
			if (!api.isAsyncLighting()) { // sync
				api.setRawLightLevel(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), lightlevel);

				if (api.isRequireRecalculateLighting()) {
					api.recalculateLighting(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ());
				}

				if (api.isRequireManuallySendingChanges()) {
					List<IChunkSectionsData> datas = api.collectChunkSections(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), lightlevel);
					for (int i = 0; i < datas.size(); i++) {
						IChunkSectionsData chunkData = datas.get(i);
						if (!moddedChunks.contains(chunkData)) {
							moddedChunks.add(chunkData);
						}
					}
				}

				for (int i = 0; i < moddedChunks.size(); i++) {
					IChunkSectionsData chunkData = moddedChunks.get(i);
					api.sendChanges(chunkData);
				}
				moddedChunks.clear();
				return true;
			}

			api.setRawLightLevel(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), lightlevel, new LCallback() {

						@Override
						public void onSuccess(String worldName, int flags, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage) {
							api.recalculateLighting(worldName, flags, blockX, blockY, blockZ, new LCallback() {

								@Override
								public void onSuccess(String worldName, int type, int blockX, int blockY, int blockZ,
										int lightlevel, LStage stage) {
									if (api.isRequireManuallySendingChanges()) {
										for (IChunkSectionsData data : api.collectChunkSections(worldName, blockX,
												blockY, blockZ, lightlevel)) {
											if (!moddedChunks.contains(data)) {
												moddedChunks.add(data);
											}
										}

										// send changes
										for (IChunkSectionsData data : moddedChunks) {
											api.sendChanges(data);
										}
									}
								}

								@Override
								public void onFailed(String worldName, int flags, int blockX, int blockY, int blockZ,
										int lightlevel, LStage stage) {
									System.out.println("create-" + stage.name() + ": Ah shit, here we go again");
								}
							});
						}

						@Override
						public void onFailed(String world, int flags, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage) {
							System.out.println("create-" + stage.name() + ": Ah shit, here we go again");
						}
					});
			moddedChunks.clear();
			return true;
		} else if (api.getLightingEngineVersion() == LightingEngineVersion.V1) {
			// create and collect changed chunks
			if (api.createLight(loc.getWorld().getName(), LightFlags.BLOCK_LIGHTING, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), lightlevel)) {
				if (api.isRequireManuallySendingChanges()) {
					for (IChunkSectionsData data : api.collectChunkSections(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), lightlevel)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}

					// send changes
					for (IChunkSectionsData data : moddedChunks) {
						api.sendChanges(data);
					}
				} else {
					System.out.println(
							"Seems with sending changes to take care of the server itself. Do not send anything.");
				}
			} else {
				System.out.println("The light level has not changed. Check your arguments.");
				return false;
			}
			moddedChunks.clear();
			return true;
		}
		return false;
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player p = event.getPlayer();
		if (event.getMessage().equals("light")) {
			p.sendMessage("At player location");
			p.sendMessage("Block state | light level: " + p.getLocation().getBlock().getState().getLightLevel());
			p.sendMessage("Block | light level: " + p.getLocation().getBlock().getLightLevel());
			p.sendMessage("Block | light from blocks: " + p.getLocation().getBlock().getLightFromBlocks());
			p.sendMessage("Block | light from sky: " + p.getLocation().getBlock().getLightFromSky());
			event.setCancelled(true);
		} else if (event.getMessage().equals("create")) {
			if (createLight(p.getLocation(), 15)) {
				p.sendMessage("Light placed!");
				event.setCancelled(true);
			}
		} else if (event.getMessage().equals("delete")) {
			if (removeLight(p.getLocation())) {
				p.sendMessage("Light deleted!");
				event.setCancelled(true);
			}
		} else if (event.getMessage().equals("bench")) {
			cycleRecreate(p.getLocation());
			event.setCancelled(true);
		}
	}

}
