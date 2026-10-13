package com.power16.carryMod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CarryConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("carry-helper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("carry-helper.json");

    private static CarryConfig instance = new CarryConfig();

    // ---- Feature toggles ----
    public boolean carryCommand = true;     // /carry ...
    public boolean counterCommands = true;  // /carry-helper count ...
    public boolean counterKeybinds = true;  // inc/dec keys
    public boolean counterMessages = true;  // chat message when the counter changes

    // ---- Saved state ----
    public int count = 0;

    public static CarryConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                CarryConfig loaded = GSON.fromJson(reader, CarryConfig.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                LOGGER.error("Failed to read config, using defaults", e);
            }
        }
        save();
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}