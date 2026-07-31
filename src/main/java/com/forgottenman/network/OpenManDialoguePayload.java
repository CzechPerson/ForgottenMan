package com.forgottenman.network;

import com.forgottenman.ForgottenMan;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: open the man's dialogue screen */
public record OpenManDialoguePayload() implements CustomPacketPayload {
    public static final OpenManDialoguePayload INSTANCE = new OpenManDialoguePayload();
    public static final CustomPacketPayload.Type<OpenManDialoguePayload> TYPE =
            new CustomPacketPayload.Type<>(ForgottenMan.id("open_man_dialogue"));
    public static final StreamCodec<ByteBuf, OpenManDialoguePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
