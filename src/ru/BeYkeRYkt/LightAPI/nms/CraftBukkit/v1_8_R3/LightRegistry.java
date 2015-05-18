package ru.BeYkeRYkt.LightAPI.nms.CraftBukkit.v1_8_R3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumSkyBlock;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import ru.BeYkeRYkt.LightAPI.events.DeleteLightEvent;
import ru.BeYkeRYkt.LightAPI.events.SetLightEvent;
import ru.BeYkeRYkt.LightAPI.nms.ILightRegistry;

public class LightRegistry implements ILightRegistry {

    private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private List<Chunk> chunks = new ArrayList<Chunk>();
    private static Field cachedChunkModified;

    @Override
    public void createLight(Location location, int light, boolean needUpdate) {
        SetLightEvent event = new SetLightEvent(location, light);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        WorldServer world = ((CraftWorld) event.getLocation().getWorld()).getHandle();
        BlockPosition position = new BlockPosition(event.getLocation().getBlockX(), event.getLocation().getBlockY(), event.getLocation().getBlockZ());
        world.a(EnumSkyBlock.BLOCK, position, event.getLightLevel());

        Block adjacent = getAdjacentAirBlock(event.getLocation().getBlock());
        recalculateBlockLighting(event.getLocation().getWorld(), adjacent.getX(), adjacent.getY(), adjacent.getZ());
        collectChunks(event.getLocation());

        if (needUpdate) {
            sendUpdateChunks();
        }
    }

    @Override
    public void deleteLight(Location location, boolean needUpdate) {
        DeleteLightEvent event = new DeleteLightEvent(location);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        recalculateBlockLighting(event.getLocation().getWorld(), event.getLocation().getBlockX(), event.getLocation().getBlockY(), event.getLocation().getBlockZ());
        collectChunks(event.getLocation());

        if (needUpdate) {
            sendUpdateChunks();
        }
    }

    private void collectChunks(Location location) {
        try {
            WorldServer nmsWorld = ((CraftWorld) location.getChunk().getWorld()).getHandle();
            for (int dX = -1; dX <= 1; dX++) {
                for (int dZ = -1; dZ <= 1; dZ++) {
                    Chunk chunk = nmsWorld.getChunkAt(location.getChunk().getX() + dX, location.getChunk().getZ() + dZ);
                    Field isModified = getChunkField(chunk);
                    if (isModified.getBoolean(chunk)) {
                        chunk.f(false);
                        if (!chunks.contains(chunk)) {
                            chunks.add(chunk);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recalculateBlockLighting(World world, int x, int y, int z) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        BlockPosition pos = new BlockPosition(x, y, z);
        nmsWorld.c(EnumSkyBlock.BLOCK, pos);
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

    private static Field getChunkField(Object chunk) throws NoSuchFieldException, SecurityException {
        if (cachedChunkModified == null) {
            cachedChunkModified = chunk.getClass().getDeclaredField("q");
            cachedChunkModified.setAccessible(true);
        }
        return cachedChunkModified;
    }

    @Override
    public void createLight(List<Location> location, int light, boolean needUpdate) {
        for (Location loc : location) {
            createLight(loc, light, false); // ???
        }

        if (needUpdate) {
            sendUpdateChunks();
        }
    }

    @Override
    public void deleteLight(List<Location> location, boolean needUpdate) {
        for (Location loc : location) {
            deleteLight(loc, false); // ???
        }

        if (needUpdate) {
            sendUpdateChunks();
        }
    }

    @Override
    public void sendUpdateChunks() {
        for (Chunk chunk : chunks) {
            sendPacket(chunk);
        }
        chunks.clear();
    }

    private void sendPacket(Chunk chunk) {
        for (EntityHuman human : chunk.world.players) {
            EntityPlayer player = (EntityPlayer) human;
            Chunk pChunk = player.world.getChunkAtWorldCoords(player.getChunkCoordinates());
            if (distanceTo(pChunk, chunk) < 5) {
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, false, 65535);
                player.playerConnection.sendPacket(packet);
            }
        }
    }

    public int distanceTo(Chunk from, Chunk to) {
        double var2 = to.locX - from.locX;
        double var4 = to.locZ - from.locZ;
        return (int) Math.sqrt(var2 * var2 + var4 * var4);
    }
}
