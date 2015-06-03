package ru.BeYkeRYkt.LightAPI;

public class LightUpdater implements Runnable {

    private LightAPI plugin;

    public LightUpdater(LightAPI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getRegistry().sendUpdateChunks();
    }

}