package com.forgottenman;

import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModBlockEntities;
import com.forgottenman.registry.ModBlocks;
import com.forgottenman.registry.ModCreativeTabs;
import com.forgottenman.registry.ModEntities;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModParticles;
import com.forgottenman.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ForgottenMan.MOD_ID)
public class ForgottenMan {
    public static final String MOD_ID = "forgottenman";

    public ForgottenMan(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
