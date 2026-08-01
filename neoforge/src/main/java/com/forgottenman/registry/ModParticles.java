package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Falling leaf particles, one per crown color */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ForgottenMan.MOD_ID);

    public static final Supplier<SimpleParticleType> SCARLET_LEAF =
            PARTICLES.register("scarlet_leaf", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> MAGENTA_LEAF =
            PARTICLES.register("magenta_leaf", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> DEEP_MAGENTA_LEAF =
            PARTICLES.register("deep_magenta_leaf", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
