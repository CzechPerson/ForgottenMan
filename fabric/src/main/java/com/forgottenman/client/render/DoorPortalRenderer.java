package com.forgottenman.client.render;

import com.forgottenman.block.MysteriousDoorBlockEntity;
import com.forgottenman.client.WildPortalState;
import com.forgottenman.client.shader.ModShaders;
import com.forgottenman.compat.ShaderCompat;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.room.RoomLayout;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
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
public final class DoorPortalRenderer {
    // Mid-plane offsets of the 3/16-thick closed door panel
    static final float PLANE_NEAR = 1.5F / 16.0F;
    static final float PLANE_FAR = 14.5F / 16.0F;
    private static final double MAX_DISTANCE_SQ = 64.0 * 64.0;
    private static final boolean FWA_LOADED = FabricLoader.getInstance().isModLoaded("fwa");
    private static final float CLOSE_LINGER = FWA_LOADED ? 8.0F : 2.0F;
    // Yaw a player has walking into the room through the return door
    private static final float ROOM_ENTRY_YAW = RoomLayout.ARRIVAL_YAW;

    private static final Set<MysteriousDoorBlockEntity> DOORS = ConcurrentHashMap.newKeySet();

    // A single door or both halves of an open double door
    record Doorway(Vec3 anchor, Direction facing, List<BlockPos> doors) {
    }

    // When each doorway was last seen open, so a closing door can linger. Crafted
    // doors used to carry this themselves; wild ones have no block entity to put
    // it on, so both kinds share this instead.
    private static final Map<BlockPos, Long> LAST_OPEN = new HashMap<>();

    public static void track(MysteriousDoorBlockEntity door) {
        DOORS.add(door);
    }

    public static void untrack(MysteriousDoorBlockEntity door) {
        DOORS.remove(door);
    }

    public static void clear() {
        DOORS.clear();
        LAST_OPEN.clear();
    }

    /** Called by LevelRendererMixin, at the same point NeoForge fires AFTER_BLOCK_ENTITIES */
    public static void renderPortalStage(DeltaTracker deltaTracker, Camera camera, Frustum frustum,
                                         Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        // Under a shaderpack the whole portal moves to the tail of renderLevel, past the
        // pack's composite. Drawn in the gbuffer stage instead, the pack drops our core
        // shaders and composites its own sky and clouds straight over the doorway. After
        // the composite our shaders land on the final image untouched and nothing
        // overwrites them, so the portal looks exactly as it does without a pack.
        if (ShaderCompat.useVanillaShaders()) {
            return;
        }
        renderStage(deltaTracker, camera, frustum, modelViewMatrix, projectionMatrix, false);
    }

    /** Called by LateRenderDispatcher at the tail of renderLevel, only under a shaderpack */
    public static void renderLate(DeltaTracker deltaTracker, Camera camera, Frustum frustum,
                                  Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        if (!ShaderCompat.useVanillaShaders()) {
            return;
        }
        renderStage(deltaTracker, camera, frustum, modelViewMatrix, projectionMatrix, true);
    }

    private static void renderStage(DeltaTracker deltaTracker, Camera camera, Frustum frustum,
                                    Matrix4f modelViewMatrix, Matrix4f projectionMatrix, boolean late) {
        // Cheap and cached, but it has to happen inside the render stage to see the
        // framebuffer another mod may have bound
        ShaderCompat.stencilAvailable();
        if ((DOORS.isEmpty() && WildPortalState.armed().isEmpty())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        long gameTime = mc.level.getGameTime();
        Vec3 cam = camera.getPosition();
        boolean inTreeRoom = mc.level.dimension() == ModDimensions.TREE_ROOM;

        List<BlockPos> visible = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (MysteriousDoorBlockEntity door : DOORS) {
            if (door.isRemoved() || door.getLevel() != mc.level) {
                DOORS.remove(door);
                continue;
            }
            considerDoor(mc.level, door.getBlockPos(), cam, gameTime, partialTick, seen, visible);
        }
        for (BlockPos pos : WildPortalState.armed()) {
            considerDoor(mc.level, pos, cam, gameTime, partialTick, seen, visible);
        }
        if (visible.isEmpty()) {
            return;
        }

        if (inTreeRoom) {
            withCameraModelView(late, modelViewMatrix, () -> drawStatic(mc.level, visible, cam));
            return;
        }
        renderPortals(groupIntoDoorways(mc.level, visible), frustum, modelViewMatrix, projectionMatrix, cam, late);
    }

    // One doorway, crafted or wild: still a lower half, still in range, still open
    // or recently so
    private static void considerDoor(Level level, BlockPos pos, Vec3 cam, long gameTime,
                                     float partialTick, Set<BlockPos> seen, List<BlockPos> out) {
        if (!seen.add(pos)) {
            return; // a crafted door the server also listed as wild
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)
                || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            LAST_OPEN.remove(pos);
            return;
        }
        if (cam.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
            return;
        }
        if (state.getValue(DoorBlock.OPEN)) {
            LAST_OPEN.put(pos.immutable(), gameTime);
        } else {
            Long last = LAST_OPEN.get(pos);
            if (last == null || gameTime - last + partialTick >= CLOSE_LINGER) {
                LAST_OPEN.remove(pos);
                return;
            }
        }
        out.add(pos.immutable());
    }

    // Merge open double doors into one doorway
    private static List<Doorway> groupIntoDoorways(Level level, List<BlockPos> doors) {
        Set<BlockPos> present = new HashSet<>(doors);
        List<Doorway> doorways = new ArrayList<>();
        Set<BlockPos> consumed = new HashSet<>();
        for (BlockPos pos : doors) {
            if (consumed.contains(pos)) {
                continue;
            }
            consumed.add(pos);
            BlockState state = level.getBlockState(pos);
            Direction facing = state.getValue(DoorBlock.FACING);
            if (state.getValue(DoorBlock.OPEN)) {
                DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
                BlockPos partnerPos = pos.relative(hinge == DoorHingeSide.LEFT
                        ? facing.getClockWise() : facing.getCounterClockWise());
                if (present.contains(partnerPos) && !consumed.contains(partnerPos)) {
                    BlockState ps = level.getBlockState(partnerPos);
                    if (ps.getBlock() instanceof DoorBlock
                            && ps.getValue(DoorBlock.FACING) == facing
                            && ps.getValue(DoorBlock.HINGE) != hinge
                            && ps.getValue(DoorBlock.OPEN)) {
                        consumed.add(partnerPos);
                        Vec3 seam = new Vec3((pos.getX() + partnerPos.getX()) / 2.0 + 0.5, pos.getY(),
                                (pos.getZ() + partnerPos.getZ()) / 2.0 + 0.5);
                        doorways.add(new Doorway(seam, facing, List.of(pos, partnerPos)));
                        continue;
                    }
                }
            }
            doorways.add(new Doorway(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                    facing, List.of(pos)));
        }
        return doorways;
    }

    private static void renderPortals(List<Doorway> doorways, Frustum frustum,
                                      Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Vec3 cam, boolean late) {
        Minecraft mc = Minecraft.getInstance();
        // A pack re-renders the world into its shadow map; portals have no business there
        if (ShaderCompat.isShadowPass()) {
            return;
        }
        if (ShaderCompat.portalUsesCompat()) {
            withCameraModelView(late, modelViewMatrix,
                    () -> CompatPortalRenderer.render(mc.level, doorways, cam, mc.level.getGameTime()));
            return;
        }
        VertexBuffer mesh = RoomMesh.get();
        ShaderInstance roomShader = ModShaders.getRoomCopyShader();
        if (mesh == null || roomShader == null) {
            return;
        }
        if (!((StencilTarget) mc.getMainRenderTarget()).forgottenman$isStencilEnabled()) {
            return; // ClientEvents turns it on between frames; never enable it mid-draw
        }
        Matrix4f proj = new Matrix4f(projectionMatrix);
        // At the tail of renderLevel RenderSystem's model-view is no longer the camera,
        // so the camera-relative quads would be drawn in screen space without this
        if (late) {
            Matrix4fStack stack = RenderSystem.getModelViewStack();
            stack.pushMatrix();
            stack.mul(modelViewMatrix);
            RenderSystem.applyModelViewMatrix();
        }

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
            drawDoorwayQuads(mc.level, doorway, cam);

            // Pass 2: black backdrop inside the mask, depth pushed to the far plane
            GlStateManager._stencilFunc(GL11.GL_EQUAL, ref, 0xFF);
            GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GlStateManager._stencilMask(0x00);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            GL11.glDepthRange(1.0, 1.0);
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
            drawDoorwayQuads(mc.level, doorway, cam);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDepthRange(0.0, 1.0);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);

            // Pass 3: the room, anchored so its return door sits in this doorway;
            // both sides look into the room, same fold as the actual teleport
            Vec3 facingVec = new Vec3(doorway.facing().getStepX(), 0.0, doorway.facing().getStepZ());
            boolean frontSide = cam.subtract(doorway.anchor()).dot(facingVec) >= 0.0;
            Direction entering = frontSide ? doorway.facing().getOpposite() : doorway.facing();
            float rad = (float) Math.toRadians(ROOM_ENTRY_YAW - entering.toYRot());
            Matrix4f modelView = new Matrix4f(modelViewMatrix)
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
                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                for (BlockPos door : doorway.doors()) {
                    addQuad(builder, mc.level, door, cam);
                }
                MeshData quadMesh = builder.build();
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
            drawDoorwayQuads(mc.level, doorway, cam);
            RenderSystem.colorMask(true, true, true, true);
        }
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GlStateManager._stencilMask(0xFF);
        RenderSystem.enableCull(); // Vanilla default
        if (late) {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Runs a draw with the camera matrix applied, needed once past the gbuffer stage. */
    private static void withCameraModelView(boolean late, Matrix4f modelViewMatrix, Runnable draw) {
        if (!late) {
            draw.run();
            return;
        }
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.mul(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();
        draw.run();
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static AABB doorwayBounds(Doorway doorway) {
        AABB box = null;
        for (BlockPos p : doorway.doors()) {
            AABB b = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 2.0, p.getZ() + 1.0);
            box = box == null ? b : box.minmax(b);
        }
        return box.inflate(0.5);
    }

    // Doorway quads in the closed-panel plane
    private static void drawDoorwayQuads(Level level, Doorway doorway, Vec3 cam) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (BlockPos door : doorway.doors()) {
            addQuad(builder, level, door, cam);
        }
        MeshData mesh = builder.build();
        if (mesh != null) {
            RenderSystem.setShader(GameRenderer::getPositionShader);
            BufferUploader.drawWithShader(mesh);
        }
    }

    // Static for tree room doors, there's no destination mesh to show
    private static void drawStatic(Level level, List<BlockPos> doors, Vec3 cam) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (BlockPos door : doors) {
            addQuad(builder, level, door, cam);
        }
        MeshData mesh = builder.build();
        if (mesh != null) {
            ModRenderTypes.PORTAL_STATIC.draw(mesh);
        }
    }

    private static void addQuad(BufferBuilder builder, Level level, BlockPos pos, Vec3 cam) {
        // Camera-relative; the model-view at this stage already has the camera rotation
        float x = (float) (pos.getX() - cam.x);
        float y = (float) (pos.getY() - cam.y);
        float z = (float) (pos.getZ() - cam.z);
        Direction facing = level.getBlockState(pos).getValue(DoorBlock.FACING);
        switch (facing) {
            case NORTH -> quadZ(builder, x, y, z + PLANE_FAR);
            case SOUTH -> quadZ(builder, x, y, z + PLANE_NEAR);
            case WEST -> quadX(builder, x + PLANE_FAR, y, z);
            case EAST -> quadX(builder, x + PLANE_NEAR, y, z);
            default -> quadZ(builder, x, y, z + PLANE_NEAR);
        }
    }

    private static void quadZ(BufferBuilder builder, float x, float y, float z) {
        builder.addVertex(x, y, z);
        builder.addVertex(x + 1.0F, y, z);
        builder.addVertex(x + 1.0F, y + 2.0F, z);
        builder.addVertex(x, y + 2.0F, z);
    }

    private static void quadX(BufferBuilder builder, float x, float y, float z) {
        builder.addVertex(x, y, z);
        builder.addVertex(x, y, z + 1.0F);
        builder.addVertex(x, y + 2.0F, z + 1.0F);
        builder.addVertex(x, y + 2.0F, z);
    }

    private DoorPortalRenderer() {
    }
}
