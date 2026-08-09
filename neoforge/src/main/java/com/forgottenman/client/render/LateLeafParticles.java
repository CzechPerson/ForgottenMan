package com.forgottenman.client.render;

import com.forgottenman.ForgottenMan;
import com.forgottenman.compat.ShaderCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Falling leaves, held back from the particle pass and redrawn after a shaderpack has
 * composited the world.
 *
 * Drawn normally they are lit from real block light, which the tree room has none of, so
 * a pack renders them nearly black -- vanilla only looks right because forceBrightLightmap
 * brightens the lightmap, which a pack never reads. Feeding them a fake full light value
 * would make them *lit*, not glowing, and LabPBR emissive maps are ignored outright by
 * integrated-PBR modes.
 *
 * So the quads are captured during the particle pass and replayed here through an unlit
 * render type. Nothing samples a lightmap, so they emit regardless of pack or settings.
 * Without a pack this never collects anything and the particle engine draws them as usual.
 */
@Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class LateLeafParticles {
    // x, y, z, u, v per vertex, four vertices per quad
    private static final List<float[]> QUADS = new ArrayList<>();
    private static final List<float[]> TINTS = new ArrayList<>();

    /** Called from FallingLeafParticle instead of emitting into the particle buffer. */
    public static void submit(float[] quad, float[] rgba) {
        QUADS.add(quad);
        TINTS.add(rgba);
    }

    public static boolean active() {
        return ShaderCompat.useVanillaShaders();
    }

    // Just after the man, so leaves layer over him the way the particle pass would
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        if (QUADS.isEmpty() || ShaderCompat.isShadowPass()) {
            QUADS.clear();
            TINTS.clear();
            return;
        }

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(event.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int q = 0; q < QUADS.size(); q++) {
            float[] v = QUADS.get(q);
            float[] c = TINTS.get(q);
            for (int i = 0; i < 4; i++) {
                int o = i * 5;
                builder.vertex(v[o], v[o + 1], v[o + 2])
                        .uv(v[o + 3], v[o + 4])
                        .color(c[0], c[1], c[2], c[3])
                        .endVertex();
            }
        }
        ModRenderTypes.LEAF_GLOW.end(builder, VertexSorting.DISTANCE_TO_ORIGIN);

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        QUADS.clear();
        TINTS.clear();
    }

    private LateLeafParticles() {
    }
}
