package com.zaremate.airport_security_system.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AirportSecuritySystemLoadingScreen extends Screen {
    public AirportSecuritySystemLoadingScreen() {
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
}
