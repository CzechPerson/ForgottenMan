package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ForgottenMan.MOD_ID);

    public static final RegistryObject<Item> MYSTERIOUS_DOOR = ITEMS.register("mysterious_door",
            () -> new DoubleHighBlockItem(ModBlocks.MYSTERIOUS_DOOR.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));

    /** The man's gift */
    public static final RegistryObject<Item> EGG = ITEMS.register("egg",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<BlockItem> PLUM_GROUND = blockItem("plum_ground", ModBlocks.PLUM_GROUND);
    public static final RegistryObject<BlockItem> PLUM_GRASS = blockItem("plum_grass", ModBlocks.PLUM_GRASS);
    public static final RegistryObject<BlockItem> VOID_BARK = blockItem("void_bark", ModBlocks.VOID_BARK);
    public static final RegistryObject<BlockItem> VOID_BARK_1 = blockItem("void_bark_1", ModBlocks.VOID_BARK_1);
    public static final RegistryObject<BlockItem> VOID_BARK_2 = blockItem("void_bark_2", ModBlocks.VOID_BARK_2);
    public static final RegistryObject<BlockItem> SCARLET_LEAVES = blockItem("scarlet_leaves", ModBlocks.SCARLET_LEAVES);
    public static final RegistryObject<BlockItem> MAGENTA_LEAVES = blockItem("magenta_leaves", ModBlocks.MAGENTA_LEAVES);
    public static final RegistryObject<BlockItem> DEEP_MAGENTA_LEAVES =
            blockItem("deep_magenta_leaves", ModBlocks.DEEP_MAGENTA_LEAVES);

    // 1.20.1 has no registerSimpleBlockItem; the name has to match the block's
    private static RegistryObject<BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {
    }
}
