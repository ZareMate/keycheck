package com.zaremate.keycheck.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KeyCheckLoadingScreen extends Screen {
    public KeyCheckLoadingScreen() {
        super(Component.literal("KeyCheck"));
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
        graphics.fill(0, 0, this.width, this.height, 0xA0101010);
        KeyCheckClient.render(graphics);
    }
}
