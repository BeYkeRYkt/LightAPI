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
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.IChunkData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightType;

public class BukkitPlugin extends JavaPlugin implements Listener {

	private Location prevLoc;

	@Override
	public void onLoad() {
		// set server implementation
		LightAPI.setLightHandler(new BukkitHandlerFactory(this).createHandler());
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	public void log(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + message);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("lightapi")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (args.length == 0) {
					player.sendMessage(ChatColor.AQUA + " ------- <LightAPI " + ChatColor.WHITE
							+ getDescription().getVersion() + "> ------- ");

					TextComponent version = new TextComponent(ChatColor.AQUA + " Current version: ");
					TextComponent update = new TextComponent(ChatColor.WHITE + getDescription().getVersion());
					update.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lightapi update"));
					update.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new ComponentBuilder("Click here for check update").create()));
					version.addExtra(update);
					player.spigot().sendMessage(version);

					player.sendMessage(ChatColor.AQUA + " Server name: " + ChatColor.WHITE + getServer().getName());
					player.sendMessage(
							ChatColor.AQUA + " Server version: " + ChatColor.WHITE + getServer().getVersion());

					TextComponent text = new TextComponent(" | ");
					TextComponent sourcecode = new TextComponent(ChatColor.AQUA + "Source code");
					sourcecode.setClickEvent(
							new ClickEvent(ClickEvent.Action.OPEN_URL, "http://github.com/BeYkeRYkt/LightAPI/"));
					sourcecode.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new ComponentBuilder("Goto the GitHub!").create()));
					text.addExtra(sourcecode);
					text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

					TextComponent developer = new TextComponent(ChatColor.AQUA + "Developer");
					developer.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://github.com/BeYkeRYkt/"));
					developer.setHoverEvent(
							new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("BeYkeRYkt").create()));
					text.addExtra(developer);
					text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

					TextComponent contributors = new TextComponent(ChatColor.AQUA + "Contributors");
					contributors.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
							"https://github.com/BeYkeRYkt/LightAPI/graphs/contributors"));
					contributors.setHoverEvent(
							new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("ALIENS!!").create()));
					text.addExtra(contributors);
					text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

					player.spigot().sendMessage(text);

					TextComponent licensed = new TextComponent(" Licensed under ");
					TextComponent MIT = new TextComponent(ChatColor.AQUA + "MIT License");
					MIT.setClickEvent(
							new ClickEvent(ClickEvent.Action.OPEN_URL, "https://opensource.org/licenses/MIT/"));
					MIT.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new ComponentBuilder("Goto for information about license!").create()));
					licensed.addExtra(MIT);
					player.spigot().sendMessage(licensed);

					if (Build.CURRENT_VERSION == Build.VERSION_CODES.ONE) {
						player.sendMessage(ChatColor.BLACK + "backwards compatibility enabled");
					}

				} else {
					log(player, ChatColor.RED + "Hmm... This command does not exist. Are you sure write correctly ?");
				}
			} else if (sender instanceof ConsoleCommandSender) {
				ConsoleCommandSender console = (ConsoleCommandSender) sender;
				if (args.length == 0) {
					console.sendMessage(ChatColor.AQUA + " ------- <LightAPI " + ChatColor.WHITE
							+ getDescription().getVersion() + "> ------- ");
					console.sendMessage(
							ChatColor.AQUA + " Current version: " + ChatColor.WHITE + getDescription().getVersion());
					console.sendMessage(ChatColor.AQUA + " Server name: " + ChatColor.WHITE + getServer().getName());
					console.sendMessage(
							ChatColor.AQUA + " Server version: " + ChatColor.WHITE + getServer().getVersion());
					console.sendMessage(ChatColor.AQUA + " Source code: " + ChatColor.WHITE
							+ "http://github.com/BeYkeRYkt/LightAPI/");
					console.sendMessage(ChatColor.AQUA + " Developer: " + ChatColor.WHITE + "BeYkeRYkt");
					console.sendMessage("");
					console.sendMessage(ChatColor.WHITE + " Licensed under: " + ChatColor.AQUA + "MIT License");

					if (Build.CURRENT_VERSION == Build.VERSION_CODES.ONE) {
						console.sendMessage(ChatColor.BLACK + "backwards compatibility enabled");
					}
				} else {
					log(console, ChatColor.RED + "Hmm... This command does not exist. Are you sure write correctly ?");
				}
			}
		}
		return true;
	}

	// testing
	private boolean debug = true;
	private boolean flag = true;

	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		if (!flag || !debug)
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
		if (!flag || !debug)
			return;
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