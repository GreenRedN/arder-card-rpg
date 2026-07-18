package com.green.fantasysim.engine;

import com.green.fantasysim.io.JsonUtil;

import java.io.InputStream;

public final class ConfigLoader {
    private ConfigLoader(){}

    public static Config loadDefault() {
        try (InputStream in = ConfigLoader.class.getResourceAsStream("/config.json")) {
            if (in == null) throw new IllegalStateException("config.json not found in resources");
            return JsonUtil.read(in, Config.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
