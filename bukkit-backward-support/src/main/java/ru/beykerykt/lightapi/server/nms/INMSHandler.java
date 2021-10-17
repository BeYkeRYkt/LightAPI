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
package ru.beykerykt.lightapi.server.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

import ru.beykerykt.lightapi.chunks.ChunkInfo;

@Deprecated
public interface INMSHandler {

    // Lights...
    void createLight(World world, int x, int y, int z, int light);

    void deleteLight(World world, int x, int y, int z);

    void recalculateLight(World world, int x, int y, int z);

    // Chunks...
    List<ChunkInfo> collectChunks(World world, int x, int y, int z);

    void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players);

    void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player);

    void sendChunkUpdate(World world, int chunkX, int y, int chunkZ, Collection<? extends Player> players);

    void sendChunkUpdate(World world, int chunkX, int y, int chunkZ, Player player);
}
