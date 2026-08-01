package com.forgottenman;

import com.forgottenman.network.ModNetworking;
import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModBlockEntities;
import com.forgottenman.registry.ModBlocks;
import com.forgottenman.registry.ModCreativeTabs;
import com.forgottenman.registry.ModEntities;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModParticles;
import com.forgottenman.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class ForgottenMan implements ModInitializer {
    public static final String MOD_ID = "forgottenman";

    @Override
    public void onInitialize() {
        // Fabric registries are eager, so order matters: blocks before the items and
        // block entities that reference them
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModParticles.register();
        ModSounds.register();
        ModAttachments.register();
        ModCreativeTabs.register();
        ModNetworking.register();
        CommonEvents.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
