package com.forgottenman.block;

import com.forgottenman.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries no data; exists so the client can track door positions for the portal
 * renderer. Deliberately not a BlockEntityRenderer, Fancy World Animations cancels
 * those at doors it animates.
 */
public class MysteriousDoorBlockEntity extends BlockEntity {
    // Client-side game time when the door was last seen open, -1 if never. After
    // closing the portal lingers: 2 ticks vanilla (chunk rebuild gap)
    private long lastOpenTime = -1L;

    public MysteriousDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MYSTERIOUS_DOOR.get(), pos, state);
    }

    public long getLastOpenTime() {
        return lastOpenTime;
    }

    public void setLastOpenTime(long time) {
        lastOpenTime = time;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            // Client class only referenced inside this guarded branch
            com.forgottenman.client.render.DoorPortalRenderer.track(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && this.level.isClientSide) {
            com.forgottenman.client.render.DoorPortalRenderer.untrack(this);
        }
        super.setRemoved();
    }
}
