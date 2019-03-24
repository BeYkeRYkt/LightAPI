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
package ru.beykerykt.minecraft.lightapi.impl.sponge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.inject.Inject;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightType;

@Plugin(id = "lightapi", name = "LightAPI", description = "Library for create invisible light", authors = {
		"BeYkeRYkt" })
public class SpongePlugin {

	@Inject
	private Logger log;

	private static SpongePlugin plugin;

	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		this.plugin = this;
		LightAPI.setLightHandler(new SpongeHandlerFactory(this).createHandler());
	}

	public Logger getLogger() {
		return log;
	}

	public static SpongePlugin getInstance() {
		return plugin;
	}

	private Location<World> prevLoc;
	// testing
	private boolean debug = true;
	private boolean flag = false;

	@Listener
	public void onPlayerChat(MessageChannelEvent.Chat event, @First final Player p) {
		// if (!flag || !debug)
		// return;
		Text message = event.getRawMessage();

		if (message.toPlain().equals("enable")) {
			flag = true;
			p.sendMessage(Text.of("Enabled!"));
		} else if (message.toPlain().equals("disable")) {
			flag = false;
			p.sendMessage(Text.of("Disabled!"));
		} else if (message.toPlain().equals("create")) {
			Location<World> playerLoc = p.getLocation();
			World world = (World) playerLoc.getExtent();
			if (LightAPI.createLight(world.getName(), LightType.BLOCK, playerLoc.getBlockX(), playerLoc.getBlockY(),
					playerLoc.getBlockZ(), 12)) {
				if (LightAPI.isRequireManuallySendingChunks()) {
					for (IChunkData data : LightAPI.collectChunks(world.getName(), playerLoc.getBlockX(),
							playerLoc.getBlockY(), playerLoc.getBlockZ(), 12 / 2)) {
						LightAPI.sendChunk(p.getWorld().getName(), data);
					}
				}
			}
		} else if (message.toPlain().equals("delete")) {
			Location<World> playerLoc = p.getLocation();
			World world = (World) playerLoc.getExtent();
			if (LightAPI.deleteLight(world.getName(), LightType.BLOCK, playerLoc.getBlockX(), playerLoc.getBlockY(),
					playerLoc.getBlockZ())) {
				if (LightAPI.isRequireManuallySendingChunks()) {
					for (IChunkData data : LightAPI.collectChunks(world.getName(), playerLoc.getBlockX(),
							playerLoc.getBlockY(), playerLoc.getBlockZ(), 12 / 2)) {
						LightAPI.sendChunk(p.getWorld().getName(), data);
					}
				}
			}
		}
	}

	@Listener
	public void onRightClick(InteractBlockEvent.Secondary event, @First Player p) {
		if (!flag || !debug)
			return;

		List<IChunkData> chunkQueue = new CopyOnWriteArrayList<IChunkData>();
		if (prevLoc != null) {
			World world = (World) prevLoc.getExtent();
			if (LightAPI.isRequireManuallySendingChunks()) {
				if (LightAPI.deleteLight(world.getName(), LightType.BLOCK, prevLoc.getBlockX(), prevLoc.getBlockY(),
						prevLoc.getBlockZ())) {
					for (IChunkData data : LightAPI.collectChunks(world.getName(), prevLoc.getBlockX(),
							prevLoc.getBlockY(), prevLoc.getBlockZ(), 12 / 2)) {
						if (!chunkQueue.contains(data)) {
							chunkQueue.add(data);
						}
					}
				}
			}
		}
		prevLoc = event.getTargetBlock().getLocation().get();
		World world = (World) prevLoc.getExtent();

		if (LightAPI.isRequireManuallySendingChunks()) {
			if (LightAPI.createLight(world.getName(), LightType.BLOCK, prevLoc.getBlockX(), prevLoc.getBlockY(),
					prevLoc.getBlockZ(), 12)) {
				for (IChunkData data : LightAPI.collectChunks(world.getName(), prevLoc.getBlockX(), prevLoc.getBlockY(),
						prevLoc.getBlockZ(), 12 / 2)) {
					if (!chunkQueue.contains(data)) {
						chunkQueue.add(data);
					}
				}
				if (!chunkQueue.isEmpty()) {
					for (IChunkData data : chunkQueue) {
						LightAPI.sendChunk(p.getWorld().getName(), data);
					}
				}
				chunkQueue.clear();
			}
		}
	}
}