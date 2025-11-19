package com.ctnh.ae2pw;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = AE2PW.MODID)
public class AE2PWConfig {
    public static AE2PWConfig INSTANCE;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(AE2PWConfig.class, ConfigFormats.yaml()).getConfigInstance();
            }
        }
    }
}
