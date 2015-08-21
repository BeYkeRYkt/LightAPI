package ru.BeYkeRYkt.LightAPI.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import ru.BeYkeRYkt.LightAPI.ChunkCoord;

public class UpdateChunkEvent extends Event implements Cancellable {

	private boolean cancel;
	private static final HandlerList handlers = new HandlerList();
	private ChunkCoord cCoord;

	public UpdateChunkEvent(ChunkCoord cCoord) {
		this.cCoord = cCoord;
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

	public ChunkCoord getChunkCoord() {
		return cCoord;
	}

	public void setChunkCoord(ChunkCoord cCoord) {
		this.cCoord = cCoord;
	}
}
