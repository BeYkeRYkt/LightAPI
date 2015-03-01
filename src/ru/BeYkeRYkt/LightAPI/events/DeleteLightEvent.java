package ru.BeYkeRYkt.LightAPI.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DeleteLightEvent extends Event implements Cancellable {

    private boolean cancel;
    private static final HandlerList handlers = new HandlerList();
    private Location loc;

    public DeleteLightEvent(Location loc) {
        this.loc = loc;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean arg0) {
        this.cancel = arg0;
    }

    /**
     * @return the loc
     */
    public Location getLocation() {
        return loc;
    }

    /**
     * @param loc
     *            the loc to set
     */
    public void setLocation(Location loc) {
        this.loc = loc;
    }

}