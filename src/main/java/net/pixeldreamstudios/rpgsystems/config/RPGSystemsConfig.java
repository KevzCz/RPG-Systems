package net.pixeldreamstudios.rpgsystems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.rpgsystems.RPGSystems;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RPGSystemsConfig {

    public static final class Systems {
        public boolean party = true;
        public boolean pet = false;
        public boolean title = true;
    }

    public static final class Party {
        public boolean logChatToConsole = true;
        public boolean useFTBTeams = false;
        public boolean usePartyAddon = false;
    }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final String FILE_NAME = "rpgsystems.json";
    private static RPGSystemsConfig INSTANCE;

    public Systems systems = new Systems();
    public Party party = new Party();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("rpgsystems").resolve(FILE_NAME);
    }

    public static synchronized RPGSystemsConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static synchronized void load() {
        Path p = path();
        boolean needsSave = false;

        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                RPGSystemsConfig loaded = GSON.fromJson(r, RPGSystemsConfig.class);

                if (loaded == null) {
                    RPGSystems.LOGGER.warn("[Config] File corrupted, using defaults");
                    INSTANCE = new RPGSystemsConfig();
                    needsSave = true;
                } else {
                    INSTANCE = mergeWithDefaults(loaded);

                    String originalJson = GSON.toJson(loaded);
                    String mergedJson = GSON.toJson(INSTANCE);

                    if (!originalJson.equals(mergedJson)) {
                        RPGSystems.LOGGER.info("[Config] Added missing fields to config");
                        needsSave = true;
                    }
                }
            } catch (Throwable t) {
                RPGSystems.LOGGER.error("[Config] Failed to load, using defaults", t);
                INSTANCE = new RPGSystemsConfig();
                needsSave = true;
            }
        } else {

            RPGSystems.LOGGER.info("[Config] Creating default config");
            INSTANCE = new RPGSystemsConfig();
            needsSave = true;
        }

        if (needsSave) {
            save();
        }
    }

    private static RPGSystemsConfig mergeWithDefaults(RPGSystemsConfig loaded) {
        RPGSystemsConfig defaults = new RPGSystemsConfig();

        if (loaded.systems == null) {
            loaded.systems = defaults.systems;
        } else {

        }

        if (loaded.party == null) {
            loaded.party = defaults.party;
        } else {
        }

        return loaded;
    }

    public static synchronized void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(INSTANCE, w);
                w.flush();
            }
            RPGSystems.LOGGER.debug("[Config] Saved to {}", p);
        } catch (IOException e) {
            RPGSystems.LOGGER.error("[Config] Failed to save", e);
        }
    }

    public static synchronized void reload() {
        INSTANCE = null;
        load();
    }
}