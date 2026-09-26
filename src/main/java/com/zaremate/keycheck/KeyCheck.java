package com.zaremate.airport_security_system;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(KeyCheck.MOD_ID)
public final class KeyCheck {
    public static final String MOD_ID = "airport_security_system";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KeyCheck(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, KeyCheckConfig.SPEC);
        NeoForge.EVENT_BUS.register(KeyCheckEvents.class);
        LOGGER.info("Airport Security System loaded.");
    }
}
