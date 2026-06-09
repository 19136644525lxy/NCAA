package com.yifei.ncaa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "ncaa.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static Config config;

    public static void init() {
        if (configFile.exists()) {
            loadConfig();
        } else {
            createDefaultConfig();
        }
    }

    private static void loadConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            NCAA.LOGGER.error("Failed to load config: {}", e.getMessage());
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        config = new Config();
        saveConfig();
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            NCAA.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    public static class Config {
        public boolean modEnabled = true;
        public String message = "You cannot switch to creative mode!";
    }
}