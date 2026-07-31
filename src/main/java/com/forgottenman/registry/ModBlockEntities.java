package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.forgottenman.block.MysteriousDoorBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final Supplier<BlockEntityType<MysteriousDoorBlockEntity>> MYSTERIOUS_DOOR = register(
            "mysterious_door",
            BlockEntityType.Builder.of(MysteriousDoorBlockEntity::new, ModBlocks.MYSTERIOUS_DOOR.get()).build(null));

    private static <T extends BlockEntityType<?>> Supplier<T> register(String name, T type) {
        T registered = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ForgottenMan.id(name), type);
        return () -> registered;
    }

    public static void register() {
    }

    private ModBlockEntities() {
    }
}
