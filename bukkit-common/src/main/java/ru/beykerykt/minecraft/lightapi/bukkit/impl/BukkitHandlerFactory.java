/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.impl;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.NotImplementedException;

import ru.beykerykt.minecraft.lightapi.common.impl.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.common.impl.IHandlerImpl;

public class BukkitHandlerFactory implements IHandlerFactory {

	private BukkitPlugin plugin;

	// current supported bukkit impl
	private static final String CRAFTBUKKIT_PKG = "org.bukkit.craftbukkit";
	private static final String NMS_PKG = "net.minecraft.server";

	public BukkitHandlerFactory(BukkitPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public IHandlerImpl createHandler() {
		IHandlerImpl handler = null;

		// load specific nms pkg if available
		String specificPkg = plugin.getConfig().getString("specific-nms-handler");
		if (specificPkg != null && !specificPkg.equalsIgnoreCase("none")) {
			plugin.log("Initial load specific handler");
			try {
				handler = (IHandlerImpl) Class.forName(specificPkg).getConstructor().newInstance();
				plugin.log("Custom handler is initialized: " + handler.getClass().getName());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
			return handler;
		}

		// First, check if CraftBukkit really is, since Bukkit is only an API, and there
		// may be several implementations (for example: Spigot, Glowstone and etc)
		String serverImplPackage = plugin.getServer().getClass().getPackage().getName();
		if (serverImplPackage.startsWith(CRAFTBUKKIT_PKG)) { // i think it's craftbukkit (?)
			// start using nms handler
			String[] line = serverImplPackage.replace(".", ",").split(",");
			String version = line[3];
			plugin.log("Your server is using version " + version);
			try {
				handler = (IHandlerImpl) Class
						.forName("ru.beykerykt.minecraft.lightapi.bukkit.impl.handlers.CraftBukkit_" + version)
						.getConstructor().newInstance();
				plugin.log("Handler is initialized: " + version);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else { // something else
			throw new NotImplementedException(plugin.getServer().getName() + " is currently not supported.");
		}
		return handler;
	}
}
