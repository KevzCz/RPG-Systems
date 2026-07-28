package net.pixeldreamstudios.rpgsystems.party;

import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;

public final class VanillaTeamsLoader {
    private static boolean initialized = false;

    public static boolean isEnabled() {
        return RPGSystemsConfig.get().party.useVanillaTeams;
    }

    public static void tryInitialize() {
        if (initialized) return;
        initialized = true;

        if (!isEnabled()) {
            RPGSystems.LOGGER.info("Vanilla Teams party source disabled in config");
            return;
        }

        RPGSystems.LOGGER.info("Vanilla Teams party source enabled");
    }
}
