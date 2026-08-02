package com.forgottenman.block;

import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.room.RoomLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

public class MysteriousDoorBlock extends DoorBlock implements EntityBlock {
    public MysteriousDoorBlock(BlockSetType type, Properties properties) {
        super(type, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MysteriousDoorBlockEntity(pos, state);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide
                || !state.getValue(OPEN)
                || state.getValue(HALF) != DoubleBlockHalf.LOWER
                || !(entity instanceof ServerPlayer player)
                || player.isOnPortalCooldown()) {
            return;
        }
        teleportThroughDoor(player, (ServerLevel) level, pos);
    }

    private static void teleportThroughDoor(ServerPlayer player, ServerLevel level, BlockPos doorPos) {
        if (level.dimension() == ModDimensions.TREE_ROOM) {
            returnThroughRememberedDoor(player, level.getServer());
        } else {
            ServerLevel target = level.getServer().getLevel(ModDimensions.TREE_ROOM);
            if (target == null) {
                return;
            }
            player.setPortalCooldown();
            player.setAttached(ModAttachments.ENTRY_DOOR, GlobalPos.of(level.dimension(), doorPos.immutable()));
            if (ModAttachments.hasMetMan(player)) {
                // A fresh door begins a fresh cycle
                ModAttachments.setMetMan(player, false);
                RoomLayout.place(target);
            } else if (!RoomLayout.isPlaced(target)) {
                RoomLayout.place(target);
            }
            RoomLayout.ensureManPresent(target, player);
            BlockPos arrival = RoomLayout.ORIGIN.offset(RoomLayout.ARRIVAL_LOCAL);
            player.teleportTo(target, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5,
                    RoomLayout.ARRIVAL_YAW, player.getXRot());
        }
    }

    // Back to the door you came through, if it still exists
    private static void returnThroughRememberedDoor(ServerPlayer player, MinecraftServer server) {
        if (player.hasAttached(ModAttachments.ENTRY_DOOR)) {
            GlobalPos entry = player.getAttached(ModAttachments.ENTRY_DOOR);
            ServerLevel target = server.getLevel(entry.dimension());
            if (target != null) {
                BlockState doorState = target.getBlockState(entry.pos());
                if (doorState.getBlock() instanceof MysteriousDoorBlock) {
                    Direction facing = doorState.getValue(FACING);
                    BlockPos exit = findExitSpot(target, entry.pos(), facing);
                    // Yaw comes from the spot we picked, before any lift
                    float yaw = exit.equals(entry.pos().relative(facing)) ? facing.toYRot() : facing.getOpposite().toYRot();
                    BlockPos safe = liftClear(target, exit);
                    player.setPortalCooldown();
                    player.teleportTo(target, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, yaw, player.getXRot());
                    if (ModAttachments.hasMetMan(player)) {
                        // The door served its purpose
                        target.destroyBlock(entry.pos(), false);
                        player.removeAttached(ModAttachments.ENTRY_DOOR);
                    }
                    return;
                }
            }
        }
        // Entry door gone or never recorded: overworld spawn
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        player.setPortalCooldown();
        BlockPos spawn = liftClear(overworld, overworld.getSharedSpawnPos());
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    // In front of the door if free, otherwise behind it
    private static BlockPos findExitSpot(ServerLevel level, BlockPos doorPos, Direction facing) {
        BlockPos front = doorPos.relative(facing);
        if (isPassable(level, front)) {
            return front;
        }
        BlockPos back = doorPos.relative(facing.getOpposite());
        if (isPassable(level, back)) {
            return back;
        }
        return front;
    }

    // Neither exit spot is guaranteed clear -- a door can be walled in on both
    // sides, and a world spawn is stored at ground level, not above it. Vanilla
    // steps the player up until they fit when it places them at spawn; same here,
    // otherwise the trip home ends inside the terrain.
    private static BlockPos liftClear(ServerLevel level, BlockPos feet) {
        BlockPos pos = feet;
        while (pos.getY() < level.getMaxBuildHeight() - 1 && !isPassable(level, pos)) {
            pos = pos.above();
        }
        return pos;
    }

    private static boolean isPassable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }
}
