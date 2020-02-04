/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.bukkit.ChunkSenderExecutorService;
import ru.beykerykt.minecraft.lightapi.bukkit.DebugListener;
import ru.beykerykt.minecraft.lightapi.common.Build;
import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.impl.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.common.impl.ImplementationPlatform;
import ru.beykerykt.minecraft.lightapi.common.impl.LightingEngineVersion;

public class BukkitPlugin extends JavaPlugin implements IBukkitPluginImpl {

	private static BukkitPlugin plugin;
	private static IBukkitHandlerImpl mHandler;

	private static final int mConfigVersion = 1;

	private static final String CRAFTBUKKIT_PKG = "org.bukkit.craftbukkit";

	public ChunkSenderExecutorService mService = null;

	// testing
	private boolean debug = true;

	@Override
	public void onLoad() {
		this.plugin = this;
		mService = new ChunkSenderExecutorService();

		try {
			FileConfiguration fc = getConfig();
			File file = new File(getDataFolder(), "config.yml");
			if (file.exists()) {
				if (fc.getInt("version") < mConfigVersion) {
					if (!file.delete()) {
						throw new IOException("Can not delete " + file.getPath());
					}
					generateConfig(file);
				}
			} else {
				generateConfig(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		try {
			mHandler = (IBukkitHandlerImpl) getHandlerFactory().createHandler();
			if (mHandler == null) {
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		} catch (Exception ex) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		// set server implementation
		LightAPI.prepare(getInstance());
		int repeat_delay = 1;
		boolean flag = getConfig().getBoolean("merge-chunk-sections");
		mService.start(repeat_delay, flag);
		if (debug) {
			getServer().getPluginManager().registerEvents(new DebugListener(this), this);
		}
	}

	@Override
	public void onDisable() {
		if (mService != null) {
			mService.shutdown();
		}
	}

	@Override
	public ImplementationPlatform getImplPlatform() {
		ImplementationPlatform platform = ImplementationPlatform.BUKKIT;
		String serverImplPackage = getServer().getClass().getPackage().getName();
		if (serverImplPackage.startsWith(CRAFTBUKKIT_PKG)) {
			platform = ImplementationPlatform.CRAFTBUKKIT;
		}
		return platform;
	}

	private IHandlerFactory getHandlerFactory() {
		return new BukkitHandlerFactory(this);
	}

	@Override
	public void log(String msg) {
		getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + msg);
	}

	public static BukkitPlugin getInstance() {
		return plugin;
	}

	public boolean isBackwardAvailable() {
		boolean flag = Build.CURRENT_VERSION <= Build.VERSION_CODES.FOUR;
		try {
			Class.forName("ru.beykerykt.lightapi.LightAPI");
			return flag & true;
		} catch (ClassNotFoundException ex) {
		}
		return flag & false;
	}

	public void log(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + message);
	}

	private void generateConfig(File file) {
		FileConfiguration fc = getConfig();
		if (!file.exists()) {
			fc.set("version", mConfigVersion);
			fc.set("specific-nms-handler", "none");
			fc.set("merge-chunk-sections", true);
			saveConfig();
		}
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

					player.sendMessage(ChatColor.AQUA + " Impl: " + ChatColor.WHITE + Build.CURRENT_IMPLEMENTATION);

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

					TextComponent developer = new TextComponent(ChatColor.AQUA + "Developers");
					List<String> authors = getDescription().getAuthors();
					String authorsLine = "none";
					if (authors.size() > 0) {
						authorsLine = authors.get(0);
						if (authors.size() > 1) {
							for (int i = 1; i < authors.size() - 1; i++) {
								authorsLine += ", " + authors.get(i);
							}
							authorsLine += " and " + authors.get(authors.size() - 1);
						}
					}
					developer.setHoverEvent(
							new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(authorsLine).create()));
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

					if (isBackwardAvailable()) {
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
					console.sendMessage(ChatColor.AQUA + " Impl: " + ChatColor.WHITE + Build.CURRENT_IMPLEMENTATION);
					console.sendMessage(ChatColor.AQUA + " Server name: " + ChatColor.WHITE + getServer().getName());
					console.sendMessage(
							ChatColor.AQUA + " Server version: " + ChatColor.WHITE + getServer().getVersion());
					console.sendMessage(ChatColor.AQUA + " Source code: " + ChatColor.WHITE
							+ "http://github.com/BeYkeRYkt/LightAPI/");
					List<String> authors = getDescription().getAuthors();
					String authorsLine = "none";
					if (authors.size() > 0) {
						authorsLine = authors.get(0);
						if (authors.size() > 1) {
							for (int i = 1; i < authors.size() - 1; i++) {
								authorsLine += ", " + authors.get(i);
							}
							authorsLine += " and " + authors.get(authors.size() - 1);
						}
					}
					console.sendMessage(ChatColor.AQUA + " Developers: " + ChatColor.WHITE + authorsLine);
					console.sendMessage("");
					console.sendMessage(ChatColor.WHITE + " Licensed under: " + ChatColor.AQUA + "MIT License");

					if (isBackwardAvailable()) {
						console.sendMessage(ChatColor.BLACK + "backwards compatibility enabled");
					}
				} else {
					log(console, ChatColor.RED + "Hmm... This command does not exist. Are you sure write correctly ?");
				}
			}
		}
		return true;
	}

	@Override
	public IBukkitHandlerImpl getHandlerImpl() {
		if (mHandler == null) {
			throw new IllegalStateException("HandlerImpl not yet initialized! Use prepare() !");
		}
		return mHandler;
	}

	@Override
	public LightingEngineVersion getLightingEngineVersion() {
		return getHandlerImpl().getLightingEngineVersion();
	}

	@Override
	public boolean isAsyncLighting() {
		return getHandlerImpl().isAsyncLighting();
	}

	@Override
	public boolean createLight(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel) {
		return createLight(worldName, flags, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public boolean createLight(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		World world = getServer().getWorld(worldName);
		return createLight(world, flags, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public boolean deleteLight(String worldName, int flags, int blockX, int blockY, int blockZ) {
		return deleteLight(worldName, flags, blockX, blockY, blockZ, null);
	}

	@Override
	public boolean deleteLight(String worldName, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		World world = getServer().getWorld(worldName);
		return deleteLight(world, flags, blockX, blockY, blockZ, callback);
	}

	@Override
	public void setRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel) {
		setRawLightLevel(worldName, flags, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public void setRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		getHandlerImpl().setRawLightLevel(worldName, flags, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public int getRawLightLevel(String worldName, int flags, int blockX, int blockY, int blockZ) {
		return getHandlerImpl().getRawLightLevel(worldName, flags, blockX, blockY, blockZ);
	}

	@Override
	public boolean isRequireRecalculateLighting() {
		return getHandlerImpl().isRequireRecalculateLighting();
	}

	@Override
	public void recalculateLighting(String worldName, int flags, int blockX, int blockY, int blockZ) {
		recalculateLighting(worldName, flags, blockX, blockY, blockZ, null);
	}

	@Override
	public void recalculateLighting(String worldName, int flags, int blockX, int blockY, int blockZ,
			LCallback callback) {
		getHandlerImpl().recalculateLighting(worldName, flags, blockX, blockY, blockZ, callback);
	}

	@Override
	public boolean isRequireManuallySendingChanges() {
		return getHandlerImpl().isRequireManuallySendingChanges();
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ,
			int lightlevel) {
		return getHandlerImpl().collectChunkSections(worldName, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(String worldName, int blockX, int blockY, int blockZ) {
		int lightlevel = getRawLightLevel(worldName, LightFlags.COMBO_LIGHTING, blockX, blockY, blockZ);
		return collectChunkSections(worldName, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public void sendChanges(String worldName, int chunkX, int chunkZ) {
		World world = getServer().getWorld(worldName);
		sendChanges(world, chunkX, chunkZ);
	}

	@Override
	public void sendChanges(IChunkSectionsData chunkData) {
		addChunkToUpdate(chunkData);
	}

	@Override
	public boolean createLight(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel) {
		return createLight(world, flags, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public boolean createLight(Location location, int flags, int lightlevel) {
		return createLight(location.getWorld(), flags, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
				lightlevel, null);
	}

	@Override
	public boolean createLight(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		return getHandlerImpl().createLight(world, flags, blockX, blockY, blockZ, lightlevel, callback);
	}

	@Override
	public boolean createLight(Location location, int flags, int lightlevel, LCallback callback) {
		return createLight(location.getWorld(), flags, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
				lightlevel, callback);
	}

	@Override
	public boolean deleteLight(World world, int flags, int blockX, int blockY, int blockZ) {
		return deleteLight(world, flags, blockX, blockY, blockZ, null);
	}

	@Override
	public boolean deleteLight(Location location, int flags) {
		return deleteLight(location.getWorld(), flags, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
				null);
	}

	@Override
	public boolean deleteLight(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		return getHandlerImpl().deleteLight(world, flags, blockX, blockY, blockZ, callback);
	}

	@Override
	public boolean deleteLight(Location location, int flags, LCallback callback) {
		return deleteLight(location.getWorld(), flags, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
				callback);
	}

	@Override
	public void setRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel) {
		setRawLightLevel(world, flags, blockX, blockY, blockZ, lightlevel, null);
	}

	@Override
	public void setRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		getHandlerImpl().setRawLightLevel(world, flags, blockX, blockY, blockZ, lightlevel, callback);
	}

	@Override
	public void setRawLightLevel(Location location, int flags, int lightlevel) {
		setRawLightLevel(location, flags, lightlevel, null);
	}

	@Override
	public void setRawLightLevel(Location location, int flags, int lightlevel, LCallback callback) {
		setRawLightLevel(location.getWorld(), flags, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
				lightlevel, callback);
	}

	@Override
	public int getRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ) {
		return getHandlerImpl().getRawLightLevel(world, flags, blockX, blockY, blockZ);
	}

	@Override
	public int getRawLightLevel(Location location, int flags) {
		return getRawLightLevel(location.getWorld(), flags, location.getBlockX(), location.getBlockY(),
				location.getBlockZ());
	}

	@Override
	public void recalculateLighting(World world, int flags, int blockX, int blockY, int blockZ) {
		recalculateLighting(world, flags, blockX, blockY, blockZ, null);
	}

	@Override
	public void recalculateLighting(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		getHandlerImpl().recalculateLighting(world, flags, blockX, blockY, blockZ, callback);
	}

	@Override
	public void recalculateLighting(Location location, int flags) {
		recalculateLighting(location, flags, null);
	}

	@Override
	public void recalculateLighting(Location location, int flags, LCallback callback) {
		recalculateLighting(location.getWorld(), flags, location.getBlockX(), location.getBlockY(),
				location.getBlockZ(), callback);
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(World world, int blockX, int blockY, int blockZ,
			int lightlevel) {
		return getHandlerImpl().collectChunkSections(world, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(World world, int blockX, int blockY, int blockZ) {
		int lightlevel = getRawLightLevel(world, LightFlags.COMBO_LIGHTING, blockX, blockY, blockZ);
		return collectChunkSections(world, blockX, blockY, blockZ, lightlevel);
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(Location location, int lightlevel) {
		return collectChunkSections(location.getWorld(), location.getBlockX(), location.getBlockY(),
				location.getBlockZ(), lightlevel);
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(Location location) {
		int lightlevel = getRawLightLevel(location, LightFlags.COMBO_LIGHTING);
		return collectChunkSections(location.getWorld(), location.getBlockX(), location.getBlockY(),
				location.getBlockZ(), lightlevel);
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ) {
		addChunkToUpdate(new BukkitChunkSectionsData(world, chunkX, chunkZ));
	}

	protected void addChunkToUpdate(IChunkSectionsData chunkData) {
		if (mService != null) {
			mService.addChunkToUpdate(chunkData);
		}
	}
}