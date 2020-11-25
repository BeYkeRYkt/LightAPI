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
package ru.beykerykt.minecraft.lightapi.bukkit.example;

import ru.beykerykt.minecraft.lightapi.common.api.ChunkData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChunkUtils {

    public static List<ChunkData> mergeChunks(List<ChunkData> input) {
        List<ChunkData> output = new ArrayList<ChunkData>();
        Iterator<ChunkData> it = input.iterator();
        while (it.hasNext()) {
            ChunkData data = it.next();
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
        }
        return output;
    }
}