package com.forgottenman.client.render;

/**
 * Implemented on vanilla's RenderTarget by RenderTargetMixin. NeoForge patches
 * enableStencil() straight into the class; Fabric has no equivalent, so the portals
 * get their stencil bits by casting the main render target to this.
 */
public interface StencilTarget {
    /**
     * Rebuilds the attachments with a stencil-capable depth texture. This destroys and
     * recreates the framebuffer, so it must be called between frames -- doing it mid-draw
     * deletes the framebuffer the game is rendering into.
     */
    void forgottenman$enableStencil();

    boolean forgottenman$isStencilEnabled();
}
