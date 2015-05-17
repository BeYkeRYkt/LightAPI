package ru.BeYkeRYkt.LightAPI.nms.CraftBukkit.v1_7_R2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_7_R2.Chunk;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import net.minecraft.server.v1_7_R2.EnumSkyBlock;
import net.minecraft.server.v1_7_R2.Packet;
import net.minecraft.server.v1_7_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_7_R2.WorldServer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import ru.BeYkeRYkt.LightAPI.nms.INMSHandler;

public class NMSHandler implements INMSHandler {

    private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private List<Chunk> chunks = new ArrayList<Chunk>();
    private static Field cachedChunkModified;

    @Override
    public void createLight(Location location, int light, boolean needUpdate) {
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        world.b(EnumSkyBlock.BLOCK, location.getBlockX(), location.getBlockY(), location.getBlockZ(), light);

        Block adjacent = getAdjacentAirBlock(location.getBlock());
        recalculateBlockLighting(location.getWorld(), adjacent.getX(), adjacent.getY(), adjacent.getZ());
        if (needUpdate) {
            collectChunks(location);
            updateChunks(location.getWorld(), location);
        }
    }

    @Override
    public void deleteLight(Location location, boolean needUpdate) {
        recalculateBlockLighting(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (needUpdate) {
            collectChunks(location);
            updateChunks(location.getWorld(), location);
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
                        // chunk.f(false);
                        isModified.setBoolean(chunk, false);
                        chunks.add(chunk);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChunks(World world, Location loc) {
        for (Chunk chunk : chunks) {
            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, false, 65535);
            sendPacket(world, loc, packet);
        }
        chunks.clear();
    }

    private void sendPacket(World world, Location loc, Packet packet) {
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(loc) < 20) {// Dev-Test
                EntityPlayer nms = ((CraftPlayer) player).getHandle();
                nms.playerConnection.sendPacket(packet);
            }
        }
    }

    public void recalculateBlockLighting(World world, int x, int y, int z) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        nmsWorld.c(EnumSkyBlock.BLOCK, x, y, z);
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
            cachedChunkModified = chunk.getClass().getDeclaredField("n");
            cachedChunkModified.setAccessible(true);
        }
        return cachedChunkModified;
    }

    @Override
    public void createLight(List<Location> location, int light, boolean needUpdate) {
        for (Location loc : location) {
            createLight(loc, light, needUpdate); // ???
        }
    }

    @Override
    public void deleteLight(List<Location> location, boolean needUpdate) {
        for (Location loc : location) {
            deleteLight(loc, needUpdate); // ???
        }
    }
}
