package com.zaremate.airport_security_system.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "airport_security_system", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class KeyCheckClientGame {
    private KeyCheckClientGame() {}

    @SubscribeEvent
    public static void renderGui(RenderGuiEvent.Post event) {
        KeyCheckClient.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyCheckClient.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        KeyCheckClient.reset();
    }
}
