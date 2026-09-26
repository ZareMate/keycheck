package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.AirportSecuritySystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class AirportSecuritySystemConfigStartPayload implements CustomPacketPayload {
    public static final Type<AirportSecuritySystemConfigStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AirportSecuritySystem.MOD_ID, "config_start"));
    public static final AirportSecuritySystemConfigStartPayload INSTANCE = new AirportSecuritySystemConfigStartPayload();
    public static final StreamCodec<FriendlyByteBuf, AirportSecuritySystemConfigStartPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private AirportSecuritySystemConfigStartPayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
