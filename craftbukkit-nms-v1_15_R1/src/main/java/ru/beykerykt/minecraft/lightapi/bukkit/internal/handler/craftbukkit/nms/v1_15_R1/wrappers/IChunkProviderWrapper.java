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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.handler.craftbukkit.nms.v1_15_R1.wrappers;

import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_15_R1.EnumSkyBlock;
import net.minecraft.server.v1_15_R1.IBlockAccess;
import net.minecraft.server.v1_15_R1.IChunkAccess;
import net.minecraft.server.v1_15_R1.IChunkProvider;
import net.minecraft.server.v1_15_R1.ILightAccess;
import net.minecraft.server.v1_15_R1.SectionPosition;
import net.minecraft.server.v1_15_R1.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import ru.beykerykt.minecraft.lightapi.bukkit.BukkitPlugin;
import ru.beykerykt.minecraft.lightapi.common.internal.storage.LightStorage;

/*
 * Sources:
 * https://github.com/flori-schwa/VarLight/blob/3a25375afa160ae1ed3301f273a505265cf3ac26/v1_15_R1/src/me/shawlaf
 * /varlight/spigot/nms/wrappers/WrappedILightAccess.java
 * https://github.com/HammerSMP/YarnSource/blob/e011e590ca873456b5787b2fe6fb915c0217fd75/net/minecraft/world/chunk
 * /light/ChunkBlockLightProvider.java#L38
 */
public class IChunkProviderWrapper implements ILightAccess, Listener {

    private final Map<ChunkCoordIntPair, IChunkAccess> mProxiesToOutput = new HashMap<>();
    private final IChunkProvider mChunkProvider;
    private final String GET_LUMINANCE_METHOD = "h"; // getLuminance() (yarn-1.14)
    private final LightStorage mStorage;

    public IChunkProviderWrapper(IChunkProvider chunkProvider, LightStorage storage) {
        this.mChunkProvider = chunkProvider;
        this.mStorage = storage;

        // enable world/chunk unload listener
        Bukkit.getPluginManager().registerEvents(this, BukkitPlugin.getInstance());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        World world = e.getWorld();
        WorldServer worldServer = (WorldServer) getWorld();

        if (! world.getName().equals(worldServer.getWorldData().getName())) {
            return;
        }

        Chunk chunk = e.getChunk();
        mProxiesToOutput.remove(new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        this.mStorage.save();
        this.mStorage.destroy();
        mProxiesToOutput.clear();
        HandlerList.unregisterAll(this);
    }

    private IChunkProvider getWrappedChunkProvider() {
        return mChunkProvider;
    }

    private int getOverrideLuminance(IChunkAccess blockAccess, BlockPosition blockPosition) {
        int fLightLevel = blockAccess.getType(blockPosition).h();

        if (mStorage.checkLightLevel(blockPosition.asLong())) {
            fLightLevel = mStorage.getLightLevel(blockPosition.asLong());
        }
        return fLightLevel;
    }

    // https://javarush.ru/quests/lectures/questcollections.level02.lecture07
    // https://martin.ankerl.com/2008/12/22/amazing-caching-proxy-in-java/
    private IChunkAccess getProxiedChunk(int x, int z) {
        // check chunk is available
        IChunkAccess originalChunk = (IChunkAccess) getWrappedChunkProvider().c(x, z);
        if (originalChunk == null) {
            return null;
        }

        // if chunk is available, get proxied chunk
        ChunkCoordIntPair chunkCoords = new ChunkCoordIntPair(x, z);
        synchronized (mProxiesToOutput) {
            if (mProxiesToOutput.containsKey(chunkCoords)) {
                return mProxiesToOutput.get(chunkCoords);
            }

            IChunkAccess res = (IChunkAccess) Proxy.newProxyInstance(IChunkAccess.class.getClassLoader(),
                    new Class[] {IChunkAccess.class}, (proxy, method, args) -> {
                        if (method.getName().equals(GET_LUMINANCE_METHOD)) {
                            return getOverrideLuminance(originalChunk, (BlockPosition) args[0]);
                        }
                        return method.invoke(originalChunk, args);
                    });
            if (res != null && ! mProxiesToOutput.containsKey(chunkCoords)) {
                mProxiesToOutput.put(chunkCoords, res);
            }
        }
        return mProxiesToOutput.get(chunkCoords);
    }

    // Override
    @Override
    public IChunkAccess c(int i, int i1) { // getChunk() (yarn-1.14)
        return getProxiedChunk(i, i1);
    }

    @Override
    public void a(EnumSkyBlock var0, SectionPosition var1) {
        getWrappedChunkProvider().a(var0, var1);
    }

    @Override
    public IBlockAccess getWorld() {
        return getWrappedChunkProvider().getWorld();
    }
}
