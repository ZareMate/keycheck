package com.zaremate.airport_security_system.network;

import com.zaremate.airport_security_system.AirportSecuritySystemConfig;
import com.zaremate.airport_security_system.client.AirportSecuritySystemClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "airport_security_system", bus = EventBusSubscriber.Bus.MOD)
public final class AirportSecuritySystemNetwork {
    private AirportSecuritySystemNetwork() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();

        registrar.playToClient(
                AirportSecuritySystemStatusPayload.TYPE,
                AirportSecuritySystemStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> AirportSecuritySystemClient.handleStatus(payload)
                )
        );

        registrar.configurationToClient(
                AirportSecuritySystemConfigStartPayload.TYPE,
                AirportSecuritySystemConfigStartPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    AirportSecuritySystemClient.beginEarlyLoading();
                    context.reply(AirportSecuritySystemConfigAckPayload.INSTANCE);
                })
        );

        registrar.configurationToServer(
                AirportSecuritySystemConfigAckPayload.TYPE,
                AirportSecuritySystemConfigAckPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        context.finishCurrentTask(AirportSecuritySystemConfigurationTask.TYPE))
        );
    }

    @SubscribeEvent
    public static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        if (!AirportSecuritySystemConfig.AUTO_CHECK_ON_JOIN.get()) return;
        if (AirportSecuritySystemConfig.blacklistedProbes().isEmpty()) return;

        var listener = event.getListener();
        if (listener.getConnection().isMemoryConnection()) return;
        if (!listener.hasChannel(AirportSecuritySystemConfigStartPayload.TYPE)
                || !listener.hasChannel(AirportSecuritySystemConfigAckPayload.TYPE)) {
            return;
        }

        event.register(new AirportSecuritySystemConfigurationTask());
    }
}
