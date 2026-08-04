package com.forgottenman.block;

import com.forgottenman.portal.PortalTravel;
import com.forgottenman.registry.ModDimensions;
import net.minecraft.core.BlockPos;
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
        // 1.20.1 takes the properties first
        super(properties, type);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MysteriousDoorBlockEntity(pos, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide
                || !state.getValue(OPEN)
                || state.getValue(HALF) != DoubleBlockHalf.LOWER
                || !(entity instanceof ServerPlayer player)
                || player.isOnPortalCooldown()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        if (level.dimension() == ModDimensions.TREE_ROOM) {
            PortalTravel.returnHome(player, serverLevel.getServer());
        } else {
            PortalTravel.enter(player, serverLevel, pos, false);
        }
    }
}
