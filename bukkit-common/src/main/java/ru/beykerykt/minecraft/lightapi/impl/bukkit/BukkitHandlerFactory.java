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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.NotImplementedException;

import ru.beykerykt.minecraft.lightapi.common.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.common.ILightHandler;

public class BukkitHandlerFactory implements IHandlerFactory {

	private BukkitPlugin plugin;

	public BukkitHandlerFactory(BukkitPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public ILightHandler createHandler() {
		String[] line = plugin.getServer().getClass().getPackage().getName().replace(".", ",").split(",");
		// First, check if CraftBukkit really is, since Bukkit is only an API, and there
		// may be several implementations (for example, Glowstone and etc)
		String impl = line[2];

		// Since the biggest modification of CraftBukkit is Spigot and its individual
		// forks, use the name 'craftbukkit' to define
		if (impl.equals("craftbukkit")) {
			String version = line[3];
			try {
				ILightHandler handler = (ILightHandler) Class
						.forName("ru.beykerykt.minecraft.lightapi.impl.bukkit.nms.NMS_" + version).getConstructor()
						.newInstance();
				plugin.getLogger().info("Your server is using version " + version);
				return handler;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else if (plugin.getServer().getName().equals("Glowstone")) {
			// may be it's Glowstone ???
			throw new NotImplementedException("Glowstone is currently not supported."); // TODO: support this
		} else {
			throw new NotImplementedException(plugin.getServer().getName() + " is currently not supported.");
		}
		return null;
	}
}
