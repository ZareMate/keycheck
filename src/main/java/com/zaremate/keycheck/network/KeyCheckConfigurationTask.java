package com.zaremate.keycheck.network;

import com.zaremate.keycheck.KeyCheck;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

public record KeyCheckConfigurationTask() implements ICustomConfigurationTask {
    public static final Type TYPE =
            new Type(ResourceLocation.fromNamespaceAndPath(KeyCheck.MOD_ID, "client_check"));

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(KeyCheckConfigStartPayload.INSTANCE);
    }
}
