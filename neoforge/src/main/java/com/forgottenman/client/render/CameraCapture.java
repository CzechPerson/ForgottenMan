package com.forgottenman.client.render;

import com.forgottenman.ForgottenMan;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * The camera model-view, kept for the late draws.
 *
 * 1.20.1 dispatches AFTER_LEVEL from `GameRenderer.renderLevel`, not from
 * `LevelRenderer`, and hands it a DIFFERENT PoseStack: the one it built the projection
 * from (projection matrix, then view bob and the nausea spin). The camera-rotated
 * model-view is a separate local that only the LevelRenderer-dispatched stages ever see.
 * So `event.getPoseStack()` at AFTER_LEVEL is the projection, and using it as a
 * model-view puts every late draw somewhere off screen — the whole compat path renders
 * nothing at all, while detection and the stencil look perfectly healthy.
 *
 * AFTER_BLOCK_ENTITIES comes from LevelRenderer with the real camera stack and always
 * fires, so the matrix is grabbed there and reused a few milliseconds later in the same
 * frame. Exact by construction: it is the same matrix the world itself was drawn with.
 *
 * 1.21.1 needs none of this — `RenderLevelStageEvent.getModelViewMatrix()` exists there.
 */
@Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class CameraCapture {
    private static final Matrix4f CAMERA = new Matrix4f();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            CAMERA.set(event.getPoseStack().last().pose());
        }
    }

    /** Live instance — copy it before mutating. */
    public static Matrix4f cameraModelView() {
        return CAMERA;
    }

    private CameraCapture() {
    }
}
