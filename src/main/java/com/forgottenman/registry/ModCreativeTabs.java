package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final Supplier<CreativeModeTab> MAIN = register("main", FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.forgottenman"))
            .icon(() -> new ItemStack(ModItems.MYSTERIOUS_DOOR.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.MYSTERIOUS_DOOR.get());
                output.accept(ModItems.EGG.get());
                output.accept(ModItems.PLUM_GROUND.get());
                output.accept(ModItems.PLUM_GRASS.get());
                output.accept(ModItems.VOID_BARK.get());
                output.accept(ModItems.VOID_BARK_1.get());
                output.accept(ModItems.VOID_BARK_2.get());
                output.accept(ModItems.SCARLET_LEAVES.get());
                output.accept(ModItems.MAGENTA_LEAVES.get());
                output.accept(ModItems.DEEP_MAGENTA_LEAVES.get());
            })
            .build());

    private static Supplier<CreativeModeTab> register(String name, CreativeModeTab tab) {
        CreativeModeTab registered = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB, ForgottenMan.id(name), tab);
        return () -> registered;
    }

    public static void register() {
    }

    private ModCreativeTabs() {
    }
}
