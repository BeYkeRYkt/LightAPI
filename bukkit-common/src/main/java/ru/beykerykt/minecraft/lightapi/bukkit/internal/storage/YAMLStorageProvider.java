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
import ru.beykerykt.minecraft.lightapi.common.internal.storage.IStorageProvider;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;

public class YAMLStorageProvider implements IStorageProvider {

    private IPlatformImpl mImpl;
    private File customConfigFile;
    private FileConfiguration customConfig;

    @Override
    public void initialization(IPlatformImpl impl) {
        mImpl = impl;
        customConfigFile = new File(BukkitPlugin.getInstance().getDataFolder(), "storage.yml");
        if (! customConfigFile.exists()) {
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
    public void shutdown() {
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
    public int saveLightLevel(String world, int blockX, int blockY, int blockZ, int lightLevel) {
        return saveLightLevel(world, BlockPosition.asLong(blockX, blockY, blockZ), lightLevel);
    }

    @Override
    public int saveLightLevel(String world, long longPos, int lightLevel) {
        customConfig.set("worlds." + world + "." + longPos, lightLevel);
        return ResultCode.SUCCESS;
    }

    @Override
    public int saveLightLevels(String world, Map<Long, Integer> map) {
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            long longPos = entry.getKey();
            int lightLevel = entry.getValue();
            customConfig.set("worlds." + world + "." + longPos, lightLevel);
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public void loadLightLevel(String world, long longPos, Map<Long, Integer> map) {
        int lightLevel = customConfig.getInt("worlds." + world + "." + longPos);
        map.put(longPos, lightLevel);
    }

    @Override
    public void loadLightLevel(String world, int blockX, int blockY, int blockZ, Map<Long, Integer> map) {
        long longPos = BlockPosition.asLong(blockX, blockY, blockZ);
        int lightLevel = customConfig.getInt("worlds." + world + "." + longPos);
        map.put(longPos, lightLevel);
    }

    @Override
    public Map<Long, Integer> loadLightLevels(String world) {
        Map<Long, Integer> map = new HashMap<>();
        ConfigurationSection section = customConfig.getConfigurationSection("worlds." + world);
        if (section == null) {
            return map;
        }
        Set<String> sPos = section.getKeys(false);
        for (String s : sPos) {
            long longPos = Long.parseLong(s);
            int lightLevel = customConfig.getInt("worlds." + world + "." + s);
            map.put(longPos, lightLevel);
            mImpl.debug("longPos: " + longPos + " lightLevel: " + lightLevel);
        }
        return map;
    }
}
