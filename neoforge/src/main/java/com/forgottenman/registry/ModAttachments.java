package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ForgottenMan.MOD_ID);

    /** The door the player last entered the tree room through; presence checked with hasData */
    public static final Supplier<AttachmentType<GlobalPos>> ENTRY_DOOR = ATTACHMENTS.register("entry_door",
            () -> AttachmentType.builder(() -> GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))
                    .serialize(GlobalPos.CODEC)
                    .copyOnDeath()
                    .build());

    /** Whether this player has talked to the man behind the tree */
    public static final Supplier<AttachmentType<Boolean>> MET_MAN = ATTACHMENTS.register("met_man",
            () -> AttachmentType.builder(() -> false)
                    .serialize(com.mojang.serialization.Codec.BOOL)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }
}
