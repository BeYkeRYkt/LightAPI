/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler;

import org.apache.commons.lang.NotImplementedException;
import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.bukkit.ConfigurationPath;
import ru.beykerykt.minecraft.lightapi.common.internal.ILightAPI;
import ru.beykerykt.minecraft.lightapi.common.internal.handler.IHandler;

import java.lang.reflect.InvocationTargetException;

public class HandlerFactory {

    private static final String CRAFTBUKKIT_PKG = "org.bukkit.craftbukkit";
    private static final String NMS_PKG = "net.minecraft.server";

    private static final String DEFAULT_PATH = "ru.beykerykt.minecraft.lightapi.bukkit.internal.handler";
    private final BukkitPlugin mPlugin;
    private final ILightAPI mImpl;

    public HandlerFactory(BukkitPlugin plugin, ILightAPI impl) {
        this.mPlugin = plugin;
        this.mImpl = impl;
    }

    protected BukkitPlugin getPlugin() {
        return mPlugin;
    }

    protected ILightAPI getPlatformImpl() {
        return mImpl;
    }

    public IHandler createHandler()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException, ClassNotFoundException, NotImplementedException {
        IHandler handler = null;

        // load specific handler if available
        String specificPkg = getPlugin().getConfig().getString(ConfigurationPath.GENERAL_SPECIFIC_HANDLER);
        if (specificPkg != null && !specificPkg.equalsIgnoreCase("none")) {
            getPlatformImpl().log("Initial load specific handler");
            handler = (IHandler) Class.forName(specificPkg).getConstructor().newInstance();
            getPlatformImpl().log("Custom handler is loaded: " + handler.getClass().getName());
            return handler;
        }

        // First, check if CraftBukkit really is, since Bukkit is only an API, and there
        // may be several implementations (for example: Spigot, Glowstone and etc)
        String serverImplPackage = getPlugin().getServer().getClass().getPackage().getName();
        String extPath = "";
        if (serverImplPackage.startsWith(CRAFTBUKKIT_PKG)) { // i think it's craftbukkit (?)
            extPath += "craftbukkit.";
            String[] line = serverImplPackage.replace(".", ",").split(",");
            String version = line[3];
            getPlatformImpl().log("Your server is using version " + version);
            Class<?> minecraftServerNMS = Class.forName(NMS_PKG + "." + version + ".MinecraftServer");
            if (minecraftServerNMS != null) {
                extPath += "nms.";
                // start using nms handler
                getPlatformImpl().log("Loading NMS handler " + version);
                handler = (IHandler) Class
                        .forName(DEFAULT_PATH + "." + extPath + version + "." + "NMSHandler")
                        .getConstructor().newInstance();
                getPlatformImpl().log("Handler is loaded: " + version);
            } else {
                throw new NotImplementedException(getPlugin().getServer().getName() + " is currently not supported.");
            }
        } else { // something else
            throw new NotImplementedException(getPlugin().getServer().getName() + " is currently not supported.");
        }
        return handler;
    }
}
