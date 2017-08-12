/**
 * Copyright 2016 The DevelopedINSIDE Project
 * Copyright 2016 - 2017 Vladimir Mikhaylov
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
 * 
 * Only for educational purposes. DO NOT USE IN PUBLIC PROJECTS.
 */
package ru.beykerykt.android.experimental.threadhotplug;

public class TaskScheduler {

	private static final boolean DEBUG = true;
	private static IHotplug hotplug;

	public static void start() {
		if (getHotplug() == null || getHotplug().isInit()) {
			debug("Hotplug is null or enabled!");
			return;
		}
		getHotplug().start();
	}

	public static void shutdown() {
		if (getHotplug() == null || !getHotplug().isInit()) {
			debug("Hotplug is null or disabled!");
			return;
		}
		getHotplug().shutdown();
	}

	public static boolean isInit() {
		if (getHotplug() == null) {
			return false;
		}
		return getHotplug().isInit();
	}

	public static void execute(Task task) {
		if (!isInit())
			return;
		getHotplug().execute(task);
	}

	public static void execute(Runnable runnable) {
		if (!isInit())
			return;
		getHotplug().execute(runnable);
	}

	public static IHotplug getHotplug() {
		return hotplug;
	}

	public static void setHotplug(IHotplug hp) {
		if (hp == null) {
			shutdown();
		}

		hotplug = hp;
	}

	public static void debug(String message) {
		if (DEBUG)
			System.out.println(message);
	}
}
