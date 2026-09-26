package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.AirportSecuritySystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class AirportSecuritySystemConfigAckPayload implements CustomPacketPayload {
    public static final Type<AirportSecuritySystemConfigAckPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AirportSecuritySystem.MOD_ID, "config_ack"));
    public static final AirportSecuritySystemConfigAckPayload INSTANCE = new AirportSecuritySystemConfigAckPayload();
    public static final StreamCodec<FriendlyByteBuf, AirportSecuritySystemConfigAckPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private AirportSecuritySystemConfigAckPayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
