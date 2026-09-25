package com.zaremate.keycheck;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class KeyCheckConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_KEYS =
            BUILDER.comment(
                    "Keybind translation keys to probe on clients.",
                    "Example: key.meteor-client.open-gui",
                    "Add one translation key per entry."
            ).defineListAllowEmpty(
                    "blacklisted_keys",
                    List.of("key.meteor-client.open-gui"),
                    () -> "",
                    value -> value instanceof String s && !s.isBlank()
            );

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

    public static List<String> blacklistedKeys() {
        return BLACKLISTED_KEYS.get().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static List<KeyProbe> blacklistedProbes() {
        return blacklistedKeys().stream()
                .map(KeyProbe::parse)
                .filter(probe -> !probe.key().isBlank())
                .toList();
    }

    public static String webhookUrl() {
        return WEBHOOK_URL.get().trim();
    }
}
