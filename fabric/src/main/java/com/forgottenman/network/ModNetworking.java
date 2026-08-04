package com.forgottenman.network;

import com.forgottenman.ForgottenMan;
import com.forgottenman.portal.WildPortals;
import com.forgottenman.entity.ManEntity;
import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModItems;
import com.forgottenman.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.20.1 predates CustomPacketPayload, so the three signals are plain channel ids
 * carrying an empty buffer. None of them needs data -- the channel is the message.
 */
public final class ModNetworking {
    /** Server to client: open the man's dialogue screen */
    public static final ResourceLocation OPEN_MAN_DIALOGUE = ForgottenMan.id("open_man_dialogue");
    /** Client to server: the dialogue ended or was dismissed; the man may leave */
    public static final ResourceLocation MAN_DIALOGUE_FINISHED = ForgottenMan.id("man_dialogue_finished");
    /** Client to server: the dialogue reached the offer, hand over the egg */
    public static final ResourceLocation GIVE_EGG = ForgottenMan.id("give_egg");
    /** Server to client: which doors currently wear a portal, whole set each time */
    public static final ResourceLocation WILD_PORTALS = ForgottenMan.id("wild_portals");

    public static void register() {
        // The client handler for OPEN_MAN_DIALOGUE is registered from the client
        // entrypoint, keeping ClientDialogueHandler off the dedicated server's path
        ServerPlayNetworking.registerGlobalReceiver(MAN_DIALOGUE_FINISHED,
                (server, player, handler, buf, sender) -> server.execute(() -> finishDialogue(player)));
        ServerPlayNetworking.registerGlobalReceiver(GIVE_EGG,
                (server, player, handler, buf, sender) -> server.execute(() -> giveEgg(player)));
    }

    /** Pushes the armed doors of one dimension to everyone standing in it */
    public static void syncWildPortals(ServerLevel level) {
        List<BlockPos> positions = List.copyOf(WildPortals.armedIn(level.dimension()));
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, WILD_PORTALS, writePositions(positions));
        }
    }

    /** One player, for joins and dimension changes */
    public static void syncWildPortals(ServerPlayer player) {
        ServerPlayNetworking.send(player, WILD_PORTALS,
                writePositions(List.copyOf(WildPortals.armedIn(player.level().dimension()))));
    }

    private static FriendlyByteBuf writePositions(List<BlockPos> positions) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buffer.writeBlockPos(pos);
        }
        return buffer;
    }

    /** Reads what writePositions wrote; used by the client receiver */
    public static List<BlockPos> readPositions(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<BlockPos> positions = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(buffer.readBlockPos());
        }
        return positions;
    }

    public static void sendOpenDialogue(ServerPlayer player) {
        ServerPlayNetworking.send(player, OPEN_MAN_DIALOGUE, PacketByteBufs.empty());
    }

    // The two client-to-server sends live in ManDialogueScreen; ClientPlayNetworking
    // is stripped on a dedicated server, so it stays out of this common class

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
