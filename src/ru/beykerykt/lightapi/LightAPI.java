package ru.beykerykt.lightapi;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import ru.beykerykt.lightapi.server.ServerManager;
import ru.beykerykt.lightapi.server.ServerModInfo;
import ru.beykerykt.lightapi.utils.Metrics;

public class LightAPI extends JavaPlugin{
	
	private static LightAPI plugin;
	
	@Override
	public void onLoad(){
		this.plugin = this;
		
		ServerModInfo craftbukkit = new ServerModInfo("CraftBukkit");
		//craftbukkit.getVersions().put("v1_7_R4", NMSHandler.class);
		ServerManager.registerServerMod(craftbukkit);
		
		ServerModInfo spigot = new ServerModInfo("Spigot");
		ServerManager.registerServerMod(spigot);
		
		ServerModInfo paperspigot = new ServerModInfo("PaperSpigot");
		ServerManager.registerServerMod(paperspigot);
		
		ServerModInfo paper = new ServerModInfo("Paper");
		ServerManager.registerServerMod(paper);
		
		ServerModInfo tacospigot = new ServerModInfo("TacoSpigot");
		ServerManager.registerServerMod(tacospigot);
	}
	
	@Override
	public void onEnable() {
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			// nothing...
		}
	}
	
	public static LightAPI getInstance() {
		return plugin;
	}
	
	public void log(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.YELLOW + "<Light" + ChatColor.RED + "API" + ChatColor.YELLOW + ">: " + ChatColor.WHITE + message);
	}
}
