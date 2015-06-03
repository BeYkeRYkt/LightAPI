package ru.BeYkeRYkt.LightAPI.nms;

import org.bukkit.Location;

public interface ILightRegistry {

    public void createLight(Location location, int light);

    public void deleteLight(Location location);

    public void collectChunks(Location loc);

    public void sendUpdateChunks();
}