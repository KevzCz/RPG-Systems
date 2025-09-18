package net.pixeldreamstudios.rpgsystems.client.enemy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DamageTypeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("rpgsystems").resolve("damage_types_client.json");

    private static final Identifier GENERIC_KILL = Identifier.of("minecraft", "generic_kill");

    public Map<String, Boolean> showByDamageType = new LinkedHashMap<>();
    public Map<String, String>  colorByDamageType = new LinkedHashMap<>();

    private static DamageTypeConfig INSTANCE;

    private DamageTypeConfig() {}

    public static DamageTypeConfig get() {
        if (INSTANCE == null) {
            boolean needSave = false;
            try {
                if (Files.exists(FILE)) {
                    INSTANCE = GSON.fromJson(Files.readString(FILE), DamageTypeConfig.class);
                    if (INSTANCE == null) {
                        INSTANCE = new DamageTypeConfig();
                        needSave = true;
                    }
                    if (INSTANCE.showByDamageType == null || INSTANCE.colorByDamageType == null) {
                        INSTANCE = ensureMaps(INSTANCE);
                        needSave = true;
                    }
                } else {
                    INSTANCE = new DamageTypeConfig();
                    needSave = true;
                }
            } catch (Exception e) {
                INSTANCE = new DamageTypeConfig();
                needSave = true;
            }

            if (ensureDefaultGenericKill(INSTANCE)) needSave = true;

            if (needSave) save();
        }
        return INSTANCE;
    }

    private static DamageTypeConfig ensureMaps(DamageTypeConfig cfg) {
        if (cfg.showByDamageType == null) cfg.showByDamageType = new LinkedHashMap<>();
        if (cfg.colorByDamageType == null) cfg.colorByDamageType = new LinkedHashMap<>();
        return cfg;
    }

    private static boolean ensureDefaultGenericKill(DamageTypeConfig cfg) {
        String key = GENERIC_KILL.toString();
        if (!cfg.showByDamageType.containsKey(key)) {
            cfg.showByDamageType.put(key, Boolean.FALSE);
            return true;
        }
        return false;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException ignored) { }
    }

    public boolean shouldShowType(Identifier id) {
        String key = id.toString();
        Boolean b = showByDamageType.get(key);
        if (b != null) return b;

        return !id.equals(GENERIC_KILL);
    }

    public static void maybePopulateFromRegistry(DynamicRegistryManager rm) {
        try {
            DamageTypeConfig cfg = get();
            Registry<DamageType> reg = rm.get(RegistryKeys.DAMAGE_TYPE);
            for (Identifier id : reg.getIds()) {
                boolean def = !id.equals(GENERIC_KILL);
                cfg.showByDamageType.putIfAbsent(id.toString(), def);
            }
            ensureDefaultGenericKill(cfg);
            save();
        } catch (Throwable ignored) { }
    }

    public static void seenDamageType(Identifier id) {
        DamageTypeConfig cfg = get();
        boolean def = !id.equals(GENERIC_KILL);
        if (cfg.showByDamageType.putIfAbsent(id.toString(), def) == null) save();
    }

    public Integer colorOverride(Identifier id) {
        String raw = colorByDamageType.get(id.toString());
        if (raw == null) return null;
        try {
            String s = raw.trim();
            if (s.startsWith("#")) s = s.substring(1);
            if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
            int rgb = (int) Long.parseLong(s, 16);
            return rgb & 0xFFFFFF;
        } catch (Exception ignored) {
            return null;
        }
    }
}
