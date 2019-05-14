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
import ru.beykerykt.minecraft.lightapi.common.MappingType;

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
	public MappingType getMappingType() {
		return MappingType.CB;
	}
	
	@Override
	public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ, int radiusBlocks) {
		World world = Bukkit.getWorld(worldName);
		return collectChunks(world, blockX, blockY, blockZ, radiusBlocks);
	}

	@Override
	public List<IChunkData> collectChunks(String worldName, int blockX, int blockY, int blockZ) {
		return collectChunks(worldName, blockX, blockY, blockZ, 15);
	}

	@Override
	public List<IChunkData> collectChunks(World world, int blockX, int blockY, int blockZ) {
		return collectChunks(world, blockX, blockY, blockZ, 15);
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
