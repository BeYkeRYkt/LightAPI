/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.beykerykt.minecraft.lightapi.common.impl.ImplementationPlatform;

/**
 * Information about the current LightAPI build
 */
public class Build {

	/**
	 * Internal version. May change during any changes in the API. The string should
	 * change from release to release.
	 */
	public static final int CURRENT_VERSION = VERSION_CODES.THREE;

	/**
	 * Platform that is being used.
	 */
	public static ImplementationPlatform CURRENT_IMPLEMENTATION = LightAPI.get().getImplementationPlatform();

	private static final List<String> map;

	static {
		map = new CopyOnWriteArrayList<String>();
		map.add(VERSION_CODES.UNKNOWN, "Unknown");
		map.add(VERSION_CODES.ONE, "One");
		map.add(VERSION_CODES.TWO, "Two");
		map.add(VERSION_CODES.THREE, "Three");
		map.add(VERSION_CODES.FOUR, "Four");
	}

	public static class VERSION_CODES {
		/**
		 * UNKNOWN
		 */
		public static int UNKNOWN = 0;

		/**
		 * 20 March 2019: First version recoded API for LightAPI
		 */
		public static int ONE = 1;

		/**
		 * NO_DATA: Disable LightAPI-Backward. Introduce ImplementationPlatform. Rewrite
		 * API for lighting changes in MC 1.14.
		 */
		public static int TWO = 2;

		/**
		 * NO_DATA: Global rewrite API for local unknown project changes. Preparatory
		 * phase.
		 */
		public static int THREE = 3;

		/**
		 * TBA: First public rewrited API version.
		 */
		public static int FOUR = 4;
	}

	public static String getNameForInt(int id) {
		final String name = map.get(id);
		if (name == null) {
			return map.get(0);
		}
		return name;
	}
}
