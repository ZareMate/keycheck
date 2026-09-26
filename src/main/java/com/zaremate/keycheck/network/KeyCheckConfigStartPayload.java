package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.KeyCheck;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class KeyCheckConfigStartPayload implements CustomPacketPayload {
    public static final Type<KeyCheckConfigStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KeyCheck.MOD_ID, "config_start"));
    public static final KeyCheckConfigStartPayload INSTANCE = new KeyCheckConfigStartPayload();
    public static final StreamCodec<FriendlyByteBuf, KeyCheckConfigStartPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private KeyCheckConfigStartPayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
