package com.forgottenman.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings, read once at startup from config/forgottenman.json. Fabric ships no
 * config API, so this is the whole of it; the file is written back with the
 * defaults filled in the first time the mod runs.
 */
public final class ModConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Shaped like the NeoForge config so both loaders read the same keys */
    private static final class Values {
        @SerializedName("random_entrances")
        RandomEntrances randomEntrances = new RandomEntrances();
    }

    /** Any door you open has a chance to contain a portal to the tree room */
    private static final class RandomEntrances {
        boolean enabled = false;
        double chance = 0.01D;
    }

    private static Values values = new Values();

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("forgottenman.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Values read = GSON.fromJson(reader, Values.class);
                if (read != null) {
                    values = read;
                }
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.error("Could not read {}, falling back to defaults", path, e);
                values = new Values();
            }
        }
        // A file missing the section entirely leaves the field null
        if (values.randomEntrances == null) {
            values.randomEntrances = new RandomEntrances();
        }
        values.randomEntrances.chance = Math.max(0.0D, Math.min(1.0D, values.randomEntrances.chance));
        save(path);
    }

    private static void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(values, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not write {}", path, e);
        }
    }

    /** Any door you open has a chance to contain a portal to the tree room */
    public static boolean randomEntrancesEnabled() {
        return values.randomEntrances.enabled;
    }

    /** Chance each time a door is opened, 0.01 being 1% */
    public static double randomEntranceChance() {
        return values.randomEntrances.chance;
    }

    private ModConfig() {
    }
}
