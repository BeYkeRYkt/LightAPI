package ru.BeYkeRYkt.LightAPI.nms;

import java.util.List;

import org.bukkit.Location;

public interface ILightRegistry {

    public void createLight(Location location, int light, boolean needUpdate);

    public void deleteLight(Location location, boolean needUpdate);

    public void createLight(List<Location> location, int light, boolean needUpdate);

    public void deleteLight(List<Location> location, boolean needUpdate);

    public void sendUpdateChunks();
}