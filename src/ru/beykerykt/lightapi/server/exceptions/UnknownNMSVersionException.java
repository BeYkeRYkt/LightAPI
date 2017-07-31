package ru.beykerykt.lightapi.server.exceptions;

public class UnknownNMSVersionException extends ServerModException {

	/**
	 * ???
	 */
	private static final long serialVersionUID = -1927826763790232512L;

	private final String modName;
	private final String nmsVersion;

	public UnknownNMSVersionException(String modName, String nmsVersion) {
		super("Could not find handler for this Bukkit " + modName + " implementation " + nmsVersion + " version.");
		this.modName = modName;
		this.nmsVersion = nmsVersion;
	}

	public String getNmsVersion() {
		return nmsVersion;
	}

	public String getModName() {
		return modName;
	}
}
