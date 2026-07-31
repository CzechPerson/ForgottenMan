package com.forgottenman.network;

import com.forgottenman.ForgottenMan;
import com.forgottenman.entity.ManEntity;
import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ForgottenMan.MOD_ID)
public final class ModNetworking {
    @SubscribeEvent
    static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenManDialoguePayload.TYPE, OpenManDialoguePayload.STREAM_CODEC,
                (payload, context) -> com.forgottenman.client.ClientDialogueHandler.openDialogue());
        registrar.playToServer(ManDialogueFinishedPayload.TYPE, ManDialogueFinishedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        finishDialogue(player);
                    }
                });
        registrar.playToServer(GiveEggPayload.TYPE, GiveEggPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        giveEgg(player);
                    }
                });
    }

    // Hands over the egg after checking the claim is legit: in the room, man nearby, not met yet
    private static void giveEgg(ServerPlayer player) {
        if (player.getData(ModAttachments.MET_MAN.get())
                || !(player.level() instanceof ServerLevel level)
                || level.dimension() != ModDimensions.TREE_ROOM
                || level.getEntitiesOfClass(ManEntity.class, player.getBoundingBox().inflate(16.0)).isEmpty()) {
            return;
        }
        ItemStack egg = new ItemStack(ModItems.EGG.get());
        if (!player.getInventory().add(egg)) {
            player.drop(egg, false);
        }
        level.playSound(null, player.blockPosition(), ModSounds.EGG_GET.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // Dialogue over: set MET_MAN and start the man's vanish
    private static void finishDialogue(ServerPlayer player) {
        player.setData(ModAttachments.MET_MAN.get(), true);
        if (player.level() instanceof ServerLevel level && level.dimension() == ModDimensions.TREE_ROOM) {
            AABB nearby = player.getBoundingBox().inflate(32.0);
            for (ManEntity man : level.getEntitiesOfClass(ManEntity.class, nearby)) {
                man.startVanishing();
            }
        }
    }

    private ModNetworking() {
    }
}
