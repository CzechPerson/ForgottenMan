package com.forgottenman.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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

    /** Mirrors the NeoForge config's wild_portals section */
    private static final class Values {
        boolean wildPortalsEnabled = false;
        double wildPortalChance = 0.01D;
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
        values.wildPortalChance = Math.max(0.0D, Math.min(1.0D, values.wildPortalChance));
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

    /** Any door you open can become a way into the tree room */
    public static boolean wildPortalsEnabled() {
        return values.wildPortalsEnabled;
    }

    /** Chance each time a door is opened, 0.01 being 1% */
    public static double wildPortalChance() {
        return values.wildPortalChance;
    }

    private ModConfig() {
    }
}
