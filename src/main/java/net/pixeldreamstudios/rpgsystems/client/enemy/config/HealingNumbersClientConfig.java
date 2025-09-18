package net.pixeldreamstudios.rpgsystems.client.enemy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HealingNumbersClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("rpgsystems").resolve("healing_numbers_client.json");

    public enum Mode { NONE, ALL }
    public enum AmountMode { APPLIED, ATTEMPTED }

    public Mode mode = Mode.ALL;
    public AmountMode amountMode = AmountMode.ATTEMPTED;

    private static HealingNumbersClientConfig INSTANCE;

    private HealingNumbersClientConfig() {}

    public static synchronized HealingNumbersClientConfig get() {
        if (INSTANCE == null) {
            boolean needSave = false;
            try {
                if (Files.exists(FILE)) {
                    INSTANCE = GSON.fromJson(Files.readString(FILE), HealingNumbersClientConfig.class);
                    if (INSTANCE == null) {
                        INSTANCE = new HealingNumbersClientConfig();
                        needSave = true;
                    } else {
                        if (INSTANCE.mode == null) { INSTANCE.mode = Mode.ALL; needSave = true; }
                        if (INSTANCE.amountMode == null) { INSTANCE.amountMode = AmountMode.ATTEMPTED; needSave = true; }
                    }
                } else {
                    INSTANCE = new HealingNumbersClientConfig();
                    needSave = true;
                }
            } catch (Exception e) {
                INSTANCE = new HealingNumbersClientConfig();
                needSave = true;
            }
            if (needSave) save();
        }
        return INSTANCE;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException ignored) { }
    }
}
