package com.forgottenman.mixin;

import com.forgottenman.CommonEvents;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the void door trade. NeoForge has EntityTickEvent.Pre for this; Fabric has no
 * per-entity tick event, so the check runs at the head of the item's own tick.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void forgottenman$voidDoorTrade(CallbackInfo ci) {
        CommonEvents.onItemEntityTick((ItemEntity) (Object) this);
    }
}
