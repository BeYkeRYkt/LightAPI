package ru.beykerykt.lightapi.chunks;

import java.util.Collection;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class ChunkInfo {

	private World world;
	private int x;
	private int y;
	private int z;
	private Collection<? extends Player> receivers;

	public ChunkInfo(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		this(world, chunkX, 256, chunkZ, players);
	}

	public ChunkInfo(World world, int chunkX, int chunk_y_height, int chunkZ, Collection<? extends Player> players) {
		this.world = world;
		this.x = chunkX;
		this.y = chunk_y_height;
		this.z = chunkZ;
		this.receivers = players;
	}

	public World getWorld() {
		return world;
	}

	public int getChunkX() {
		return x;
	}

	public int getChunkZ() {
		return z;
	}

	public int getChunkYHeight() {
		return y;
	}

	public Collection<? extends Player> getReceivers() {
		return receivers;
	}

	public void setReceivers(Collection<? extends Player> receivers) {
		this.receivers = receivers;
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
		if (!(obj instanceof ChunkInfo)) {
			return false;
		}
		ChunkInfo other = (ChunkInfo) obj;
		if (world == null) {
			if (other.world != null) {
				return false;
			}
		} else if (!world.getName().equals(other.world.getName())) {
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
