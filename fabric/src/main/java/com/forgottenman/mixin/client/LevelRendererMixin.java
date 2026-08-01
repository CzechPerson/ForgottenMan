package com.forgottenman.mixin.client;

import com.forgottenman.client.render.DoorPortalRenderer;
import com.forgottenman.client.render.InfiniteRoomRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
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
 * This is the same point NeoForge fires RenderLevelStageEvent.AFTER_SKY from: straight
 * after renderSky returns, while depth is still freshly cleared, so the copies write
 * depth and the real room's terrain occludes them afterwards.
 *
 * The frustum is rebuilt from the two fields rather than captured out of the local
 * variable table, which keeps this working regardless of how the method compiles.
 */
@Mixin(LevelRenderer.class)
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
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
                    shift = At.Shift.AFTER))
    private void forgottenman$afterSky(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
                                       GameRenderer gameRenderer, LightTexture lightTexture,
                                       Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Frustum frustum = this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
        InfiniteRoomRenderer.renderAfterSky(deltaTracker, camera, frustum, frustumMatrix, projectionMatrix);
    }

    /**
     * The door portals, at NeoForge's AFTER_BLOCK_ENTITIES point: the last stage that
     * still targets the stencil-equipped main framebuffer on every graphics mode.
     * Anchored on the "destroyProgress" profiler string, which is the next statement
     * after NeoForge's dispatch and a far more stable target than an invoke ordinal.
     */
    @Inject(
            method = "renderLevel",
            at = @At(value = "CONSTANT", args = "stringValue=destroyProgress"))
    private void forgottenman$afterBlockEntities(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
                                                 GameRenderer gameRenderer, LightTexture lightTexture,
                                                 Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Frustum frustum = this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
        DoorPortalRenderer.renderPortalStage(deltaTracker, camera, frustum, frustumMatrix, projectionMatrix);
    }
}
