package ru.beykerykt.lightapi;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import ru.beykerykt.lightapi.server.ServerModManager;
import ru.beykerykt.lightapi.server.ServerModInfo;
import ru.beykerykt.lightapi.utils.Metrics;

public class LightAPI extends JavaPlugin{
	
	private static LightAPI plugin;
	
	@Override
	public void onLoad(){
		this.plugin = this;
		
		ServerModInfo craftbukkit = new ServerModInfo("CraftBukkit");
		//craftbukkit.getVersions().put("v1_7_R4", NMSHandler.class);
		ServerModManager.registerServerMod(craftbukkit);
		
		ServerModInfo spigot = new ServerModInfo("Spigot");
		ServerModManager.registerServerMod(spigot);
		
		ServerModInfo paperspigot = new ServerModInfo("PaperSpigot");
		ServerModManager.registerServerMod(paperspigot);
		
		ServerModInfo paper = new ServerModInfo("Paper");
		ServerModManager.registerServerMod(paper);
		
		ServerModInfo tacospigot = new ServerModInfo("TacoSpigot");
		ServerModManager.registerServerMod(tacospigot);
	}
	
	@Override
	public void onEnable() {
		ServerModManager.init();
		
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
