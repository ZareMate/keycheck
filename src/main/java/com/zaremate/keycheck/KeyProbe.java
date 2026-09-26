package com.zaremate.airport_security;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public record KeyProbe(String key, Mode mode) {
    public enum Mode {
        METEOR,
        TRANSLATE,
        KEYBIND
    }

    public static KeyProbe parse(String raw) {
        String value = raw.trim();
        int separator = value.indexOf(':');

        if (separator > 0) {
            String prefix = value.substring(0, separator).trim().toUpperCase(Locale.ROOT);
            String key = value.substring(separator + 1).trim();

            try {
                return new KeyProbe(key, Mode.valueOf(prefix));
            } catch (IllegalArgumentException ignored) {
                // Keep backwards-compatible behavior for an unrecognized prefix.
            }
        }

        if (value.equalsIgnoreCase("key.meteor-client.open-gui"))
            return new KeyProbe(value, Mode.METEOR);

        // Advanced XRay exposes these as translation keys, not keybind names.
        if (value.equalsIgnoreCase("xray.debug.init")
                || value.equalsIgnoreCase("xray.overlay")) {
            return new KeyProbe(value, Mode.TRANSLATE);
        }

        return new KeyProbe(value, Mode.KEYBIND);
    }

    public Component component() {
        return switch (mode) {
            case METEOR, TRANSLATE ->
                    Component.translatableWithFallback(key, fallback());
            case KEYBIND ->
                    Component.keybind(key);
        };
    }

    public String fallback() {
        return "⟦NO_" + key.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_") + "⟧";
    }
}
