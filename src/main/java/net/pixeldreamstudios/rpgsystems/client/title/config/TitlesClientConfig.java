package net.pixeldreamstudios.rpgsystems.client.title.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TitlesClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "titles_client.json";
    private static TitlesClientConfig INSTANCE;

    public boolean showOwnTitle = true;
    public boolean showOthersTitles = true;

    public static TitlesClientConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static TitlesClientConfig load() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    TitlesClientConfig cfg = GSON.fromJson(r, TitlesClientConfig.class);
                    if (cfg != null) {
                        return cfg;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        TitlesClientConfig fresh = new TitlesClientConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path())) {
                GSON.toJson(this, w);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("rpgsystems").resolve(FILE_NAME);
    }
}
