package ru.beykerykt.lightapi.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.beykerykt.lightapi.server.nms.INMSHandler;

public class ServerModInfo {
	
	private String modName; 
	private Map<String, Class<? extends INMSHandler>> versions;
	
	public ServerModInfo(String modname) {
		this.modName = modname;
		this.versions = new ConcurrentHashMap<String, Class<? extends INMSHandler>>();
	}
	
	public String getModName() {
		return modName;
	}
	
	public Map<String, Class<? extends INMSHandler>> getVersions() {
		return versions;
	}
}