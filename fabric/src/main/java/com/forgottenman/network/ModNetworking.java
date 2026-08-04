package com.forgottenman.network;

import com.forgottenman.entity.ManEntity;
import com.forgottenman.portal.WildPortals;
import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ModNetworking {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenManDialoguePayload.TYPE, OpenManDialoguePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ManDialogueFinishedPayload.TYPE, ManDialogueFinishedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(GiveEggPayload.TYPE, GiveEggPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(WildPortalsPayload.TYPE, WildPortalsPayload.STREAM_CODEC);

        // The client handler for OpenManDialoguePayload is registered from the client
        // entrypoint, keeping ClientDialogueHandler off the dedicated server's path
        ServerPlayNetworking.registerGlobalReceiver(ManDialogueFinishedPayload.TYPE,
                (payload, context) -> context.server().execute(() -> finishDialogue(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(GiveEggPayload.TYPE,
                (payload, context) -> context.server().execute(() -> giveEgg(context.player())));
    }

    // Hands over the egg after checking the claim is legit: in the room, man nearby, not met yet
    private static void giveEgg(ServerPlayer player) {
        if (ModAttachments.hasMetMan(player)
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
        ModAttachments.setMetMan(player, true);
        if (player.level() instanceof ServerLevel level && level.dimension() == ModDimensions.TREE_ROOM) {
            AABB nearby = player.getBoundingBox().inflate(32.0);
            for (ManEntity man : level.getEntitiesOfClass(ManEntity.class, nearby)) {
                man.startVanishing();
            }
        }
    }

    /** Pushes the armed doors of one dimension to everyone standing in it */
    public static void syncWildPortals(ServerLevel level) {
        List<BlockPos> positions = List.copyOf(WildPortals.armedIn(level.dimension()));
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, new WildPortalsPayload(positions));
        }
    }

    /** One player, for joins and dimension changes */
    public static void syncWildPortals(ServerPlayer player) {
        ServerPlayNetworking.send(player,
                new WildPortalsPayload(List.copyOf(WildPortals.armedIn(player.level().dimension()))));
    }

    private ModNetworking() {
    }
}
