package com.forgottenman.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings, per world. The crafted mysterious door ignores all of
 * this; it only governs the doors the world already has.
 */
public final class ModConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue WILD_PORTALS_ENABLED;
    private static final ModConfigSpec.DoubleValue WILD_PORTAL_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("wild_portals");
        WILD_PORTALS_ENABLED = builder
                .comment("Let any door you open become a way into the tree room.",
                        "The portal lasts until that door closes or you leave the room,",
                        "and the door itself is never destroyed.")
                .define("enabled", false);
        WILD_PORTAL_CHANCE = builder
                .comment("Chance each time you open a door. 0.01 is 1%.")
                .defineInRange("chance", 0.01D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean wildPortalsEnabled() {
        return WILD_PORTALS_ENABLED.get();
    }

    public static double wildPortalChance() {
        return WILD_PORTAL_CHANCE.get();
    }

    private ModConfig() {
    }
}
