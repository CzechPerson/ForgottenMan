package com.forgottenman.network;

import com.forgottenman.ForgottenMan;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the dialogue ended or was dismissed; the man may leave */
public record ManDialogueFinishedPayload() implements CustomPacketPayload {
    public static final ManDialogueFinishedPayload INSTANCE = new ManDialogueFinishedPayload();
    public static final CustomPacketPayload.Type<ManDialogueFinishedPayload> TYPE =
            new CustomPacketPayload.Type<>(ForgottenMan.id("man_dialogue_finished"));
    public static final StreamCodec<ByteBuf, ManDialogueFinishedPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
