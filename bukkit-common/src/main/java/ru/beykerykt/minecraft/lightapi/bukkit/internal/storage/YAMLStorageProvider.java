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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCodes;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.IPlatformImpl;
import ru.beykerykt.minecraft.lightapi.common.internal.impl.storage.IStorageProvider;
import ru.beykerykt.minecraft.lightapi.common.internal.utils.BlockPosition;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class YAMLStorageProvider implements IStorageProvider {

    private File customConfigFile;
    private FileConfiguration customConfig;

    @Override
    public void initialization(IPlatformImpl impl) {
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
    public int saveLightSource(String world, int blockX, int blockY, int blockZ, int lightlevel) {
        return saveLightSource(world, BlockPosition.asLong(blockX, blockY, blockZ), lightlevel);
    }

    @Override
    public int saveLightSource(String world, long longPos, int lightlevel) {
        customConfig.set("worlds." + world + "." + longPos, lightlevel);
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public int saveLightSources(String world, Map<Long, Integer> map) {
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            long longPos = entry.getKey();
            int lightlevel = entry.getValue();
            customConfig.set("worlds." + world + "." + longPos, lightlevel);
        }
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResultCodes.SUCCESS;
    }

    @Override
    public void loadLightSource(String world, long longPos, Map<Long, Integer> map) {
        int lightlevel = customConfig.getInt("worlds." + world + "." + longPos);
        map.put(longPos, lightlevel);
    }

    @Override
    public void loadLightSource(String world, int blockX, int blockY, int blockZ, Map<Long, Integer> map) {
        long longPos = BlockPosition.asLong(blockX, blockY, blockZ);
        int lightlevel = customConfig.getInt("worlds." + world + "." + longPos);
        map.put(longPos, lightlevel);
    }

    @Override
    public Map<Long, Integer> loadLightSources(String world) {
        Map<Long, Integer> map = new HashMap<Long, Integer>();
        ConfigurationSection section = customConfig.getConfigurationSection("worlds." + world);
        if (section == null) {
            return map;
        }
        Set<String> sPos = section.getKeys(false);
        for (String s : sPos) {
            long longPos = Long.parseLong(s);
            int lightlevel = customConfig.getInt("worlds." + world + "." + s);
            map.put(longPos, lightlevel);
            System.out.println("longPos: " + longPos + " lightlevel: " + lightlevel);
        }
        return map;
    }
}