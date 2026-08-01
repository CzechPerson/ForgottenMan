package com.forgottenman.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Crown block that sheds falling leaf particles from its underside; only blocks with air below emit */
public class TreeLeavesBlock extends Block {
    private final Supplier<SimpleParticleType> fallingLeaf;

    public TreeLeavesBlock(Supplier<SimpleParticleType> fallingLeaf, Properties properties) {
        super(properties);
        this.fallingLeaf = fallingLeaf;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) {
            return;
        }
        BlockPos below = pos.below();
        if (level.getBlockState(below).isAir()) {
            ParticleUtils.spawnParticleBelow(level, pos, random, fallingLeaf.get());
        }
    }
}
