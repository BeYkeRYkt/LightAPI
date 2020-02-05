/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
 * Copyright (c) 2019 Qveshn
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
package ru.beykerykt.minecraft.lightapi.bukkit.impl.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.bukkit.impl.BukkitHandlerImpl;
import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.LightFlags;
import ru.beykerykt.minecraft.lightapi.common.callback.LCallback;
import ru.beykerykt.minecraft.lightapi.common.callback.LStage;
import ru.beykerykt.minecraft.lightapi.common.impl.LightingEngineVersion;

public class CraftBukkit_v1_13_R2 extends BukkitHandlerImpl {

	private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
			BlockFace.WEST };

	// Qveshn - start
	private int getDeltaLight(int x, int dx) {
		return (((x ^ ((-dx >> 4) & 15)) + 1) & (-(dx & 1)));
	}

	public int asSectionMask(int sectionY) {
		return 1 << sectionY;
	}

	private int getThreeSectionsMask(int y) {
		return (isValidSectionY(y) ? asSectionMask(y) : 0) | (isValidSectionY(y - 1) ? asSectionMask(y - 1) : 0)
				| (isValidSectionY(y + 1) ? asSectionMask(y + 1) : 0);
	}

	public boolean isValidSectionY(int sectionY) {
		return sectionY >= 0 && sectionY <= 16;
	}
	// Qvesh - end

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

	private int distanceTo(Chunk from, Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName()))
			return 100;
		double var2 = to.locX - from.locX;
		double var4 = to.locZ - from.locZ;
		return (int) Math.sqrt(var2 * var2 + var4 * var4);
	}

	public int parseViewDistance(EntityPlayer player) {
		int viewDistance = Bukkit.getViewDistance();
		try {
			int playerViewDistance = Integer.valueOf(player.clientViewDistance);
			if (playerViewDistance < viewDistance) {
				viewDistance = playerViewDistance;
			}
		} catch (Exception e) {
			// silent
		}
		return viewDistance;
	}

	/***********************************************************************************************************************/
	@Override
	public List<String> getAuthors() {
		return new ArrayList<String>(Arrays.asList("BeYkeRYkt", "Qvesh"));
	}

	@Override
	public boolean createLight(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
			}
			return false;
		}

		int oldlightlevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		setRawLightLevel(world, flags, blockX, blockY, blockZ, lightlevel, callback);

		Block adjacent = getAdjacentAirBlock(world.getBlockAt(blockX, blockY, blockZ));
		int ax = adjacent.getX();
		int ay = adjacent.getY();
		int az = adjacent.getZ();
		recalculateLighting(world, flags, ax, ay, az, callback);

		// check light
		int newLightLevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		if (newLightLevel >= oldlightlevel) {
			if (callback != null) {
				callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
			}
			return true;
		}

		if (callback != null) {
			callback.onFailed(world.getName(), flags, blockX, blockY, blockZ, lightlevel, LStage.CREATING);
		}
		return false;
	}

	@Override
	public boolean deleteLight(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, 0, LStage.DELETING);
			}
			return false;
		}

		int oldlightlevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		recalculateLighting(world, flags, blockX, blockY, blockZ, callback);

		// check light
		int newLightLevel = getRawLightLevel(world, flags, blockX, blockY, blockZ);
		if (newLightLevel != oldlightlevel) {
			if (callback != null) {
				callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, 0, LStage.DELETING);
			}
			return true;
		}

		if (callback != null) {
			callback.onFailed(world.getName(), flags, blockX, blockY, blockZ, 0, LStage.DELETING);
		}
		return false;
	}

	@Override
	public LightingEngineVersion getLightingEngineVersion() {
		return LightingEngineVersion.V1;
	}

	@Override
	public boolean isAsyncLighting() {
		return false;
	}

	@Override
	public void setRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ, int lightlevel,
			LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, lightlevel, LStage.WRITTING);
			}
			return;
		}
		final int finalLightLevel = lightlevel < 0 ? 0 : lightlevel > 15 ? 15 : lightlevel;
		if (finalLightLevel == 0) {
			recalculateLighting(world, flags, blockX, blockY, blockZ, callback);
			if (callback != null) {
				callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, finalLightLevel, LStage.WRITTING);
			}
			return;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
			worldServer.a(EnumSkyBlock.BLOCK, position, finalLightLevel);
		}
		if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
			worldServer.a(EnumSkyBlock.SKY, position, finalLightLevel);
		}
		if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
			worldServer.a(EnumSkyBlock.BLOCK, position, finalLightLevel);
			worldServer.a(EnumSkyBlock.SKY, position, finalLightLevel);
		}
		if (callback != null) {
			callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, finalLightLevel, LStage.WRITTING);
		}
	}

	@Override
	public int getRawLightLevel(World world, int flags, int blockX, int blockY, int blockZ) {
		if (world == null) {
			return -1;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
		int lightlevel = -1;
		if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
			lightlevel = worldServer.getBrightness(EnumSkyBlock.BLOCK, position);
		} else if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
			lightlevel = worldServer.getBrightness(EnumSkyBlock.SKY, position);
		} else if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
			lightlevel = worldServer.getLightLevel(position);
		}
		return lightlevel;
	}

	@Override
	public boolean isRequireRecalculateLighting() {
		return true;
	}

	@Override
	public void recalculateLighting(World world, int flags, int blockX, int blockY, int blockZ, LCallback callback) {
		if (world == null) {
			if (callback != null) {
				callback.onFailed("", flags, blockX, blockY, blockZ, 0, LStage.RECALCULATING);
			}
			return;
		}
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		BlockPosition adjacentPosition = new BlockPosition(blockX, blockY, blockZ);
		if ((flags & LightFlags.BLOCK_LIGHTING) == LightFlags.BLOCK_LIGHTING) {
			worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
		}
		if ((flags & LightFlags.SKY_LIGHTING) == LightFlags.SKY_LIGHTING) {
			worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
		}
		if ((flags & LightFlags.COMBO_LIGHTING) == LightFlags.COMBO_LIGHTING) {
			// i don't known
			worldServer.c(EnumSkyBlock.BLOCK, adjacentPosition);
			worldServer.c(EnumSkyBlock.SKY, adjacentPosition);
		}
		if (callback != null) {
			callback.onSuccess(world.getName(), flags, blockX, blockY, blockZ, 0, LStage.RECALCULATING);
		}
	}

	@Override
	public boolean isRequireManuallySendingChanges() {
		return true;
	}

	@Override
	public List<IChunkSectionsData> collectChunkSections(World world, int blockX, int blockY, int blockZ,
			int lightlevel) {
		List<IChunkSectionsData> list = new ArrayList<IChunkSectionsData>();
		if (world == null) {
			return list;
		}

		final int finalLightLevel = lightlevel < 0 ? 0 : lightlevel > 15 ? 15 : lightlevel;
		if (finalLightLevel > 0) {
			for (int dX = -1; dX <= 1; dX++) {
				int lightLevelX = finalLightLevel - getDeltaLight(blockX & 15, dX);
				if (lightLevelX > 0) {
					for (int dZ = -1; dZ <= 1; dZ++) {
						int lightLevelZ = lightLevelX - getDeltaLight(blockZ & 15, dZ);
						if (lightLevelZ > 0) {
							int chunkX = blockX >> 4;
							int chunkZ = blockZ >> 4;
							int sectionMask = 0;
							for (int dY = -1; dY <= 1; dY++) {
								if (lightLevelZ > getDeltaLight(blockY & 15, dY)) {
									int sectionY = (blockY >> 4) + dY;
									if (isValidSectionY(sectionY)) {
										sectionMask |= asSectionMask(sectionY);
									}
								}
							}

							// don't add null section mask
							if (sectionMask > 0) {
								IChunkSectionsData chunkData = new BukkitChunkSectionsData(world, chunkX + dX,
										chunkZ + dZ, sectionMask, sectionMask);
								if (!list.contains(chunkData)) {
									list.add(chunkData);
								}
							}
						}
					}
				}
			}
		}
		return list;
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ) {
		if (world == null) {
			return;
		}
		for (int i = 0; i < world.getPlayers().size(); i++) {
			Player player = world.getPlayers().get(i);
			Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
			EntityPlayer human = ((CraftPlayer) player).getHandle();
			Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
			int playerViewDistance = parseViewDistance(human);

			if (distanceTo(pChunk, chunk) <= playerViewDistance) {
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
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkSectionY, int chunkZ) {
		if (world == null) {
			return;
		}
		for (int i = 0; i < world.getPlayers().size(); i++) {
			Player player = world.getPlayers().get(i);
			Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
			EntityPlayer human = ((CraftPlayer) player).getHandle();
			Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
			int playerViewDistance = parseViewDistance(human);

			if (distanceTo(pChunk, chunk) <= playerViewDistance) {
				// Last argument is bit-mask what chunk sections to update. Only lower 16 bits
				// are used.
				// There are 16 sections in chunk. Each section height=16. So, y-coordinate
				// varies from 0 to 255.
				int mask = getThreeSectionsMask(chunkSectionY);
				PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, mask);
				human.playerConnection.sendPacket(packet);
			}
		}
	}

	@Override
	public void sendChanges(World world, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
		if (world == null) {
			return;
		}
		for (int i = 0; i < world.getPlayers().size(); i++) {
			Player player = world.getPlayers().get(i);
			Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(chunkX, chunkZ);
			EntityPlayer human = ((CraftPlayer) player).getHandle();
			Chunk pChunk = human.world.getChunkAtWorldCoords(human.getChunkCoordinates());
			int playerViewDistance = parseViewDistance(human);

			if (distanceTo(pChunk, chunk) <= playerViewDistance) {
				// Last argument is bit-mask what chunk sections to update. Only lower 16 bits
				// are used.
				// There are 16 sections in chunk. Each section height=16. So, y-coordinate
				// varies from 0 to 255.
				// We know that max light=15 (15 blocks). So, it is enough to update only 3
				// sections: y\16-1, y\16, y\16+1
				int mask = sectionMaskSky | sectionMaskBlock;
				PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, mask);
				human.playerConnection.sendPacket(packet);
			}
		}
	}
}
