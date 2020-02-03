/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LightBulb {

	private String id;

	private String world;
	private int blockX;
	private int blockY;
	private int blockZ;

	private int lightFlags;
	private int lightlevel;

	private boolean isTickable;
	private boolean markRecalculate;
	private boolean isLightUp;

	private List<IChunkSectionsData> chunkList = null;

	public LightBulb(String id, String world, int blockX, int blockY, int blockZ) {
		this.id = id;
		this.world = world;
		this.blockX = blockX;
		this.blockY = blockY;
		this.blockZ = blockZ;
		this.lightFlags = LightFlags.NONE;
		this.lightlevel = 0;
		this.isTickable = false;
		this.markRecalculate = false;
		this.isLightUp = false;
		this.chunkList = new CopyOnWriteArrayList<IChunkSectionsData>();
	}

	public String getId() {
		return id;
	}

	public String getWorldName() {
		return world;
	}

	public void setWorldName(String world) {
		this.world = world;
	}

	public int getBlockX() {
		return blockX;
	}

	public int getBlockY() {
		return blockY;
	}

	public int getBlockZ() {
		return blockZ;
	}

	public void moveToNewPosition(int blockX, int blockY, int blockZ) {
		this.blockX = blockX;
		this.blockY = blockY;
		this.blockZ = blockZ;
	}

	public int getChunkX() {
		return getBlockX() >> 4;
	}

	public int getChunkSector() {
		return getBlockY() >> 4;
	}

	public int getChunkZ() {
		return getBlockZ() >> 4;
	}

	public void addFlag(int flag) {
		this.lightFlags |= flag;
	}

	public void removeFlag(int flag) {
		this.lightFlags &= ~flag;
	}

	public boolean isFlagSet(int flags) {
		return (flags & getFlags()) == getFlags();
	}

	public void setFlags(int flags) {
		this.lightFlags = flags;
	}

	public int getFlags() {
		return lightFlags;
	}

	public int getLightLevel() {
		return lightlevel;
	}

	public void setLightLevel(int lightlevel) {
		this.lightlevel = lightlevel;
	}

	public boolean isTickable() {
		return isTickable;
	}

	public void setTickable(boolean isTickable) {
		this.isTickable = isTickable;
	}

	public void createLight() {
		LightAPI.get().setRawLightLevel(getWorldName(), lightFlags, blockX, blockY, blockZ, lightlevel);
		isLightUp = true;
		collectChunks();
	}

	public void deleteLight() {
		LightAPI.get().setRawLightLevel(getWorldName(), lightFlags, blockX, blockY, blockZ, 0);
		isLightUp = false;
		collectChunks();
	}

	public boolean isMarkedForRecalculate() {
		return markRecalculate;
	}

	public void markForRecalculate() {
		this.markRecalculate = true;
	}

	protected void doneRecalculate() {
		this.markRecalculate = false;
		chunkList.clear();
	}

	public boolean isLightUp() {
		return isLightUp;
	}

	protected void onTick() {
		int worldLightLevel = LightAPI.get().getRawLightLevel(world, lightFlags, blockX, blockY, blockZ);
		if (worldLightLevel < getLightLevel()) {
			if (isLightUp() && !isMarkedForRecalculate()) {
				createLight();
				markForRecalculate();
			}
		}
	}

	public List<IChunkSectionsData> getModifiedChunks() {
		return chunkList;
	}

	private void collectChunks() {
		List<IChunkSectionsData> list = LightAPI.get().collectChunkSections(world, blockX, blockY, blockZ,
				getLightLevel());
		for (int i = 0; i < list.size(); i++) {
			IChunkSectionsData coords = list.get(i);
			if (!chunkList.contains(coords)) {
				chunkList.add(coords);
			}
		}
		list.clear();
		list = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + blockX;
		result = prime * result + blockY;
		result = prime * result + blockZ;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (isTickable ? 1231 : 1237);
		result = prime * result + lightFlags;
		result = prime * result + lightlevel;
		result = prime * result + ((world == null) ? 0 : world.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LightBulb other = (LightBulb) obj;
		if (blockX != other.blockX)
			return false;
		if (blockY != other.blockY)
			return false;
		if (blockZ != other.blockZ)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isTickable != other.isTickable)
			return false;
		if (lightFlags != other.lightFlags)
			return false;
		if (lightlevel != other.lightlevel)
			return false;
		if (world == null) {
			if (other.world != null)
				return false;
		} else if (!world.equals(other.world))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LightBulb [id=" + id + ", world=" + world + ", blockX=" + blockX + ", blockY=" + blockY + ", blockZ="
				+ blockZ + ", lightFlags=" + lightFlags + ", lightlevel=" + lightlevel + ", isTickable=" + isTickable
				+ "]";
	}
}
