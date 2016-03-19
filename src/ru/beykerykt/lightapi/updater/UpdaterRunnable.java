package ru.beykerykt.lightapi.updater;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simple solution to stop the main thread being blocked
 *
 * @author Connor Spencer Harries
 */
public class UpdaterRunnable implements Runnable {

	/**
	 * Store the parent {@link Updater} instance.
	 */
	private final Updater updater;

	/**
	 * Create a new {@link de.albionco.updater.UpdaterRunnable} with an {@link Updater} as the parent.
	 *
	 * @param parent
	 *            instace of {@link Updater}
	 */
	public UpdaterRunnable(Updater parent) {
		this.updater = parent;
	}

	/**
	 * Use reflection to invoke the run method on our {@link Updater}
	 */
	@Override
	public void run() {
		try {
			Method method = updater.getClass().getDeclaredMethod("run");
			method.setAccessible(true);

			method.invoke(updater);

			method.setAccessible(false);

		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
