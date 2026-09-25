package com.zaremate.keycheck.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "keycheck", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class KeyCheckClientGame {
    private KeyCheckClientGame() {}

    @SubscribeEvent
    public static void renderGui(RenderGuiEvent.Post event) {
        KeyCheckClient.render(event.getGuiGraphics());
    }
}
