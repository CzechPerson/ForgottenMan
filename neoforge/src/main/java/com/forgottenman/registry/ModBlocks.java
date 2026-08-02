package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.forgottenman.block.MysteriousDoorBlock;
import com.forgottenman.block.PlumGrassBlock;
import com.forgottenman.block.TreeLeavesBlock;
import com.forgottenman.block.VoidWallBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ForgottenMan.MOD_ID);

    public static final RegistryObject<MysteriousDoorBlock> MYSTERIOUS_DOOR = BLOCKS.register("mysterious_door",
            () -> new MysteriousDoorBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .pushReaction(PushReaction.DESTROY)));

    // Tree room blocks, palette sampled from the game (#612C61 floor, #202040 trunk,
    // #E02040 / #C00080 / #A00080 crown)
    public static final RegistryObject<Block> PLUM_GROUND = BLOCKS.register("plum_ground",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> PLUM_GRASS = BLOCKS.register("plum_grass",
            () -> new PlumGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));

    // Bark shades gradient up the trunk
    public static final RegistryObject<Block> VOID_BARK = BLOCKS.register("void_bark",
            () -> new Block(barkProperties()));

    public static final RegistryObject<Block> VOID_BARK_1 = BLOCKS.register("void_bark_1",
            () -> new Block(barkProperties()));

    public static final RegistryObject<Block> VOID_BARK_2 = BLOCKS.register("void_bark_2",
            () -> new Block(barkProperties()));

    public static final RegistryObject<Block> SCARLET_LEAVES = BLOCKS.register("scarlet_leaves",
            () -> new TreeLeavesBlock(ModParticles.SCARLET_LEAF, leavesProperties()));

    public static final RegistryObject<Block> MAGENTA_LEAVES = BLOCKS.register("magenta_leaves",
            () -> new TreeLeavesBlock(ModParticles.MAGENTA_LEAF, leavesProperties()));

    public static final RegistryObject<Block> DEEP_MAGENTA_LEAVES = BLOCKS.register("deep_magenta_leaves",
            () -> new TreeLeavesBlock(ModParticles.DEEP_MAGENTA_LEAF, leavesProperties()));

    public static final RegistryObject<Block> VOID_WALL = BLOCKS.register("void_wall",
            () -> new VoidWallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()));

    private static BlockBehaviour.Properties barkProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0F)
                .sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties leavesProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.4F)
                .sound(SoundType.WOOL);
    }

    private ModBlocks() {
    }
}
