package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForgottenMan.MOD_ID);

    public static final DeferredItem<Item> MYSTERIOUS_DOOR = ITEMS.register("mysterious_door",
            () -> new DoubleHighBlockItem(ModBlocks.MYSTERIOUS_DOOR.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));

    /** The man's gift */
    public static final DeferredItem<Item> EGG = ITEMS.register("egg",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<BlockItem> PLUM_GROUND = ITEMS.registerSimpleBlockItem(ModBlocks.PLUM_GROUND);
    public static final DeferredItem<BlockItem> PLUM_GRASS = ITEMS.registerSimpleBlockItem(ModBlocks.PLUM_GRASS);
    public static final DeferredItem<BlockItem> VOID_BARK = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_BARK);
    public static final DeferredItem<BlockItem> VOID_BARK_1 = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_BARK_1);
    public static final DeferredItem<BlockItem> VOID_BARK_2 = ITEMS.registerSimpleBlockItem(ModBlocks.VOID_BARK_2);
    public static final DeferredItem<BlockItem> SCARLET_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.SCARLET_LEAVES);
    public static final DeferredItem<BlockItem> MAGENTA_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.MAGENTA_LEAVES);
    public static final DeferredItem<BlockItem> DEEP_MAGENTA_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.DEEP_MAGENTA_LEAVES);

    private ModItems() {
    }
}
