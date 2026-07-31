package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public final class ModItems {
    public static final Supplier<Item> MYSTERIOUS_DOOR = register("mysterious_door",
            new DoubleHighBlockItem(ModBlocks.MYSTERIOUS_DOOR.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));

    /** The man's gift */
    public static final Supplier<Item> EGG = register("egg",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Supplier<Item> PLUM_GROUND = blockItem("plum_ground", ModBlocks.PLUM_GROUND);
    public static final Supplier<Item> PLUM_GRASS = blockItem("plum_grass", ModBlocks.PLUM_GRASS);
    public static final Supplier<Item> VOID_BARK = blockItem("void_bark", ModBlocks.VOID_BARK);
    public static final Supplier<Item> VOID_BARK_1 = blockItem("void_bark_1", ModBlocks.VOID_BARK_1);
    public static final Supplier<Item> VOID_BARK_2 = blockItem("void_bark_2", ModBlocks.VOID_BARK_2);
    public static final Supplier<Item> SCARLET_LEAVES = blockItem("scarlet_leaves", ModBlocks.SCARLET_LEAVES);
    public static final Supplier<Item> MAGENTA_LEAVES = blockItem("magenta_leaves", ModBlocks.MAGENTA_LEAVES);
    public static final Supplier<Item> DEEP_MAGENTA_LEAVES = blockItem("deep_magenta_leaves", ModBlocks.DEEP_MAGENTA_LEAVES);

    private static Supplier<Item> blockItem(String name, Supplier<Block> block) {
        return register(name, new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Item> Supplier<T> register(String name, T item) {
        T registered = Registry.register(BuiltInRegistries.ITEM, ForgottenMan.id(name), item);
        return () -> registered;
    }

    public static void register() {
    }

    private ModItems() {
    }
}
