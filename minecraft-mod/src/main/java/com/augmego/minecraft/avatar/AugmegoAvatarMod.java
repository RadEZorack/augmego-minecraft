package com.augmego.minecraft.avatar;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AugmegoAvatarMod implements ModInitializer {
    public static final String MOD_ID = "augmegoavatar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing avatar proof of concept");
    }
}
