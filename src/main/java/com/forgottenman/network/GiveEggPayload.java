package com.forgottenman.network;

import com.forgottenman.ForgottenMan;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the dialogue reached the offer, hand over the egg */
public record GiveEggPayload() implements CustomPacketPayload {
    public static final GiveEggPayload INSTANCE = new GiveEggPayload();
    public static final CustomPacketPayload.Type<GiveEggPayload> TYPE =
            new CustomPacketPayload.Type<>(ForgottenMan.id("give_egg"));
    public static final StreamCodec<ByteBuf, GiveEggPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
