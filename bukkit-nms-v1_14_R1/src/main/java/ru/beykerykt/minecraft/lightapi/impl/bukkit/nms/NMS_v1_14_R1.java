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
package ru.beykerykt.minecraft.lightapi.impl.bukkit.nms;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EnumSkyBlock;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.LightEngineBlock;
import net.minecraft.server.v1_14_R1.LightEngineSky;
import net.minecraft.server.v1_14_R1.LightEngineThreaded;
import net.minecraft.server.v1_14_R1.PacketPlayOutLightUpdate;
import net.minecraft.server.v1_14_R1.ThreadedMailbox;
import net.minecraft.server.v1_14_R1.WorldServer;
import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LCallback;
import ru.beykerykt.minecraft.lightapi.common.LReason;
import ru.beykerykt.minecraft.lightapi.common.LStage;
import ru.beykerykt.minecraft.lightapi.common.LightType;
import ru.beykerykt.minecraft.lightapi.common.LightingEngineVersion;
import ru.beykerykt.minecraft.lightapi.impl.bukkit.BukkitChunkData;
import ru.beykerykt.minecraft.lightapi.impl.bukkit.NMSLightHandler;

/**
 * 
 * Interface implementation for NMS (Net Minecraft Server) version 1.14
 * 
 * @author BeYkeRYkt
 *
 */
public class NMS_v1_14_R1 extends NMSLightHandler {

	private Field cachedMailboxField;
	private Method cachedMailboxMethod;
	private ReentrantLock locker = new ReentrantLock();

	private Field getMailboxField(Object lightEngine) throws NoSuchFieldException, SecurityException {
		if (cachedMailboxField == null) {
			cachedMailboxField = lightEngine.getClass().getDeclaredField("b");
			cachedMailboxField.setAccessible(true);
		}
		return cachedMailboxField;
	}

	private Method getCheckMailboxMethod(Object lightEngine) throws NoSuchMethodException, SecurityException {
		if (cachedMailboxMethod == null) {
			cachedMailboxMethod = lightEngine.getClass().getDeclaredMethod("d");
			cachedMailboxMethod.setAccessible(true);
		}
		return cachedMailboxMethod;
	}

	@SuppressWarnings("unchecked")
	private boolean isThreadMailboxWorking(LightEngineThreaded lightEngine) {
		boolean flag = false;
		try {
			ThreadedMailbox<Runnable> threadedMailbox = ((ThreadedMailbox<Runnable>) getMailboxField(lightEngine)
					.get(lightEngine));
			flag = ((boolean) getCheckMailboxMethod(threadedMailbox).invoke(threadedMailbox, null));
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return flag;
	}

	private int distanceTo(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.getPos().x - from.getPos().x;
		double var4 = to.getPos().z - from.getPos().z;
		return (int) Math.sqrt(var2 * var2 + var4 * var4);
	}

	/***********************************************************************************************************************/
	@Override
	public boolean createLight(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (world == null || type == null) {
			if (callback != null) {
				callback.onFailed(world.getName(), type, blockX, blockY, blockZ, lightlevel, LStage.CREATING,
						LReason.NULL_ARGS);
			}
			return false;
		}

		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel, callback);
		recalculateLighting(world, type, blockX, blockY, blockZ, callback);

		// check light
		if (world.getBlockAt(blockX, blockY, blockZ).getLightFromBlocks() >= lightlevel) {
			if (callback != null) {
				callback.onSuccess(world.getName(), type, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
			}
			return true;
		}
		if (callback != null) {
			callback.onFailed(world.getName(), type, blockX, blockY, blockZ, lightlevel, LStage.CREATING,
					LReason.NO_LIGHT_CHANGES);
		}
		return false;
	}

	@Override
	public boolean deleteLight(World world, LightType type, int blockX, int blockY, int blockZ, LCallback callback) {
		if (world == null || type == null) {
			if (callback != null) {
				callback.onFailed(world.getName(), type, blockX, blockY, blockZ, 0, LStage.DELETING, LReason.NULL_ARGS);
			}
			return false;
		}

		Block candidateBlock = world.getBlockAt(blockX, blockY, blockZ);
		int oldlightlevel = candidateBlock.getLightFromBlocks();

		setRawLightLevel(world, type, blockX, blockY, blockZ, 0, callback);
		recalculateLighting(world, type, blockX, blockY, blockZ, callback);

		// check light
		if (candidateBlock.getLightFromBlocks() != oldlightlevel) {
			if (callback != null) {
				callback.onSuccess(world.getName(), type, blockX, blockY, blockZ, 0, LStage.DELETING);
			}
			return true;
		}
		if (callback != null) {
			callback.onFailed(world.getName(), type, blockX, blockY, blockZ, 0, LStage.DELETING,
					LReason.NO_LIGHT_CHANGES);
		}
		return false;
	}

	@Override
	public void setRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (world == null || type == null) {
			if (callback != null) {
				callback.onFailed(world.getName(), type, blockX, blockY, blockZ, lightlevel, LStage.WRITTING,
						LReason.NULL_ARGS);
			}
			return;
		}
		if (lightlevel < 0) {
			lightlevel = 0;
		} else if (lightlevel > 15) {
			lightlevel = 15;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

		// no relight while ThreadMailbox is working
		if (isThreadMailboxWorking(lightEngine)) {
			while (isThreadMailboxWorking(lightEngine)) {
				try {
					Thread.sleep(50L); // TEMP FIX: wait a tick
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		if (type == LightType.BLOCK) {
			LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
			if (lightlevel == 0) {
				leb.a(position);
			} else {
				leb.a(position, lightlevel);
			}
		} else {
			LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
			if (lightlevel == 0) {
				les.a(position);
			} else {
				les.a(position, lightlevel);
			}
		}

		if (callback != null) {
			callback.onSuccess(world.getName(), type, blockX, blockY, blockZ, lightlevel, LStage.WRITTING);
		}
	}

	@Override
	public int getRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ) {
		if (world == null || type == null) {
			return 0;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		EnumSkyBlock esb = EnumSkyBlock.BLOCK;
		if (type == LightType.SKY) {
			esb = EnumSkyBlock.SKY;
		}
		return worldServer.getBrightness(esb, position);
	}

	@Override
	public void recalculateLighting(World world, LightType type, int blockX, int blockY, int blockZ,
			LCallback callback) {
		if (world == null || type == null) {
			if (callback != null) {
				callback.onFailed(world.getName(), type, blockX, blockY, blockZ, 0, LStage.RECALCULATING,
						LReason.NULL_ARGS);
			}
			return;
		}

		WorldServer worldServer = ((CraftWorld) world).getHandle();
		LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

		// Do not recalculate if no changes!
		if (!lightEngine.a()) {
			if (callback != null) {
				callback.onFailed(world.getName(), type, blockX, blockY, blockZ, 0, LStage.RECALCULATING,
						LReason.NO_LIGHT_CHANGES);
			}
			return;
		}

		if (worldServer.getMinecraftServer().isMainThread()) {
			// no relight while ThreadMailbox is working
			if (isThreadMailboxWorking(lightEngine)) {
				// Ah shit, here we go again
				while (isThreadMailboxWorking(lightEngine)) {
					try {
						Thread.sleep(50L); // TEMP FIX: wait a tick
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (type == LightType.BLOCK) {
				LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
				leb.a(Integer.MAX_VALUE, true, true);
			} else {
				LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
				les.a(Integer.MAX_VALUE, true, true);
			}
		} else {
			locker.lock();
			try {
				// no relight while ThreadMailbox is working
				if (isThreadMailboxWorking(lightEngine)) {
					while (isThreadMailboxWorking(lightEngine)) {
						Thread.sleep(50L); // TEMP FIX: wait a tick
					}
				}

				Chunk chunk = worldServer.getChunkAt(blockX >> 4, blockZ >> 4);
				CompletableFuture<IChunkAccess> future = lightEngine.a(chunk, true);
				future.join();
			} catch (Exception e) {
				System.out.println("Ah shit, here we go again");
				e.printStackTrace();
				if (callback != null) {
					callback.onFailed(world.getName(), type, blockX, blockY, blockZ, 0, LStage.RECALCULATING,
							LReason.THROW_EXCEPTION);
				}
			} finally {
				locker.unlock();
			}
		}
		if (callback != null) {
			callback.onSuccess(world.getName(), type, blockX, blockY, blockZ, 0, LStage.RECALCULATING);
		}
	}

	@Override
	public LightingEngineVersion getLightingEngineVersion() {
		return LightingEngineVersion.V2;
	}

	@Override
	public boolean isAsyncLighting() {
		return true;
	}

	@Override
	public boolean isRequireManuallySendingChanges() {
		return true;
	}

	@Override
	public List<IChunkData> collectChunks(World world, int x, int y, int z, int lightlevel) {
		if (world == null) {
			return null;
		}
		if (lightlevel > 15 || lightlevel <= 0) {
			lightlevel = 15;
		}
		int radiusBlocks = lightlevel;
		List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();
		WorldServer nmsWorld = ((CraftWorld) world).getHandle();
		for (int dX = -radiusBlocks; dX <= radiusBlocks; dX += radiusBlocks) {
			for (int dZ = -radiusBlocks; dZ <= radiusBlocks; dZ += radiusBlocks) {
				int chunkX = (x + dX) >> 4;
				int chunkZ = (z + dZ) >> 4;
				if (nmsWorld.isChunkLoaded(chunkX, chunkZ)) {
					Chunk chunk = nmsWorld.getChunkAt(chunkX, chunkZ);
					IChunkData cCoord = new BukkitChunkData(world, chunk.getPos().x, y, chunk.getPos().z,
							world.getPlayers());
					if (chunk.isNeedsSaving() && !list.contains(cCoord)) {
						list.add(cCoord);
					}
				}
			}
		}
		return list;
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ, Player player) {
		if (world == null || player == null) {
			return;
		}
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		int playerViewDistance = human.clientViewDistance;

		if (playerViewDistance > Bukkit.getViewDistance()) {
			playerViewDistance = Bukkit.getViewDistance();
		}

		if (distanceTo(pChunk, chunk) <= playerViewDistance) {
			PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e());
			human.playerConnection.sendPacket(packet);
		}
	}

	@Override
	public void sendChanges(World world, int chunkX, int y, int chunkZ, Player player) {
		if (world == null || player == null) {
			return;
		}
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		int playerViewDistance = human.clientViewDistance;

		if (playerViewDistance > Bukkit.getViewDistance()) {
			playerViewDistance = Bukkit.getViewDistance();
		}

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
			int chunkSection = (y / 16);
			int mask = ((1 << (chunkSection - 1)) | (1 << (chunkSection)) | (1 << (chunkSection + 1))) << 1;

			PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e(), mask, mask);
			human.playerConnection.sendPacket(packet);
		}
	}
}
