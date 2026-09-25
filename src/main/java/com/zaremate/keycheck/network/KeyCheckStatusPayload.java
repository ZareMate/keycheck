package com.zaremate.keycheck.network;

import com.zaremate.keycheck.KeyCheck;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KeyCheckStatusPayload(int state, int completed, int total, int detected, int protectedCount)
        implements CustomPacketPayload {
    public static final int START = 0;
    public static final int PROGRESS = 1;
    public static final int COMPLETE = 2;
    public static final int FAILED = 3;

    public static final Type<KeyCheckStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KeyCheck.MOD_ID, "status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyCheckStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, KeyCheckStatusPayload::state,
                    ByteBufCodecs.VAR_INT, KeyCheckStatusPayload::completed,
                    ByteBufCodecs.VAR_INT, KeyCheckStatusPayload::total,
                    ByteBufCodecs.VAR_INT, KeyCheckStatusPayload::detected,
                    ByteBufCodecs.VAR_INT, KeyCheckStatusPayload::protectedCount,
                    KeyCheckStatusPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
