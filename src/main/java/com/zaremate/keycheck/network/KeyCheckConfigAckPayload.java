package com.zaremate.airport_security.network;

import com.zaremate.airport_security.KeyCheck;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class KeyCheckConfigAckPayload implements CustomPacketPayload {
    public static final Type<KeyCheckConfigAckPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KeyCheck.MOD_ID, "config_ack"));
    public static final KeyCheckConfigAckPayload INSTANCE = new KeyCheckConfigAckPayload();
    public static final StreamCodec<FriendlyByteBuf, KeyCheckConfigAckPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private KeyCheckConfigAckPayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
