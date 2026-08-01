package com.forgottenman;

import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class CommonEvents {
    // portal/trigger.ogg is exactly 4.0s; at pitch 0.5 it lasts 8s (160 ticks)
    private static final float CLAIM_PITCH = 0.5F;
    private static final int RETURN_DELAY_TICKS = 164;

    private static final List<PendingDoor> PENDING = new ArrayList<>();

    private static final class PendingDoor {
        final ResourceKey<Level> dimension;
        @Nullable
        final UUID owner;
        final Vec3 fallback;
        final int count;
        int ticksLeft = RETURN_DELAY_TICKS;

        PendingDoor(ResourceKey<Level> dimension, @Nullable UUID owner, Vec3 fallback, int count) {
            this.dimension = dimension;
            this.owner = owner;
            this.fallback = fallback;
            this.count = count;
        }
    }

    public static void register() {
        // nothing in the tree room can be broken, unless you're in creative
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                level.dimension() != ModDimensions.TREE_ROOM || player.isCreative());

        ServerTickEvents.END_SERVER_TICK.register(CommonEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    /** The door recipe, called from ItemEntityMixin since Fabric has no entity tick event */
    public static void onItemEntityTick(ItemEntity item) {
        if (item.level().isClientSide) {
            return;
        }
        Level level = item.level();
        if (item.getY() >= level.getMinBuildHeight() - 8) {
            return;
        }
        ItemStack stack = item.getItem();
        if (stack.isEmpty() || !stack.is(ItemTags.DOORS)) {
            return; // mysterious doors are in #minecraft:doors too, so the void
                    // claims and returns those as well instead of eating them
        }
        Entity owner = item.getOwner();
        Vec3 fallback = owner != null ? owner.position()
                : Vec3.atBottomCenterOf(((ServerLevel) level).getSharedSpawnPos());
        PENDING.add(new PendingDoor(level.dimension(),
                owner != null ? owner.getUUID() : null, fallback, stack.getCount()));
        item.discard();
        // play the claim where the thrower stands, not down in the void
        level.playSound(null, fallback.x, fallback.y, fallback.z,
                ModSounds.VOID_CLAIM.get(), SoundSource.PLAYERS, 0.9F, CLAIM_PITCH);
    }

    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<PendingDoor> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingDoor pending = iterator.next();
            if (--pending.ticksLeft > 0) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = pending.owner != null
                    ? server.getPlayerList().getPlayer(pending.owner) : null;
            ServerLevel level = player != null ? (ServerLevel) player.level()
                    : server.getLevel(pending.dimension);
            if (level == null) {
                continue;
            }
            Vec3 pos = player != null ? player.position() : pending.fallback;
            ItemEntity door = new ItemEntity(level, pos.x, pos.y + 0.3, pos.z,
                    new ItemStack(ModItems.MYSTERIOUS_DOOR.get(), pending.count));
            door.setDeltaMovement(0.0, 0.15, 0.0);
            door.setNoPickUpDelay();
            level.addFreshEntity(door);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.x, pos.y + 0.6, pos.z, 40, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private CommonEvents() {
    }
}
