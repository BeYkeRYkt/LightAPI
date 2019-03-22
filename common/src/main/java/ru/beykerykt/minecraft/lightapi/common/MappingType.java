/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Vladimir Mikhailov <beykerykt@gmail.com>
 * Copyright (c) 2016-2017 The ImplexDevOne Project
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

public enum MappingType {
	/**
	 * N/A
	 */
	UNKNOWN(0),

	/**
	 * Original names in the obfuscated Minecraft binary.
	 */
	VANILLA(1),

	/**
	 * Contain unique names for all obfuscated methods, fields and parameters, as
	 * well as human-readable names for the classes
	 */
	SRG(2),

	/**
	 * Contain human-readable names largely contributed by the community
	 */
	MCP(3),

	/**
	 * Default NMS from CraftBukkit
	 */
	CB(4),

	/**
	 * ???
	 */
	CUSTOM(5);

	private final int id;

	private MappingType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
