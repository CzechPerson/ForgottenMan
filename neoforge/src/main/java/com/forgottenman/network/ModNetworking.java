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
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 1.20.1 predates CustomPacketPayload, so the three signals travel on one
 * SimpleChannel. None of them carries data -- the message type is the message.
 */
public final class ModNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ForgottenMan.id("main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    /** Server to client: open the man's dialogue screen */
    public static final class OpenManDialogue {
        static final OpenManDialogue INSTANCE = new OpenManDialogue();
    }

    /** Client to server: the dialogue reached the offer, hand over the egg */
    public static final class GiveEgg {
        static final GiveEgg INSTANCE = new GiveEgg();
    }

    /** Client to server: the dialogue ended or was dismissed; the man may leave */
    public static final class ManDialogueFinished {
        static final ManDialogueFinished INSTANCE = new ManDialogueFinished();
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(OpenManDialogue.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> { })
                .decoder(buffer -> OpenManDialogue.INSTANCE)
                .consumerMainThread((message, context) -> {
                    // The client class is only named inside this lambda body, so a
                    // dedicated server never has to load it
                    com.forgottenman.client.ClientDialogueHandler.openDialogue();
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ManDialogueFinished.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> { })
                .decoder(buffer -> ManDialogueFinished.INSTANCE)
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        finishDialogue(player);
                    }
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(GiveEgg.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> { })
                .decoder(buffer -> GiveEgg.INSTANCE)
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        giveEgg(player);
                    }
                    context.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendOpenDialogue(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenManDialogue.INSTANCE);
    }

    public static void sendGiveEgg() {
        CHANNEL.sendToServer(GiveEgg.INSTANCE);
    }

    public static void sendDialogueFinished() {
        CHANNEL.sendToServer(ManDialogueFinished.INSTANCE);
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

    private ModNetworking() {
    }
}
