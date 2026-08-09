package com.forgottenman.client.render;

import com.forgottenman.ForgottenMan;
import com.forgottenman.compat.ShaderCompat;
import com.forgottenman.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

/**
 * Puts the void back under a shaderpack.
 *
 * TreeRoomSpecialEffects cancels sky, clouds and weather, but those are vanilla paths a
 * pack never consults -- it draws its own sky from its own programs, and Iris even
 * overrides the cloud setting. Forcing the fog colour black through
 * ViewportEvent.ComputeFogColor was tried and had no effect either.
 *
 * So instead of negotiating, paint over it: a fullscreen black quad at the far plane with
 * depth LEQUAL, which passes only where nothing was drawn. Sky pixels are at the cleared
 * far depth and get blacked out; terrain, the island and our own late draws all wrote
 * nearer depth and are untouched, whatever order the AFTER_LEVEL handlers run in.
 *
 * Clouds write real depth nearer than the far plane, so they survive this and need the
 * cloud setting itself turned off.
 */
public final class VoidSkyRenderer {
    /** Called from LevelRendererMixin at the tail of renderLevel. */
    public static void render() {
        if (!ShaderCompat.useVanillaShaders() || ShaderCompat.isShadowPass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != ModDimensions.TREE_ROOM) {
            return;
        }

        // Identity matrices so the quad is written straight in clip space
        Matrix4f previousProjection = RenderSystem.getProjectionMatrix();
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);

        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        // LEQUAL, not GEQUAL: incoming depth is 1.0, so it passes only where the stored
        // depth is still the cleared 1.0 -- pixels nothing drew. GEQUAL passes against
        // everything and blacks out the whole dimension.
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(-1.0F, -1.0F, 1.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
        builder.addVertex(1.0F, -1.0F, 1.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
        builder.addVertex(1.0F, 1.0F, 1.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
        builder.addVertex(-1.0F, 1.0F, 1.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private VoidSkyRenderer() {
    }
}
