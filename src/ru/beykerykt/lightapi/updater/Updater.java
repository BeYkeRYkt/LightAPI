package ru.beykerykt.lightapi.updater;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Class that tries to find updates on a GitHub repository.
 *
 * @author Connor Spencer Harries
 * @version 1.0.2
 */
@SuppressWarnings("unused")
public class Updater {

	/**
	 * Pattern used to match semantic versioning compliant strings.
	 * <p/>
	 * Major: matcher.group(1) Minor: matcher.group(2) Patch: matcher.group(3)
	 * <p/>
	 * Does detect suffixes such as RC though they're unused as of now.
	 */
	protected static Pattern regex = Pattern.compile("(?:[v]?)([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?", Pattern.CASE_INSENSITIVE);

	/**
	 * URL template for all API calls
	 */
	protected static String api = "https://api.github.com/repos/{{ REPOSITORY }}/releases";

	/**
	 * Store the query result.
	 */
	private Response result = Response.NO_UPDATE;

	/**
	 * User agent to use when making requests, according to the API it's preferred if this is your username.
	 *
	 * See https://developer.github.com/v3/#user-agent-required
	 */
	private String agent = "Albioncode";

	/**
	 * Store the repository to lookup.
	 */
	private String repository = null;

	/**
	 * Should I print to System.out?
	 */
	private boolean verbose = false;

	/**
	 * Store the <strong>latest</strong> version.
	 */
	private Version version;

	/**
	 * Store the version passed in the constructor.
	 */
	private Version current;

	/**
	 * Thread that does the heavy lifting.
	 */
	private Thread thread;

	/**
	 * URL to query.
	 */
	private URL url;

	// BeYkeRYkt
	private String body;

	/**
	 * Create a new {@link Updater} using integers as the major, minor and patch.
	 *
	 * @param major
	 *            current major
	 * @param minor
	 *            current minor
	 * @param patch
	 *            current patch
	 * @param repository
	 *            github repository to query
	 */
	public Updater(int major, int minor, int patch, String repository) throws Exception {
		this(Version.parse(major + "." + minor + "." + patch), repository, false);
	}

	/**
	 * Create a new {@link Updater} using integers as the major, minor and patch.
	 *
	 * @param major
	 *            current major
	 * @param minor
	 *            current minor
	 * @param patch
	 *            current patch
	 * @param repository
	 *            github repository to query
	 */
	public Updater(int major, int minor, int patch, String repository, boolean verbose) throws Exception {
		this(Version.parse(major + "." + minor + "." + patch), repository, verbose);
	}

	/**
	 * Create a new {@link Updater} using a {@link java.lang.String}
	 *
	 * @param version
	 *            string containing valid semver string
	 * @param repository
	 *            github repository to query
	 * @throws Exception
	 *             error whilst parsing semver string
	 */
	public Updater(String version, String repository) throws Exception {
		this(Version.parse(version), repository, false);
	}

	/**
	 * Create a new {@link Updater} using a {@link java.lang.String}
	 *
	 * @param version
	 *            string containing valid semver string
	 * @param repository
	 *            github repository to query
	 * @param verbose
	 *            print information to System.out
	 * @throws Exception
	 *             error whilst parsing semver string
	 */
	public Updater(String version, String repository, boolean verbose) throws Exception {
		this(Version.parse(version), repository, verbose);
	}

	/**
	 * Create a new {@link Updater} using a {@link de.albionco.updater.Version} object.
	 *
	 * @param repository
	 *            github repository to query
	 */
	public Updater(Version version, String repository) throws Exception {
		this(version, repository, false);
	}

	/**
	 * Create a new {@link Updater} using a {@link java.lang.String}
	 *
	 * @param version
	 *            string containing valid semver string
	 * @param repository
	 *            github repository to query
	 * @param verbose
	 *            print information to console
	 * @throws Exception
	 *             error whilst parsing semver string
	 */
	public Updater(Version version, String repository, boolean verbose) throws Exception {
		if (version == null) {
			throw new Exception("Provided version is not semver compliant!");
		}

		this.repository = repository;
		this.current = version;
		this.verbose = verbose;

		try {
			this.url = new URL(api.replace("{{ REPOSITORY }}", this.repository));
			log(Level.INFO, "Set the URL to get");
		} catch (NumberFormatException ex) {
			log(Level.SEVERE, "Unable to parse semver string!");
			throw new Exception("Unable to parse semver string!");
		} catch (MalformedURLException ex) {
			log(Level.SEVERE, "Invalid URL, return failed response.");
			result = Response.FAILED;
			throw new Exception(ex.getMessage());
		}

		if (this.result != Response.FAILED) {
			this.thread = new Thread(new UpdaterRunnable(this));
			this.thread.start();
		}
	}

	/**
	 * Actually lookup the JSON data.
	 */
	private void run() {
		try {
			final URLConnection connection = url.openConnection();
			connection.setConnectTimeout(6000);

			connection.addRequestProperty("Accept", "application/vnd.github.v3+json");
			log(Level.INFO, "Opening connection to API");

			final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			final StringBuilder buffer = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}

			JSONArray releases = (JSONArray) JSONValue.parse(buffer.toString());
			log(Level.INFO, "Parsing the returned JSON");

			if (releases.isEmpty()) {
				log(Level.WARNING, "Appears there were no releases, setting result");
				this.result = Response.REPO_NO_RELEASES;
				return;
			}

			JSONObject release = (JSONObject) releases.get(0);
			String tag = release.get("tag_name").toString();

			// BeYkeRYkt
			String body = release.get("body").toString();

			if (isSemver(tag)) {
				this.version = Version.parse(tag);
				// BeYkeRYkt
				this.body = body;

				if (current.compare(version)) {
					log(Level.INFO, "Hooray, we found a semver compliant update!");
					this.result = Response.SUCCESS;
				} else {
					log(Level.INFO, "The version you specified is the latest version available!");
					this.result = Response.NO_UPDATE;
				}
			} else {
				log(Level.WARNING, "Version string is not semver compliant!");
				this.result = Response.REPO_NOT_SEMVER;
			}
		} catch (Exception e) {
			if (e.getMessage().contains("HTTP response code: 403")) {
				log(Level.WARNING, "GitHub denied our HTTP request!");
				this.result = Response.GITHUB_DENY;
			} else if (e.getMessage().contains("HTTP response code: 404")) {
				log(Level.WARNING, "The specified repository could not be found!");
				this.result = Response.REPO_NOT_FOUND;
			} else if (e.getMessage().contains("HTTP response code: 500")) {
				log(Level.WARNING, "Internal server error");
				this.result = Response.GITHUB_ERROR;
			} else {
				log(Level.SEVERE, "Failed to check for updates!");
				this.result = Response.FAILED;
				this.version = null;
			}
		}
	}

	private void exit(Response response) {
		if (response != Response.SUCCESS) {
			this.result = response;
			this.version = null;
		}
	}

	/**
	 * @return {@link java.lang.String} the version that GitHub tells us about.
	 */
	public String getLatestVersion() {
		waitForThread();

		if (version == null) {
			log(Level.INFO, "Latest version is undefined, return message.");
			return "Please check #getResult()";
		}

		log(Level.INFO, "Somebody queried the latest version");
		return version.toString();
	}

	/**
	 * @return {@link de.albionco.updater.Response}
	 */
	public Response getResult() {
		log(Level.INFO, "Somebody queried the update result");
		waitForThread();
		return this.result;
	}

	/**
	 * @return the update repository
	 */
	public String getRepository() {
		log(Level.INFO, "Somebody queried the repository");
		return this.repository;
	}

	/**
	 * Try and wait for the thread to finish executing.
	 */
	private void waitForThread() {
		if ((this.thread != null) && this.thread.isAlive()) {
			try {
				this.thread.join();
				log(Level.INFO, "Trying to join thread");
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Test if the version string contains a valid semver string
	 *
	 * @param version
	 *            version to test
	 * @return true if valid
	 */
	public static boolean isSemver(String version) {
		return regex.matcher(version).matches();
	}

	private void log(Level level, String message) {
		if (this.verbose) {
			String msg = String.format("[%s] %s", level.toString().toUpperCase(), message);
			if (level != Level.SEVERE) {
				System.out.println(msg);
			} else {
				System.err.println(msg);
			}
		}
	}

	// BeYkeRYkt
	public String getChanges() {
		return body;
	}

	public Version getVersion() {
		waitForThread();
		if (version == null) {
			log(Level.INFO, "Latest version is undefined, return message.");
			return null;
		}

		log(Level.INFO, "Somebody queried the latest version");
		return version;
	}
}
