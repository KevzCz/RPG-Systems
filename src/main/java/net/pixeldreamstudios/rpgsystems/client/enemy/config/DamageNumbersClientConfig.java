package net.pixeldreamstudios.rpgsystems.client.enemy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DamageNumbersClientConfig {
    public enum ShowMode { ALL, PLAYERS_ONLY, NONE }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("rpgsystems").resolve("damage_numbers_client.json");

    public boolean enabled = true;
    public ShowMode showMode = ShowMode.ALL;

    public boolean showPetDamage = true;
    public boolean onlyShowPartyDamage = false;

    public double viewDistance = 32.0;

    private static DamageNumbersClientConfig INSTANCE;

    private DamageNumbersClientConfig() {}

    public static DamageNumbersClientConfig get() {
        if (INSTANCE == null) {
            try {
                if (Files.exists(FILE)) {
                    INSTANCE = GSON.fromJson(Files.readString(FILE), DamageNumbersClientConfig.class);
                    if (INSTANCE.showMode == null) INSTANCE.showMode = ShowMode.ALL;
                } else {
                    INSTANCE = new DamageNumbersClientConfig();
                }
            } catch (Exception e) {
                INSTANCE = new DamageNumbersClientConfig();
            }
        }
        return INSTANCE;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException ignored) { }
    }

    public double viewDistanceSq() {
        double d = Math.max(2.0, Math.min(256.0, viewDistance));
        return d * d;
    }
}
