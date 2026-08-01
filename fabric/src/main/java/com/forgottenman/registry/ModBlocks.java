package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.forgottenman.block.MysteriousDoorBlock;
import com.forgottenman.block.PlumGrassBlock;
import com.forgottenman.block.TreeLeavesBlock;
import com.forgottenman.block.VoidWallBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final Supplier<MysteriousDoorBlock> MYSTERIOUS_DOOR = register("mysterious_door",
            new MysteriousDoorBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .pushReaction(PushReaction.DESTROY)));

    // Tree room blocks, palette sampled from the game (#612C61 floor, #202040 trunk,
    // #E02040 / #C00080 / #A00080 crown)
    public static final Supplier<Block> PLUM_GROUND = register("plum_ground",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .sound(SoundType.STONE)));

    public static final Supplier<Block> PLUM_GRASS = register("plum_grass",
            new PlumGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));

    // Bark shades gradient up the trunk
    public static final Supplier<Block> VOID_BARK = register("void_bark", new Block(barkProperties()));

    public static final Supplier<Block> VOID_BARK_1 = register("void_bark_1", new Block(barkProperties()));

    public static final Supplier<Block> VOID_BARK_2 = register("void_bark_2", new Block(barkProperties()));

    public static final Supplier<Block> SCARLET_LEAVES = register("scarlet_leaves",
            new TreeLeavesBlock(ModParticles.SCARLET_LEAF, leavesProperties()));

    public static final Supplier<Block> MAGENTA_LEAVES = register("magenta_leaves",
            new TreeLeavesBlock(ModParticles.MAGENTA_LEAF, leavesProperties()));

    public static final Supplier<Block> DEEP_MAGENTA_LEAVES = register("deep_magenta_leaves",
            new TreeLeavesBlock(ModParticles.DEEP_MAGENTA_LEAF, leavesProperties()));

    /** Invisible, indestructible, impassable; fills the void around the room */
    public static final Supplier<Block> VOID_WALL = register("void_wall",
            new VoidWallBlock(BlockBehaviour.Properties.of()
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

    // Fabric registries are eager; the returned supplier keeps every call site that
    // used NeoForge's DeferredBlock.get() working unchanged
    private static <T extends Block> Supplier<T> register(String name, T block) {
        T registered = Registry.register(BuiltInRegistries.BLOCK, ForgottenMan.id(name), block);
        return () -> registered;
    }

    /** Touching this class runs the static initialisers above, which do the registering */
    public static void register() {
    }

    private ModBlocks() {
    }
}
