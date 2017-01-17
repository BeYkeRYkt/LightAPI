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
package org.implexdevone.beykerykt.bukkit.lightapi;

import java.util.Collection;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.implexdevone.beykerykt.bukkit.lightapi.chunks.ChunkInfo;
import org.implexdevone.beykerykt.bukkit.lightapi.server.nms.INMSHandler;
import org.implexdevone.beykerykt.bukkit.lightapi.server.nms.craftbukkit.CraftBukkit_v1_11_R1;

public class LightAPI extends JavaPlugin {

	private static LightAPI plugin;
	private static INMSHandler handler;

	@SuppressWarnings("static-access")
	@Override
	public void onLoad() {
		this.plugin = this;
		this.handler = new CraftBukkit_v1_11_R1();
	}

	public static LightAPI getInstance() {
		return plugin;
	}

	public static INMSHandler getNMSHandler() {
		return handler;
	}

	public static boolean createLight(World world, int x, int y, int z, int lightlevel) {
		if (getInstance().isEnabled()) {
			getNMSHandler().createLight(world, x, y, z, lightlevel);
			return true;
		}
		return false;
	}

	public static boolean deleteLight(World world, int x, int y, int z) {
		if (getInstance().isEnabled()) {
			getNMSHandler().deleteLight(world, x, y, z);
			return true;
		}
		return false;
	}

	public static boolean recalculateLight(World world, int x, int y, int z) {
		if (getInstance().isEnabled()) {
			getNMSHandler().recalculateLight(world, x, y, z);
			return true;
		}
		return false;
	}

	public static List<ChunkInfo> collectChunks(World world, int x, int y, int z) {
		if (getInstance().isEnabled()) {
			return getNMSHandler().collectChunks(world, x, y, z);
		}
		return null;
	}

	public static boolean updateChunk(ChunkInfo info) {
		if (getInstance().isEnabled()) {
			getNMSHandler().sendChunkUpdate(info.getWorld(), info.getChunkX(), info.getChunkYHeight(), info.getChunkZ(), info.getReceivers());
			return true;
		}
		return false;
	}

	public static boolean updateChunks(World world, int x, int y, int z, Collection<? extends Player> players) {
		if (getInstance().isEnabled()) {
			for (ChunkInfo info : collectChunks(world, x, y, z)) {
				info.setReceivers(players);
				updateChunk(info);
			}
			return true;
		}
		return false;
	}

	public static boolean updateChunk(World world, int x, int y, int z, Collection<? extends Player> players) {
		if (getInstance().isEnabled()) {
			getNMSHandler().sendChunkUpdate(world, x >> 4, y, z >> 4, players);
			return true;
		}
		return false;
	}
}
