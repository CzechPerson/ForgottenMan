package com.forgottenman.compat;

import com.forgottenman.ForgottenMan;
import com.forgottenman.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI integration, only loaded when JEI is present. Documents the void door trade
 * from both directions.
 */
@JeiPlugin
public class JeiIntegration implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ForgottenMan.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(new ItemStack(ModItems.MYSTERIOUS_DOOR.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("forgottenman.info.void_door"));

        List<ItemStack> doors = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.DOORS)) {
            if (holder.value() != ModItems.MYSTERIOUS_DOOR.get()) {
                doors.add(new ItemStack(holder.value()));
            }
        }
        if (!doors.isEmpty()) {
            registration.addIngredientInfo(doors, VanillaTypes.ITEM_STACK,
                    Component.translatable("forgottenman.info.void_door"));
        }
    }
}
