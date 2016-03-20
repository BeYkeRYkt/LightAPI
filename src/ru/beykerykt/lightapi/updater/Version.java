package ru.beykerykt.lightapi.updater;

import java.util.regex.Matcher;

/**
 * Simple major.minor.patch storage system with getters.
 *
 * @author Connor Spencer Harries
 */
public class Version {

	/**
	 * Store the version major.
	 */
	private final int major;

	/**
	 * Store the version minor.
	 */
	private final int minor;

	/**
	 * Store the version patch.
	 */
	private final int patch;

	/**
	 * Create a new instance of the {@link de.albionco.updater.Version} class.
	 *
	 * @param major
	 *            semver major
	 * @param minor
	 *            semver minor
	 * @param patch
	 *            semver patch
	 */
	public Version(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	/**
	 * Quick method for parsing version strings and matching them using the {@link java.util.regex.Pattern} in {@link Updater}
	 *
	 * @param version
	 *            semver string to parse
	 * @return {@link de.albionco.updater.Version} if valid semver string
	 */
	public static Version parse(String version) {
		Matcher matcher = Updater.regex.matcher(version);

		if (matcher.matches()) {
			int x = Integer.parseInt(matcher.group(1));
			int y = Integer.parseInt(matcher.group(2));
			int z = Integer.parseInt(matcher.group(3));

			return new Version(x, y, z);
		}

		return null;
	}

	/**
	 * @return semver major
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * @return semver minor
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * @return semver patch
	 */
	public int getPatch() {
		return patch;
	}

	/**
	 * @return joined version string.
	 */
	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}

	/**
	 * Little method to see if the input version is greater than ours.
	 *
	 * @param version
	 *            input {@link de.albionco.updater.Version} object
	 * @return true if the version is greater than ours
	 */
	public boolean compare(Version version) {
		int result = version.getMajor() - this.getMajor();
		if (result == 0) {
			result = version.getMinor() - this.getMinor();
			if (result == 0) {
				result = version.getPatch() - this.getPatch();
			}
		}
		return result > 0;
	}
}
