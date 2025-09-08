package net.pixeldreamstudios.rpgsystems.client.party.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PartyHudClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "rpgsystems_client.json";
    private static PartyHudClientConfig INSTANCE;

    public boolean hudEnabled = true;
    public boolean showArrows = true;
    public boolean showHpBar = true;
    public boolean showHungerBar = true;
    public boolean showStaminaBar = false;
    public boolean showManaBar = false;
    public boolean showRpgManaBar = false;

    public int partyHudX = -2;
    public int partyHudY = 10;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("rpgsystems")
                .resolve(FILE_NAME);
    }

    public static synchronized PartyHudClientConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static synchronized void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                INSTANCE = GSON.fromJson(r, PartyHudClientConfig.class);
            } catch (Throwable ignored) {
            }
        }
        if (INSTANCE == null) INSTANCE = new PartyHudClientConfig();
        INSTANCE.enforceMaxThreeBars();
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

    public void enforceMaxThreeBars() {
        int count = 0;
        if (showHpBar) count++;
        if (showHungerBar) count++;
        if (showStaminaBar) count++;
        if (showManaBar) count++;
        if (showRpgManaBar) count++;
        if (count <= 3) return;
        if (showRpgManaBar && count > 3) { showRpgManaBar = false; count--; }
        if (showManaBar    && count > 3) { showManaBar    = false; count--; }
        if (showStaminaBar && count > 3) { showStaminaBar = false; count--; }
        if (showHungerBar  && count > 3) { showHungerBar  = false; count--; }
        if (count > 3) showHpBar = false;
    }
}
