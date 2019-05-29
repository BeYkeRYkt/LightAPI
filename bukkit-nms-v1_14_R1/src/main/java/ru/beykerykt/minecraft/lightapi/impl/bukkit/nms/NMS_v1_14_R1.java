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

	private static Field cachedMailboxField;
	private static Method cachedMailboxMethod;
	private ReentrantLock locker = new ReentrantLock();

	private static Field getMailboxField(Object lightEngine) throws NoSuchFieldException, SecurityException {
		if (cachedMailboxField == null) {
			cachedMailboxField = lightEngine.getClass().getDeclaredField("b");
			cachedMailboxField.setAccessible(true);
		}
		return cachedMailboxField;
	}

	private static Method getCheckMailboxMethod(Object lightEngine) throws NoSuchMethodException, SecurityException {
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

	private int distanceToSquared(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.getPos().x - from.getPos().x;
		double var4 = to.getPos().z - from.getPos().z;
		return (int) (var2 * var2 + var4 * var4);
	}

	/***********************************************************************************************************************/
	@Override
	public boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		return createLight(world, type, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public boolean createLight(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		if (world == null || type == null) {
			return false;
		}

		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel);
		recalculateLighting(world, type, blockX, blockY, blockZ);

		// check light
		if (world.getBlockAt(blockX, blockY, blockZ).getLightFromBlocks() == lightlevel
				|| world.getBlockAt(blockX, blockY, blockZ).getLightFromBlocks() >= lightlevel) {
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteLight(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return deleteLight(world, type, blockX, blockY, blockZ);
	}

	@Override
	public boolean deleteLight(World world, LightType type, int blockX, int blockY, int blockZ) {
		if (world == null || type == null) {
			return false;
		}
		Block candidateBlock = world.getBlockAt(blockX, blockY, blockZ);
		int oldlightlevel = candidateBlock.getLightFromBlocks();

		setRawLightLevel(world, type, blockX, blockY, blockZ, 0);
		recalculateLighting(world, type, blockX, blockY, blockZ);

		// check light
		if (candidateBlock.getLightFromBlocks() != oldlightlevel) {
			return true;
		}
		return false;
	}

	@Override
	public void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public void setRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		if (world == null || type == null) {
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
		if (!isThreadMailboxWorking(lightEngine)) {
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
		}
	}

	@Override
	public int getRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return getRawLightLevel(world, type, blockX, blockY, blockZ);
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
	public void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		recalculateLighting(world, type, blockX, blockY, blockZ);
	}

	@Override
	public void recalculateLighting(World world, LightType type, int blockX, int blockY, int blockZ) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

		// Do not recalculate if no changes!
		if (!lightEngine.a()) {
			return;
		}

		if (worldServer.getMinecraftServer().isMainThread()) {
			// no relight while ThreadMailbox is working
			if (!isThreadMailboxWorking(lightEngine)) {
				if (type == LightType.BLOCK) {
					LightEngineBlock leb = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);
					leb.a(Integer.MAX_VALUE, true, true);
				} else {
					LightEngineSky les = (LightEngineSky) lightEngine.a(EnumSkyBlock.SKY);
					les.a(Integer.MAX_VALUE, true, true);
				}
			}
		} else {
			locker.lock();
			try {
				// no relight while ThreadMailbox is working
				if (!isThreadMailboxWorking(lightEngine)) {
					Chunk chunk = worldServer.getChunkAt(blockX >> 4, blockZ >> 4);
					CompletableFuture<IChunkAccess> future = lightEngine.a(chunk, true);
					future.join();
				}
			} catch (Exception e) {
				System.out.println("Ah shit, here we go again");
				e.printStackTrace();
			} finally {
				locker.unlock();
			}
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
		int radiusBlocks = lightlevel / 2;
		if (radiusBlocks > 8 || radiusBlocks <= 0) {
			radiusBlocks = 8;
		}
		List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();
		try {
			WorldServer nmsWorld = ((CraftWorld) world).getHandle();
			for (int dX = -radiusBlocks; dX <= radiusBlocks; dX += radiusBlocks) {
				for (int dZ = -radiusBlocks; dZ <= radiusBlocks; dZ += radiusBlocks) {
					int chunkX = (x + dX) >> 4;
					int chunkZ = (z + dZ) >> 4;
					if (nmsWorld.isChunkLoaded(chunkX, chunkZ)) {
						Chunk chunk = nmsWorld.getChunkAt(chunkX, chunkZ);
						IChunkData cCoord = new BukkitChunkData(world, chunk.getPos().x, y, chunk.getPos().z,
								world.getPlayers());
						if (!list.contains(cCoord)) {
							list.add(cCoord);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
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
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
			PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunk.getPos(), chunk.e());
			human.playerConnection.sendPacket(packet);
		}
	}
}
