package com.forgottenman.client.render;

import com.forgottenman.ForgottenMan;
import com.forgottenman.entity.ManEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The man is an upright billboard quad, fully procedural: the censor shader builds a
 * humanoid silhouette out of SDFs, warps it, tears slices off and fills it with
 * static. Depth testing against the trunk hides him from the entrance.
 */
public class ManRenderer extends EntityRenderer<ManEntity> {
    private static final ResourceLocation TEXTURE = ForgottenMan.id("textures/block/void_bark.png");
    private static final float HALF_WIDTH = 0.65F;
    private static final float HEIGHT = 2.3F;
    // Read by the CENSOR render type's setup shard at flush time
    public static float vanishProgress;

    public ManRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ManEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        int vanishTicks = entity.getVanishTicks();
        vanishProgress = vanishTicks < 0 ? 0.0F
                : Math.min(1.0F, (vanishTicks + partialTick) / (float) ManEntity.VANISH_DURATION);
        poseStack.pushPose();
        // Upright billboard: rotate about Y only
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        float yaw = (float) Mth.atan2(cam.x - entity.getX(), cam.z - entity.getZ());
        poseStack.mulPose(Axis.YP.rotation(yaw));

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CENSOR);
        consumer.vertex(pose, -HALF_WIDTH, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        consumer.vertex(pose, HALF_WIDTH, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        consumer.vertex(pose, HALF_WIDTH, HEIGHT, 0.0F).uv(1.0F, 0.0F).endVertex();
        consumer.vertex(pose, -HALF_WIDTH, HEIGHT, 0.0F).uv(0.0F, 0.0F).endVertex();
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ManEntity entity) {
        return TEXTURE; // Never sampled, the censor shader is fully procedural
    }
}
