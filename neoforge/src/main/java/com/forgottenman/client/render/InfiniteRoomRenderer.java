package com.forgottenman.client.render;

import com.forgottenman.ForgottenMan;
import com.forgottenman.client.shader.ModShaders;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.room.RoomLayout;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Mirror hall: inside the tree room the baked room mesh is redrawn as a grid of
 * copies floating in the void. Adjacent copies are mirror images, each on its own
 * desynced clock, darkening with distance.
 *
 * Drawn at AFTER_SKY where depth is freshly cleared: copies write depth and the real
 * room occludes them later. Copies are gathered and culled first, then drawn front
 * to back.
 */
@EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class InfiniteRoomRenderer {
    private static final int RANGE_XZ = 4;
    private static final int RANGE_Y = 1;
    private static final float SPACING_XZ = 28.0F; // Must exceed the room footprint (21) so copies never overlap the real room
    private static final float SPACING_Y = 26.0F;
    private static final float FADE_START = 24.0F;
    private static final float FADE_END = 130.0F;
    private static final float CENTER_X = RoomLayout.SIZE_X / 2.0F;
    private static final float CENTER_Y = RoomLayout.SIZE_Y / 2.0F;
    private static final float CENTER_Z = RoomLayout.SIZE_Z / 2.0F;
    // Frustum padding: bob (0.7) + drift (0.3) + max shader wobble (~0.27)
    private static final double CULL_PAD = 2.0;

    private record CopyDraw(int i, int j, int k, float ease, double ox, double oy, double oz, double distSq) {
    }

    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int mirrorLevel = RealityState.getMirrorLevel();
        if (mc.level == null || mc.level.dimension() != ModDimensions.TREE_ROOM) {
            ringProgress = 0.0F;
            hallProgress = 0.0F;
            return;
        }
        // Copies glide out from the real room to their slots instead of popping in
        float dt = event.getPartialTick().getGameTimeDeltaTicks();
        ringProgress = approach(ringProgress, mirrorLevel >= 1 ? 1.0F : 0.0F, dt * 0.045F);
        hallProgress = approach(hallProgress, mirrorLevel >= 2 ? 1.0F : 0.0F, dt * 0.035F);
        if (ringProgress <= 0.001F && hallProgress <= 0.001F) {
            return;
        }
        VertexBuffer mesh = RoomMesh.get();
        ShaderInstance shader = ModShaders.getRoomCopyShader();
        if (mesh == null || shader == null) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        Frustum frustum = event.getFrustum();
        // Shared drift clock, wrapped to keep float precision
        float time = (float) (mc.level.getGameTime() % 240000L)
                + event.getPartialTick().getGameTimeDeltaPartialTick(false);

        List<CopyDraw> draws = new ArrayList<>();
        for (int i = -RANGE_XZ; i <= RANGE_XZ; i++) {
            for (int k = -RANGE_XZ; k <= RANGE_XZ; k++) {
                for (int j = -RANGE_Y; j <= RANGE_Y; j++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue; // The real room lives here
                    }
                    // First ring and the rest of the hall animate as separate waves
                    boolean firstRing = j == 0 && Math.abs(i) <= 1 && Math.abs(k) <= 1;
                    float progress = firstRing ? ringProgress : hallProgress;
                    if (progress <= 0.001F) {
                        continue;
                    }
                    float ease = progress * progress * (3.0F - 2.0F * progress);

                    // Ease keeps copies from drifting while still merged into the room
                    float phase = hash(i, j, k) * 6.2832F;
                    float speed = 0.008F + 0.007F * hash(k, i, j);
                    float bob = (float) Math.sin(time * speed + phase) * 0.7F * ease;
                    float driftX = (float) Math.sin(time * speed * 0.61F + phase * 1.7F) * 0.3F * ease;
                    float driftZ = (float) Math.cos(time * speed * 0.53F + phase * 2.3F) * 0.3F * ease;

                    double ox = RoomLayout.ORIGIN.getX() + i * SPACING_XZ * ease + driftX;
                    double oy = RoomLayout.ORIGIN.getY() + j * SPACING_Y * ease + bob;
                    double oz = RoomLayout.ORIGIN.getZ() + k * SPACING_XZ * ease + driftZ;

                    double cx = ox + CENTER_X - cam.x;
                    double cy = oy + CENTER_Y - cam.y;
                    double cz = oz + CENTER_Z - cam.z;
                    double distSq = cx * cx + cy * cy + cz * cz;
                    if (distSq > (FADE_END + 16.0) * (FADE_END + 16.0)) {
                        continue; // Fully faded to black anyway
                    }
                    // Reflection is about the room center, so the box is the same either way
                    if (!frustum.isVisible(new AABB(ox - CULL_PAD, oy - CULL_PAD, oz - CULL_PAD,
                            ox + RoomLayout.SIZE_X + CULL_PAD, oy + RoomLayout.SIZE_Y + CULL_PAD,
                            oz + RoomLayout.SIZE_Z + CULL_PAD))) {
                        continue;
                    }
                    draws.add(new CopyDraw(i, j, k, ease, ox, oy, oz, distSq));
                }
            }
        }
        if (draws.isEmpty()) {
            return;
        }
        // Front to back, far copies fail depth early
        draws.sort(Comparator.comparingDouble(CopyDraw::distSq));

        Matrix4f baseRotation = event.getModelViewMatrix();
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());

        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        // Mirrored copies flip winding, the cull face is picked per copy below
        RenderSystem.enableCull();
        // Depth state at AFTER_SKY isn't guaranteed (depends on other installed
        // mods); without the depth test the copies render inside-out
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL, the vanilla default
        RenderSystem.depthMask(true);
        shader.safeGetUniform("FadeStart").set(FADE_START);
        shader.safeGetUniform("FadeEnd").set(FADE_END);
        shader.safeGetUniform("FogTint").set(0.0F, 0.0F, 0.0F, 1.0F);
        AbstractUniform desync = shader.safeGetUniform("Desync");
        AbstractUniform distortion = shader.safeGetUniform("Distortion");

        Matrix4f modelView = new Matrix4f();
        mesh.bind();
        for (CopyDraw draw : draws) {
            int i = draw.i();
            int j = draw.j();
            int k = draw.k();
            float ease = draw.ease();
            // Odd number of mirrored axes = flipped winding
            boolean flipped = ((i ^ j ^ k) & 1) != 0;
            GL11.glCullFace(flipped ? GL11.GL_FRONT : GL11.GL_BACK);

            modelView.set(baseRotation)
                    .translate((float) (draw.ox() - cam.x), (float) (draw.oy() - cam.y), (float) (draw.oz() - cam.z))
                    // Mirror about the room center, alternating per axis
                    .translate(CENTER_X, CENTER_Y, CENTER_Z)
                    .scale((i & 1) == 0 ? 1.0F : -1.0F, (j & 1) == 0 ? 1.0F : -1.0F, (k & 1) == 0 ? 1.0F : -1.0F)
                    .translate(-CENTER_X, -CENTER_Y, -CENTER_Z);

            float dist = (float) Math.sqrt(draw.distSq());
            desync.set(hash(i, j, k) * 40.0F);
            // Copies wobble hard while tearing away / merging back
            distortion.set(0.35F + 1.2F * dist / FADE_END + (1.0F - ease) * 1.6F);
            // Darken only, no hue shift; 0.9 base matches the real island's lightmap
            float ring = Math.max(Math.abs(i), Math.max(Math.abs(j), Math.abs(k)));
            float brightness = 0.9F * (float) Math.pow(0.82, ring) * (0.25F + 0.75F * ease);
            RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);

            mesh.drawWithShader(modelView, projection, shader);
        }
        VertexBuffer.unbind();

        GL11.glCullFace(GL11.GL_BACK); // Vanilla never touches the cull face, put it back
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float ringProgress;
    private static float hallProgress;

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }

    private static float hash(int i, int j, int k) {
        int h = i * 374761393 + j * 668265263 + k * 2147483647;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65536.0F;
    }

    private InfiniteRoomRenderer() {
    }
}
