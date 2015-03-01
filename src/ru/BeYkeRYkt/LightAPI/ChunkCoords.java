package ru.BeYkeRYkt.LightAPI;

import org.bukkit.Chunk;
import org.bukkit.World;

public class ChunkCoords {

    private final int x;
    private final int z;
    private final World world;

    public ChunkCoords(Chunk chunk) {
        this.x = chunk.getX();
        this.z = chunk.getZ();
        this.world = chunk.getWorld();
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the z
     */
    public int getZ() {
        return z;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    @Override
    public String toString() {
        return "ChunkCoords{" + "x=" + getX() + "z=" + getZ() + '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((world == null) ? 0 : world.hashCode());
        result = prime * result + x;
        result = prime * result + z;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ChunkCoords)) {
            return false;
        }
        ChunkCoords other = (ChunkCoords) obj;
        if (world == null) {
            if (other.world != null) {
                return false;
            }
        } else if (!world.equals(other.world)) {
            return false;
        }
        if (x != other.x) {
            return false;
        }
        if (z != other.z) {
            return false;
        }
        return true;
    }
}