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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EnumSkyBlock;
import net.minecraft.server.v1_13_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_13_R2.WorldServer;
import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LCallback;
import ru.beykerykt.minecraft.lightapi.common.LReason;
import ru.beykerykt.minecraft.lightapi.common.LStage;
import ru.beykerykt.minecraft.lightapi.common.LightType;
import ru.beykerykt.minecraft.lightapi.common.LightingEngineVersion;
import ru.beykerykt.minecraft.lightapi.impl.bukkit.BukkitChunkData;
import ru.beykerykt.minecraft.lightapi.impl.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.impl.bukkit.NMSLightHandler;

/**
 * 
 * Interface implementation for NMS (Net Minecraft Server) version 1.13.2
 * 
 * @author BeYkeRYkt
 *
 */
public class NMS_v1_13_R2 extends NMSLightHandler {

	private static Field cachedChunkModified;

	private static Field getChunkField(Object chunk) throws NoSuchFieldException, SecurityException {
		if (cachedChunkModified == null) {
			cachedChunkModified = chunk.getClass().getDeclaredField("x");
			cachedChunkModified.setAccessible(true);
		}
		return cachedChunkModified;
	}

	private int distanceTo(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.locX - from.locX;
		double var4 = to.locZ - from.locZ;
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

		Block adjacent = BukkitPlugin.getAdjacentAirBlock(world.getBlockAt(blockX, blockY, blockZ));
		int ax = adjacent.getX();
		int ay = adjacent.getY();
		int az = adjacent.getZ();
		recalculateLighting(world, type, ax, ay, az, callback);

		// check light
		if (world.getBlockAt(blockX, blockY, blockZ).getLightFromBlocks() == lightlevel
				|| world.getBlockAt(blockX, blockY, blockZ).getLightFromBlocks() >= lightlevel) {
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
		if (lightlevel == 0) {
			recalculateLighting(world, type, blockX, blockY, blockZ, callback);
			if (callback != null) {
				callback.onSuccess(world.getName(), type, blockX, blockY, blockZ, lightlevel, LStage.WRITTING);
			}
			return;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		EnumSkyBlock esb = EnumSkyBlock.BLOCK;
		if (type == LightType.SKY) {
			esb = EnumSkyBlock.SKY;
		}
		worldServer.a(esb, position, lightlevel);
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
		BlockPosition adjacentPosition = new BlockPosition(blockX, blockY, blockZ);
		EnumSkyBlock esb = EnumSkyBlock.BLOCK;
		if (type == LightType.SKY) {
			esb = EnumSkyBlock.SKY;
		}
		worldServer.c(esb, adjacentPosition);
		if (callback != null) {
			callback.onSuccess(world.getName(), type, blockX, blockY, blockZ, 0, LStage.RECALCULATING);
		}
	}

	@Override
	public LightingEngineVersion getLightingEngineVersion() {
		return LightingEngineVersion.V1;
	}

	@Override
	public boolean isAsyncLighting() {
		return false;
	}

	@Override
	public boolean isRequireManuallySendingChanges() {
		return true;
	}

	@Override
	public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ, int lightlevel) {
		if (world == null) {
			return null;
		}
		if (lightlevel > 15) {
			lightlevel = 15;
		}
		int radiusBlocks = lightlevel - 1;
		List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();
		try {
			WorldServer nmsWorld = ((CraftWorld) world).getHandle();
			for (int dX = -radiusBlocks; dX <= radiusBlocks; dX += radiusBlocks) {
				for (int dZ = -radiusBlocks; dZ <= radiusBlocks; dZ += radiusBlocks) {
					int chunkX = (blockX + dX) >> 4;
					int chunkZ = (blockZ + dZ) >> 4;
					if (nmsWorld.getChunkProvider().isLoaded(chunkX, chunkZ)) {
						Chunk chunk = nmsWorld.getChunkAt(chunkX, chunkZ);
						Field isModified = getChunkField(chunk);
						if (isModified.getBoolean(chunk)) {
							IChunkData cCoord = new BukkitChunkData(world, chunk.locX, blockY, chunk.locZ,
									world.getPlayers());
							if (!list.contains(cCoord)) {
								list.add(cCoord);
							}
							chunk.a(false);
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
		int playerViewDistance = human.clientViewDistance;

		if (playerViewDistance > Bukkit.getViewDistance()) {
			playerViewDistance = Bukkit.getViewDistance();
		}

		if (distanceTo(pChunk, chunk) <= playerViewDistance) {
			// Last argument is bit-mask what chunk sections to update. Only lower 16 bits
			// are used.
			// There are 16 sections in chunk. Each section height=16. So, y-coordinate
			// varies from 0 to 255.
			// Use 0x1ffff instead 0xffff because of little bug in PacketPlayOutMapChunk
			// constructor.
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, 0x1ffff);
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
			// Last argument is bit-mask what chunk sections to update. Only lower 16 bits
			// are used.
			// There are 16 sections in chunk. Each section height=16. So, y-coordinate
			// varies from 0 to 255.
			// We know that max light=15 (15 blocks). So, it is enough to update only 3
			// sections: y\16-1, y\16, y\16+1
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, (7 << (y >> 4)) >> 1);
			human.playerConnection.sendPacket(packet);
		}
	}
}
