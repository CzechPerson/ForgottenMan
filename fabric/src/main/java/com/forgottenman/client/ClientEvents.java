package com.forgottenman.client;

import com.forgottenman.client.render.DoorPortalRenderer;
import com.forgottenman.client.render.StencilTarget;
import com.forgottenman.client.shader.RealityBreakEffect;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public final class ClientEvents {
    private static TreeRoomMusic music;
    private static boolean wasInRoom;
    private static int musicRetryTimer;
    private static boolean stencilReady;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RealityState.setMirrorLevel(0); // Fresh world, fresh reality
            DoorPortalRenderer.clear();
            WildPortalState.clear();
            music = null;
        });

        // The mirror copies and the door portals hook LevelRenderer directly (see
        // LevelRendererMixin) because Fabric has no post-sky or post-block-entity
        // stage. Only the finishing post pass fits a stock Fabric stage.
        WorldRenderEvents.LAST.register(context -> RealityBreakEffect.renderAfterLevel());
    }

    private static void onClientTick(Minecraft mc) {
        // Enabling stencil destroys and recreates the main framebuffer, so it has to
        // happen between frames. Client ticks run before bindWrite; doing it from the
        // portal renderer instead deleted the framebuffer mid-draw and broke every
        // later pass (notably the held item) until the game restarted.
        if (!stencilReady) {
            stencilReady = true;
            ((StencilTarget) mc.getMainRenderTarget()).forgottenman$enableStencil();
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

    private ClientEvents() {
    }
}
