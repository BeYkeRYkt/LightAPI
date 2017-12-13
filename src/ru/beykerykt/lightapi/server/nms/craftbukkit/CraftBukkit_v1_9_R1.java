/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2016
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
package ru.beykerykt.lightapi.server.nms.craftbukkit;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_9_R1.BlockPosition;
import net.minecraft.server.v1_9_R1.Chunk;
import net.minecraft.server.v1_9_R1.EntityPlayer;
import net.minecraft.server.v1_9_R1.EnumSkyBlock;
import net.minecraft.server.v1_9_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_9_R1.WorldServer;
import ru.beykerykt.lightapi.chunks.ChunkInfo;
import ru.beykerykt.lightapi.server.nms.INMSHandler;

public class CraftBukkit_v1_9_R1 implements INMSHandler {

	private static Field cachedChunkModified;

	@Override
	public void createLight(World world, int x, int y, int z, int light) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(x, y, z);
		worldServer.a(EnumSkyBlock.BLOCK, position, light);
	}

	@Override
	public void deleteLight(World world, int x, int y, int z) {
		recalculateLight(world, x, y, z);
	}

	@Override
	public void recalculateLight(World world, int x, int y, int z) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(x, y, z);
		worldServer.c(EnumSkyBlock.BLOCK, position);
	}

	@Override
	public List<ChunkInfo> collectChunks(World world, int x, int y, int z) {
		List<ChunkInfo> list = new CopyOnWriteArrayList<ChunkInfo>();
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		try {
			WorldServer nmsWorld = ((CraftWorld) world).getHandle();
			for (int dX = -1; dX <= 1; dX++) {
				for (int dZ = -1; dZ <= 1; dZ++) {
					if (nmsWorld.getChunkProviderServer().isChunkLoaded(chunkX + dX, chunkZ + dZ)) {
						Chunk chunk = nmsWorld.getChunkAt(chunkX + dX, chunkZ + dZ);
						Field isModified = getChunkField(chunk);
						if (isModified.getBoolean(chunk)) {
							ChunkInfo cCoord = new ChunkInfo(world, chunk.locX, y, chunk.locZ, world.getPlayers());
							list.add(cCoord);
							chunk.f(false);
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
	public void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		for (Player player : players) {
			sendChunkUpdate(world, chunkX, chunkZ, player);
		}
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player) {
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
			// Last argument is bit-mask what chunk sections to update. Only lower 16 bits are used.
			// There are 16 sections in chunk. Each section height=16. So, y-coordinate varies from 0 to 255.
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, false, 0xffff);
			human.playerConnection.sendPacket(packet);
		}
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int y, int chunkZ, Collection<? extends Player> players) {
		for (Player player : players) {
			sendChunkUpdate(world, chunkX, y, chunkZ, player);
		}
	}

	@Override
	public void sendChunkUpdate(World world, int chunkX, int y, int chunkZ, Player player) {
		Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
		EntityPlayer human = ((CraftPlayer) player).getHandle();
		Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
		if (distanceToSquared(pChunk, chunk) < 5 * 5) {
			// Last argument is bit-mask what chunk sections to update. Only lower 16 bits are used.
			// There are 16 sections in chunk. Each section height=16. So, y-coordinate varies from 0 to 255.
			// We know that max light=15 (15 blocks). So, it is enough to update only 3 sections: y\16-1, y\16, y\16+1
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, false, (7 << (y >> 4)) >> 1);
			human.playerConnection.sendPacket(packet);
		}
	}

	private static Field getChunkField(Object chunk) throws NoSuchFieldException, SecurityException {
		if (cachedChunkModified == null) {
			cachedChunkModified = chunk.getClass().getDeclaredField("r");
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
}
