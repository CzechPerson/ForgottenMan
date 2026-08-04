package com.forgottenman.portal;

import com.forgottenman.block.MysteriousDoorBlock;
import com.forgottenman.network.ModNetworking;
import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.room.RoomLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Both ways through a doorway. The crafted door and a door the world grew a portal
 * on travel identically; they differ only in what happens to the door afterwards.
 */
public final class PortalTravel {
    /** @param wild a door of the world's own, not a crafted one */
    public static void enter(ServerPlayer player, ServerLevel level, BlockPos doorPos, boolean wild) {
        ServerLevel target = level.getServer().getLevel(ModDimensions.TREE_ROOM);
        if (target == null) {
            return;
        }
        player.setPortalCooldown();
        player.setData(ModAttachments.ENTRY_DOOR.get(), GlobalPos.of(level.dimension(), doorPos.immutable()));
        player.setData(ModAttachments.ENTRY_WILD.get(), wild);
        if (!wild && player.getData(ModAttachments.MET_MAN.get())) {
            // A fresh crafted door begins a fresh cycle. A wild one is a repeat
            // visit, not a new start, so it leaves that alone.
            player.setData(ModAttachments.MET_MAN.get(), false);
            RoomLayout.place(target);
        } else if (!RoomLayout.isPlaced(target)) {
            RoomLayout.place(target);
        }
        RoomLayout.ensureManPresent(target, player);
        BlockPos arrival = RoomLayout.ORIGIN.offset(RoomLayout.ARRIVAL_LOCAL);
        player.teleportTo(target, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5,
                RoomLayout.ARRIVAL_YAW, player.getXRot());
    }

    /** Back to the door you came through, if it still exists */
    public static void returnHome(ServerPlayer player, MinecraftServer server) {
        if (player.hasData(ModAttachments.ENTRY_DOOR.get())) {
            GlobalPos entry = player.getData(ModAttachments.ENTRY_DOOR.get());
            boolean wild = player.getData(ModAttachments.ENTRY_WILD.get());
            ServerLevel target = server.getLevel(entry.dimension());
            if (target != null) {
                BlockState doorState = target.getBlockState(entry.pos());
                boolean usable = wild
                        ? doorState.getBlock() instanceof DoorBlock
                        : doorState.getBlock() instanceof MysteriousDoorBlock;
                if (usable) {
                    Direction facing = doorState.getValue(DoorBlock.FACING);
                    BlockPos exit = findExitSpot(target, entry.pos(), facing);
                    // Yaw comes from the spot we picked, before any lift
                    float yaw = exit.equals(entry.pos().relative(facing))
                            ? facing.toYRot() : facing.getOpposite().toYRot();
                    BlockPos safe = liftClear(target, exit);
                    player.setPortalCooldown();
                    player.teleportTo(target, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
                            yaw, player.getXRot());
                    if (wild) {
                        // The portal was on loan, the door was not: take one back,
                        // leave the other standing
                        WildPortals.disarm(target, entry.pos());
                        ModNetworking.syncWildPortals(target);
                        player.removeData(ModAttachments.ENTRY_DOOR.get());
                        player.setData(ModAttachments.ENTRY_WILD.get(), false);
                    } else if (player.getData(ModAttachments.MET_MAN.get())) {
                        // The door served its purpose
                        target.destroyBlock(entry.pos(), false);
                        player.removeData(ModAttachments.ENTRY_DOOR.get());
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

    private PortalTravel() {
    }
}
