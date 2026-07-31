package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

/** Falling leaf particles, one per crown color */
public final class ModParticles {
    public static final Supplier<SimpleParticleType> SCARLET_LEAF = register("scarlet_leaf");
    public static final Supplier<SimpleParticleType> MAGENTA_LEAF = register("magenta_leaf");
    public static final Supplier<SimpleParticleType> DEEP_MAGENTA_LEAF = register("deep_magenta_leaf");

    // SimpleParticleType's constructor is not public in vanilla, so this goes through
    // Fabric's factory rather than `new SimpleParticleType(false)` like on NeoForge
    private static Supplier<SimpleParticleType> register(String name) {
        SimpleParticleType type = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                ForgottenMan.id(name), FabricParticleTypes.simple());
        return () -> type;
    }

    public static void register() {
    }

    private ModParticles() {
    }
}
