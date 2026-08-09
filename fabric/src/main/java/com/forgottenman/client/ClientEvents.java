package com.forgottenman.client;

import com.forgottenman.client.render.DoorPortalRenderer;
import com.forgottenman.client.render.StencilTarget;
import com.forgottenman.compat.ShaderCompat;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
            ShaderCompat.invalidate();
            music = null;
        });

        // Every render hook lives in LevelRendererMixin: Fabric has no post-sky or
        // post-block-entity stage, and the late draws (LateRenderDispatcher) must run
        // after Iris's RETURN injection, which no Fabric API event guarantees.
    }

    private static void onClientTick(Minecraft mc) {
        ShaderCompat.tick();
        // Enabling stencil destroys and recreates the main framebuffer, so it has to
        // happen between frames. Client ticks run before bindWrite; doing it from the
        // portal renderer instead deleted the framebuffer mid-draw and broke every
        // later pass (notably the held item) until the game restarted.
        if (!stencilReady) {
            stencilReady = true;
            ((StencilTarget) mc.getMainRenderTarget()).forgottenman$enableStencil();
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

    private ClientEvents() {
    }
}
