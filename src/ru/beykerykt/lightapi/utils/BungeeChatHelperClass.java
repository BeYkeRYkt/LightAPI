package ru.beykerykt.lightapi.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.beykerykt.lightapi.LightAPI;

public class BungeeChatHelperClass {

	public static void sendMessageAboutPlugin(Player player, LightAPI plugin) {
		player.sendMessage(ChatColor.AQUA + " ------- <LightAPI " + ChatColor.WHITE + plugin.getDescription().getVersion() + "> ------- ");

		TextComponent version = new TextComponent(ChatColor.AQUA + " Current version: ");
		TextComponent update = new TextComponent(ChatColor.WHITE + plugin.getDescription().getVersion());
		update.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lightapi update"));
		update.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here for check update").create()));
		version.addExtra(update);
		player.spigot().sendMessage(version);

		player.sendMessage(ChatColor.AQUA + " Server name: " + ChatColor.WHITE + plugin.getServer().getName());
		player.sendMessage(ChatColor.AQUA + " Server version: " + ChatColor.WHITE + plugin.getServer().getVersion());

		TextComponent text = new TextComponent(" | ");
		TextComponent sourcecode = new TextComponent(ChatColor.AQUA + "Source code");
		sourcecode.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://github.com/BeYkeRYkt/LightAPI/"));
		sourcecode.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Goto the GitHub!").create()));
		text.addExtra(sourcecode);
		text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

		TextComponent developer = new TextComponent(ChatColor.AQUA + "Developer");
		developer.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://github.com/BeYkeRYkt/"));
		developer.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("BeYkeRYkt").create()));
		text.addExtra(developer);
		text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

		TextComponent contributors = new TextComponent(ChatColor.AQUA + "Contributors");
		contributors.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BeYkeRYkt/LightAPI/graphs/contributors"));
		contributors.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("ALIENS!!").create()));
		text.addExtra(contributors);
		text.addExtra(new TextComponent(ChatColor.WHITE + " | "));

		player.spigot().sendMessage(text);

		TextComponent licensed = new TextComponent(" Licensed under ");
		TextComponent MIT = new TextComponent(ChatColor.AQUA + "MIT License");
		MIT.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://opensource.org/licenses/MIT/"));
		MIT.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Goto for information about license!").create()));
		licensed.addExtra(MIT);
		player.spigot().sendMessage(licensed);
	}

}
