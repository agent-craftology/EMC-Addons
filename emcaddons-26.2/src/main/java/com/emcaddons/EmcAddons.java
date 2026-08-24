package com.emcaddons;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmcAddons implements ModInitializer {
	public static final String MOD_ID = "emcaddons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[EMC Addons] Mod initialized");
	}
}
