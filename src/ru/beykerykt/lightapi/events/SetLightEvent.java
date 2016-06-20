package ru.beykerykt.lightapi.events;

import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SetLightEvent extends Event implements Cancellable {

	private boolean cancel;
	private static final HandlerList handlers = new HandlerList();
	private World world;
	private int x;
	private int y;
	private int z;
	private int level;
	private boolean async;

	public SetLightEvent(World world, int x, int y, int z, int level, boolean async) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.level = level;
		this.async = async;
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

	public int getLightLevel() {
		return level;
	}

	public void setLightLevel(int level) {
		this.level = level;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public World getWorld() {
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public boolean isAsync() {
		return async;
	}
	
	public void setAsync(boolean flag){
		this.async = flag;
	}
}
