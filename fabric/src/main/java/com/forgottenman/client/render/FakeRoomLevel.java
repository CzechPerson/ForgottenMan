package com.forgottenman.client.render;

import com.forgottenman.room.RoomLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * Fake, fullbright view of the tree room (local coordinates) used to bake its
 * geometry into a mesh on the client.
 */
public final class FakeRoomLevel implements BlockAndTintGetter {
    public static final FakeRoomLevel INSTANCE = new FakeRoomLevel();

    private FakeRoomLevel() {
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return RoomLayout.blocks().getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        // Vanilla directional face shading so the baked copies match the real room
        if (!shade) {
            return 1.0F;
        }
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        // Only reachable through the getBrightness defaults overridden above
        return Minecraft.getInstance().level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return 0xFFFFFF; // No biome-tinted blocks in the room
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public int getMinBuildHeight() {
        return -64;
    }
}
