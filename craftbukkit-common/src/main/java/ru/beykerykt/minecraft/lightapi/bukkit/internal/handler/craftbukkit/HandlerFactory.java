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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandler;
import ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.IHandlerFactory;

public class HandlerFactory implements IHandlerFactory {

    private static final String CRAFTBUKKIT_PKG = "org.bukkit.craftbukkit";

    @Override
    public IHandler createHandler() throws Exception{
        IHandler handler = null;
        String serverImplPackage = Bukkit.getServer().getClass().getPackage().getName();
        String extPath = "";

        if (serverImplPackage.startsWith(CRAFTBUKKIT_PKG)) { // make sure it's craftbukkit
            String[] line = serverImplPackage.replace(".", ",").split(",");
            String version = line[3];
            extPath += "nms.";
            // start using nms handler
            handler = (IHandler) Class
                    .forName(getClass().getPackage().getName() + "." + extPath + version + "." + "NMSHandler")
                    .getConstructor().newInstance();
        } else { // something else
            throw new NotImplementedException(Bukkit.getName() + " is currently not supported.");
        }
        return handler;
    }
}
