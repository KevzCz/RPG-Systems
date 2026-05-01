package net.pixeldreamstudios.rpgsystems.party;

import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;

public final class FTBTeamsLoader {
    private static boolean initialized = false;

    /**
     * Safe to call even when FTB Teams is not installed — does NOT load FTBTeamsIntegration.
     * Use this as the guard everywhere instead of FTBTeamsIntegration.isEnabled().
     */
    public static boolean isEnabled() {
        return FabricLoader.getInstance().isModLoaded("ftbteams") && RPGSystemsConfig.get().party.useFTBTeams;
    }

    public static void tryInitialize() {
        if (initialized) return;
        initialized = true;

        if (!FabricLoader.getInstance().isModLoaded("ftbteams")) {
            RPGSystems.LOGGER.info("FTB Teams not found, using native party system");
            return;
        }

        try {
            FTBTeamsIntegration.init();
            FTBTeamsEventListener.register();
            FTBTeamsChatBridge.register();

            RPGSystems.LOGGER.info("FTB Teams integration initialized");
        } catch (Throwable t) {
            RPGSystems.LOGGER.error("Failed to initialize FTB Teams integration", t);
        }
    }
}