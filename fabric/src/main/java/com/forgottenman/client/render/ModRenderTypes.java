package com.forgottenman.client.render;

import com.forgottenman.client.shader.ModShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class ModRenderTypes extends RenderType {
    // 1.20.1 keeps RenderStateShard's NO_CULL protected and exposes it nowhere else.
    // Extending RenderType (itself a RenderStateShard) is what makes it reachable;
    // this class is never instantiated.
    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    /** The man: procedural quad, opaque with shader-side discard so depth testing hides him behind the trunk */
    public static final RenderType CENSOR = RenderType.create(
            "forgottenman_censor",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(ModShaders::getCensorShader))
                    .setCullState(RenderStateShard.NO_CULL)
                    // Batched draws flush later, so the vanish progress is pushed at setup time
                    .setTexturingState(new RenderStateShard.TexturingStateShard("forgottenman_vanish",
                            () -> {
                                var shader = ModShaders.getCensorShader();
                                if (shader != null) {
                                    shader.safeGetUniform("Vanish").set(ManRenderer.vanishProgress);
                                }
                            },
                            () -> {}))
                    .createCompositeState(false));

    /** Dark red static for doorways without a live feed */
    public static final RenderType PORTAL_STATIC = RenderType.create(
            "forgottenman_portal_static",
            DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(ModShaders::getPortalStaticShader))
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    // Used only when the framebuffer genuinely has no stencil (Fabulous layer targets and
    // the like). Vanilla POSITION_COLOR, coloured per cell on the CPU by
    // CompatPortalRenderer. Under a shaderpack the real portal runs instead, past the
    // pack's composite, with the mod's own shaders.

    /** Portal aperture. Depth-writing, so it is also the backdrop and the seal. */
    public static final RenderType PORTAL_SURFACE_COMPAT = RenderType.create(
            "forgottenman_portal_surface_compat",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /** Scanline shimmer over the aperture. No depth write. */
    public static final RenderType PORTAL_SHIMMER_COMPAT = RenderType.create(
            "forgottenman_portal_shimmer_compat",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1024,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setCullState(RenderStateShard.NO_CULL)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * Falling leaves, drawn past a shaderpack's composite so they glow.
     *
     * Unlit by construction: POSITION_TEX_COLOR samples the particle atlas and writes it
     * straight out, with no lightmap and no gbuffer for a pack to relight from. That is
     * real emission rather than a faked light value, and it needs nothing switched on in
     * the pack -- LabPBR specular maps are ignored entirely by integrated-PBR modes.
     */
    public static final RenderType LEAF_GLOW = RenderType.create(
            "forgottenman_leaf_glow",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));
}
