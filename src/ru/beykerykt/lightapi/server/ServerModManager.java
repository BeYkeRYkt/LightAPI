/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2016
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
package ru.beykerykt.lightapi.server;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.server.exceptions.UnknownModImplementationException;
import ru.beykerykt.lightapi.server.exceptions.UnknownNMSVersionException;
import ru.beykerykt.lightapi.server.nms.INMSHandler;

public class ServerModManager {

	private static Map<String, ServerModInfo> supportImpl = new ConcurrentHashMap<String, ServerModInfo>();
	private static INMSHandler handler;

	public static void init() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, UnknownNMSVersionException, UnknownModImplementationException {
		shutdown();

		// Init handler...
		String modName = Bukkit.getVersion().split("-")[1];
		if (!supportImpl.containsKey(modName)) {
			modName = Bukkit.getName();
		}
		ServerModInfo impl = supportImpl.get(modName);
		if (impl != null) {
			// try {
			String folder_version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			if (impl.getVersions().containsKey(folder_version)) {
				final Class<? extends INMSHandler> clazz = impl.getVersions().get(folder_version);
				// Check if we have a NMSHandler class at that location.
				if (INMSHandler.class.isAssignableFrom(clazz)) {
					handler = clazz.getConstructor().newInstance();
					LightAPI.getInstance().log(Bukkit.getConsoleSender(), "Loading handler for " + impl.getModName() + " " + Bukkit.getVersion());
				}
			} else {
				throw new UnknownNMSVersionException(modName, folder_version);
			}
		} else {
			throw new UnknownModImplementationException(modName);
		}
	}

	public static void shutdown() {
		if (isInitialized()) {
			handler = null;
		}
	}

	public static boolean isInitialized() {
		return handler != null;
	}

	public static boolean registerServerMod(ServerModInfo info) {
		if (supportImpl.containsKey(info.getModName())) {
			return false;
		}
		supportImpl.put(info.getModName(), info);
		return true;
	}

	public static boolean unregisterServerMod(String modName) {
		if (supportImpl.containsKey(modName)) {
			return false;
		}
		supportImpl.remove(modName);
		return true;
	}

	public static INMSHandler getNMSHandler() {
		return handler;
	}
}
