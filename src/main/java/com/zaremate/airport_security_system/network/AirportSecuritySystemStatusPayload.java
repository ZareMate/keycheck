package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.AirportSecuritySystem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AirportSecuritySystemStatusPayload(int state, int completed, int total, int detected, int protectedCount)
        implements CustomPacketPayload {
    public static final int START = 0;
    public static final int PROGRESS = 1;
    public static final int COMPLETE = 2;
    public static final int FAILED = 3;

    public static final Type<AirportSecuritySystemStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AirportSecuritySystem.MOD_ID, "status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AirportSecuritySystemStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AirportSecuritySystemStatusPayload::state,
                    ByteBufCodecs.VAR_INT, AirportSecuritySystemStatusPayload::completed,
                    ByteBufCodecs.VAR_INT, AirportSecuritySystemStatusPayload::total,
                    ByteBufCodecs.VAR_INT, AirportSecuritySystemStatusPayload::detected,
                    ByteBufCodecs.VAR_INT, AirportSecuritySystemStatusPayload::protectedCount,
                    AirportSecuritySystemStatusPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
