package com.forgottenman.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings, per world. The crafted mysterious door ignores all of
 * this; it only governs the doors the world already has.
 */
public final class ModConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue RANDOM_ENTRANCES_ENABLED;
    private static final ModConfigSpec.DoubleValue RANDOM_ENTRANCE_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("random_entrances");
        RANDOM_ENTRANCES_ENABLED = builder
                .comment("Any door you open has a chance to contain a portal to the tree room")
                .define("enabled", false);
        RANDOM_ENTRANCE_CHANCE = builder
                .comment("Chance each time you open a door. 0.01 is 1%.")
                .defineInRange("chance", 0.01D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean randomEntrancesEnabled() {
        return RANDOM_ENTRANCES_ENABLED.get();
    }

    public static double randomEntranceChance() {
        return RANDOM_ENTRANCE_CHANCE.get();
    }

    private ModConfig() {
    }
}
