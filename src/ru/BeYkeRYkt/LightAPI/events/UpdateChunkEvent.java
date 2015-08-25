package ru.BeYkeRYkt.LightAPI.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import ru.BeYkeRYkt.LightAPI.ChunkInfo;

public class UpdateChunkEvent extends Event implements Cancellable {

	private boolean cancel;
	private static final HandlerList handlers = new HandlerList();
	private ChunkInfo cCoord;

	public UpdateChunkEvent(ChunkInfo cCoord) {
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

	public ChunkInfo getChunkInfo() {
		return cCoord;
	}

	public void setChunkInfo(ChunkInfo cCoord) {
		this.cCoord = cCoord;
	}
}
