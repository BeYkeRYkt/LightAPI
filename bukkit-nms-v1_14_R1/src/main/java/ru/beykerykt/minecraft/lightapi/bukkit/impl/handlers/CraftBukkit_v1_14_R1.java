/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
 * Copyright (c) 2019 Qveshn
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
package ru.beykerykt.minecraft.lightapi.bukkit.impl.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EnumSkyBlock;
import net.minecraft.server.v1_14_R1.LightEngineBlock;
import net.minecraft.server.v1_14_R1.LightEngineGraph;
import net.minecraft.server.v1_14_R1.LightEngineLayer;
import net.minecraft.server.v1_14_R1.LightEngineSky;
import net.minecraft.server.v1_14_R1.LightEngineStorage;
import net.minecraft.server.v1_14_R1.LightEngineThreaded;
import net.minecraft.server.v1_14_R1.PacketPlayOutLightUpdate;
import net.minecraft.server.v1_14_R1.SectionPosition;
import net.minecraft.server.v1_14_R1.ThreadedMailbox;
import net.minecraft.server.v1_14_R1.WorldServer;
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.bukkit.impl.BukkitHandlerImpl;
import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.callback.LStage;
import ru.beykerykt.minecraft.lightapi.common.impl.LightingEngineVersion;

public class CraftBukkit_v1_14_R1 extends BukkitHandlerImpl {

	// Qveshn - start
	private Field lightEngine_ThreadedMailbox;
	private Field threadedMailbox_State;
	private Method threadedMailbox_DoLoopStep;
	private Field lightEngineLayer_c;
	private Method lightEngineStorage_c;
	private Method lightEngineGraph_a;

	public CraftBukkit_v1_14_R1() {
		try {
			threadedMailbox_DoLoopStep = ThreadedMailbox.class.getDeclaredMethod("f");
			threadedMailbox_DoLoopStep.setAccessible(true);
			threadedMailbox_State = ThreadedMailbox.class.getDeclaredField("c");
			threadedMailbox_State.setAccessible(true);
			lightEngine_ThreadedMailbox = LightEngineThreaded.class.getDeclaredField("b");
			lightEngine_ThreadedMailbox.setAccessible(true);

			lightEngineLayer_c = LightEngineLayer.class.getDeclaredField("c");
			lightEngineLayer_c.setAccessible(true);
			lightEngineStorage_c = LightEngineStorage.class.getDeclaredMethod("c");
			lightEngineStorage_c.setAccessible(true);
			lightEngineGraph_a = LightEngineGraph.class.getDeclaredMethod("a", long.class, long.class, int.class,
					boolean.class);
			lightEngineGraph_a.setAccessible(true);
		} catch (Exception e) {
			throw toRuntimeException(e);
		}
	}

	private int getDeltaLight(int x, int dx) {
		return (((x ^ ((-dx >> 4) & 15)) + 1) & (-(dx & 1)));
	}

	public int asSectionMask(int sectionY) {
		return 1 << sectionY + 1;
	}

	public boolean isValidSectionY(int sectionY) {
		return sectionY >= -1 && sectionY <= 16;
	}

	private static RuntimeException toRuntimeException(Throwable e) {
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		}
		Class<? extends Throwable> cls = e.getClass();
		return new RuntimeException(String.format("(%s) %s",
				RuntimeException.class.getPackage().equals(cls.getPackage()) ? cls.getSimpleName() : cls.getName(),
				e.getMessage()), e);
	}

	@SuppressWarnings({ "unchecked" })
	private void executeSync(LightEngineThreaded lightEngine, Runnable task) {
		try {
			// ##### STEP 1: Pause light engine mailbox to process its tasks. #####
			ThreadedMailbox<Runnable> threadedMailbox = (ThreadedMailbox<Runnable>) lightEngine_ThreadedMailbox
					.get(lightEngine);
			// State flags bit mask:
			// 0x0001 - Closing flag (ThreadedMailbox is closing if non zero).
			// 0x0002 - Busy flag (ThreadedMailbox performs a task from queue if non zero).
			AtomicInteger stateFlags = (AtomicInteger) threadedMailbox_State.get(threadedMailbox);
			int flags; // to hold values from stateFlags
			long timeToWait = -1;
			// Trying to set bit 1 in state bit mask when it is not set yet.
			// This will break the loop in other thread where light engine mailbox processes
			// the taks.
			while (!stateFlags.compareAndSet(flags = stateFlags.get() & ~2, flags | 2)) {
				if ((flags & 1) != 0) {
					// ThreadedMailbox is closing. The light engine mailbox may also stop processing
					// tasks.
					// The light engine mailbox can be close due to server shutdown or unloading
					// (closing) the world.
					// I am not sure is it unsafe to process our tasks while the world is closing is
					// closing,
					// but will try it (one can throw exception here if it crashes the server).
					if (timeToWait == -1) {
						// Try to wait 3 seconds until light engine mailbox is busy.
						timeToWait = System.currentTimeMillis() + 3 * 1000;
						LightAPI.get().getPluginImpl().log("ThreadedMailbox is closing. Will wait...");
					} else if (System.currentTimeMillis() >= timeToWait) {
						throw new RuntimeException("Failed to enter critical section while ThreadedMailbox is closing");
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException ignored) {
					}
				}
			}
			try {
				// ##### STEP 2: Safely running the task while the mailbox process is stopped.
				// #####
				task.run();
			} finally {
				// STEP 3: ##### Continue light engine mailbox to process its tasks. #####
				// Firstly: Clearing busy flag to allow ThreadedMailbox to use it for running
				// light engine tasks.
				while (!stateFlags.compareAndSet(flags = stateFlags.get(), flags & ~2))
					;
				// Secondly: IMPORTANT! The main loop of ThreadedMailbox was broken. Not
				// completed tasks may still be
				// in the queue. Therefore, it is important to start the loop again to process
				// tasks from the queue.
				// Otherwise, the main server thread may be frozen due to tasks stuck in the
				// queue.
				threadedMailbox_DoLoopStep.invoke(threadedMailbox);
			}
		} catch (InvocationTargetException e) {
			throw toRuntimeException(e.getCause());
		} catch (IllegalAccessException e) {
			throw toRuntimeException(e);
		}
	}

	private void lightEngineLayer_a(LightEngineLayer<?, ?> les, BlockPosition var0, int var1) {
		try {
			LightEngineStorage<?> ls = (LightEngineStorage<?>) lightEngineLayer_c.get(les);
			lightEngineStorage_c.invoke(ls);
			lightEngineGraph_a.invoke(les, 9223372036854775807L, var0.asLong(), 15 - var1, true);
		} catch (InvocationTargetException e) {
			throw toRuntimeException(e.getCause());
		} catch (IllegalAccessException e) {
			throw toRuntimeException(e);
		}
	}
	// Qveshn - end

	private int distanceTo(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.getPos().x - from.getPos().x;
		double var4 = to.getPos().z - from.getPos().z;
		return (int) Math.sqrt(var2 * var2 + var4 * var4);
	}

	public int parseViewDistance(EntityPlayer player) {
		int viewDistance = Bukkit.getViewDistance();
		try {
			int playerViewDistance = Integer.valueOf(player.clientViewDistance);
			if (playerViewDistance < viewDistance) {
				viewDistance = playerViewDistance;
			}
		} catch (Exception e) {
			// silent
		}
		return viewDistance;
	}

	/***********************************************************************************************************************/
	@Override
	public boolean createLight(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
			}
			return false;
		}

		int oldlightlevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);

		setRawLightLevel(world, flags, blockX, blockY, blockZ, lightlevel, callback);
		recalculateLighting(world, flags, blockX, blockY, blockZ, callback);

		// check light
		int newLightLevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		if (newLightLevel >= oldlightlevel) {
			if (callback != null) {
				callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
			}
			return true;
		}

		if (callback != null) {
			callback.onFailed(world.getName(), flags, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
		}
		return false;
	}

	@Override
	public boolean deleteLight(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, 0, LStage.DELETING);
			}
			return false;
		}

		int oldlightlevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		setRawLightLevel(world, flags, blockX, blockY, blockZ, 0, callback);
		recalculateLighting(world, flags, blockX, blockY, blockZ, callback);

		// check light
		int newLightLevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		if (newLightLevel != oldlightlevel) {
			if (callback != null) {
				callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, newLightLevel, LStage.DELETING);
			}
			return true;
		}

		if (callback != null) {
			callback.onFailed(world.getName(), flags, blockX, blockY, blockZ, newLightLevel, LStage.DELETING);
		}
		return false;
	}

	@Override
	public LightingEngineVersion getLightingEngineVersion() {
		return LightingEngineVersion.V2;
	}

	@Override
	public boolean isAsyncLighting() {
		return false;
	}

	@Override
	public void setRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, lightlevel, LStage.WRITTING);
			}
			return;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		final BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();
		final int finalLightLevel = lightlevel < 0 ? 0 : lightlevel > 15 ? 15 : lightlevel;

		executeSync(lightEngine, () -> {
			// block lighting
			if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
				LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
				if (finalLightLevel == 0) {
					leb.a(position);
				} else if (leb.a(SectionPosition.a(position)) != null) {
					try {
						leb.a(position, finalLightLevel);
					} catch (NullPointerException ignore) {
						// To prevent problems with the absence of the NibbleArray, even
						// if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
					}
				}
			}

			// sky lighting
			if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
				LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
				if (finalLightLevel == 0) {
					les.a(position);
				} else if (les.a(SectionPosition.a(position)) != null) {
					try {
						lightEngineLayer_a(les, position, finalLightLevel);
					} catch (NullPointerException ignore) {
						// To prevent problems with the absence of the NibbleArray, even
						// if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
					}
				}
			}

			// combo
			if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
				LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
				if (finalLightLevel == 0) {
					leb.a(position);
				} else if (leb.a(SectionPosition.a(position)) != null) {
					try {
						leb.a(position, finalLightLevel);
					} catch (NullPointerException ignore) {
						// To prevent problems with the absence of the NibbleArray, even
						// if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
					}
				}

				LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
				if (finalLightLevel == 0) {
					les.a(position);
				} else if (les.a(SectionPosition.a(position)) != null) {
					try {
						lightEngineLayer_a(les, position, finalLightLevel);
					} catch (NullPointerException ignore) {
						// To prevent problems with the absence of the NibbleArray, even
						// if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
					}
				}
			}
		});

		if (callback != null) {
			callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, finalLightLevel, LStage.WRITTING);
		}
	}

	@Override
	public int getRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ) {
		int lightlevel = -1;
		if (world == null) {
			return lightlevel;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
			lightlevel = worldServer.getBrightness(EnumSkyBlock.BLOCK, position);
		} else if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
			lightlevel = worldServer.getBrightness(EnumSkyBlock.SKY, position);
		} else if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
			lightlevel = worldServer.getLightLevel(position);
		}
		return lightlevel;
	}

	@Override
	public boolean isRequireRecalculateLighting() {
		return true;
	}

	@Override
	public void recalculateLighting(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, 0, LStage.RECALCULATING);
			}
			return;
		}

		WorldServer worldServer = ((CraftWorld) world).getHandle();
		final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

		// Do not recalculate if no changes!
		if (!lightEngine.a()) {
			if (callback != null) {
				int lightlevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
				callback.onFailed(world.getName(), flags, blockX, blockY, blockZ, lightlevel, LStage.RECALCULATING);
			}
			return;
		}

		executeSync(lightEngine, () -> {
			// block lighting
			if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
				LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
				leb.a(Integer.MAX_VALUE, true, true);
			}

			// sky lighting
			if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
				LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
				les.a(Integer.MAX_VALUE, true, true);
			}

			// combo
			if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
				LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
				LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);

				// nms
				int maxUpdateCount = Integer.MAX_VALUE;
				int integer4 = maxUpdateCount / 2;
				int integer5 = leb.a(integer4, true, true);
				int integer6 = maxUpdateCount - integer4 + integer5;
				int integer7 = les.a(integer6, true, true);
				if (integer5 == 0 && integer7 > 0) {
					leb.a(integer7, true, true);
				}
			}
		});

		if (callback != null) {
			int lightlevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
			callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, lightlevel, LStage.RECALCULATING);
		}
	}

	@Override
	public boolean isRequireManuallySendingChanges() {
		return true;
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(World world, int blockX, int blockY, int blockZ,
			int lightlevel) {
		List<IChunkSectionsData> list = new ArrayList<IChunkSectionsData>();
		int finalLightLevel = lightlevel;

		if (world == null) {
			return list;
		}

		if (lightlevel < 0) {
			finalLightLevel = 0;
		} else if (lightlevel > 15) {
			finalLightLevel = 15;
		}

		for (int dX = -1; dX <= 1; dX++) {
			int lightLevelX = finalLightLevel - getDeltaLight(blockX & 15, dX);
			if (lightLevelX > 0) {
				for (int dZ = -1; dZ <= 1; dZ++) {
					int lightLevelZ = lightLevelX - getDeltaLight(blockZ & 15, dZ);
					if (lightLevelZ > 0) {
						int chunkX = blockX >> 4;
						int chunkZ = blockZ >> 4;
						int sectionMask = 0;
						for (int dY = -1; dY <= 1; dY++) {
							if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
								int sectionY = (blockY >> 4) + dY;
								if (isValidSectionY(sectionY)) {
									sectionMask |= asSectionMask(sectionY);
								}
							}
						}

						// don't add null section mask
						if (sectionMask > 0) {
							IChunkSectionsData chunkData = new BukkitChunkSectionsData(world, chunkX + dX, chunkZ + dZ,
									sectionMask, sectionMask);
							if (!list.contains(chunkData)) {
								list.add(chunkData);
							}
						}
					}
				}
			}
		}
		return list;
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ) {
		if (world == null) {
			return;
		}
		for (int i = 0; i < world.getPlayers().size(); i++) {
			Player player = world.getPlayers().get(i);
			Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
			EntityPlayer human = ((CraftPlayer) player).getHandle();
			Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
			int playerViewDistance = parseViewDistance(human);

			if (distanceTo(pChunk, chunk) <= playerViewDistance) {
				PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e());
				human.playerConnection.sendPacket(packet);
			}
		}
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkSectionY, int chunkZ) {
		if (world == null) {
			return;
		}
		for (int i = 0; i < world.getPlayers().size(); i++) {
			Player player = world.getPlayers().get(i);
			Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
			EntityPlayer human = ((CraftPlayer) player).getHandle();
			Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
			int playerViewDistance = parseViewDistance(human);

			if (distanceTo(pChunk, chunk) <= playerViewDistance) {
				// https://wiki.vg/index.php?title=Pre-release_protocol&oldid=14804#Update_Light
				// https://github.com/flori-schwa/VarLight/blob/b9349499f9c9fb995c320f95eae9698dd85aad5c/v1_14_R1/src/me/florian/varlight/nms/v1_14_R1/NmsAdapter_1_14_R1.java#L451
				//
				// Two last argument is bit-mask what chunk sections to update. Mask containing
				// 18 bits, with the lowest bit corresponding to chunk section -1 (in the void,
				// y=-16 to y=-1) and the highest bit for chunk section 16 (above the world,
				// y=256 to y=271).
				//
				// There are 16 sections in chunk. Each section height=16. So, y-coordinate
				// varies from 0 to 255.
				// We know that max light=15 (15 blocks). So, it is enough to update only 3
				// sections: (y\16)-1, y\16, (y\16)+1
				int blockMask = asSectionMask(chunkSectionY);
				int skyMask = blockMask;

				PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e(), skyMask,
						blockMask);
				human.playerConnection.sendPacket(packet);
			}
		}
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
		if (world == null) {
			return;
		}
		for (int i = 0; i < world.getPlayers().size(); i++) {
			Player player = world.getPlayers().get(i);
			Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
			EntityPlayer human = ((CraftPlayer) player).getHandle();
			Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
			int playerViewDistance = parseViewDistance(human);

			if (distanceTo(pChunk, chunk) <= playerViewDistance) {
				// https://wiki.vg/index.php?title=Pre-release_protocol&oldid=14804#Update_Light
				// https://github.com/flori-schwa/VarLight/blob/b9349499f9c9fb995c320f95eae9698dd85aad5c/v1_14_R1/src/me/florian/varlight/nms/v1_14_R1/NmsAdapter_1_14_R1.java#L451
				//
				// Two last argument is bit-mask what chunk sections to update. Mask containing
				// 18 bits, with the lowest bit corresponding to chunk section -1 (in the void,
				// y=-16 to y=-1) and the highest bit for chunk section 16 (above the world,
				// y=256 to y=271).
				PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e(),
						sectionMaskSky, sectionMaskBlock);
				human.playerConnection.sendPacket(packet);
			}
		}
	}
}
