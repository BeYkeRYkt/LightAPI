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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import ru.beykerykt.minecraft.lightapi.common.LightType;
import ru.beykerykt.minecraft.lightapi.impl.bukkit.BukkitChunkData;
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

	private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
			BlockFace.WEST };

	private static Block getAdjacentAirBlock(Block block) {
		for (BlockFace face : SIDES) {
			if (block.getY() == 0x0 && face == BlockFace.DOWN) // 0
				continue;
			if (block.getY() == 0xFF && face == BlockFace.UP) // 255
				continue;

			Block candidate = block.getRelative(face);

			if (!candidate.getType().isOccluding()) {
				return candidate;
			}
		}
		return block;
	}

	public boolean recalculateLight(World world, LightType type, int x, int y, int z) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition adjacentPosition = new BlockPosition(x, y, z);
		EnumSkyBlock esb = EnumSkyBlock.BLOCK;
		if (type == LightType.SKY) {
			esb = EnumSkyBlock.SKY;
		}
		return worldServer.c(esb, adjacentPosition);
	}

	private static Field getChunkField(Object chunk) throws NoSuchFieldException, SecurityException {
		if (cachedChunkModified == null) {
			cachedChunkModified = chunk.getClass().getDeclaredField("x");
			cachedChunkModified.setAccessible(true);
		}
		return cachedChunkModified;
	}

	private int distanceToSquared(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.locX - from.locX;
		double var4 = to.locZ - from.locZ;
		return (int) (var2 * var2 + var4 * var4);
	}

	/***********************************************************************************************************************/
	@Override
	public boolean createLight(String worldName, LightType type, int x, int y, int z, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		return createLight(world, type, x, y, z, lightlevel);
	}

	@Override
	public boolean createLight(World world, LightType type, int x, int y, int z, int lightlevel) {
		if (world == null || type == null) {
			return false;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(x, y, z);
		EnumSkyBlock esb = EnumSkyBlock.BLOCK;
		if (type == LightType.SKY) {
			esb = EnumSkyBlock.SKY;
		}
		worldServer.a(esb, position, lightlevel);

		Block adjacent = getAdjacentAirBlock(world.getBlockAt(x, y, z));
		int ax = adjacent.getX();
		int ay = adjacent.getY();
		int az = adjacent.getZ();
		recalculateLight(world, type, ax, ay, az);

		// check light
		if (world.getBlockAt(x, y, z).getLightFromBlocks() == lightlevel
				|| world.getBlockAt(x, y, z).getLightFromBlocks() >= lightlevel) {
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteLight(String worldName, LightType type, int x, int y, int z) {
		World world = Bukkit.getWorld(worldName);
		return deleteLight(world, type, x, y, z);
	}

	@Override
	public boolean deleteLight(World world, LightType type, int x, int y, int z) {
		if (world == null || type == null) {
			return false;
		}
		Block candidateBlock = world.getBlockAt(x, y, z);
		int oldlightlevel = candidateBlock.getLightFromBlocks();
		recalculateLight(world, type, x, y, z);

		// check light
		if (candidateBlock.getLightFromBlocks() != oldlightlevel) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isRequireManuallySendingChunks() {
		return true;
	}

	@Override
	public List<IChunkData> collectChunks(String worldName, int x, int y, int z, int radiusBlocks) {
		World world = Bukkit.getWorld(worldName);
		return collectChunks(world, x, y, z, radiusBlocks);
	}

	@Override
	public List<IChunkData> collectChunks(World world, int x, int y, int z, int radiusBlocks) {
		if (world == null) {
			return null;
		}
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
					if (nmsWorld.isChunkLoaded(chunkX, chunkZ, false)) {
						Chunk chunk = nmsWorld.getChunkAt(chunkX, chunkZ);
						Field isModified = getChunkField(chunk);
						if (isModified.getBoolean(chunk)) {
							IChunkData cCoord = new BukkitChunkData(world, chunk.locX, y, chunk.locZ,
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
	public void sendChunk(String worldName, int chunkX, int chunkZ, String playerName) {
		World world = Bukkit.getWorld(worldName);
		Player player = Bukkit.getPlayer(playerName);
		sendChunk(world, chunkX, chunkZ, player);
	}

	@Override
	public void sendChunk(String worldName, int chunkX, int y, int chunkZ, String playerName) {
		World world = Bukkit.getWorld(worldName);
		Player player = Bukkit.getPlayer(playerName);
		sendChunk(world, chunkX, y, chunkZ, player);
	}

	@Override
	public void sendChunk(String worldName, IChunkData chunkData, String playerName) {
		World world = Bukkit.getWorld(worldName);
		Player player = Bukkit.getPlayer(playerName);
		if (chunkData == null || world == null || player == null) {
			return;
		}
		if (chunkData instanceof BukkitChunkData) {
			BukkitChunkData bcd = (BukkitChunkData) chunkData;
			sendChunk(world, bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ(), player);
		} else {
			sendChunk(world, chunkData.getChunkX(), chunkData.getChunkZ(), player);
		}
	}

	@Override
	public void sendChunk(String worldName, int chunkX, int chunkZ) {
		World world = Bukkit.getWorld(worldName);
		for (Player player : world.getPlayers()) {
			sendChunk(world, chunkX, chunkZ, player);
		}
	}

	@Override
	public void sendChunk(String worldName, int chunkX, int y, int chunkZ) {
		World world = Bukkit.getWorld(worldName);
		for (Player player : world.getPlayers()) {
			sendChunk(world, chunkX, y, chunkZ, player);
		}
	}

	@Override
	public void sendChunk(String worldName, IChunkData chunkData) {
		World world = Bukkit.getWorld(worldName);
		if (chunkData == null || world == null) {
			return;
		}
		if (chunkData instanceof BukkitChunkData) {
			BukkitChunkData bcd = (BukkitChunkData) chunkData;
			sendChunk(world, bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ(), bcd.getReceivers());
		} else {
			for (Player player : world.getPlayers()) {
				sendChunk(world, chunkData.getChunkX(), chunkData.getChunkZ(), player);
			}
		}
	}

	@Override
	public void sendChunk(World world, int chunkX, int chunkZ, Player player) {
		if (world == null || player == null) {
			return;
		}
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
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
	public void sendChunk(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		for (Player player : players) {
			sendChunk(world, chunkX, chunkZ, player);
		}
	}

	@Override
	public void sendChunk(World world, int chunkX, int y, int chunkZ, Player player) {
		if (world == null || player == null) {
			return;
		}
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
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

	@Override
	public void sendChunk(World world, int chunkX, int y, int chunkZ, Collection<? extends Player> players) {
		for (Player player : players) {
			sendChunk(world, chunkX, y, chunkZ, player);
		}
	}

	@Override
	public void sendChunk(BukkitChunkData chunkData) {
		if (chunkData == null) {
			return;
		}
		sendChunk(chunkData.getWorld(), chunkData.getChunkX(), chunkData.getChunkYHeight(), chunkData.getChunkZ(),
				chunkData.getReceivers());
	}

	@Override
	public void sendChunk(BukkitChunkData chunkData, Player player) {
		if (chunkData == null || player == null) {
			return;
		}
		sendChunk(chunkData.getWorld(), chunkData.getChunkX(), chunkData.getChunkYHeight(), chunkData.getChunkZ(),
				player);
	}
}
