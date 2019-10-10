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
package ru.beykerykt.minecraft.lightapi.impl.bukkit;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.ImplementationPlatform;
import ru.beykerykt.minecraft.lightapi.common.LCallback;
import ru.beykerykt.minecraft.lightapi.common.LightType;

/**
 * 
 * Interface implementation for NMS (Net Minecraft Server)
 * 
 * @author BeYkeRYkt
 *
 */
public abstract class NMSLightHandler implements IBukkitLightHandler {

	@Override
	public ImplementationPlatform getImplementationPlatform() {
		return ImplementationPlatform.CRAFTBUKKIT;
	}

	@Override
	public boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		return createLight(world, type, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public boolean createLight(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		return createLight(world, type, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public boolean createLight(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		World world = Bukkit.getWorld(worldName);
		return createLight(world, type, blockX, blockY, blockZ, lightlevel, callback);
	}

	@Override
	public boolean deleteLight(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return deleteLight(world, type, blockX, blockY, blockZ);
	}

	@Override
	public boolean deleteLight(World world, LightType type, int blockX, int blockY, int blockZ) {
		return deleteLight(world, type, blockX, blockY, blockZ, null);
	}

	@Override
	public boolean deleteLight(String worldName, LightType type, int blockX, int blockY, int blockZ,
			LCallback callback) {
		World world = Bukkit.getWorld(worldName);
		return deleteLight(world, type, blockX, blockY, blockZ, callback);
	}

	@Override
	public void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		World world = Bukkit.getWorld(worldName);
		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public void setRawLightLevel(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel) {
		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public void setRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		World world = Bukkit.getWorld(worldName);
		setRawLightLevel(world, type, blockX, blockY, blockZ, lightlevel, callback);
	}

	@Override
	public int getRawLightLevel(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return getRawLightLevel(world, type, blockX, blockY, blockZ);
	}

	@Override
	public void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		recalculateLighting(world, type, blockX, blockY, blockZ);
	}

	@Override
	public void recalculateLighting(String worldName, LightType type, int blockX, int blockY, int blockZ,
			LCallback callback) {
		World world = Bukkit.getWorld(worldName);
		recalculateLighting(world, type, blockX, blockY, blockZ, callback);
	}

	@Override
	public void recalculateLighting(World world, LightType type, int blockX, int blockY, int blockZ) {
		recalculateLighting(world, type, blockX, blockY, blockZ, null);
	}

	@Override
	public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ, int radiusBlocks) {
		World world = Bukkit.getWorld(worldName);
		return collectChunks(world, blockX, blockY, blockZ, radiusBlocks);
	}

	@Override
	public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ) {
		World world = Bukkit.getWorld(worldName);
		return collectChunks(world, blockX, blockY, blockZ);
	}

	@Override
	public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ) {
		if (world == null) {
			return null;
		}
		int lightlevel = world.getBlockAt(blockX, blockY, blockZ).getLightFromBlocks();
		return collectChunks(world, blockX, blockY, blockZ, lightlevel);
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
			sendChanges(bcd.getWorld(), bcd.getChunkX(), bcd.getChunkYHeight(), bcd.getChunkZ());
		}
	}
}
