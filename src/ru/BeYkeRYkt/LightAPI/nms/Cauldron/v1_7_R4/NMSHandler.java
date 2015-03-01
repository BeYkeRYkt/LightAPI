package ru.BeYkeRYkt.LightAPI.nms.Cauldron.v1_7_R4;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.vanilla.entity.Entity;
import net.minecraft.vanilla.entity.player.EntityPlayer;
import net.minecraft.vanilla.server.management.PlayerManager;
import net.minecraft.vanilla.world.EnumSkyBlock;
import net.minecraft.vanilla.world.IWorldAccess;
import net.minecraft.vanilla.world.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.cauldron.v1_7_R4.CraftWorld;

import ru.BeYkeRYkt.LightAPI.nms.INMSHandler;

public class NMSHandler implements INMSHandler {

    private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static Method cachedPlayerChunk;
    private static Field cachedDirtyField;
    private List<IWorldAccess> worlds = new ArrayList<IWorldAccess>();

    @Override
    public void initWorlds() {
        for (World worlds : Bukkit.getWorlds()) {
            net.minecraft.vanilla.world.World nmsWorld = ((CraftWorld) worlds).getHandle();
            IWorldAccess access = getLightIWorldAccess(worlds);

            nmsWorld.addWorldAccess(access);
            this.worlds.add(access);
        }
    }

    @Override
    public void unloadWorlds() {
        try {
            for (World worlds : Bukkit.getWorlds()) {
                net.minecraft.vanilla.world.World nmsWorld = ((CraftWorld) worlds).getHandle();

                for (IWorldAccess access : this.worlds) {
                    Field field = net.minecraft.vanilla.world.World.class.getDeclaredField("field_73021_x");
                    field.setAccessible(true);
                    ((List<?>) field.get(nmsWorld)).remove(access);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recalculateBlockLighting(World world, int x, int y, int z) {
        net.minecraft.vanilla.world.World nmsWorld = ((CraftWorld) world).getHandle();
        nmsWorld.func_147451_t(x, y, z);
    }

    @Override
    public void createLight(Location location, int light) {
        net.minecraft.vanilla.world.World world = ((CraftWorld) location.getWorld()).getHandle();
        world.setLightValue(EnumSkyBlock.Block, location.getBlockX(), location.getBlockY(), location.getBlockZ(), light);

        Block adjacent = getAdjacentAirBlock(location.getBlock());
        recalculateBlockLighting(location.getWorld(), adjacent.getX(), adjacent.getY(), adjacent.getZ());
        updateChunk(location);
    }

    public void updateChunk(Location location) {
        try {
            // from: https://gist.github.com/aadnk/5841942
            // Thanks Comphenix!
            net.minecraft.vanilla.world.World nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
            PlayerManager map = ((WorldServer) nmsWorld).getPlayerManager();

            for (int dX = -1; dX <= 1; dX++) {
                for (int dZ = -1; dZ <= 1; dZ++) {
                    Object playerChunk = getPlayerCountMethod().invoke(map, location.getChunk().getX() + dX, location.getChunk().getZ() + dZ, false);

                    if (playerChunk != null) {
                        Field dirtyField = getDirtyField(playerChunk);
                        int dirtyCount = (Integer) dirtyField.get(playerChunk);
                        if (dirtyCount > 0 && dirtyCount < 64) {
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
        net.minecraft.vanilla.world.World world = ((CraftWorld) loc.getWorld()).getHandle();
        world.updateLightByType(EnumSkyBlock.Block, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        updateChunk(loc);
    }

    private static Method getPlayerCountMethod() throws NoSuchMethodException, SecurityException {
        if (cachedPlayerChunk == null) {
            cachedPlayerChunk = PlayerManager.class.getDeclaredMethod("func_72690_a", int.class, int.class, boolean.class);
            cachedPlayerChunk.setAccessible(true);
        }
        return cachedPlayerChunk;
    }

    private static Field getDirtyField(Object playerChunk) throws NoSuchFieldException, SecurityException {
        if (cachedDirtyField == null) {
            cachedDirtyField = playerChunk.getClass().getDeclaredField("field_73262_e");
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
        net.minecraft.vanilla.world.World nmsWorld = ((CraftWorld) world).getHandle();
        final PlayerManager map = ((WorldServer) nmsWorld).getPlayerManager();
        return new IWorldAccess() {

            @Override
            public void broadcastSound(int arg0, int arg1, int arg2, int arg3, int arg4) {
            }

            @Override
            public void destroyBlockPartially(int arg0, int arg1, int arg2, int arg3, int arg4) {
            }

            @Override
            public void markBlockForRenderUpdate(int arg0, int arg1, int arg2) {
                map.func_151250_a(arg0, arg1, arg2);
            }

            @Override
            public void markBlockForUpdate(int arg0, int arg1, int arg2) {
            }

            @Override
            public void markBlockRangeForRenderUpdate(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
            }

            @Override
            public void onEntityCreate(Entity arg0) {
            }

            @Override
            public void onEntityDestroy(Entity arg0) {
            }

            @Override
            public void onStaticEntitiesChanged() {
            }

            @Override
            public void playAuxSFX(EntityPlayer arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
            }

            @Override
            public void playRecord(String arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void playSound(String arg0, double arg1, double arg2, double arg3, float arg4, float arg5) {
            }

            @Override
            public void playSoundToNearExcept(EntityPlayer arg0, String arg1, double arg2, double arg3, double arg4, float arg5, float arg6) {
            }

            @Override
            public void spawnParticle(String arg0, double arg1, double arg2, double arg3, double arg4, double arg5, double arg6) {
            }
        };
    }
}