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
