package com.zaremate.keycheck.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "keycheck", bus = EventBusSubscriber.Bus.MOD)
public final class KeyCheckNetwork {
    private KeyCheckNetwork() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToClient(
                        KeyCheckStatusPayload.TYPE,
                        KeyCheckStatusPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> com.zaremate.keycheck.client.KeyCheckClient.handleStatus(payload)
                        )
                );
    }
}
