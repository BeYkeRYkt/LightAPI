package ru.beykerykt.lightapi.updater;

/**
 * Enumeration of possible responses from the updater.
 *
 * @author Connor Spencer Harries
 */
public enum Response {

	/**
	 * GitHub could not find the repository.
	 */
	REPO_NOT_FOUND,

	/**
	 * The latest release on GitHub isn't semver compliant.
	 */
	REPO_NOT_SEMVER,

	/**
	 * No releases have been made on the repository.
	 */
	REPO_NO_RELEASES,

	/**
	 * An update has been found.
	 */
	SUCCESS,

	/**
	 * An error occured whilst trying to find updates.
	 */
	FAILED,

	/**
	 * GitHub denied the connection. This is most likely due to too many connections being opened to the API within a small period of time.
	 */
	GITHUB_DENY,

	/**
	 * Used to indicate a server error such as HTTP status code 500.
	 */
	GITHUB_ERROR,

	/**
	 * The specified version is already the latest version
	 */
	NO_UPDATE;

	@Override
	public String toString() {
		return this.name();
	}

}
