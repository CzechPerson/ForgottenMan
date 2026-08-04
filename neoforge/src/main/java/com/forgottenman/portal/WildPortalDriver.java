package com.forgottenman.portal;

import com.forgottenman.ForgottenMan;
import com.forgottenman.block.MysteriousDoorBlock;
import com.forgottenman.config.ModConfig;
import com.forgottenman.network.ModNetworking;
import com.forgottenman.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Grows portals on the world's own doors, when the config lets it.
 *
 * The roll happens a tick after the click rather than during it: at click time the
 * door has not moved yet, and waiting one tick also catches the case where another
 * mod swings both halves of a double door from the one interaction.
 */
@EventBusSubscriber(modid = ForgottenMan.MOD_ID)
public final class WildPortalDriver {
    private record Pending(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !ModConfig.randomEntrancesEnabled()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || state.getBlock() instanceof MysteriousDoorBlock) {
            return; // the crafted door is already a portal and needs no luck
        }
        // The upper half is the same doorway; normalise so both clicks agree
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (WildPortals.isOpenDoor(event.getLevel(), lower)) {
            return; // already open, this click is closing it
        }
        PENDING.add(new Pending(event.getLevel().dimension(), lower.immutable()));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) {
            resolvePending(event.getServer());
        }
        Set<ResourceKey<Level>> changed = WildPortals.sweep(event.getServer().getAllLevels());
        for (ResourceKey<Level> dimension : changed) {
            ServerLevel level = event.getServer().getLevel(dimension);
            if (level != null) {
                ModNetworking.syncWildPortals(level);
            }
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            checkWalkedThrough(player);
        }
    }

    private static void resolvePending(MinecraftServer server) {
        List<Pending> pending = List.copyOf(PENDING);
        PENDING.clear();
        if (!ModConfig.randomEntrancesEnabled()) {
            return;
        }
        double chance = ModConfig.randomEntranceChance();
        if (chance <= 0.0) {
            return;
        }
        for (Pending entry : pending) {
            ServerLevel level = server.getLevel(entry.dimension());
            if (level == null || !level.isLoaded(entry.pos()) || !WildPortals.isOpenDoor(level, entry.pos())) {
                continue; // the click did not end up opening it
            }
            if (WildPortals.isArmed(level, entry.pos()) || level.random.nextDouble() >= chance) {
                continue;
            }
            WildPortals.arm(level, entry.pos());
            ModNetworking.syncWildPortals(level);
        }
    }

    // Vanilla doors have no entityInside of ours to hook, so the walk-through is
    // caught by looking at where each player is standing
    private static void checkWalkedThrough(ServerPlayer player) {
        if (player.isOnPortalCooldown()
                || !(player.level() instanceof ServerLevel level)
                || level.dimension() == ModDimensions.TREE_ROOM
                || WildPortals.armedIn(level.dimension()).isEmpty()) {
            return; // nothing armed here, so no need to look at where anyone stands
        }
        BlockPos feet = player.blockPosition();
        BlockPos lower = feet;
        BlockState state = level.getBlockState(feet);
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            lower = feet.below();
        }
        if (!WildPortals.isArmed(level, lower) || !WildPortals.isOpenDoor(level, lower)) {
            return;
        }
        PortalTravel.enter(player, level, lower, true);
    }

    /** New arrivals and dimension hoppers need the set for wherever they now are */
    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.syncWildPortals(player);
        }
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.syncWildPortals(player);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        WildPortals.clear();
        PENDING.clear();
    }

    private WildPortalDriver() {
    }
}
