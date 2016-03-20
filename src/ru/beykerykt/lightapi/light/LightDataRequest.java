package ru.beykerykt.lightapi.light;

import org.bukkit.World;

import ru.beykerykt.lightapi.request.DataRequest;

public class LightDataRequest implements DataRequest {

	public enum RequestType {
		CREATE,
		DELETE,
		RECALCULATE;
	}

	private boolean ready;
	private RequestType type;
	private World world;
	private int x;
	private int y;
	private int z;
	private int lightlevel;

	public LightDataRequest(World world, int x, int y, int z, int lightlevel, RequestType type) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.lightlevel = lightlevel;
		this.type = type;
		this.ready = false;
	}

	public World getWorld() {
		return world;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int getLightLevel() {
		return lightlevel;
	}

	public RequestType getRequestType() {
		return type;
	}

	@Override
	public boolean isReady() {
		return ready;
	}

	@Override
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((world == null) ? 0 : world.hashCode());
		result = prime * result + x;
		result = prime * result + y;
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
		if (!(obj instanceof LightDataRequest)) {
			return false;
		}
		LightDataRequest other = (LightDataRequest) obj;
		if (type != other.type) {
			return false;
		}
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
		if (y != other.y) {
			return false;
		}
		if (z != other.z) {
			return false;
		}
		return true;
	}
}
