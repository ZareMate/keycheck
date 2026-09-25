package com.zaremate.keycheck;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class KeyCheckConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_KEYS =
            BUILDER.comment(
                    "Probe entries in the format TYPE|TRANSLATION_KEY.",
                    "Types: KEYBIND, TRANSLATE, METEOR.",
                    "Example: METEOR|key.meteor-client.open-gui.",
                    "A bare key is accepted for backwards compatibility and is treated as KEYBIND."
            ).defineListAllowEmpty(
                    "blacklisted_keys",
                    List.of("METEOR|key.meteor-client.open-gui"),
                    () -> "",
                    value -> value instanceof String s && !s.isBlank()
            );

    public static final ModConfigSpec.BooleanValue AUTO_CHECK_ON_JOIN =
            BUILDER.comment("Automatically check players after they join the server.")
                    .define("auto_check_on_join", true);

    public static final ModConfigSpec.BooleanValue ONLY_FIRST_JOIN =
            BUILDER.comment("When enabled, automatically check each UUID only once while the server is running.")
                    .define("only_first_join", false);

    public static final ModConfigSpec.IntValue JOIN_CHECK_DELAY_TICKS =
            BUILDER.comment("Ticks to wait after a player joins before starting the automatic check.")
                    .defineInRange("join_check_delay_ticks", 60, 0, 1200);

    public static final ModConfigSpec.BooleanValue WEBHOOK_ENABLED =
            BUILDER.comment("Send every completed check result to Discord.")
                    .define("webhook_enabled", false);

    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL =
            BUILDER.comment("Discord webhook URL. Keep this private.")
                    .define("webhook_url", "");

    public static final ModConfigSpec.IntValue TIMEOUT_TICKS =
            BUILDER.comment("Ticks to wait for a client response for each batch.")
                    .defineInRange("timeout_ticks", 60, 10, 200);

    public static final ModConfigSpec.BooleanValue LOG_CLEAN_CHECKS =
            BUILDER.comment("Log checks that found no blacklisted keybinds.")
                    .define("log_clean_checks", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private KeyCheckConfig() {}

    public static List<CheckProbe> probes() {
        return BLACKLISTED_KEYS.get().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(KeyCheckConfig::parseProbe)
                .toList();
    }

    private static CheckProbe parseProbe(String value) {
        int separator = value.indexOf('|');
        if (separator < 0)
            return new CheckProbe(CheckType.KEYBIND, value);

        String type = value.substring(0, separator).trim();
        String key = value.substring(separator + 1).trim();

        if (key.isEmpty())
            return new CheckProbe(CheckType.KEYBIND, value);

        return new CheckProbe(CheckType.parse(type), key);
    }

    public static List<String> blacklistedKeys() {
        return probes().stream().map(CheckProbe::key).toList();
    }

    public static String webhookUrl() {
        return WEBHOOK_URL.get().trim();
    }

    public record CheckProbe(CheckType type, String key) {
        public Component component() {
            return switch (type) {
                case KEYBIND -> Component.keybind(key);
                case TRANSLATE -> Component.translatableWithFallback(key, "⟦KC_" + safeId() + "⟧");
                case METEOR -> Component.translatableWithFallback(key, key);
            };
        }

        public String fallback() {
            return switch (type) {
                case KEYBIND -> key;
                case TRANSLATE -> "⟦KC_" + safeId() + "⟧";
                case METEOR -> key;
            };
        }

        private String safeId() {
            return Integer.toUnsignedString(key.hashCode(), 16);
        }

        @Override
        public String toString() {
            return type + "|" + key;
        }
    }
}
