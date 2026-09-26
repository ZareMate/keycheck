package com.zaremate.airport_security_system.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KeyCheckLoadingScreen extends Screen {
    public KeyCheckLoadingScreen() {
        super(Component.literal("Airport Security System"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        KeyCheckClient.render(graphics);
    }
}
