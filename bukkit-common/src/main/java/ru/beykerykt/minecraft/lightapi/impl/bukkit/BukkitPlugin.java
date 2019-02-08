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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightType;

public class BukkitPlugin extends JavaPlugin implements Listener {

	private Location prevLoc;

	@Override
	public void onLoad() {
		// set server implementation
		try {
			if (LightAPI.setLightHandler(getBukkitLightHandler())) {
				getLogger().info("Done!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	private IBukkitLightHandler getBukkitLightHandler()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException {
		String version = getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
		getLogger().info("Your server is using version " + version);
		return (IBukkitLightHandler) Class.forName("ru.beykerykt.minecraft.lightapi2.impl.bukkit.nms.NMS_" + version)
				.getConstructor().newInstance();
	}

	private boolean flag = true;

	// testing
	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		if (!flag)
			return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();

			if (prevLoc != null) {
				LightAPI.deleteLight(prevLoc.getWorld().getName(), LightType.BLOCK, prevLoc.getBlockX(),
						prevLoc.getBlockY(), prevLoc.getBlockZ());

				if (LightAPI.isRequireManuallySendingChunks()) {
					for (IChunkData data : LightAPI.collectChunks(prevLoc.getWorld().getName(), prevLoc.getBlockX(),
							prevLoc.getBlockY(), prevLoc.getBlockZ(), 12 / 2)) {
						if (!list.contains(data)) {
							list.add(data);
						}
					}
				}
			}
			prevLoc = event.getClickedBlock().getLocation();
			LightAPI.createLight(prevLoc.getWorld().getName(), LightType.BLOCK, prevLoc.getBlockX(),
					prevLoc.getBlockY(), prevLoc.getBlockZ(), 12);
			if (LightAPI.isRequireManuallySendingChunks()) {
				for (IChunkData data : LightAPI.collectChunks(prevLoc.getWorld().getName(), prevLoc.getBlockX(),
						prevLoc.getBlockY(), prevLoc.getBlockZ(), 12 / 2)) {
					if (!list.contains(data)) {
						list.add(data);
					}
				}

				for (IChunkData data : list) {
					LightAPI.sendChunk(p.getWorld().getName(), data);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(AsyncPlayerChatEvent event) {
		Player p = event.getPlayer();
		if (event.getMessage().equals("light")) {
			p.sendMessage("At player location");
			p.sendMessage("Block state | light level: " + p.getLocation().getBlock().getState().getLightLevel());
			p.sendMessage("Block | light level: " + p.getLocation().getBlock().getLightLevel());
			p.sendMessage("Block | light from blocks: " + p.getLocation().getBlock().getLightFromBlocks());
			p.sendMessage("Block | light from sky: " + p.getLocation().getBlock().getLightFromSky());
		} else if (event.getMessage().equals("create")) {
			if (LightAPI.createLight(p.getWorld().getName(), LightType.BLOCK, p.getLocation().getBlockX(),
					p.getLocation().getBlockY(), p.getLocation().getBlockZ(), 15)) {
				p.sendMessage("Light placed!");

				if (LightAPI.isRequireManuallySendingChunks()) {
					for (IChunkData data : LightAPI.collectChunks(p.getWorld().getName(), p.getLocation().getBlockX(),
							p.getLocation().getBlockY(), p.getLocation().getBlockZ(), 15 / 2)) {
						LightAPI.sendChunk(p.getWorld().getName(), data);
					}
				}

				event.setCancelled(true);
			}
		} else if (event.getMessage().equals("delete")) {
			if (LightAPI.deleteLight(p.getWorld().getName(), LightType.BLOCK, p.getLocation().getBlockX(),
					p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
				p.sendMessage("Light deleted!");

				if (LightAPI.isRequireManuallySendingChunks()) {
					for (IChunkData data : LightAPI.collectChunks(p.getWorld().getName(), p.getLocation().getBlockX(),
							p.getLocation().getBlockY(), p.getLocation().getBlockZ(), 15 / 2)) {
						LightAPI.sendChunk(p.getWorld().getName(), data);
					}
				}

				event.setCancelled(true);
			}
		} else if (event.getMessage().equals("disable")) {
			flag = false;
			event.setCancelled(true);
		} else if (event.getMessage().equals("enable")) {
			flag = true;
			event.setCancelled(true);
		}
	}
}