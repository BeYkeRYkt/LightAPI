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

public class LightFlags {

	/**
	 * N/A
	 */
	public static final int NONE = 0;

	/**
	 * Light from the blocks (torch, glowstone, etc.)
	 */
	public static final int BLOCK_LIGHTING = 1;

	/**
	 * Light from sky
	 */
	public static final int SKY_LIGHTING = 2;

	/**
	 * <ul>
	 * <li>At setting light level: Applies new parameters for BLOCK_LIGHTING and
	 * SKY_LIGHTING.
	 * <li>At recalculating: Makes a BLOCK_LIGHTING allocation based on
	 * SKY_LIGHTING.
	 * <li>At getting light level: returns the overall lighting level.
	 * </ul>
	 */
	public static final int COMBO_LIGHTING = 4; // 15
}
