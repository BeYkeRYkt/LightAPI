/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Vladimir Mikhailov <beykerykt@gmail.com>
 * Copyright (c) 2016-2017 The ImplexDevOne Project
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
package ru.beykerykt.minecraft.lightapi.impl.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.World;

import ru.beykerykt.minecraft.lightapi.common.LCallback;
import ru.beykerykt.minecraft.lightapi.common.LReason;
import ru.beykerykt.minecraft.lightapi.common.LStage;
import ru.beykerykt.minecraft.lightapi.common.LightType;

public abstract class BukkitCallback implements LCallback {

	public abstract void onSuccess(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LStage stage);

	public abstract void onFailed(World world, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LStage stage, LReason reason);

	@Override
	public void onSuccess(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LStage stage) {
		World world = Bukkit.getWorld(worldName);
		onSuccess(world, type, blockX, blockY, blockZ, lightlevel, stage);
	}

	@Override
	public void onFailed(String worldName, LightType type, int blockX, int blockY, int blockZ, int lightlevel,
			LStage stage, LReason reason) {
		World world = Bukkit.getWorld(worldName);
		onFailed(world, type, blockX, blockY, blockZ, lightlevel, stage, reason);
	}
}
