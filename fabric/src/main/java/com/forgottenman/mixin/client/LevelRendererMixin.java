package com.forgottenman.mixin.client;

import com.forgottenman.client.render.DoorPortalRenderer;
import com.forgottenman.client.render.InfiniteRoomRenderer;
import com.forgottenman.client.render.LateRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's WorldRenderEvents has no post-sky stage, so the mirror copies get one here.
 * This is the same point Forge fires RenderLevelStageEvent.AFTER_SKY from: straight
 * after renderSky returns, while depth is still freshly cleared, so the copies write
 * depth and the real room's terrain occludes them afterwards.
 *
 * The frustum is rebuilt from the two fields rather than captured out of the local
 * variable table, which keeps this working regardless of how the method compiles.
 *
 * Priority 2000: Iris composites the world from an @Inject at renderLevel's RETURN, at
 * the default priority 1000. Applying after it puts the tail callback below closer to
 * the return instruction, so the late draws run after the composite and land on the
 * final image instead of being painted over. Without Iris the priority changes nothing.
 */
@Mixin(value = LevelRenderer.class, priority = 2000)
public abstract class LevelRendererMixin {
    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    @Nullable
    private Frustum capturedFrustum;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
                    shift = At.Shift.AFTER))
    private void forgottenman$afterSky(PoseStack poseStack, float partialTick, long finishNanoTime,
                                       boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                       LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        Frustum frustum = this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
        // The level pose stack already carries the camera rotation
        InfiniteRoomRenderer.renderAfterSky(partialTick, camera, frustum,
                poseStack.last().pose(), projectionMatrix);
    }

    /**
     * The door portals, at Forge's AFTER_BLOCK_ENTITIES point: the last stage that
     * still targets the stencil-equipped main framebuffer on every graphics mode.
     * Anchored on the "destroyProgress" profiler string, which is the next statement
     * after Forge's dispatch and a far more stable target than an invoke ordinal.
     */
    @Inject(
            method = "renderLevel",
            at = @At(value = "CONSTANT", args = "stringValue=destroyProgress"))
    private void forgottenman$afterBlockEntities(PoseStack poseStack, float partialTick, long finishNanoTime,
                                                 boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                                 LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        Frustum frustum = this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
        DoorPortalRenderer.renderPortalStage(partialTick, camera, frustum,
                poseStack.last().pose(), projectionMatrix);
    }

    /**
     * The late draws, at Forge's AFTER_LEVEL point. Under a shaderpack every effect
     * moves here, past the pack's composite; the dispatcher keeps them in the order
     * event priority keeps them on Forge.
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void forgottenman$afterLevel(PoseStack poseStack, float partialTick, long finishNanoTime,
                                         boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                         LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        Frustum frustum = this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
        LateRenderDispatcher.render(partialTick, camera, frustum, poseStack.last().pose(), projectionMatrix);
    }
}
