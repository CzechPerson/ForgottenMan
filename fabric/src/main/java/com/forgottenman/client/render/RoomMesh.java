package com.forgottenman.client.render;

import com.forgottenman.room.RoomLayout;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Bakes the tree room into a static vertex buffer once, on the render thread.
 * The same mesh is drawn by the mirror copies and the door portals.
 */
public final class RoomMesh {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private static VertexBuffer buffer;
    private static boolean attempted;
    @Nullable
    private static VertexBuffer aoBuffer;
    private static boolean aoAttempted;

    @Nullable
    public static VertexBuffer get() {
        if (!attempted) {
            attempted = true;
            buffer = bake(false);
        }
        return buffer;
    }

    /**
     * The same room with ambient occlusion, for redrawing the real island over a
     * shaderpack's version of it. AO is wrong for the mirror copies -- it baked glitchy
     * near-black patches -- but the island needs the corner darkening the chunk renderer
     * gives it, or the overdraw reads as too flat.
     */
    @Nullable
    public static VertexBuffer getAmbientOccluded() {
        if (!aoAttempted) {
            aoAttempted = true;
            aoBuffer = bake(true);
        }
        return aoBuffer;
    }

    @Nullable
    private static VertexBuffer bake(boolean ambientOcclusion) {
        try {
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            PoseStack pose = new PoseStack();
            RandomSource random = RandomSource.create(42L);
            for (Map.Entry<BlockPos, BlockState> entry : RoomLayout.blocks().entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                if (state.getRenderShape() != RenderShape.MODEL) {
                    continue;
                }
                // Keep the return door out of the mesh, it would fill the portal aperture
                if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
                    continue;
                }
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                if (ambientOcclusion) {
                    dispatcher.getModelRenderer().tesselateBlock(FakeRoomLevel.INSTANCE,
                            dispatcher.getBlockModel(state), state, pos, pose, builder, true, random,
                            state.getSeed(pos), OverlayTexture.NO_OVERLAY);
                } else {
                    // No AO for the copies, it baked glitchy near-black patches; shading
                    // comes from FakeRoomLevel
                    dispatcher.getModelRenderer().tesselateWithoutAO(FakeRoomLevel.INSTANCE,
                            dispatcher.getBlockModel(state), state, pos, pose, builder, true, random,
                            state.getSeed(pos), OverlayTexture.NO_OVERLAY);
                }
                pose.popPose();
            }
            MeshData mesh = builder.build();
            if (mesh == null) {
                LOGGER.warn("Tree room mesh baked empty");
                return null;
            }
            VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vertexBuffer.bind();
            vertexBuffer.upload(mesh);
            VertexBuffer.unbind();
            return vertexBuffer;
        } catch (Exception e) {
            LOGGER.error("Failed to bake tree room mesh", e);
            return null;
        }
    }

    private RoomMesh() {
    }
}
