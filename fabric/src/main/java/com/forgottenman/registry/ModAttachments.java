package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.GlobalPos;

public final class ModAttachments {
    /**
     * The door the player last entered the tree room through; presence checked with
     * hasAttached. No default value, so an absent attachment reads as "never entered".
     */
    public static final AttachmentType<GlobalPos> ENTRY_DOOR = AttachmentRegistry.<GlobalPos>builder()
            .persistent(GlobalPos.CODEC)
            .copyOnDeath()
            .buildAndRegister(ForgottenMan.id("entry_door"));

    /** Whether this player has talked to the man behind the tree */
    public static final AttachmentType<Boolean> MET_MAN = AttachmentRegistry.<Boolean>builder()
            .initializer(() -> false)
            .persistent(Codec.BOOL)
            .copyOnDeath()
            .buildAndRegister(ForgottenMan.id("met_man"));

    public static void register() {
    }

    /** Null-safe read; an absent attachment means they have not met him */
    public static boolean hasMetMan(AttachmentTarget target) {
        return Boolean.TRUE.equals(target.getAttached(MET_MAN));
    }

    public static void setMetMan(AttachmentTarget target, boolean met) {
        target.setAttached(MET_MAN, met);
    }

    private ModAttachments() {
    }
}
