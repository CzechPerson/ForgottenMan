package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.forgottenman.entity.ManEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ForgottenMan.MOD_ID);

    public static final Supplier<EntityType<ManEntity>> MAN = ENTITY_TYPES.register("man",
            () -> EntityType.Builder.of(ManEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build("forgottenman:man"));

    private ModEntities() {
    }
}
