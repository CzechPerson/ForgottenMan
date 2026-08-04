package com.forgottenman.network;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: every door in the player's current dimension currently wearing
 * a portal. Sent whole rather than as deltas -- the set is a handful of positions
 * at most, and a full replace cannot drift out of sync.
 */
public record WildPortalsPayload(List<BlockPos> positions) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WildPortalsPayload> TYPE =
            new CustomPacketPayload.Type<>(ForgottenMan.id("wild_portals"));

    public static final StreamCodec<FriendlyByteBuf, WildPortalsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.positions().size());
                for (BlockPos pos : payload.positions()) {
                    buffer.writeBlockPos(pos);
                }
            },
            buffer -> {
                int count = buffer.readVarInt();
                List<BlockPos> positions = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    positions.add(buffer.readBlockPos());
                }
                return new WildPortalsPayload(positions);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
