/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.lightapi.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.beykerykt.lightapi.LightAPI;

public class BungeeChatHelperClass {

	public static boolean hasBungeeChatAPI() {
		try {
			Class<?> clazz = Class.forName("net.md_5.bungee.api.chat.TextComponent");
			if (clazz != null) {
				clazz = null;
				return true;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

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
