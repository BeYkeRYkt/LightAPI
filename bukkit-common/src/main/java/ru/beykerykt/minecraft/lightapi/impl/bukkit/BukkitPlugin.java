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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import ru.beykerykt.minecraft.lightapi.common.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.common.IPluginImpl;
import ru.beykerykt.minecraft.lightapi.common.ImplementationPlatform;
import ru.beykerykt.minecraft.lightapi.common.LCallback;
import ru.beykerykt.minecraft.lightapi.common.LReason;
import ru.beykerykt.minecraft.lightapi.common.LStage;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.LightType;
import ru.beykerykt.minecraft.lightapi.common.LightingEngineVersion;

public class BukkitPlugin extends JavaPlugin implements Listener, IPluginImpl {

	private static BukkitPlugin plugin;
	private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
			BlockFace.WEST };

	// testing
	private Location prevLoc;
	private boolean debug = true;
	private boolean flag = true;

	// lightapi_extented
	private IBukkitLightHandler mHandler;

	@Override
	public void onLoad() {
		this.plugin = this;
		// set server implementation
		LightAPI.prepare(getInstance());
	}

	@Override
	public void onEnable() {
		if (debug) {
			getServer().getPluginManager().registerEvents(this, this);
		}
		if (LightAPI.get().getImplementationPlatform() == ImplementationPlatform.BUKKIT) {
			mHandler = (IBukkitLightHandler) LightAPI.get().getLightHandler();
		}
	}

	@Override
	public ImplementationPlatform getImplPlatform() {
		return ImplementationPlatform.BUKKIT;
	}

	@Override
	public IHandlerFactory getHandlerFactory() {
		return new BukkitHandlerFactory(this);
	}

	@Override
	public void log(String msg) {
		getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "<LightAPI>: " + ChatColor.WHITE + msg);
	}

	public static BukkitPlugin getInstance() {
		return plugin;
	}

	public static Block getAdjacentAirBlock(Block block) {
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
					console.sendMessage(ChatColor.AQUA + " Impl: " + ChatColor.WHITE + Build.CURRENT_IMPLEMENTATION);
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

	private boolean removeLight(Location loc) {
		List<IChunkData> moddedChunks = new CopyOnWriteArrayList<IChunkData>();
		int oldBlockLight = loc.getBlock().getLightFromBlocks();

		if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0, new LCallback() {

						@Override
						public void onSuccess(String worldName, LightType type, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage) {
							LightAPI.get().recalculateLighting(worldName, type, blockX, blockY, blockZ,
									new LCallback() {

										@Override
										public void onSuccess(String worldName, LightType type, int blockX, int blockY,
												int blockZ, int lightlevel, LStage stage) {
											if (LightAPI.get().isRequireManuallySendingChanges()) {
												for (IChunkData data : LightAPI.get().collectChunks(
														loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(),
														loc.getBlockZ(), lightlevel)) {
													if (!moddedChunks.contains(data)) {
														moddedChunks.add(data);
													}
												}

												// send changes
												for (IChunkData data : moddedChunks) {
													LightAPI.get().sendChanges(data);
												}
											} else {
												System.out.println(
														"Seems with sending changes to take care of the server itself. Do not send anything.");
											}
										}

										@Override
										public void onFailed(String worldName, LightType type, int blockX, int blockY,
												int blockZ, int lightlevel, LStage stage, LReason reason) {
											// Ah shit, here we go again
											System.out
													.println("remove-" + stage.name() + ": Ah shit, here we go again");
										}
									});
						}

						@Override
						public void onFailed(String worldName, LightType type, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage, LReason reason) {
							// Ah shit, here we go again
							System.out.println("remove-" + stage.name() + ": Ah shit, here we go again");
						}
					});

			moddedChunks.clear();
			return true;
		} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
			// delete and collect changed chunks
			if (LightAPI.get().deleteLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ())) {
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), oldBlockLight)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				} else {
					System.out.println(
							"Seems with sending changes to take care of the server itself. Do not send anything.");
				}
			} else {
				System.out.println("The light level has not changed. Check your arguments.");
				return false;
			}

			// send changes
			for (IChunkData data : moddedChunks) {
				LightAPI.get().sendChanges(data);
			}
			moddedChunks.clear();
			return true;
		}
		return false;
	}

	private boolean createLight(Location loc, int lightlevel) {
		List<IChunkData> moddedChunks = new CopyOnWriteArrayList<IChunkData>();

		if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), lightlevel, new BukkitCallback() {

						@Override
						public void onSuccess(World world, LightType type, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage) {
							LightAPI.get().recalculateLighting(world.getName(), type, blockX, blockY, blockZ,
									new BukkitCallback() {

										@Override
										public void onSuccess(World world, LightType type, int blockX, int blockY,
												int blockZ, int lightlevel, LStage stage) {
											if (LightAPI.get().isRequireManuallySendingChanges()) {
												for (IChunkData data : LightAPI.get().collectChunks(world.getName(),
														blockX, blockY, blockZ, lightlevel)) {
													LightAPI.get().sendChanges(data);
												}
											}
										}

										@Override
										public void onFailed(World world, LightType type, int blockX, int blockY,
												int blockZ, int lightlevel, LStage stage, LReason reason) {
											// Ah shit, here we go again
											System.out.println("create-" + stage.name() + "-" + reason.name()
													+ ": Ah shit, here we go again");
										}
									});
						}

						@Override
						public void onFailed(World world, LightType type, int blockX, int blockY, int blockZ,
								int lightlevel, LStage stage, LReason reason) {
							// Ah shit, here we go again
							System.out.println("create-" + stage.name() + ": Ah shit, here we go again");
						}
					});
			moddedChunks.clear();
			return true;
		} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
			// create and collect changed chunks
			if (LightAPI.get().createLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), lightlevel)) {
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), lightlevel)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				} else {
					System.out.println(
							"Seems with sending changes to take care of the server itself. Do not send anything.");
				}
			} else {
				System.out.println("The light level has not changed. Check your arguments.");
				return false;
			}

			// send changes
			for (IChunkData data : moddedChunks) {
				LightAPI.get().sendChanges(data);
			}
			moddedChunks.clear();
			return true;
		}
		return false;
	}

	private void recreateLight(Location loc) {
		List<IChunkData> moddedChunks = new CopyOnWriteArrayList<IChunkData>();
		int oldBlockLight = loc.getBlock().getLightFromBlocks();

		// pre 1.14
		if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
			// delete and collect changed chunks
			if (LightAPI.get().deleteLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ())) {
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), oldBlockLight)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				} else {
					System.out.println(
							"Seems with sending changes to take care of the server itself. Do not send anything.");
				}
			} else {
				System.out.println("The light level has not changed. Check your arguments.");
			}

			// create and collect changed chunks
			if (LightAPI.get().createLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight)) {
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), oldBlockLight)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				} else {
					System.out.println(
							"Seems with sending changes to take care of the server itself. Do not send anything.");
				}
			} else {
				System.out.println("The light level has not changed. Check your arguments.");
			}
			// 1.14+
		} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
			/**
			 * (IN 1.14) createLight() and deleteLight() are designed to be used once,
			 * because after you call them, they immediately trigger lighting recalculation.
			 * To avoid unnecessary recalculations, you can use setRawLightLevel() and call
			 * recalculateLighting() after all changes.
			 */
			// delete and collect changed chunks
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0);
			if (LightAPI.get().isRequireManuallySendingChanges()) {
				for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), oldBlockLight)) {
					if (!moddedChunks.contains(data)) {
						moddedChunks.add(data);
					}
				}
			}

			// create and collect changed chunks
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
			LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
					loc.getBlockY(), loc.getBlockZ());
			if (LightAPI.get().isRequireManuallySendingChanges()) {
				for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), oldBlockLight)) {
					if (!moddedChunks.contains(data)) {
						moddedChunks.add(data);
					}
				}
			}
		}

		// send changes
		for (IChunkData data : moddedChunks) {
			LightAPI.get().sendChanges(data);
		}
		moddedChunks.clear();
	}

	private boolean disableHardBench = false;

	/**
	 * Benchmark
	 * 
	 * @param loc - world location
	 */
	private void cycleRecreate(Location loc) {
		int oldBlockLight = 15;

		///////////////////////////////////////////
		long time_start = System.currentTimeMillis();
		for (int i = 0; i < 99; i++) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0);
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
		}
		if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
			LightAPI.get().createLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
		} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), oldBlockLight);
			LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
					loc.getBlockY(), loc.getBlockZ());
		}
		long time_end = System.currentTimeMillis() - time_start;
		System.out.println("< #1 cycle raw + once recalc: " + time_end);
		LightAPI.get().deleteLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
				loc.getBlockZ());
		LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
				loc.getBlockZ());

		///////////////////////////////////////////
		time_start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
					loc.getBlockZ(), 0);
			if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
				LightAPI.get().createLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ(), oldBlockLight);
			} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
				LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), oldBlockLight);
				LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ());
			}
		}
		time_end = System.currentTimeMillis() - time_start;
		System.out.println("< #2 cycle raw + cycle recalc: " + time_end);
		LightAPI.get().deleteLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
				loc.getBlockZ());
		LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
				loc.getBlockZ());

		///////////////////////////////////////////
		if (!disableHardBench) {
			try {
				time_start = System.currentTimeMillis();
				for (int i = 0; i < 100; i++) {
					if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
						LightAPI.get().deleteLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ());
						LightAPI.get().createLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ(), oldBlockLight);
					} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
						LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ(), 0);
						LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ());
						LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ(), oldBlockLight);
						LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
								loc.getBlockY(), loc.getBlockZ());
					}
				}
				time_end = System.currentTimeMillis() - time_start;
				System.out.println("< #3 cycle raw + cycle recalc: " + time_end);
				LightAPI.get().deleteLight(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(), loc.getBlockY(),
						loc.getBlockZ());
				LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ());
			} catch (Exception ex) {
				ex.printStackTrace();
				disableHardBench = true;
			}
		}
	}

	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		int lightlevel = 12;
		if (!flag || !debug)
			return;

		if (event.getItem() == null)
			return;
		if (event.getItem().getType() == Material.GLOWSTONE_DUST) {
			event.setCancelled(true);
			if (LightAPI.get().getLightingEngineVersion() != LightingEngineVersion.V2)
				return; // Sorry but i'm lazy

			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				List<IChunkData> moddedChunks = new CopyOnWriteArrayList<IChunkData>();
				Location loc = event.getClickedBlock().getLocation();
				int blockLight = event.getClickedBlock().getLightFromBlocks();
				// delete and collect changed chunks
				LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), 0);
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), blockLight)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				}

				// create and collect changed chunks
				LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), blockLight - 1);
				LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ());
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), blockLight - 1)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				}

				for (IChunkData data : moddedChunks) {
					LightAPI.get().sendChanges(data);
				}
				return;
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				List<IChunkData> moddedChunks = new CopyOnWriteArrayList<IChunkData>();
				Location loc = event.getClickedBlock().getLocation();
				int blockLight = event.getClickedBlock().getLightFromBlocks();

				// create and collect changed chunks
				LightAPI.get().setRawLightLevel(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ(), blockLight + 1);
				LightAPI.get().recalculateLighting(loc.getWorld().getName(), LightType.BLOCK, loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ());
				if (LightAPI.get().isRequireManuallySendingChanges()) {
					for (IChunkData data : LightAPI.get().collectChunks(loc.getWorld().getName(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), blockLight + 1)) {
						if (!moddedChunks.contains(data)) {
							moddedChunks.add(data);
						}
					}
				}

				for (IChunkData data : moddedChunks) {
					LightAPI.get().sendChanges(data);
				}
				return;
			}
		} else if (event.getItem().getType() == Material.STICK) {
			event.setCancelled(true);
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();

				// pre 1.14
				if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
					if (prevLoc != null) {
						int blockLight = prevLoc.getBlock().getLightFromBlocks();
						// remove and collect changed chunks
						if (LightAPI.get().deleteLight(prevLoc.getWorld().getName(), LightType.BLOCK,
								prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ())) {
							if (LightAPI.get().isRequireManuallySendingChanges()) {
								for (IChunkData data : LightAPI.get().collectChunks(prevLoc.getWorld().getName(),
										prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), blockLight)) {
									if (!list.contains(data)) {
										list.add(data);
									}
								}
							}
						}
					}
					prevLoc = event.getClickedBlock().getLocation();
					if (LightAPI.get().createLight(prevLoc.getWorld().getName(), LightType.BLOCK, prevLoc.getBlockX(),
							prevLoc.getBlockY(), prevLoc.getBlockZ(), lightlevel)) {
						if (LightAPI.get().isRequireManuallySendingChanges()) {
							for (IChunkData data : LightAPI.get().collectChunks(prevLoc.getWorld().getName(),
									prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), lightlevel)) {
								if (!list.contains(data)) {
									list.add(data);
								}
							}
						}
					}

					for (IChunkData data : list) {
						LightAPI.get().sendChanges(data);
					}
					// 1.14+
				} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
					if (prevLoc != null) {
						// remove and collect changed chunks
						int blockLight = prevLoc.getBlock().getLightFromBlocks();
						LightAPI.get().setRawLightLevel(prevLoc.getWorld().getName(), LightType.BLOCK,
								prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), 0);
						if (LightAPI.get().isRequireManuallySendingChanges()) {
							for (IChunkData data : LightAPI.get().collectChunks(prevLoc.getWorld().getName(),
									prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), blockLight)) {
								if (!list.contains(data)) {
									list.add(data);
								}
							}
						}
					}
					prevLoc = event.getClickedBlock().getLocation();
					LightAPI.get().setRawLightLevel(prevLoc.getWorld().getName(), LightType.BLOCK, prevLoc.getBlockX(),
							prevLoc.getBlockY(), prevLoc.getBlockZ(), lightlevel);
					LightAPI.get().recalculateLighting(prevLoc.getWorld().getName(), LightType.BLOCK,
							prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ());
					if (LightAPI.get().isRequireManuallySendingChanges()) {
						for (IChunkData data : LightAPI.get().collectChunks(prevLoc.getWorld().getName(),
								prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), lightlevel)) {
							if (!list.contains(data)) {
								list.add(data);
							}
						}
					}

					for (IChunkData data : list) {
						LightAPI.get().sendChanges(data);
					}
				}
			} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				List<IChunkData> list = new CopyOnWriteArrayList<IChunkData>();

				// pre 1.14
				if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V1) {
					if (prevLoc != null) {
						int blockLight = prevLoc.getBlock().getLightFromBlocks();
						// remove and collect changed chunks
						if (LightAPI.get().deleteLight(prevLoc.getWorld().getName(), LightType.BLOCK,
								prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ())) {
							if (LightAPI.get().isRequireManuallySendingChanges()) {
								for (IChunkData data : LightAPI.get().collectChunks(prevLoc.getWorld().getName(),
										prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), blockLight)) {
									if (!list.contains(data)) {
										list.add(data);
									}
								}
							}
						}
					}
					// 1.14+
				} else if (LightAPI.get().getLightingEngineVersion() == LightingEngineVersion.V2) {
					if (prevLoc != null) {
						// remove and collect changed chunks
						int blockLight = prevLoc.getBlock().getLightFromBlocks();
						LightAPI.get().setRawLightLevel(prevLoc.getWorld().getName(), LightType.BLOCK,
								prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), 0);
						LightAPI.get().recalculateLighting(prevLoc.getWorld().getName(), LightType.BLOCK,
								prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ());
						if (LightAPI.get().isRequireManuallySendingChanges()) {
							for (IChunkData data : LightAPI.get().collectChunks(prevLoc.getWorld().getName(),
									prevLoc.getBlockX(), prevLoc.getBlockY(), prevLoc.getBlockZ(), blockLight)) {
								if (!list.contains(data)) {
									list.add(data);
								}
							}
						}
					}
				}
				for (IChunkData data : list) {
					LightAPI.get().sendChanges(data);
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
			if (createLight(p.getLocation(), 15)) {
				p.sendMessage("Light placed!");
				event.setCancelled(true);
			}
		} else if (event.getMessage().equals("delete")) {
			if (removeLight(p.getLocation())) {
				p.sendMessage("Light deleted!");
				event.setCancelled(true);
			}
		} else if (event.getMessage().equals("recreate")) {
			recreateLight(p.getLocation());
			event.setCancelled(true);
		} else if (event.getMessage().equals("bench")) {
			cycleRecreate(p.getLocation());
			event.setCancelled(true);
		} else if (event.getMessage().equals("disable")) {
			flag = false;
			event.setCancelled(true);
		} else if (event.getMessage().equals("enable")) {
			flag = true;
			event.setCancelled(true);
		}
	}
}