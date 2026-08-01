package com.forgottenman.client;

import com.forgottenman.client.gui.ManDialogueScreen;
import net.minecraft.client.Minecraft;

/** Client-only bridge for the networking layer */
public final class ClientDialogueHandler {
    public static void openDialogue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new ManDialogueScreen());
        }
    }

    private ClientDialogueHandler() {
    }
}
