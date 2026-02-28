package net.pixeldreamstudios.rpgsystems.party;

import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.rpgsystems.RPGSystems;

public final class PartyAddonLoader {
    private static boolean initialized = false;

    public static void tryInitialize() {
        if (initialized) return;
        initialized = true;

        if (!FabricLoader.getInstance().isModLoaded("partyaddon")) {
            RPGSystems.LOGGER.info("PartyAddon not found, skipping integration");
            return;
        }

        try {
            PartyAddonIntegration.init();

            RPGSystems.LOGGER.info("PartyAddon integration initialized");
        } catch (Throwable t) {
            RPGSystems.LOGGER.error("Failed to initialize PartyAddon integration", t);
        }
    }
}
