package com.forgottenman.mixin.client;

import com.forgottenman.client.ScreenShake;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the reality-shatter camera shake. NeoForge fires ViewportEvent.ComputeCameraAngles
 * for this; Fabric has nothing equivalent, so the offsets go on once the camera has
 * finished setting itself up.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void forgottenman$applyShake(BlockGetter level, Entity entity, boolean detached,
                                         boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (!ScreenShake.active()) {
            return;
        }
        this.setRotation(this.getYRot() + ScreenShake.yawOffset(partialTick),
                this.getXRot() + ScreenShake.pitchOffset(partialTick));
    }
}
