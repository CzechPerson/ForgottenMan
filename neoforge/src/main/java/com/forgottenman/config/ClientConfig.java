package com.forgottenman.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side settings. Never loaded on a dedicated server.
 */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.EnumValue<EffectsMode> EFFECTS_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("effects");
        EFFECTS_MODE = builder
                .comment("How the portal, the man and the mirror copies are drawn.",
                        "auto: full effects, dropping to the shaderpack-safe path where needed",
                        "full: always the full effects, even under a shaderpack that breaks them",
                        "compat: always the shaderpack-safe path")
                .defineEnum("effects_mode", EffectsMode.AUTO);
        builder.pop();
        SPEC = builder.build();
    }

    /** Defaults to AUTO until the config file has been read. */
    public static EffectsMode effectsMode() {
        return SPEC.isLoaded() ? EFFECTS_MODE.get() : EffectsMode.AUTO;
    }

    private ClientConfig() {
    }
}
