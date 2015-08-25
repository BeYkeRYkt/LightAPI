package ru.BeYkeRYkt.LightAPI;

import org.bukkit.World;

public class ChunkInfo {

	private World world;
	private int chunkX;
	private int chunkZ;

	public ChunkInfo(World world, int chunkX, int chunkZ) {
		this.world = world;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public World getWorld() {
		return world;
	}

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((world == null) ? 0 : world.hashCode());
		result = prime * result + chunkX;
		result = prime * result + chunkZ;
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
		if (!(obj instanceof ChunkInfo)) {
			return false;
		}
		ChunkInfo other = (ChunkInfo) obj;
		return toString().equals(other.toString());
	}

	@Override
	public String toString() {
		return "ChunkInfo [world=" + world.getName() + ", x=" + chunkX + ", z=" + chunkZ + "]";
	}
}
