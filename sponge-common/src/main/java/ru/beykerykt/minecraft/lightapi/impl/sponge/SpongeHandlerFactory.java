/**
 * The MIT License (MIT)
 *
 * <p>Copyright (c) 2015 Vladimir Mikhailov <beykerykt@gmail.com> Copyright (c) 2016-2017 The
 * ImplexDevOne Project
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.impl.sponge;

import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;

import java.lang.reflect.InvocationTargetException;

import ru.beykerykt.minecraft.lightapi.common.IHandlerFactory;
import ru.beykerykt.minecraft.lightapi.common.ILightHandler;

public class SpongeHandlerFactory implements IHandlerFactory {

    private SpongePlugin plugin;

    public SpongeHandlerFactory(SpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ILightHandler createHandler() {
        MinecraftVersion version = Sponge.getGame().getPlatform().getMinecraftVersion();
        plugin.getLogger().info("Your server is using version " + version.getName());
        String ver = "UNKNOWN";
        if (version.getName().equals("1.12.2")) {
            ver = "v1_12_R1";
        }
        try {
            return (ISpongeLightHandler) Class.forName(
                    "ru.beykerykt.minecraft.lightapi.impl.sponge.mcp.MCP_" + ver).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
