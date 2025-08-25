package net.pixeldreamstudios.rpgsystems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
public final class RPGSystemsConfig {

    public static final class Systems {
        public boolean party = true;
        public boolean pet = false;
    }

    public static final class Party {
        public boolean logChatToConsole = true;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                INSTANCE = GSON.fromJson(r, RPGSystemsConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new RPGSystemsConfig();
                    save();
                }
            } catch (Throwable t) {
                INSTANCE = new RPGSystemsConfig();
                save();
            }
        } else {
            INSTANCE = new RPGSystemsConfig();
            save();
        }
    }

    public static synchronized void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (IOException ignored) {
        }
    }
}
