package com.forgottenman.entity;

import com.forgottenman.network.OpenManDialoguePayload;
import com.forgottenman.registry.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * The man behind the tree
 */
public class ManEntity extends Entity {
    // -1 = present; >= 0 = ticks spent dissolving
    private static final EntityDataAccessor<Integer> DATA_VANISH_TICKS =
            SynchedEntityData.defineId(ManEntity.class, EntityDataSerializers.INT);
    public static final int VANISH_DURATION = 50;

    public ManEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VANISH_TICKS, -1);
    }

    public int getVanishTicks() {
        return this.entityData.get(DATA_VANISH_TICKS);
    }

    public void startVanishing() {
        if (getVanishTicks() < 0) {
            this.entityData.set(DATA_VANISH_TICKS, 0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            int vanish = getVanishTicks();
            if (vanish >= 0) {
                if (vanish >= VANISH_DURATION) {
                    this.discard();
                } else {
                    this.entityData.set(DATA_VANISH_TICKS, vanish + 1);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (getVanishTicks() < 0 && player instanceof ServerPlayer serverPlayer
                && !ModAttachments.hasMetMan(serverPlayer)) {
            ServerPlayNetworking.send(serverPlayer, OpenManDialoguePayload.INSTANCE);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
