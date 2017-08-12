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

public interface IHotplug {

	public void start();

	public boolean isInit();

	public void shutdown();

	public void execute(Task task);

	public void execute(Runnable runnable);

	public int getMaxThreads();

	public void setMaxThreads(int max_threads);

	public int getCurrentThreads();

	public void setCurrentThreads(int current_threads);
}
