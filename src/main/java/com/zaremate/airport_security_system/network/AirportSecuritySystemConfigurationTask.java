package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.AirportSecuritySystem;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

public record AirportSecuritySystemConfigurationTask() implements ICustomConfigurationTask {
    public static final Type TYPE =
            new Type(ResourceLocation.fromNamespaceAndPath(AirportSecuritySystem.MOD_ID, "client_check"));

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(AirportSecuritySystemConfigStartPayload.INSTANCE);
    }
}
