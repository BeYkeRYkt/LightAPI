package ru.beykerykt.lightapi;

public enum Build {

	UNKNOWN("unknown", 0),
	ANDROMEDA("Andromeda", 1);

	private String name;
	private int id;

	private Build(String name, int id) {
		this.name = name;
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public static Build getCurrentBuild() {
		return Build.ANDROMEDA;
	}

	public static Build getBuildFromId(int id) {
		for (Build build : values()) {
			if (build.getId() == id) {
				return build;
			}
		}
		return null;
	}
}
