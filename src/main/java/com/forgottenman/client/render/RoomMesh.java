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
import net.neoforged.neoforge.client.model.data.ModelData;
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
    public static VertexBuffer get() {
        if (!attempted) {
            attempted = true;
            bake();
        }
        return buffer;
    }

    private static void bake() {
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
                // No AO, it baked glitchy near-black patches; shading comes from FakeRoomLevel
                dispatcher.getModelRenderer().tesselateWithoutAO(FakeRoomLevel.INSTANCE,
                        dispatcher.getBlockModel(state), state, pos, pose, builder, true, random,
                        state.getSeed(pos), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                pose.popPose();
            }
            MeshData mesh = builder.build();
            if (mesh == null) {
                LOGGER.warn("Tree room mesh baked empty");
                return;
            }
            VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vertexBuffer.bind();
            vertexBuffer.upload(mesh);
            VertexBuffer.unbind();
            buffer = vertexBuffer;
        } catch (Exception e) {
            LOGGER.error("Failed to bake tree room mesh", e);
        }
    }

    private RoomMesh() {
    }
}
