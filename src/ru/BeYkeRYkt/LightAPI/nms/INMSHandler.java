package ru.BeYkeRYkt.LightAPI.nms;

import org.bukkit.Location;

public interface INMSHandler {

    public void createLight(Location location, int light);

    public void deleteLight(Location location);

    public void initWorlds();

    public void unloadWorlds();
}