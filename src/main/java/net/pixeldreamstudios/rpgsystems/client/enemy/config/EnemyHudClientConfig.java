package net.pixeldreamstudios.rpgsystems.client.enemy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnemyHudClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("rpgsystems").resolve("enemy_hud_client.json");

    public boolean showHealthbarOnNearbyHpChanges = true;

    private static EnemyHudClientConfig INSTANCE;

    private EnemyHudClientConfig() {}

    public static EnemyHudClientConfig get() {
        if (INSTANCE == null) {
            try {
                if (Files.exists(FILE)) {
                    INSTANCE = GSON.fromJson(Files.readString(FILE), EnemyHudClientConfig.class);
                } else {
                    INSTANCE = new EnemyHudClientConfig();
                }
            } catch (Exception e) {
                INSTANCE = new EnemyHudClientConfig();
            }
        }
        return INSTANCE;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException ignored) {}
    }
}
