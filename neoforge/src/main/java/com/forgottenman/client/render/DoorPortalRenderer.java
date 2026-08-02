package com.forgottenman.client.render;

import com.forgottenman.ForgottenMan;
import com.forgottenman.block.MysteriousDoorBlockEntity;
import com.forgottenman.client.shader.ModShaders;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.room.RoomLayout;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stencil portals through open mysterious doors
 *
 * Runs at AFTER_BLOCK_ENTITIES
 *
 * Five passes per doorway: stencil mask, black far-depth backdrop, stencil-clipped
 * room mesh, scanline overlay, depth-only seal (keeps water/clouds/particles drawn
 * later out of the doorway)
 * A double door counts as one doorway
 */
@Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class DoorPortalRenderer {
    // Mid-plane offsets of the 3/16-thick closed door panel
    private static final float PLANE_NEAR = 1.5F / 16.0F;
    private static final float PLANE_FAR = 14.5F / 16.0F;
    private static final double MAX_DISTANCE_SQ = 64.0 * 64.0;
    private static final boolean FWA_LOADED = ModList.get().isLoaded("fwa");
    private static final float CLOSE_LINGER = FWA_LOADED ? 8.0F : 2.0F;
    // Yaw a player has walking into the room through the return door
    private static final float ROOM_ENTRY_YAW = RoomLayout.ARRIVAL_YAW;

    private static final Set<MysteriousDoorBlockEntity> DOORS = ConcurrentHashMap.newKeySet();

    // A single door or both halves of an open double door
    private record Doorway(Vec3 anchor, Direction facing, List<MysteriousDoorBlockEntity> doors) {
    }

    public static void track(MysteriousDoorBlockEntity door) {
        DOORS.add(door);
    }

    public static void untrack(MysteriousDoorBlockEntity door) {
        DOORS.remove(door);
    }

    public static void clear() {
        DOORS.clear();
    }

    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || DOORS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        long gameTime = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        boolean inTreeRoom = mc.level.dimension() == ModDimensions.TREE_ROOM;

        List<MysteriousDoorBlockEntity> visible = new ArrayList<>();
        for (MysteriousDoorBlockEntity door : DOORS) {
            if (door.isRemoved() || door.getLevel() != mc.level) {
                DOORS.remove(door);
                continue;
            }
            BlockState state = door.getBlockState();
            if (!(state.getBlock() instanceof DoorBlock)
                    || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }
            BlockPos pos = door.getBlockPos();
            if (cam.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                continue;
            }
            if (state.getValue(DoorBlock.OPEN)) {
                door.setLastOpenTime(gameTime);
            } else if (door.getLastOpenTime() < 0
                    || gameTime - door.getLastOpenTime() + partialTick >= CLOSE_LINGER) {
                continue;
            }
            visible.add(door);
        }
        if (visible.isEmpty()) {
            return;
        }

        if (inTreeRoom) {
            drawStatic(visible, cam);
            return;
        }
        renderPortals(groupIntoDoorways(visible), event, cam);
    }

    // Merge open double doors into one doorway
    private static List<Doorway> groupIntoDoorways(List<MysteriousDoorBlockEntity> doors) {
        Map<BlockPos, MysteriousDoorBlockEntity> byPos = new HashMap<>();
        for (MysteriousDoorBlockEntity door : doors) {
            byPos.put(door.getBlockPos(), door);
        }
        List<Doorway> doorways = new ArrayList<>();
        Set<BlockPos> consumed = new HashSet<>();
        for (MysteriousDoorBlockEntity door : doors) {
            BlockPos pos = door.getBlockPos();
            if (consumed.contains(pos)) {
                continue;
            }
            consumed.add(pos);
            BlockState state = door.getBlockState();
            Direction facing = state.getValue(DoorBlock.FACING);
            if (state.getValue(DoorBlock.OPEN)) {
                DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
                BlockPos partnerPos = pos.relative(hinge == DoorHingeSide.LEFT
                        ? facing.getClockWise() : facing.getCounterClockWise());
                MysteriousDoorBlockEntity partner = byPos.get(partnerPos);
                if (partner != null && !consumed.contains(partnerPos)) {
                    BlockState ps = partner.getBlockState();
                    if (ps.getValue(DoorBlock.FACING) == facing
                            && ps.getValue(DoorBlock.HINGE) != hinge
                            && ps.getValue(DoorBlock.OPEN)) {
                        consumed.add(partnerPos);
                        Vec3 seam = new Vec3((pos.getX() + partnerPos.getX()) / 2.0 + 0.5, pos.getY(),
                                (pos.getZ() + partnerPos.getZ()) / 2.0 + 0.5);
                        doorways.add(new Doorway(seam, facing, List.of(door, partner)));
                        continue;
                    }
                }
            }
            doorways.add(new Doorway(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                    facing, List.of(door)));
        }
        return doorways;
    }

    private static void renderPortals(List<Doorway> doorways, RenderLevelStageEvent event, Vec3 cam) {
        Minecraft mc = Minecraft.getInstance();
        VertexBuffer mesh = RoomMesh.get();
        ShaderInstance roomShader = ModShaders.getRoomCopyShader();
        if (mesh == null || roomShader == null) {
            return;
        }
        if (!mc.getMainRenderTarget().isStencilEnabled()) {
            return; // ClientEvents turns it on between frames; never enable it mid-draw
        }
        Frustum frustum = event.getFrustum();
        Matrix4f proj = new Matrix4f(event.getProjectionMatrix());

        // Same mesh and uniforms for every doorway, set up once per frame
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        roomShader.safeGetUniform("Distortion").set(0.0F);
        roomShader.safeGetUniform("Desync").set(0.0F);
        roomShader.safeGetUniform("FadeStart").set(28.0F);
        roomShader.safeGetUniform("FadeEnd").set(70.0F);
        roomShader.safeGetUniform("FogTint").set(0.0F, 0.0F, 0.0F, 1.0F);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        // Stencil func/mask/op go through GlStateManager, which caches them -- setting
        // them raw leaves the cache lying and lets state leak into other mods' draws
        // One stencil clear per frame; each doorway gets its own ref so stale masks can't match
        GlStateManager._stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        int drawn = 0;
        for (Doorway doorway : doorways) {
            if (!frustum.isVisible(doorwayBounds(doorway))) {
                continue;
            }
            // 8-bit stencil, refs wrap at 255
            if (drawn > 0 && drawn % 255 == 0) {
                GlStateManager._stencilMask(0xFF);
                RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
            }
            int ref = (drawn % 255) + 1;
            drawn++;

            // Quads are single-sided but the portal shows from both sides
            RenderSystem.disableCull();

            // Pass 1: stencil the doorway's visible pixels, no color/depth writes
            GlStateManager._stencilMask(0xFF); // Pass 2 of the previous doorway left this at 0x00
            GlStateManager._stencilFunc(GL11.GL_ALWAYS, ref, 0xFF);
            GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            drawDoorwayQuads(doorway, cam);

            // Pass 2: black backdrop inside the mask, depth pushed to the far plane
            GlStateManager._stencilFunc(GL11.GL_EQUAL, ref, 0xFF);
            GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GlStateManager._stencilMask(0x00);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            GL11.glDepthRange(1.0, 1.0);
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
            drawDoorwayQuads(doorway, cam);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDepthRange(0.0, 1.0);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);

            // Pass 3: the room, anchored so its return door sits in this doorway;
            // both sides look into the room, same fold as the actual teleport
            Vec3 facingVec = new Vec3(doorway.facing().getStepX(), 0.0, doorway.facing().getStepZ());
            boolean frontSide = cam.subtract(doorway.anchor()).dot(facingVec) >= 0.0;
            Direction entering = frontSide ? doorway.facing().getOpposite() : doorway.facing();
            float rad = (float) Math.toRadians(ROOM_ENTRY_YAW - entering.toYRot());
            Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose())
                    .translate((float) (doorway.anchor().x - cam.x),
                            (float) (doorway.anchor().y - cam.y),
                            (float) (doorway.anchor().z - cam.z))
                    .rotateY(rad)
                    .translate(-(RoomLayout.RETURN_DOOR_LOCAL.getX() + 0.5F),
                            -RoomLayout.RETURN_DOOR_LOCAL.getY(),
                            -(RoomLayout.RETURN_DOOR_LOCAL.getZ() + 0.5F));

            RenderSystem.enableCull();
            mesh.bind();
            mesh.drawWithShader(modelView, proj, roomShader);
            VertexBuffer.unbind();

            // Pass 4: scanline shimmer
            ShaderInstance overlay = ModShaders.getPortalOverlayShader();
            if (overlay != null) {
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
                BufferBuilder builder = beginQuads();
                for (MysteriousDoorBlockEntity door : doorway.doors()) {
                    addQuad(builder, door, cam);
                }
                BufferBuilder.RenderedBuffer quadMesh = builder.endOrDiscardIfEmpty();
                if (quadMesh != null) {
                    RenderSystem.setShader(ModShaders::getPortalOverlayShader);
                    BufferUploader.drawWithShader(quadMesh);
                }
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
            }

            // Pass 5: seal, write the door plane's depth across the mask so nothing
            // drawn later ends up inside the portal
            RenderSystem.disableCull();
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(true);
            drawDoorwayQuads(doorway, cam);
            RenderSystem.colorMask(true, true, true, true);
        }
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GlStateManager._stencilMask(0xFF);
        RenderSystem.enableCull(); // Vanilla default
    }

    private static AABB doorwayBounds(Doorway doorway) {
        AABB box = null;
        for (MysteriousDoorBlockEntity door : doorway.doors()) {
            BlockPos p = door.getBlockPos();
            AABB b = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 2.0, p.getZ() + 1.0);
            box = box == null ? b : box.minmax(b);
        }
        return box.inflate(0.5);
    }

    // Doorway quads in the closed-panel plane
    private static void drawDoorwayQuads(Doorway doorway, Vec3 cam) {
        BufferBuilder builder = beginQuads();
        for (MysteriousDoorBlockEntity door : doorway.doors()) {
            addQuad(builder, door, cam);
        }
        BufferBuilder.RenderedBuffer mesh = builder.endOrDiscardIfEmpty();
        if (mesh != null) {
            RenderSystem.setShader(GameRenderer::getPositionShader);
            BufferUploader.drawWithShader(mesh);
        }
    }

    // Static for tree room doors, there's no destination mesh to show
    private static void drawStatic(List<MysteriousDoorBlockEntity> doors, Vec3 cam) {
        BufferBuilder builder = beginQuads();
        for (MysteriousDoorBlockEntity door : doors) {
            addQuad(builder, door, cam);
        }
        ModRenderTypes.PORTAL_STATIC.end(builder, VertexSorting.DISTANCE_TO_ORIGIN);
    }

    private static void addQuad(BufferBuilder builder, MysteriousDoorBlockEntity door, Vec3 cam) {
        BlockPos pos = door.getBlockPos();
        // Camera-relative; the model-view at this stage already has the camera rotation
        float x = (float) (pos.getX() - cam.x);
        float y = (float) (pos.getY() - cam.y);
        float z = (float) (pos.getZ() - cam.z);
        Direction facing = door.getBlockState().getValue(DoorBlock.FACING);
        switch (facing) {
            case NORTH -> quadZ(builder, x, y, z + PLANE_FAR);
            case SOUTH -> quadZ(builder, x, y, z + PLANE_NEAR);
            case WEST -> quadX(builder, x + PLANE_FAR, y, z);
            case EAST -> quadX(builder, x + PLANE_NEAR, y, z);
            default -> quadZ(builder, x, y, z + PLANE_NEAR);
        }
    }

    private static void quadZ(BufferBuilder builder, float x, float y, float z) {
        builder.vertex(x, y, z).endVertex();
        builder.vertex(x + 1.0F, y, z).endVertex();
        builder.vertex(x + 1.0F, y + 2.0F, z).endVertex();
        builder.vertex(x, y + 2.0F, z).endVertex();
    }

    private static void quadX(BufferBuilder builder, float x, float y, float z) {
        builder.vertex(x, y, z).endVertex();
        builder.vertex(x, y, z + 1.0F).endVertex();
        builder.vertex(x, y + 2.0F, z + 1.0F).endVertex();
        builder.vertex(x, y + 2.0F, z).endVertex();
    }

    // 1.20.1 has a single shared builder rather than Tesselator.begin per draw
    private static BufferBuilder beginQuads() {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        return builder;
    }

    private DoorPortalRenderer() {
    }
}
