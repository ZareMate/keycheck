package com.zaremate.airport_security_system;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class AirportSecuritySystemConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_KEYS =
            BUILDER.comment(
                    "Probe entries in the format TYPE:TRANSLATION_KEY.",
                    "Types: KEYBIND, TRANSLATE, METEOR.",
                    "Example: METEOR:key.meteor-client.open-gui",
                    "A bare key is accepted for backwards compatibility."
            ).defineListAllowEmpty(
                    "blacklisted_keys",
                    List.of(
                            "METEOR:key.meteor-client.open-gui",
                            "KEYBIND:xray.config.toggle",
                            "TRANSLATE:bleachhack.module.killaura",
                            "TRANSLATE:emc.module.killaura.name",
                            "TRANSLATE:coffee.module.killaura.name",
                            "TRANSLATE:key.wdl.startStop",
                            "TRANSLATE:autoclicker-fabric.hud.holding",
                            "TRANSLATE:key.antiafk.toggle",
                            "KEYBIND:key.auto-clicker_.toggle",
                            "TRANSLATE:addon.trouser-streak.name",
                            "TRANSLATE:module.trouser-streak.NewerNewChunks.description",
                            "TRANSLATE:module.trouser-streak.AnHero.description",
                            "TRANSLATE:key.ui-utils.restore_screen",
                            "TRANSLATE:cracker.already",
                            "TRANSLATE:swd.screen.config.title",
                            "TRANSLATE:addon.glazed.name",
                            "TRANSLATE:itemscroller.hotkey.name.openconfigscreen",
                            "TRANSLATE:baritone.setting.allowBreak",
                            "KEYBIND:key.freecam.toggle",
                            "KEYBIND:key.wurst.zoom",
                            "KEYBIND:key.killaura",
                            "KEYBIND:key.autoswitch.toggle",
                            "KEYBIND:key.antiafk.toggle",
                            "KEYBIND:key.lumina.open_click_gui",
                            "KEYBIND:key.chestesp.toggle",
                            "KEYBIND:key.autofish.open_gui"
                    ),
                    () -> "",
                    value -> value instanceof String s && !s.isBlank()
            );

    public static final ModConfigSpec.ConfigValue<String> COMMAND_PERMISSION =
            BUILDER.comment("LuckPerms permission required to use /airport_security_system.")
                    .define("command_permission", "airport_security_system.command");

    public static final ModConfigSpec.ConfigValue<String> JOIN_BYPASS_PERMISSION =
            BUILDER.comment("LuckPerms permission that exempts a player from automatic join checks.")
                    .define("join_bypass_permission", "airport_security_system.join.bypass");

    public static final ModConfigSpec.ConfigValue<String> BROADCAST_PERMISSION =
            BUILDER.comment("LuckPerms permission that allows receiving Airport Security System result broadcasts.")
                    .define("broadcast_permission", "airport_security_system.alerts");

    public static final ModConfigSpec.BooleanValue AUTO_CHECK_ON_JOIN =
            BUILDER.comment("Automatically check players after they join the server.")
                    .define("auto_check_on_join", true);

    public static final ModConfigSpec.IntValue JOIN_CHECK_CHANCE_PERCENT =
            BUILDER.comment("Chance in percent that an eligible player is automatically checked on join.")
                    .defineInRange("join_check_chance_percent", 10, 0, 100);

    public static final ModConfigSpec.BooleanValue ONLY_FIRST_JOIN =
            BUILDER.comment("When enabled, automatically check each UUID only once while the server is running.")
                    .define("only_first_join", false);

    public static final ModConfigSpec.IntValue JOIN_CHECK_DELAY_TICKS =
            BUILDER.comment("Ticks to wait after a player joins before starting the automatic check.")
                    .defineInRange("join_check_delay_ticks", 120, 0, 1200);

    public static final ModConfigSpec.BooleanValue WEBHOOK_ENABLED =
            BUILDER.comment("Send every completed check result to Discord.")
                    .define("webhook_enabled", false);

    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL =
            BUILDER.comment("Discord webhook URL. Keep this private.")
                    .define("webhook_url", "");

    public static final ModConfigSpec.IntValue TIMEOUT_TICKS =
            BUILDER.comment("Ticks to wait for a client response for each batch.")
                    .defineInRange("timeout_ticks", 120, 10, 200);

    public static final ModConfigSpec.BooleanValue LOG_CLEAN_CHECKS =
            BUILDER.comment("Log checks that found no blacklisted keybinds.")
                    .define("log_clean_checks", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AirportSecuritySystemConfig() {}

    public static List<AirportSecuritySystemProbe> blacklistedProbes() {
        return BLACKLISTED_KEYS.get().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AirportSecuritySystemProbe::parse)
                .toList();
    }


    public static String webhookUrl() {
        return WEBHOOK_URL.get().trim();
    }
}
