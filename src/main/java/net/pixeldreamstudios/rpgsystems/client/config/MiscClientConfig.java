package net.pixeldreamstudios.rpgsystems.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MiscClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "rpgsystems_misc_client.json";
    private static MiscClientConfig INSTANCE;

    public int inviteHudX = 0;
    public int inviteHudY = 10;

    public int joinRequestHudX = 0;
    public int joinRequestHudY = 10;

    public int inviteInventoryX = 8;
    public int inviteInventoryY = 8;

    public int handledTitlesBtnOffsetX = 64;
    public int handledTitlesBtnOffsetY = 67;

    public int handledPartyBtnOffsetX = 28;
    public int handledPartyBtnOffsetY = 67;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("rpgsystems")
                .resolve(FILE_NAME);
    }

    public static synchronized MiscClientConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static synchronized void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                INSTANCE = GSON.fromJson(r, MiscClientConfig.class);
            } catch (Throwable ignored) {
            }
        }
        if (INSTANCE == null) INSTANCE = new MiscClientConfig();
        save();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path())) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Throwable ignored) {
        }
    }

    public void resetDefaults() {
        inviteHudX = 0;
        inviteHudY = 10;
        joinRequestHudX = 0;
        joinRequestHudY = 10;
        inviteInventoryX = 8;
        inviteInventoryY = 8;
        handledTitlesBtnOffsetX = 64;
        handledTitlesBtnOffsetY = 67;
        handledPartyBtnOffsetX = 28;
        handledPartyBtnOffsetY = 67;
    }
}
