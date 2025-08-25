// net/pixeldreamstudios/rpgsystems/client/enemy/config/DamageNumbersClientConfig.java
package net.pixeldreamstudios.rpgsystems.client.enemy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DamageNumbersClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("rpgsystems").resolve("damage_numbers_client.json");

    private static DamageNumbersClientConfig INSTANCE;

    // === Settings ===
    public boolean enabled = true;
    /** View distance in blocks (client-side cull). */
    public double viewDistance = 48.0;
    /** If true, show damage numbers caused by the player's pets (when the server marks them). */
    public boolean showPetDamage = true;

    private DamageNumbersClientConfig() {}

    public static DamageNumbersClientConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                INSTANCE = GSON.fromJson(Files.readString(FILE), DamageNumbersClientConfig.class);
            } else {
                INSTANCE = new DamageNumbersClientConfig();
                save();
            }
        } catch (IOException e) {
            INSTANCE = new DamageNumbersClientConfig();
        }
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
