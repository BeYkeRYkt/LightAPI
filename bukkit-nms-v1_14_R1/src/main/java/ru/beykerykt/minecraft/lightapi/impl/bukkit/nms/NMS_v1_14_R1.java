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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

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
import net.minecraft.server.v1_14_R1.PacketPlayOutLightUpdate;
import net.minecraft.server.v1_14_R1.WorldServer;
import ru.beykerykt.minecraft.lightapi.common.LightingEngineVersion;
import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LightType;
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

	private int distanceToSquared(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.getPos().x - from.getPos().x;
		double var4 = to.getPos().z - from.getPos().z;
		return (int) (var2 * var2 + var4 * var4);
	}

	/***********************************************************************************************************************/
	@Override
	public synchronized boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		return createLight(world, type, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public synchronized boolean createLight(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
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
	public synchronized boolean deleteLight(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return deleteLight(world, type, blockX, blockY, blockZ);
	}

	@Override
	public synchronized boolean deleteLight(World world, LightType type, int blockX, int blockY, int blockZ) {
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
	public synchronized void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public synchronized void setRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		if (world == null || type == null) {
			return;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);

		if (lightlevel <= 0) {
			if (type == LightType.BLOCK) {
				LightEngineBlock leb = (LightEngineBlock) worldServer.getChunkProvider().getLightEngine()
						.a(EnumSkyBlock.BLOCK);
				leb.a(position);
			} else {
				LightEngineSky les = (LightEngineSky) worldServer.getChunkProvider().getLightEngine()
						.a(EnumSkyBlock.SKY);
				les.a(position);
			}
			return;
		}
		if (type == LightType.BLOCK) {
			LightEngineBlock leb = (LightEngineBlock) worldServer.getChunkProvider().getLightEngine()
					.a(EnumSkyBlock.BLOCK);
			leb.a(position, lightlevel);
		} else {
			LightEngineSky les = (LightEngineSky) worldServer.getChunkProvider().getLightEngine().a(EnumSkyBlock.SKY);
			les.a(position, lightlevel);
		}
	}

	@Override
	public synchronized int getRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return getRawLightLevel(world, type, blockX, blockY, blockZ);
	}

	@Override
	public synchronized int getRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ) {
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
	public synchronized void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		recalculateLighting(world, type, blockX, blockY, blockZ);
	}

	@Override
	public synchronized void recalculateLighting(World world, LightType type, int blockX, int blockY, int blockZ) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();

		// Do not recalculate if no changes!
		if (!worldServer.getChunkProvider().getLightEngine().a()) {
			return;
		}

		Chunk chunk = worldServer.getChunkAt(blockX >> 4, blockZ >> 4);
		CompletableFuture<IChunkAccess> future = worldServer.getChunkProvider().getLightEngine().a(chunk, true);
		if (worldServer.getMinecraftServer().isMainThread()) {
			worldServer.getMinecraftServer().awaitTasks(future::isDone);
		} else {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
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
	public List<IChunkData> collectChunks(String worldName, int x, int y, int z, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		return collectChunks(world, x, y, z, lightlevel);
	}

	@Override
	public List<IChunkData> collectChunks(String worldName, int x, int y, int z) {
		return collectChunks(worldName, x, y, z, 15);
	}

	@Override
	public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ) {
		return collectChunks(world, blockX, blockY, blockZ, 15);
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

	@Override
	public void sendChanges(IChunkData chunkData, Player player) {
		if (chunkData == null || player == null) {
			return;
		}
		if (chunkData instanceof BukkitChunkData) {
			BukkitChunkData bcd = (BukkitChunkData) chunkData;
			sendChanges(bcd.getWorld(), bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ(), player);
		}
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ) {
		if (world == null)
			return;
		for (Player player : world.getPlayers()) {
			sendChanges(world, chunkX, chunkZ, player);
		}
	}

	@Override
	public void sendChanges(World world, int chunkX, int blockY, int chunkZ) {
		if (world == null)
			return;
		for (Player player : world.getPlayers()) {
			sendChanges(world, chunkX, blockY, chunkZ, player);
		}
	}

	@Override
	public void sendChanges(String worldName, int chunkX, int chunkZ, String playerName) {
		World world = Bukkit.getWorld(worldName);
		Player player = Bukkit.getPlayer(playerName);
		sendChanges(world, chunkX, chunkZ, player);
	}

	@Override
	public void sendChanges(String worldName, int chunkX, int blockY, int chunkZ, String playerName) {
		World world = Bukkit.getWorld(worldName);
		Player player = Bukkit.getPlayer(playerName);
		sendChanges(world, chunkX, blockY, chunkZ, player);
	}

	@Override
	public void sendChanges(IChunkData chunkData, String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		sendChanges(chunkData, player);
	}

	@Override
	public void sendChanges(String worldName, int chunkX, int chunkZ) {
		World world = Bukkit.getWorld(worldName);
		sendChanges(world, chunkX, chunkZ);
	}

	@Override
	public void sendChanges(String worldName, int chunkX, int blockY, int chunkZ) {
		World world = Bukkit.getWorld(worldName);
		sendChanges(world, chunkX, blockY, chunkZ);
	}

	@Override
	public void sendChanges(IChunkData chunkData) {
		if (chunkData == null)
			return;
		if (chunkData instanceof BukkitChunkData) {
			BukkitChunkData bcd = (BukkitChunkData) chunkData;
			for (Player player : bcd.getReceivers()) {
				sendChanges(bcd.getWorld(), bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ(), player);
			}
		}
	}
}
