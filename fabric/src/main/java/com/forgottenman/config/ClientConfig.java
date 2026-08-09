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
 * Client-side settings, read once from config/forgottenman-client.json.
 *
 * Kept apart from ModConfig because that one loads at common init, which runs on a
 * dedicated server. This is loaded from the client entrypoint only.
 */
public final class ClientConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Shaped like the NeoForge spec so both loaders read the same keys */
    private static final class Values {
        Effects effects = new Effects();
    }

    private static final class Effects {
        @SerializedName("effects_mode")
        String effectsMode = "auto";
    }

    private static Values values = new Values();
    private static EffectsMode mode = EffectsMode.AUTO;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("forgottenman-client.json");
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
        if (values.effects == null) {
            values.effects = new Effects();
        }
        mode = parse(values.effects.effectsMode);
        values.effects.effectsMode = mode.name().toLowerCase(java.util.Locale.ROOT);
        save(path);
    }

    private static EffectsMode parse(String raw) {
        if (raw != null) {
            try {
                return EffectsMode.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                LOGGER.warn("Unknown effects_mode '{}', using auto", raw);
            }
        }
        return EffectsMode.AUTO;
    }

    private static void save(Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(values, writer);
        } catch (IOException e) {
            LOGGER.error("Could not write {}", path, e);
        }
    }

    public static EffectsMode effectsMode() {
        return mode;
    }

    private ClientConfig() {
    }
}
