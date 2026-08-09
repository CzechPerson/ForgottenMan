package com.forgottenman.client.render;

import com.forgottenman.client.shader.RealityBreakEffect;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

/**
 * The late draws, in one place because their order matters and Fabric has no event
 * priorities: the void backdrop first (it fills only pixels nothing wrote), then the
 * island and its copies, then the portals so the island cannot cover them, then the
 * man, then the leaves layered over him the way the particle pass would, and the post
 * chain last, over everything.
 *
 * Called from LevelRendererMixin at the tail of renderLevel -- the same point NeoForge
 * fires AFTER_LEVEL from, where the same order is kept by event priority. The mixin's
 * priority puts this after Iris's own RETURN injection, which is where a pack
 * composites the world, so everything drawn here lands on the final image.
 *
 * Each renderer gates itself: with no shaderpack only the post chain does anything.
 */
public final class LateRenderDispatcher {
    public static void render(DeltaTracker deltaTracker, Camera camera, Frustum frustum,
                              Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        VoidSkyRenderer.render();
        InfiniteRoomRenderer.renderLate(deltaTracker, camera, frustum, modelViewMatrix, projectionMatrix);
        DoorPortalRenderer.renderLate(deltaTracker, camera, frustum, modelViewMatrix, projectionMatrix);
        LateManRenderer.render(camera, modelViewMatrix, deltaTracker.getGameTimeDeltaPartialTick(false));
        LateLeafParticles.render(modelViewMatrix);
        RealityBreakEffect.renderAfterLevel();
    }

    private LateRenderDispatcher() {
    }
}
