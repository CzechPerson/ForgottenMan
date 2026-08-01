package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.forgottenman.entity.ManEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public final class ModEntities {
    public static final Supplier<EntityType<ManEntity>> MAN = register("man",
            EntityType.Builder.of(ManEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build("forgottenman:man"));

    private static <T extends EntityType<?>> Supplier<T> register(String name, T type) {
        T registered = Registry.register(BuiltInRegistries.ENTITY_TYPE, ForgottenMan.id(name), type);
        return () -> registered;
    }

    public static void register() {
    }

    private ModEntities() {
    }
}
