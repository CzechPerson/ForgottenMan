package com.forgottenman.mixin.client;

import com.forgottenman.client.render.StencilTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

/**
 * Reimplements NeoForge's RenderTarget#enableStencil patch, which Fabric has no
 * equivalent for. Vanilla allocates the depth attachment as plain DEPTH_COMPONENT32F,
 * so there are no stencil bits and the door portals' masking silently does nothing.
 *
 * Both redirects mirror exactly what the NeoForge patch changes: the depth texture's
 * format, and the attachment point it is bound to. Everything else in createBuffers
 * (status check, clear, unbind) runs untouched, which is why this redirects the two
 * calls in place rather than rebuilding the attachment afterwards.
 *
 * MainTarget builds its first framebuffer in its own constructor path, which this does
 * not touch -- stencil only appears once enableStencil triggers a resize.
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements StencilTarget {
    @Shadow
    public int viewWidth;

    @Shadow
    public int viewHeight;

    @Shadow
    public abstract void resize(int width, int height, boolean onOsx);

    @Unique
    private boolean forgottenman$stencilEnabled;

    @Override
    public boolean forgottenman$isStencilEnabled() {
        return this.forgottenman$stencilEnabled;
    }

    @Override
    public void forgottenman$enableStencil() {
        if (this.forgottenman$stencilEnabled) {
            return;
        }
        this.forgottenman$stencilEnabled = true;
        this.resize(this.viewWidth, this.viewHeight, Minecraft.ON_OSX);
    }

    // The depth texture, ordinal 0 of the two _texImage2D calls (colour is the other).
    // DEPTH_COMPONENT32F carries no stencil bits, so swap it for DEPTH32F_STENCIL8.
    @Redirect(
            method = "createBuffers",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V",
                    ordinal = 0))
    private void forgottenman$stencilDepthFormat(int target, int level, int internalFormat, int width, int height,
                                                 int border, int format, int type, @Nullable IntBuffer pixels) {
        if (this.forgottenman$stencilEnabled) {
            GlStateManager._texImage2D(target, level, GL30.GL_DEPTH32F_STENCIL8, width, height, border,
                    GL30.GL_DEPTH_STENCIL, GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV, pixels);
        } else {
            GlStateManager._texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
        }
    }

    // The depth attachment, ordinal 1 of the two _glFramebufferTexture2D calls
    // (colour is ordinal 0). A combined depth-stencil texture has to be bound to
    // DEPTH_STENCIL_ATTACHMENT, not DEPTH_ATTACHMENT.
    @Redirect(
            method = "createBuffers",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glFramebufferTexture2D(IIIII)V",
                    ordinal = 1))
    private void forgottenman$stencilAttachment(int target, int attachment, int textureTarget, int texture, int level) {
        GlStateManager._glFramebufferTexture2D(target,
                this.forgottenman$stencilEnabled ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : attachment,
                textureTarget, texture, level);
    }
}
