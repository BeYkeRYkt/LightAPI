package ru.beykerykt.lightapi.updater;

import java.util.regex.Matcher;

import org.bukkit.ChatColor;

import ru.beykerykt.lightapi.LightAPI;

public enum UpdateType {
	OUTDATE(ChatColor.GRAY + "Outdate"),
	MAJOR(ChatColor.RED + "Major"),
	MINOR(ChatColor.YELLOW + "Minor"),
	PATCH(ChatColor.GREEN + "Patch");

	private String name;

	UpdateType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static UpdateType compareVersion(String newVersion) {
		Integer[] pluginMatcher = getMatchers(LightAPI.getInstance().getDescription().getVersion());
		Integer[] updateMatcher = getMatchers(newVersion);

		if (pluginMatcher[0] < updateMatcher[0]) {
			return UpdateType.MAJOR;
		}

		if (pluginMatcher[1] < updateMatcher[1]) {
			return UpdateType.MINOR;
		}

		if (pluginMatcher[2] < updateMatcher[2]) {
			return UpdateType.PATCH;
		}

		return UpdateType.OUTDATE;
	}

	public static Integer[] getMatchers(String version) {
		Matcher matcher = Updater.regex.matcher(version);
		Integer[] list = new Integer[3];
		if (matcher.matches()) {
			list[0] = Integer.parseInt(matcher.group(1));
			list[1] = Integer.parseInt(matcher.group(2));
			list[2] = Integer.parseInt(matcher.group(3));
		}
		return list;
	}
}
