/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Vladimir Mikhailov <beykerykt@gmail.com>
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

package ru.beykerykt.minecraft.lightapi.bukkit.example;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;

public class EntityTracker implements Listener {

    private final int RADIUS = 32;
    private List<Entity> mTrackedEntityList = new CopyOnWriteArrayList<>();
    private final Runnable searchRun = () -> {
        for (Player player : Bukkit.getOnlinePlayers()) {
            searchEntities(player);
        }
    };
    private Map<UUID, Location> lightLocations = new HashMap<>();
    private BukkitPlugin mPlugin;
    private final Runnable updateRun = () -> updateLights();
    private int taskId1;
    private int taskId2;

    public EntityTracker(BukkitPlugin plugin) {
        this.mPlugin = plugin;
    }

    public void start() {
        this.taskId1 = Bukkit.getScheduler().runTaskTimerAsynchronously(mPlugin, updateRun, 1, 0).getTaskId();
        this.taskId2 = Bukkit.getScheduler().runTaskTimer(mPlugin, searchRun, 10, 0).getTaskId();

        Bukkit.getPluginManager().registerEvents(this, mPlugin);
    }

    public void shutdown() {
        for (Location loc : lightLocations.values()) {
            int blockX = loc.getBlockX();
            int blockY = loc.getBlockY();
            int blockZ = loc.getBlockZ();
            getPlugin().mLightAPI.setLightLevel(loc.getWorld().getName(), blockX, blockY, blockZ, 0,
                    LightFlag.BLOCK_LIGHTING, EditPolicy.FORCE_IMMEDIATE, SendPolicy.IGNORE, null);
        }

        lightLocations.clear();
        mTrackedEntityList.clear();
        Bukkit.getScheduler().cancelTask(taskId1);
        Bukkit.getScheduler().cancelTask(taskId2);
        HandlerList.unregisterAll(this);
    }

    private BukkitPlugin getPlugin() {
        return mPlugin;
    }

    private boolean isEntityInRadius(Location center, double radius, Entity entity) {
        return isInRadius(center, entity.getLocation(), radius);
    }

    private boolean isInRadius(Location center, Location loc, double radius) {
        if (!loc.getWorld().equals(center.getWorld())) {
            return false;
        }
        return center.distanceSquared(loc) <= (radius * radius);
    }

    private boolean containsInEntityList(Entity entity) {
        for (Entity e : mTrackedEntityList) {
            if (e.getUniqueId().equals(entity.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private int randomValue(int min, int max) {
        int d = max + Math.abs(min) + 1;
        return (int) (Math.random() * d) - max;
    }

    // check only world and positions
    private boolean equalsLocations(Location firstLoc, Location secondLoc) {
        return firstLoc.getWorld().getName().equals(secondLoc.getWorld().getName())
                && firstLoc.getBlockX() == secondLoc.getBlockX()
                && firstLoc.getBlockY() == secondLoc.getBlockY()
                && firstLoc.getBlockZ() == secondLoc.getBlockZ();
    }

    private void setLightLevel(Location loc, int lightLevel) {
        int blockX = loc.getBlockX();
        int blockY = loc.getBlockY();
        int blockZ = loc.getBlockZ();
        getPlugin().mLightAPI.setLightLevel(loc.getWorld().getName(), blockX, blockY, blockZ, lightLevel);
    }

    private void updateLights() {
        List<Location> removeLightLocs = new ArrayList<>();
        List<Location> newLightUpLocs = new ArrayList<>();

        // update current locations
        synchronized (mTrackedEntityList) {
            for (Entity entity : mTrackedEntityList) {
                UUID entityUUID = entity.getUniqueId();
                Location entityLoc = entity.getLocation().clone();
                if (!lightLocations.containsKey(entityUUID)) {
                    lightLocations.put(entityUUID, entityLoc);
                    newLightUpLocs.add(entityLoc);
                    continue;
                }

                boolean entityIsAvailable = false;
                // check player distance
                for (Player player : entity.getWorld().getPlayers()) {
                    if (isEntityInRadius(player.getLocation(), RADIUS, entity)) {
                        entityIsAvailable = true;
                        break;
                    }
                }
                entityIsAvailable &= !entity.isDead();

                Location lastKnowLocation = lightLocations.get(entityUUID);
                if (!entityIsAvailable) {
                    removeLightLocs.add(lastKnowLocation);
                    lightLocations.remove(entityUUID);
                    mTrackedEntityList.remove(entity);
                    continue;
                }

                if (!equalsLocations(lastKnowLocation, entityLoc)) {
                    removeLightLocs.add(lastKnowLocation);
                    newLightUpLocs.add(entityLoc);
                    lightLocations.replace(entityUUID, entityLoc);
                }
            }
        }

        // light down old locations
        if (!removeLightLocs.isEmpty()) {
            for (Location loc : removeLightLocs) {
                setLightLevel(loc, 0);
            }
        }

        // light up new locations
        if (!newLightUpLocs.isEmpty()) {
            for (Location loc : newLightUpLocs) {
                int value = 14;
                setLightLevel(loc, value);
            }
        }
        removeLightLocs.clear();
        newLightUpLocs.clear();
    }

    private void searchEntities(Player player) {
        Iterator<Entity> entityList = player.getWorld().getEntitiesByClasses(new Class[] {
                Monster.class,
                Item.class,
                Projectile.class
        }).iterator();
        while (entityList.hasNext()) {
            Entity entity = entityList.next();
            if (!isEntityInRadius(player.getLocation(), RADIUS, entity)) {
                continue;
            }
            synchronized (mTrackedEntityList) {
                if (containsInEntityList(entity)) {
                    continue;
                }
                mTrackedEntityList.add(entity);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        synchronized (mTrackedEntityList) {
            if (mTrackedEntityList.contains(entity)) {
                UUID uuid = entity.getUniqueId();
                Location lastKnowLocation = lightLocations.get(uuid);
                setLightLevel(lastKnowLocation, 0);
                lightLocations.remove(uuid);
                mTrackedEntityList.remove(entity);
            }
        }
    }
}
