/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.bukkit.internal.chunks;

import ru.beykerykt.minecraft.lightapi.bukkit.internal.impl.handler.IBukkitHandlerInternal;
import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BukkitChunkObserver implements IBukkitChunkObserver {
    private IBukkitHandlerInternal mHandler;
    private List<ChunkData> sendingChunks = new CopyOnWriteArrayList<>();
    private boolean isMergeEnabled;

    public BukkitChunkObserver(IBukkitHandlerInternal handler) {
        this.mHandler = handler;
    }

    private IBukkitHandlerInternal getHandler() {
        return this.mHandler;
    }

    @Override
    public boolean isMergeChunksEnabled() {
        return this.isMergeEnabled;
    }

    @Override
    public void setMergeChunksEnabled(boolean enabled) {
        this.isMergeEnabled = enabled;
    }

    @Override
    public void shutdown() {
        sendingChunks.clear();
    }

    @Override
    public void onTick() {
        // go send chunks
        Iterator<ChunkData> it = sendingChunks.iterator();
        while (it.hasNext()) {
            ChunkData data = it.next();
            getHandler().sendChunk(data);
            sendingChunks.remove(data);
        }

    }

    @Override
    public void notifyUpdateChunks(String worldName, int blockX, int blockY, int blockZ, int lightLevel) {
        collectChunks(worldName, blockX, blockY, blockZ, lightLevel, sendingChunks);
    }

    @Override
    public void notifyUpdateChunk(String worldName, int blockX, int blockY, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int sectionY = getHandler().getSectionFromY(blockY);
        int sectionMask = getHandler().asSectionMask(sectionY);
        notifyUpdateChunk(worldName, chunkX, chunkZ, sectionMask, sectionMask);
    }

    @Override
    public void notifyUpdateChunk(String worldName, int chunkX, int chunkZ, int sectionMaskSky, int sectionMaskBlock) {
        if (isMergeChunksEnabled()) {
            // go found this little shit
            Iterator<ChunkData> itc = sendingChunks.iterator();
            boolean found = false;
            while (itc.hasNext()) {
                ChunkData data_c = itc.next();
                if (data_c.getWorldName().equals(worldName) &&
                        data_c.getChunkX() == chunkX &&
                        data_c.getChunkZ() == chunkZ) {
                    if (!data_c.checkSectionMaskSky(sectionMaskSky)) {
                        data_c.addSectionMaskSky(sectionMaskSky);
                    }

                    if (!data_c.checkSectionMaskBlock(sectionMaskBlock)) {
                        data_c.addSectionMaskBlock(sectionMaskBlock);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Not found? Create a new one
                ChunkData data = new ChunkData(worldName, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
                sendingChunks.add(data);
            }
        } else {
            ChunkData data = new ChunkData(worldName, chunkX, chunkZ, sectionMaskSky, sectionMaskBlock);
            if (!sendingChunks.contains(data)) {
                sendingChunks.add(data);
            }
        }
    }

    protected void collectChunks(String worldName, int blockX, int blockY, int blockZ, int lightLevel,
                                 List<ChunkData> output) {
        List<ChunkData> input = getHandler().collectChunkSections(worldName, blockX, blockY, blockZ, lightLevel);
        Iterator<ChunkData> it = input.iterator();
        while (it.hasNext()) {
            ChunkData data = it.next();

            if (isMergeChunksEnabled()) {
                // go found this shit
                Iterator<ChunkData> itc = output.iterator();
                boolean found = false;
                while (itc.hasNext()) {
                    ChunkData data_c = itc.next();
                    if (data_c.getWorldName().equals(data.getWorldName()) &&
                            data_c.getChunkX() == data.getChunkX() &&
                            data_c.getChunkZ() == data.getChunkZ()) {
                        if (!data_c.checkSectionMaskBlock(data.getSectionMaskBlock())) {
                            data_c.addSectionMaskBlock(data.getSectionMaskBlock());
                        }

                        if (!data_c.checkSectionMaskSky(data.getSectionMaskSky())) {
                            data_c.addSectionMaskSky(data.getSectionMaskSky());
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    output.add(data);
                }
            } else {
                if (!output.contains(data)) {
                    output.add(data);
                }
            }
            it.remove();
        }
    }
}
