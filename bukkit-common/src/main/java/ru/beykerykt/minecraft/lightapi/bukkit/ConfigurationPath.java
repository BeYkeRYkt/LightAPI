/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2021 Vladimir Mikhailov <beykerykt@gmail.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.bukkit;

public class ConfigurationPath {

    /*
     * General
     */
    public static final String GENERAL_TITLE = "general";
    public static final String GENERAL_VERSION = GENERAL_TITLE + ".version";
    public static final String GENERAL_DEBUG = GENERAL_TITLE + ".debug";
    public static final String GENERAL_RELIGHT_STRATEGY = GENERAL_TITLE + ".relight-strategy";
    public static final String GENERAL_SPECIFIC_HANDLER = GENERAL_TITLE + ".specific-handler";
    public static final String GENERAL_SPECIFIC_STORAGE_PROVIDER = GENERAL_TITLE + ".specific-storage-provider";
    /*
     * Background service
     */
    public static final String BACKGROUND_SERVICE_TITLE = "background-service";
    public static final String BACKGROUND_SERVICE_TICK_DELAY = BACKGROUND_SERVICE_TITLE + ".tick-period";
    public static final String BACKGROUND_SERVICE_CORE_POOL_SIZE = BACKGROUND_SERVICE_TITLE + ".corePoolSize";

    /*
     * Chunk observer
     */
    public static final String CHUNK_OBSERVER_TITLE = "chunk-observer";
    @Deprecated
    public static final String CHUNK_OBSERVER_MERGE_CHUNK_SECTIONS = CHUNK_OBSERVER_TITLE + ".merge-chunk-sections";

    /*
     * Light observer
     */
    public static final String LIGHT_OBSERVER_TITLE = "light-observer";
    public static final String LIGHT_OBSERVER_MAX_TIME_MS_IN_PER_TICK = LIGHT_OBSERVER_TITLE + ".max-time-ms-in-per" +
            "-tick";
    public static final String LIGHT_OBSERVER_MAX_ITERATIONS_IN_PER_TICK = LIGHT_OBSERVER_TITLE + ".max" +
            "-iterations-in-per-tick";
}