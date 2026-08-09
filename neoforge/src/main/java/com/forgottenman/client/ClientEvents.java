package com.forgottenman.client;

import com.forgottenman.ForgottenMan;
import com.forgottenman.client.gui.ManDialogueScreen;
import com.forgottenman.client.particle.FallingLeafParticle;
import com.forgottenman.client.render.DoorPortalRenderer;
import com.forgottenman.client.render.ManRenderer;
import com.forgottenman.compat.ShaderCompat;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModEntities;
import com.forgottenman.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RealityState.setMirrorLevel(0); // Fresh world, fresh reality
        DoorPortalRenderer.clear();
        WildPortalState.clear();
        ShaderCompat.invalidate();
        music = null;
    }

    private static TreeRoomMusic music;
    private static boolean wasInRoom;
    private static int musicRetryTimer;
    private static boolean stencilReady;

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ShaderCompat.tick();
        // Enabling stencil destroys and recreates the main framebuffer, so it has to
        // happen between frames. Client ticks run before bindWrite; doing it from the
        // portal renderer instead deleted the framebuffer mid-draw and broke every
        // later pass (notably the held item) until the game restarted.
        if (!stencilReady) {
            stencilReady = true;
            mc.getMainRenderTarget().enableStencil();
        }
        // Keeps the verdict fresh and logs it when it flips, so a bug report says which
        // path was running. Cached, so this is about one check a second. No GL here --
        // the framebuffer probe only runs inside the render stage.
        if (mc.level != null) {
            ShaderCompat.effectsUseCompat();
        }
        boolean inRoom = mc.level != null && mc.level.dimension() == ModDimensions.TREE_ROOM;
        if (inRoom) {
            mc.getMusicManager().stopPlaying(); // Vanilla music stays out of the void
            boolean shouldStart = !wasInRoom;
            // If the sound engine dropped the loop somehow, retry every ~5s
            if (!shouldStart && ++musicRetryTimer >= 100) {
                musicRetryTimer = 0;
                shouldStart = music == null || !mc.getSoundManager().isActive(music);
            }
            if (shouldStart) {
                music = new TreeRoomMusic();
                mc.getSoundManager().play(music);
            }
        } else {
            music = null;
            musicRetryTimer = 0;
        }
        wasInRoom = inRoom;
    }

    // The held item is lit from real block light, which the tree room has none of, so a
    // pack renders it black; without a pack forceBrightLightmap already brightens it and
    // this is a no-op. RenderHandEvent has no light setter, so cancel and reissue the same
    // render fullbright -- safe from recursion, Forge fires this before calling
    // renderArmWithItem, which an access transformer opens up.
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null
                || mc.level.dimension() != ModDimensions.TREE_ROOM
                || !ShaderCompat.useVanillaShaders()) {
            return;
        }
        event.setCanceled(true);
        mc.gameRenderer.itemInHandRenderer.renderArmWithItem(mc.player, event.getPartialTick(),
                event.getInterpolatedPitch(), event.getHand(), event.getSwingProgress(),
                event.getItemStack(), event.getEquipProgress(), event.getPoseStack(),
                event.getMultiBufferSource(), LightTexture.FULL_BRIGHT);
    }

    @SubscribeEvent
    static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // The dialogue hides the hotbar cluster. 1.20.1 draws the XP level inside the
        // experience bar overlay, so those two cover all three layers.
        if (Minecraft.getInstance().screen instanceof ManDialogueScreen
                && (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()
                || event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type())) {
            event.setCanceled(true);
        }
    }

    /** Registration rides the mod bus, which 1.20.1 keeps separate from the game bus */
    @Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(TreeRoomSpecialEffects.ID, new TreeRoomSpecialEffects());
        }

        @SubscribeEvent
        static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // The door portal is not a BlockEntityRenderer (see DoorPortalRenderer),
            // Fancy World Animations cancels those at doors it animates
            event.registerEntityRenderer(ModEntities.MAN.get(), ManRenderer::new);
        }

        @SubscribeEvent
        static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.SCARLET_LEAF.get(), FallingLeafParticle.Provider::new);
            event.registerSpriteSet(ModParticles.MAGENTA_LEAF.get(), FallingLeafParticle.Provider::new);
            event.registerSpriteSet(ModParticles.DEEP_MAGENTA_LEAF.get(), FallingLeafParticle.Provider::new);
        }

        private Registration() {
        }
    }

    private ClientEvents() {
    }
}
