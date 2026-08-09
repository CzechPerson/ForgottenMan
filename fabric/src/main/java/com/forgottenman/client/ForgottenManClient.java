package com.forgottenman.client;

import com.forgottenman.block.MysteriousDoorBlockEntity;
import com.forgottenman.client.particle.FallingLeafParticle;
import com.forgottenman.client.render.DoorPortalRenderer;
import com.forgottenman.client.render.ManRenderer;
import com.forgottenman.client.shader.ModShaders;
import com.forgottenman.network.ModNetworking;
import com.forgottenman.registry.ModBlocks;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModEntities;
import com.forgottenman.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.minecraft.client.renderer.RenderType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ForgottenManClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client settings stay out of ModConfig, which loads at common init and would
        // run on a dedicated server
        com.forgottenman.config.ClientConfig.load();

        // The door portal is not a BlockEntityRenderer (see DoorPortalRenderer),
        // Fancy World Animations cancels those at doors it animates
        EntityRendererRegistry.register(ModEntities.MAN.get(), ManRenderer::new);

        // The grass model carries "render_type": "cutout", which only NeoForge reads.
        // Without this the tufts draw on the solid layer and their transparent 87% is
        // rendered opaque.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PLUM_GRASS.get(), RenderType.cutout());

        DimensionRenderingRegistry.registerDimensionEffects(
                TreeRoomSpecialEffects.ID, new TreeRoomSpecialEffects());
        // Nothing in the sky but the mirror copies: empty renderers stand in for the
        // renderSky/renderClouds/renderSnowAndRain overrides NeoForge allows
        DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.TREE_ROOM, context -> { });
        DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.TREE_ROOM, context -> { });
        DimensionRenderingRegistry.registerWeatherRenderer(ModDimensions.TREE_ROOM, context -> { });

        // Vanilla BlockEntity has no onLoad hook, so door tracking is driven from here
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof MysteriousDoorBlockEntity door) {
                DoorPortalRenderer.track(door);
            }
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof MysteriousDoorBlockEntity door) {
                DoorPortalRenderer.untrack(door);
            }
        });

        ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
        particles.register(ModParticles.SCARLET_LEAF.get(), FallingLeafParticle.Provider::new);
        particles.register(ModParticles.MAGENTA_LEAF.get(), FallingLeafParticle.Provider::new);
        particles.register(ModParticles.DEEP_MAGENTA_LEAF.get(), FallingLeafParticle.Provider::new);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_MAN_DIALOGUE,
                (client, handler, buf, sender) -> client.execute(ClientDialogueHandler::openDialogue));
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.WILD_PORTALS, (client, handler, buf, sender) -> {
            var positions = ModNetworking.readPositions(buf);
            client.execute(() -> WildPortalState.set(positions));
        });

        ModShaders.register();
        ScreenShake.register();
        ClientEvents.register();
    }
}
