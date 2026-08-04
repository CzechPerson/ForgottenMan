package com.forgottenman;

import com.forgottenman.network.ModNetworking;
import com.forgottenman.registry.ModBlockEntities;
import com.forgottenman.registry.ModBlocks;
import com.forgottenman.registry.ModCreativeTabs;
import com.forgottenman.registry.ModEntities;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModParticles;
import com.forgottenman.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ForgottenMan.MOD_ID)
public class ForgottenMan {
    public static final String MOD_ID = "forgottenman";

    public ForgottenMan() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        // Player data rides in the persistent NBT tag, so there is nothing to register
        ModNetworking.register();
        // Per world: the wild portals it governs are a property of the save
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                com.forgottenman.config.ModConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
