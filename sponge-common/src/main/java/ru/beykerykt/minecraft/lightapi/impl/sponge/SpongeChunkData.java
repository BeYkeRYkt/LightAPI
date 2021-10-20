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
package ru.beykerykt.minecraft.lightapi.impl.sponge;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import java.util.Collection;

import ru.beykerykt.minecraft.lightapi.common.IChunkData;

/**
 * https://wiki.vg/Chunk_Format#Packet_structure
 *
 * @author BeYkeRYkt
 */
public class SpongeChunkData implements IChunkData {

    private World world;
    private int x;
    private int y;
    private int z;
    private Collection<Player> receivers;

    public SpongeChunkData(World world, int chunkX, int chunkZ, Collection<Player> playersName) {
        this(world, chunkX, 255, chunkZ, playersName);
    }

    public SpongeChunkData(World world, int chunkX, int chunk_y_height, int chunkZ, Collection<Player> playersName) {
        this.world = world;
        this.x = chunkX;
        this.y = chunk_y_height;
        this.z = chunkZ;
        this.receivers = playersName;
    }

    public World getWorld() {
        return world;
    }

    /**
     * @return World name
     */
    @Override
    public String getWorldName() {
        return world.getName();
    }

    /**
     * @return Chunk X Coordinate
     */
    @Override
    public int getChunkX() {
        return x;
    }

    /**
     * @return Chunk Z Coordinate
     */
    @Override
    public int getChunkZ() {
        return z;
    }

    /**
     * Bitmask with bits set to 1 for every 16×16×16 chunk section whose data is included in Data. The
     * least significant bit represents the chunk section at the bottom of the chunk column (from y=0
     * to y=15).
     *
     * @return The height (0 - 255) on which the chunk column depends
     */
    public int getChunkYHeight() {
        return y;
    }

    public void setChunkYHeight(int y) {
        this.y = y;
    }

    /**
     * @return A list of player to which this chunk can be sent.
     */
    public Collection<Player> getReceivers() {
        return receivers;
    }

    public void setReceivers(Collection<Player> receivers) {
        this.receivers = receivers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((world == null) ? 0 : world.hashCode());
        result = prime * result + x;
        result = prime * result + (y >> 4);
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        SpongeChunkData other = (SpongeChunkData) obj;
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
        if ((y >> 4) != (other.y >> 4)) {
            return false;
        }
        if (z != other.z) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SpongeChunkData [worldName=" + world.getName() + ", x=" + x + ", y=" + y + ", z=" + z + "]";
    }
}
