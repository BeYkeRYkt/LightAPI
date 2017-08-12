package ru.beykerykt.lightapi.server.exceptions;

public class UnknownModImplementationException extends ServerModException {

	/**
	 * ???
	 */
	private static final long serialVersionUID = -1754539191843175633L;

	private final String modName;

	public UnknownModImplementationException(String modName) {
		super("Could not find handler for this Bukkit implementation: " + modName);
		this.modName = modName;
	}

	public String getModName() {
		return modName;
	}
}
