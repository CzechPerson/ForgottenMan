package com.forgottenman.compat;

import com.forgottenman.ForgottenMan;
import com.forgottenman.registry.ModItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;

import java.util.List;

/**
 * EMI integration, only loaded when EMI is present. The void door trade shows as an
 * Information entry on every door.
 */
@EmiEntrypoint
public class EmiIntegration implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipe(new EmiInfoRecipe(
                List.of(EmiStack.of(ModItems.MYSTERIOUS_DOOR.get()), EmiIngredient.of(ItemTags.DOORS)),
                List.of(Component.translatable("forgottenman.info.void_door")),
                ForgottenMan.id("void_door_info")));
    }
}
