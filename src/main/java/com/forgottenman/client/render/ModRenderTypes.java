package com.forgottenman.client.render;

import com.forgottenman.client.shader.ModShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class ModRenderTypes {
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

    private ModRenderTypes() {
    }
}
