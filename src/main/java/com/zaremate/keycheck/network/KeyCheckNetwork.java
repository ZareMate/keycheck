package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.KeyCheckConfig;
import com.zaremate.airport_security_system.client.KeyCheckClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "airport_security_system", bus = EventBusSubscriber.Bus.MOD)
public final class KeyCheckNetwork {
    private KeyCheckNetwork() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();

        registrar.playToClient(
                KeyCheckStatusPayload.TYPE,
                KeyCheckStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> KeyCheckClient.handleStatus(payload)
                )
        );

        registrar.configurationToClient(
                KeyCheckConfigStartPayload.TYPE,
                KeyCheckConfigStartPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    KeyCheckClient.beginEarlyLoading();
                    context.reply(KeyCheckConfigAckPayload.INSTANCE);
                })
        );

        registrar.configurationToServer(
                KeyCheckConfigAckPayload.TYPE,
                KeyCheckConfigAckPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        context.finishCurrentTask(KeyCheckConfigurationTask.TYPE))
        );
    }

    @SubscribeEvent
    public static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        if (!KeyCheckConfig.AUTO_CHECK_ON_JOIN.get()) return;
        if (KeyCheckConfig.blacklistedProbes().isEmpty()) return;

        var listener = event.getListener();
        if (listener.getConnection().isMemoryConnection()) return;
        if (!listener.hasChannel(KeyCheckConfigStartPayload.TYPE)
                || !listener.hasChannel(KeyCheckConfigAckPayload.TYPE)) {
            return;
        }

        event.register(new KeyCheckConfigurationTask());
    }
}
