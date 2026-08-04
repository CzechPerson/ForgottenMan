package com.forgottenman.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: every door in the player's current dimension currently wearing
 * a portal. Sent whole rather than as deltas -- the set is a handful of positions
 * at most, and a full replace cannot drift out of sync.
 *
 * A plain SimpleChannel message here; 1.20.1 has no CustomPacketPayload.
 */
public final class WildPortalsPayload {
    private final List<BlockPos> positions;

    public WildPortalsPayload(List<BlockPos> positions) {
        this.positions = positions;
    }

    public List<BlockPos> positions() {
        return positions;
    }

    public static void encode(WildPortalsPayload message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.positions.size());
        for (BlockPos pos : message.positions) {
            buffer.writeBlockPos(pos);
        }
    }

    public static WildPortalsPayload decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<BlockPos> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(buffer.readBlockPos());
        }
        return new WildPortalsPayload(positions);
    }
}
