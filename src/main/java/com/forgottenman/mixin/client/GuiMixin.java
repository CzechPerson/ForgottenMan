package com.forgottenman.mixin.client;

import com.forgottenman.client.gui.ManDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the hotbar cluster while the man is talking. NeoForge cancels the HOTBAR,
 * EXPERIENCE_BAR and EXPERIENCE_LEVEL gui layers; Fabric has no layer-cancel hook in
 * 1.21.1, so the three matching Gui methods get cancelled instead.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void forgottenman$hideHotbar(CallbackInfo ci) {
        if (forgottenman$dialogueOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void forgottenman$hideExperienceBar(CallbackInfo ci) {
        if (forgottenman$dialogueOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void forgottenman$hideExperienceLevel(CallbackInfo ci) {
        if (forgottenman$dialogueOpen()) {
            ci.cancel();
        }
    }

    private static boolean forgottenman$dialogueOpen() {
        return Minecraft.getInstance().screen instanceof ManDialogueScreen;
    }
}
