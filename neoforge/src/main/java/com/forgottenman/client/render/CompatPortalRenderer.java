package com.forgottenman.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Doorway surfaces drawn without the mod's core shaders.
 *
 * Only for framebuffers that genuinely lack a stencil -- Fabulous layer targets and
 * target-swapping mods. A shaderpack does NOT cost us the stencil: measured, the draw
 * framebuffer is the main render target with the stencil attached, so under a pack the
 * real masked portal runs instead, moved past the pack's composite.
 *
 * Colour is a Java port of portal_static.fsh / portal_overlay.fsh. One visible
 * difference: those hash on gl_FragCoord, so the static sat still in screen space;
 * sampled per cell it is locked to the door surface instead.
 */
final class CompatPortalRenderer {
    // Cells across the 1-block width and the 2-block height
    private static final int CELLS_U = 24;
    private static final int CELLS_V = 48;
    private static final int SHIMMER_ROWS = 64;

    static void render(Level level, List<DoorPortalRenderer.Doorway> doorways, Vec3 cam, long gameTime) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (DoorPortalRenderer.Doorway doorway : doorways) {
            for (BlockPos door : doorway.doors()) {
                emitAperture(builder, level, door, cam, gameTime);
            }
        }
        // Depth-writing, so this doubles as the far backdrop and the seal that keeps
        // water, clouds and particles from drawing inside the doorway
        ModRenderTypes.PORTAL_SURFACE_COMPAT.end(builder, VertexSorting.DISTANCE_TO_ORIGIN);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (DoorPortalRenderer.Doorway doorway : doorways) {
            for (BlockPos door : doorway.doors()) {
                emitShimmer(builder, level, door, cam, gameTime);
            }
        }
        ModRenderTypes.PORTAL_SHIMMER_COMPAT.end(builder, VertexSorting.DISTANCE_TO_ORIGIN);
    }

    private static void emitAperture(BufferBuilder builder, Level level, BlockPos pos,
                                     Vec3 cam, long gameTime) {
        Plane plane = planeOf(level, pos, cam);
        if (plane == null) {
            return;
        }
        for (int j = 0; j < CELLS_V; j++) {
            for (int i = 0; i < CELLS_U; i++) {
                float n = hash(i, j, gameTime);
                float t = n * n;
                // mix(vec3(0.02, 0.0, 0.01), vec3(0.32, 0.05, 0.08), n * n)
                float r = 0.02F + (0.32F - 0.02F) * t;
                float g = 0.05F * t;
                float b = 0.01F + (0.08F - 0.01F) * t;
                plane.cell(builder, i / (float) CELLS_U, j / (float) CELLS_V,
                        1.0F / CELLS_U, 1.0F / CELLS_V, r, g, b, 1.0F);
            }
        }
    }

    private static void emitShimmer(BufferBuilder builder, Level level, BlockPos pos,
                                    Vec3 cam, long gameTime) {
        Plane plane = planeOf(level, pos, cam);
        if (plane == null) {
            return;
        }
        for (int j = 0; j < SHIMMER_ROWS; j++) {
            float n = hash(0, j, gameTime);
            plane.cell(builder, 0.0F, j / (float) SHIMMER_ROWS,
                    1.0F, 1.0F / SHIMMER_ROWS, n, n, n, 0.06F);
        }
    }

    // fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123), as in both fragment shaders
    private static float hash(int i, int j, long gameTime) {
        double d = (i + gameTime) * 127.1 + j * 311.7;
        double v = Math.sin(d) * 43758.5453123;
        return (float) (v - Math.floor(v));
    }

    /** Camera-relative closed-panel plane: an origin plus a width and a height axis. */
    private record Plane(float ox, float oy, float oz, float ux, float uz) {
        void cell(BufferBuilder builder, float u, float v, float du, float dv,
                  float r, float g, float b, float a) {
            float x0 = ox + ux * u;
            float z0 = oz + uz * u;
            float x1 = ox + ux * (u + du);
            float z1 = oz + uz * (u + du);
            float y0 = oy + 2.0F * v;
            float y1 = oy + 2.0F * (v + dv);
            builder.vertex(x0, y0, z0).color(r, g, b, a).endVertex();
            builder.vertex(x1, y0, z1).color(r, g, b, a).endVertex();
            builder.vertex(x1, y1, z1).color(r, g, b, a).endVertex();
            builder.vertex(x0, y1, z0).color(r, g, b, a).endVertex();
        }
    }

    private static Plane planeOf(Level level, BlockPos pos, Vec3 cam) {
        if (!(level.getBlockState(pos).getBlock() instanceof DoorBlock)) {
            return null;
        }
        float x = (float) (pos.getX() - cam.x);
        float y = (float) (pos.getY() - cam.y);
        float z = (float) (pos.getZ() - cam.z);
        Direction facing = level.getBlockState(pos).getValue(DoorBlock.FACING);
        return switch (facing) {
            case NORTH -> new Plane(x, y, z + DoorPortalRenderer.PLANE_FAR, 1.0F, 0.0F);
            case WEST -> new Plane(x + DoorPortalRenderer.PLANE_FAR, y, z, 0.0F, 1.0F);
            case EAST -> new Plane(x + DoorPortalRenderer.PLANE_NEAR, y, z, 0.0F, 1.0F);
            default -> new Plane(x, y, z + DoorPortalRenderer.PLANE_NEAR, 1.0F, 0.0F);
        };
    }

    private CompatPortalRenderer() {
    }
}
