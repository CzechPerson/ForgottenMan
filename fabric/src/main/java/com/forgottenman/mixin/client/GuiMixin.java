package com.forgottenman.mixin.client;

import com.forgottenman.client.gui.ManDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the hotbar cluster while the man is talking. Forge cancels the HOTBAR and
 * EXPERIENCE_BAR overlays; Fabric has no overlay-cancel hook, so the matching Gui
 * methods get cancelled instead. 1.20.1 draws the XP level inside renderExperienceBar,
 * so these two cover all three layers.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void forgottenman$hideHotbar(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
        if (forgottenman$dialogueOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void forgottenman$hideExperienceBar(GuiGraphics graphics, int x, CallbackInfo ci) {
        if (forgottenman$dialogueOpen()) {
            ci.cancel();
        }
    }

    private static boolean forgottenman$dialogueOpen() {
        return Minecraft.getInstance().screen instanceof ManDialogueScreen;
    }
}
