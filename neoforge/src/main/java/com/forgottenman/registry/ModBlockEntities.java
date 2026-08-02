package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.forgottenman.block.MysteriousDoorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForgottenMan.MOD_ID);

    public static final Supplier<BlockEntityType<MysteriousDoorBlockEntity>> MYSTERIOUS_DOOR =
            BLOCK_ENTITIES.register("mysterious_door", () ->
                    BlockEntityType.Builder.of(MysteriousDoorBlockEntity::new, ModBlocks.MYSTERIOUS_DOOR.get()).build(null));

    private ModBlockEntities() {
    }
}
