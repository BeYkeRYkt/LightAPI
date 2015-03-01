package ru.BeYkeRYkt.LightAPI.nms.CraftBukkit.v1_7_R2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_7_R2.Entity;
import net.minecraft.server.v1_7_R2.EntityHuman;
import net.minecraft.server.v1_7_R2.EnumSkyBlock;
import net.minecraft.server.v1_7_R2.IWorldAccess;
import net.minecraft.server.v1_7_R2.PlayerChunkMap;
import net.minecraft.server.v1_7_R2.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R2.CraftWorld;

import ru.BeYkeRYkt.LightAPI.nms.INMSHandler;

public class NMSHandler implements INMSHandler {

    private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static Method cachedPlayerChunk;
    private static Field cachedDirtyField;
    private List<IWorldAccess> worlds = new ArrayList<IWorldAccess>();

    @Override
    public void initWorlds() {
        for (World worlds : Bukkit.getWorlds()) {
            WorldServer nmsWorld = ((CraftWorld) worlds).getHandle();
            IWorldAccess access = getLightIWorldAccess(worlds);

            nmsWorld.addIWorldAccess(access);
            this.worlds.add(access);
        }
    }

    @Override
    public void unloadWorlds() {
        try {
            for (World worlds : Bukkit.getWorlds()) {
                WorldServer nmsWorld = ((CraftWorld) worlds).getHandle();

                for (IWorldAccess access : this.worlds) {
                    Field field = net.minecraft.server.v1_7_R2.World.class.getDeclaredField("u");
                    field.setAccessible(true);
                    ((List<?>) field.get(nmsWorld)).remove(access);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recalculateBlockLighting(World world, int x, int y, int z) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        nmsWorld.t(x, y, z);
    }

    @Override
    public void createLight(Location location, int light) {
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        world.b(EnumSkyBlock.BLOCK, location.getBlockX(), location.getBlockY(), location.getBlockZ(), light);

        Block adjacent = getAdjacentAirBlock(location.getBlock());
        recalculateBlockLighting(location.getWorld(), adjacent.getX(), adjacent.getY(), adjacent.getZ());
        updateChunk(location);
    }

    public void updateChunk(Location location) {
        try {
            // from: https://gist.github.com/aadnk/5841942
            // Thanks Comphenix!
            WorldServer nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
            PlayerChunkMap map = nmsWorld.getPlayerChunkMap();

            for (int dX = -1; dX <= 1; dX++) {
                for (int dZ = -1; dZ <= 1; dZ++) {
                    Object playerChunk = getPlayerCountMethod().invoke(map, location.getChunk().getX() + dX, location.getChunk().getZ() + dZ, false);

                    if (playerChunk != null) {
                        Field dirtyField = getDirtyField(playerChunk);
                        int dirtyCount = (Integer) dirtyField.get(playerChunk);
                        if (dirtyCount > 0 && dirtyCount != 1 && dirtyCount < 64) {
                            dirtyField.set(playerChunk, 64);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteLight(Location loc) {
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
        world.c(EnumSkyBlock.BLOCK, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        updateChunk(loc);
    }

    private static Method getPlayerCountMethod() throws NoSuchMethodException, SecurityException {
        if (cachedPlayerChunk == null) {
            cachedPlayerChunk = PlayerChunkMap.class.getDeclaredMethod("a", int.class, int.class, boolean.class);
            cachedPlayerChunk.setAccessible(true);
        }
        return cachedPlayerChunk;
    }

    private static Field getDirtyField(Object playerChunk) throws NoSuchFieldException, SecurityException {
        if (cachedDirtyField == null) {
            cachedDirtyField = playerChunk.getClass().getDeclaredField("dirtyCount");
            cachedDirtyField.setAccessible(true);
        }
        return cachedDirtyField;
    }

    public Block getAdjacentAirBlock(Block block) {
        for (BlockFace face : SIDES) {
            if (block.getY() == 0x0 && face == BlockFace.DOWN)
                continue;
            if (block.getY() == 0xFF && face == BlockFace.UP)
                continue;

            Block candidate = block.getRelative(face);

            if (candidate.getType().isTransparent()) {
                return candidate;
            }
        }
        return block;
    }

    public IWorldAccess getLightIWorldAccess(final org.bukkit.World world) {
        final PlayerChunkMap map = ((CraftWorld) world).getHandle().getPlayerChunkMap();
        return new IWorldAccess() {

            @Override
            // markBlockForUpdate
            public void a(int x, int y, int z) {
            }

            @Override
            // markBlockForRenderUpdate
            public void b(int x, int y, int z) {
                map.flagDirty(x, y, z);
            }

            @Override
            // destroyBlockPartially
            public void b(int arg0, int arg1, int arg2, int arg3, int arg4) {
            }

            @Override
            // playAuxSFX
            public void a(EntityHuman arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
            }

            @Override
            // markBlockRangeForRenderUpdate
            public void a(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
                // Ignore
            }

            @Override
            // broadcastSound
            public void a(int arg0, int arg1, int arg2, int arg3, int arg4) {
            }

            @Override
            // playSound
            public void a(String arg0, double arg1, double arg2, double arg3, float arg4, float arg5) {
            }

            @Override
            // playSoundToNearExcept
            public void a(EntityHuman arg0, String arg1, double arg2, double arg3, double arg4, float arg5, float arg6) {
            }

            @Override
            // spawnParticle
            public void a(String arg0, double arg1, double arg2, double arg3, double arg4, double arg5, double arg6) {
            }

            @Override
            // playRecord
            public void a(String arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            // onEntityCreate
            public void a(Entity arg0) {
            }

            @Override
            // onEntityDestroy (probably)
            public void b(Entity arg0) {
            }

            @Override
            public void b() {
            }
        };
    }
}