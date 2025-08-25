package net.pixeldreamstudios.rpgsystems.client.enemy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HealingNumbersClientConfig {
    public enum Mode { NONE, PLAYERS, SPELLS, ALL_ENTITIES }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("rpgsystems").resolve("healing_numbers_client.json");

    private static HealingNumbersClientConfig INSTANCE;
    public Mode mode = Mode.ALL_ENTITIES;
    public double allEntitiesViewDistance = 48.0;

    private HealingNumbersClientConfig() {}

    public static HealingNumbersClientConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                INSTANCE = GSON.fromJson(Files.readString(FILE), HealingNumbersClientConfig.class);
            } else {
                INSTANCE = new HealingNumbersClientConfig();
                save();
            }
        } catch (IOException e) {
            INSTANCE = new HealingNumbersClientConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException ignored) { }
    }

    public double viewDistanceSq() {
        double d = Math.max(2.0, Math.min(256.0, allEntitiesViewDistance));
        return d * d;
    }
}
