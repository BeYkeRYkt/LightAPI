package ru.beykerykt.lightapi.chunks;

import java.util.Collection;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class ChunkInfo {

	private World world;
	private int x;
	private int z;
	private Collection<? extends Player> receivers;

	public ChunkInfo(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
		this.world = world;
		this.x = chunkX;
		this.z = chunkZ;
		this.receivers = players;
	}

	public World getWorld() {
		return world;
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
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
