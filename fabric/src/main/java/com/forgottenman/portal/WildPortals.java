package com.forgottenman.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The doors the world has grown a portal on, keyed by dimension and stored as the
 * lower half's position.
 *
 * Deliberately not persisted: a portal already ends when its door closes, so a
 * restart landing you in the same state is consistent rather than surprising.
 * Which door a travelling player has to come back to lives on the player instead,
 * so a restart never strands anyone in the room.
 */
public final class WildPortals {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ARMED = new HashMap<>();

    public static boolean isArmed(Level level, BlockPos lowerHalf) {
        Set<BlockPos> set = ARMED.get(level.dimension());
        return set != null && set.contains(lowerHalf);
    }

    public static Set<BlockPos> armedIn(ResourceKey<Level> dimension) {
        Set<BlockPos> set = ARMED.get(dimension);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    /** Arms the whole doorway, so either half of a double door lets you through */
    public static void arm(ServerLevel level, BlockPos lowerHalf) {
        Set<BlockPos> set = ARMED.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>());
        set.add(lowerHalf.immutable());
        BlockPos partner = partnerOf(level, lowerHalf);
        if (partner != null) {
            set.add(partner);
        }
    }

    public static void disarm(ServerLevel level, BlockPos lowerHalf) {
        Set<BlockPos> set = ARMED.get(level.dimension());
        if (set == null) {
            return;
        }
        set.remove(lowerHalf);
        BlockPos partner = partnerOf(level, lowerHalf);
        if (partner != null) {
            set.remove(partner);
        }
        if (set.isEmpty()) {
            ARMED.remove(level.dimension());
        }
    }

    public static void clear() {
        ARMED.clear();
    }

    /**
     * Drops doors that have closed or stopped being doors. Returns the dimensions
     * that changed, so only those need resyncing.
     */
    public static Set<ResourceKey<Level>> sweep(Iterable<ServerLevel> levels) {
        Set<ResourceKey<Level>> changed = new HashSet<>();
        for (ServerLevel level : levels) {
            Set<BlockPos> set = ARMED.get(level.dimension());
            if (set == null || set.isEmpty()) {
                continue;
            }
            // A door in an unloaded chunk is left alone; reading it would force-load
            // the chunk every tick just to police an easter egg
            if (set.removeIf(pos -> level.isLoaded(pos) && !isOpenDoor(level, pos))) {
                changed.add(level.dimension());
            }
            if (set.isEmpty()) {
                ARMED.remove(level.dimension());
            }
        }
        return changed;
    }

    public static boolean isOpenDoor(BlockGetter level, BlockPos lowerHalf) {
        BlockState state = level.getBlockState(lowerHalf);
        return state.getBlock() instanceof DoorBlock
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && state.getValue(DoorBlock.OPEN);
    }

    /**
     * The other half of a double door: same facing, opposite hinge, sitting on the
     * hinge side. Matches how the renderer decides a pair is one doorway.
     */
    @Nullable
    public static BlockPos partnerOf(BlockGetter level, BlockPos lowerHalf) {
        BlockState state = level.getBlockState(lowerHalf);
        if (!(state.getBlock() instanceof DoorBlock)
                || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        Direction facing = state.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        BlockPos side = lowerHalf.relative(hinge == DoorHingeSide.LEFT
                ? facing.getClockWise() : facing.getCounterClockWise());
        BlockState other = level.getBlockState(side);
        if (other.getBlock() instanceof DoorBlock
                && other.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && other.getValue(DoorBlock.FACING) == facing
                && other.getValue(DoorBlock.HINGE) != hinge) {
            return side.immutable();
        }
        return null;
    }

    private WildPortals() {
    }
}
