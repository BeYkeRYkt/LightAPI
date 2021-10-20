/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.lightapi.chunks;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;

@Deprecated
public class ChunkInfo {

    private final World world;
    private final int x;
    private final int z;
    private int y;
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

    public void setChunkYHeight(int y) {
        this.y = y;
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
        return z == other.z;
    }

    @Override
    public String toString() {
        return "ChunkInfo [world=" + world + ", x=" + x + ", y=" + y + ", z=" + z + "]";
    }
}
