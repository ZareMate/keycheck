package com.zaremate.keycheck.client;

import com.zaremate.keycheck.network.KeyCheckConfigAckPayload;
import com.zaremate.keycheck.network.KeyCheckConfigStartPayload;
import com.zaremate.keycheck.network.KeyCheckStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = "keycheck", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class KeyCheckClient {
    private static volatile boolean active;
    private static volatile boolean earlyLoadingActive;
    private static volatile boolean blockingScreenRequested;
    private static volatile int state;
    private static volatile int completed;
    private static volatile int total;
    private static volatile int detected;
    private static volatile int protectedCount;
    private static volatile long hideAtNanos;

    private KeyCheckClient() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(KeyCheckConfigStartPayload.TYPE, (payload, context) -> {
            beginEarlyLoading();
            context.reply(KeyCheckConfigAckPayload.INSTANCE);
        });
        event.register(KeyCheckStatusPayload.TYPE, (payload, context) -> handleStatus(payload));
    }

    public static void handleStatus(KeyCheckStatusPayload payload) {
        active = payload.state() != KeyCheckStatusPayload.COMPLETE
                && payload.state() != KeyCheckStatusPayload.FAILED;
        state = payload.state();
        completed = payload.completed();
        total = payload.total();
        detected = payload.detected();
        protectedCount = payload.protectedCount();

        if (active && earlyLoadingActive)
            blockingScreenRequested = true;

        if (!active) {
            earlyLoadingActive = false;
            blockingScreenRequested = false;
            hideAtNanos = System.nanoTime() + 800_000_000L;
            closeBlockingScreen();
        }
    }

    public static void reset() {
        active = false;
        earlyLoadingActive = false;
        blockingScreenRequested = false;
        state = 0;
        completed = 0;
        total = 0;
        detected = 0;
        protectedCount = 0;
        hideAtNanos = 0;
        closeBlockingScreen();
    }

    public static void tick() {
        if (!blockingScreenRequested || !active) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return;
        if (minecraft.screen instanceof KeyCheckLoadingScreen) return;

        minecraft.setScreen(new KeyCheckLoadingScreen());
    }

    public static void render(GuiGraphics graphics) {
        if (!active && System.nanoTime() >= hideAtNanos)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.font == null)
            return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int boxWidth = Math.min(420, width - 40);
        int boxHeight = 112;
        int left = (width - boxWidth) / 2;
        int top = Math.max(24, height / 2 - boxHeight / 2);

        graphics.fill(left + 2, top + 2, left + boxWidth + 2, top + boxHeight + 2, 0x55000000);
        graphics.fill(left, top, left + boxWidth, top + boxHeight, 0xE8101010);
        graphics.fill(left, top, left + boxWidth, top + 2, 0xFF4FA3FF);

        graphics.drawCenteredString(
                minecraft.font,
                "KeyCheck",
                width / 2,
                top + 14,
                0xFFFFFFFF
        );

        String title = switch (state) {
            case KeyCheckStatusPayload.COMPLETE -> "Check complete";
            case KeyCheckStatusPayload.FAILED -> "Check could not be completed";
            default -> "Verifying your client";
        };

        graphics.drawCenteredString(
                minecraft.font,
                title,
                width / 2,
                top + 34,
                0xFFE0E0E0
        );

        if (total > 0) {
            int barLeft = left + 28;
            int barRight = left + boxWidth - 28;
            int barTop = top + 57;
            int barBottom = barTop + 10;
            int progress = Math.max(0, Math.min(total, completed));
            int fillRight = barLeft + (barRight - barLeft) * progress / total;

            graphics.fill(barLeft, barTop, barRight, barBottom, 0xFF303030);
            if (fillRight > barLeft)
                graphics.fill(barLeft, barTop, fillRight, barBottom, 0xFF4FA3FF);

            graphics.drawCenteredString(
                    minecraft.font,
                    completed + " / " + total + " probes",
                    width / 2,
                    top + 74,
                    0xFFBDBDBD
            );
        }

        if (detected > 0 || protectedCount > 0) {
            String result = "Findings: " + detected + " detected";
            if (protectedCount > 0)
                result += ", " + protectedCount + " protected";

            graphics.drawCenteredString(
                    minecraft.font,
                    result,
                    width / 2,
                    top + 93,
                    0xFFFFD166
            );
        } else {
            graphics.drawCenteredString(
                    minecraft.font,
                    "Please wait...",
                    width / 2,
                    top + 93,
                    0xFF8A8A8A
            );
        }
    }

    private static void beginEarlyLoading() {
        active = true;
        earlyLoadingActive = true;
        blockingScreenRequested = true;
        state = KeyCheckStatusPayload.START;
        completed = 0;
        total = 0;
        detected = 0;
        protectedCount = 0;
        hideAtNanos = 0;
    }

    private static void closeBlockingScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen instanceof KeyCheckLoadingScreen)
            minecraft.setScreen(null);
    }
}
