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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.internal.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageFile;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.ChunkUtils;

public class YAMLStorageFile implements IStorageFile {

    private IPlatformImpl mPlatform;
    private File customConfigFile;
    private FileConfiguration customConfig;

    public YAMLStorageFile(IPlatformImpl platform) {
        mPlatform = platform;
    }

    @Override
    public void onStart() {
        customConfigFile = new File(BukkitPlugin.getInstance().getDataFolder(), "storage.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            BukkitPlugin.getInstance().saveResource("storage.yml", false);
        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShutdown() {
        if (customConfig != null) {
            try {
                customConfig.save(customConfigFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            customConfig = null;
        }
        customConfigFile = null;
    }

    @Override
    public int writeLightLevel(String world, long longPos, int lightLevel, int lightFlag) {
        int chunkX = BlockPosition.unpackLongX(longPos) >> 4;
        int chunkZ = BlockPosition.unpackLongZ(longPos) >> 4;
        long chunkKey = ChunkUtils.getChunkKey(chunkX, chunkZ);
        customConfig.set("worlds." + world + "." + chunkKey + "." + longPos + "." + lightFlag, lightLevel);
        return ResultCode.SUCCESS;
    }

    @Override
    public int deleteLightLevel(String world, long longPos, int lightFlag) {
        int chunkX = BlockPosition.unpackLongX(longPos) >> 4;
        int chunkZ = BlockPosition.unpackLongZ(longPos) >> 4;
        long chunkKey = ChunkUtils.getChunkKey(chunkX, chunkZ);
        customConfig.set("worlds." + world + "." + chunkKey + "." + longPos + "." + lightFlag, null);
        ConfigurationSection longPosSection = customConfig.getConfigurationSection(
                "worlds." + world + "." + chunkKey + "." + longPos);
        if (longPosSection == null || longPosSection.getKeys(false).isEmpty()) {
            customConfig.set("worlds." + world + "." + chunkKey + "." + longPos, null);
        }
        ConfigurationSection chunkSection = customConfig.getConfigurationSection("worlds." + world + "." + chunkKey);
        if (chunkSection == null || chunkSection.getKeys(false).isEmpty()) {
            customConfig.set("worlds." + world + "." + chunkKey, null);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public boolean containsChunk(String world, int chunkX, int chunkZ, int lightFlag) {
        long chunkKey = ChunkUtils.getChunkKey(chunkX, chunkZ);
        ConfigurationSection section = customConfig.getConfigurationSection("worlds." + world + "." + chunkKey);
        return section != null;
    }

    @Override
    public Map<Long, Integer> loadLightDataForChunk(String world, int chunkX, int chunkZ, int lightFlag) {
        Map<Long, Integer> map = new HashMap<>();
        long chunkKey = ChunkUtils.getChunkKey(chunkX, chunkZ);
        ConfigurationSection section = customConfig.getConfigurationSection("worlds." + world + "." + chunkKey);
        if (section == null) {
            return map;
        }
        Set<String> sPos = section.getKeys(false);
        for (String s : sPos) {
            long longPos = Long.parseLong(s);
            int lightLevel = customConfig.getInt("worlds." + world + "." + chunkKey + "." + s + "." + lightFlag);
            map.put(longPos, lightLevel);
        }
        return map;
    }
}
