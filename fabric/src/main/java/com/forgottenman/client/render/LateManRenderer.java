package com.forgottenman.client.render;

import com.forgottenman.compat.ShaderCompat;
import com.forgottenman.entity.ManEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The man, drawn after a shaderpack has composited the world.
 *
 * Inside the gbuffer stage a pack owns everything: our censor program is not one of its
 * gbuffers programs, so the draw is dropped or relit from data it never wrote. Past the
 * composite a draw with our own ShaderInstance lands straight on the final image and
 * looks exactly like it does without a pack. The depth buffer is still bound, so the
 * 2x2 trunk keeps occluding him.
 *
 * Without a pack this is inert -- ManRenderer draws him the ordinary batched way.
 */
public final class LateManRenderer {
    private static final double RANGE = 64.0;

    /** Called from LevelRendererMixin at the tail of renderLevel. */
    public static void render(Camera camera, Matrix4f modelViewMatrix, float partialTick) {
        if (!ShaderCompat.useVanillaShaders() || ShaderCompat.isShadowPass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = camera.getPosition();
        AABB near = new AABB(cam.subtract(RANGE, RANGE, RANGE), cam.add(RANGE, RANGE, RANGE));
        var men = mc.level.getEntitiesOfClass(ManEntity.class, near);
        if (men.isEmpty()) {
            return;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        // RenderType.end draws through RenderSystem's model-view, which 1.20.1 pops back
        // to identity after the chunk layers -- without this the quad is built in
        // camera-relative coords but drawn in screen space.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();

        for (ManEntity man : men) {
            int vanishTicks = man.getVanishTicks();
            ManRenderer.vanishProgress = vanishTicks < 0 ? 0.0F
                    : Math.min(1.0F, (vanishTicks + partialTick) / (float) ManEntity.VANISH_DURATION);

            double ex = Mth.lerp(partialTick, man.xo, man.getX());
            double ey = Mth.lerp(partialTick, man.yo, man.getY());
            double ez = Mth.lerp(partialTick, man.zo, man.getZ());
            // Same upright billboard as ManRenderer, built camera-relative here because
            // there is no entity PoseStack at this stage
            float yaw = (float) Mth.atan2(cam.x - ex, cam.z - ez);
            float sin = Mth.sin(yaw);
            float cos = Mth.cos(yaw);
            float ox = (float) (ex - cam.x);
            float oy = (float) (ey - cam.y);
            float oz = (float) (ez - cam.z);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            addCorner(builder, ox, oy, oz, sin, cos, -ManRenderer.HALF_WIDTH, 0.0F, 0.0F, 1.0F);
            addCorner(builder, ox, oy, oz, sin, cos, ManRenderer.HALF_WIDTH, 0.0F, 1.0F, 1.0F);
            addCorner(builder, ox, oy, oz, sin, cos, ManRenderer.HALF_WIDTH, ManRenderer.HEIGHT, 1.0F, 0.0F);
            addCorner(builder, ox, oy, oz, sin, cos, -ManRenderer.HALF_WIDTH, ManRenderer.HEIGHT, 0.0F, 0.0F);
            ModRenderTypes.CENSOR.end(builder, VertexSorting.DISTANCE_TO_ORIGIN);
        }

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // Rotation about Y only, applied on the CPU: the quad faces the camera horizontally
    private static void addCorner(BufferBuilder builder, float ox, float oy, float oz,
                                  float sin, float cos, float localX, float localY,
                                  float u, float v) {
        float x = ox + localX * cos;
        float z = oz - localX * sin;
        builder.vertex(x, oy + localY, z).uv(u, v).endVertex();
    }

    private LateManRenderer() {
    }
}
