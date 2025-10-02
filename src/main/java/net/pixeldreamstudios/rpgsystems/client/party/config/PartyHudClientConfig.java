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
    private static final String FILE_NAME = "rpgsystems_partyhud_client.json";
    private static PartyHudClientConfig INSTANCE;

    public enum HudStyle { ORIGINAL, SIMPLE }

    public boolean hudEnabled = true;
    public boolean showArrows = true;
    public boolean showHpBar = true;
    public boolean showHungerBar = true;
    public boolean showStaminaBar = false;
    public boolean showManaBar = false;
    public boolean showRpgManaBar = false;

    public int partyHudX = -2;
    public int partyHudY = 10;

    public float hudScale = 1.0f;
    public int maxVisiblePartyHuds = -1;
    public HudStyle hudStyle = HudStyle.ORIGINAL;

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
        boolean needSave = false;
        PartyHudClientConfig loaded = null;

        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                loaded = GSON.fromJson(r, PartyHudClientConfig.class);
            } catch (Throwable t) {
                try { Files.move(p, p.resolveSibling(FILE_NAME + ".bak")); } catch (Throwable ignored) {}
                needSave = true;
            }
        } else {
            needSave = true;
        }

        INSTANCE = (loaded != null) ? loaded : new PartyHudClientConfig();

        if (INSTANCE.enforceMaxThreeBars()) needSave = true;
        if (INSTANCE.normalizeVisibilityLimit()) needSave = true;
        if (INSTANCE.normalizeScale()) needSave = true;
        if (INSTANCE.hudStyle == null) { INSTANCE.hudStyle = HudStyle.ORIGINAL; needSave = true; }
        if (needSave) save();
    }

    public static synchronized void save() {
        if (INSTANCE == null) INSTANCE = new PartyHudClientConfig();
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Throwable ignored) {}
    }

    public boolean enforceMaxThreeBars() {
        int count = 0;
        if (showHpBar) count++;
        if (showHungerBar) count++;
        if (showStaminaBar) count++;
        if (showManaBar) count++;
        if (showRpgManaBar) count++;

        boolean changed = false;
        if (count > 3) { if (showRpgManaBar) { showRpgManaBar = false; changed = true; count--; } }
        if (count > 3) { if (showManaBar)    { showManaBar    = false; changed = true; count--; } }
        if (count > 3) { if (showStaminaBar) { showStaminaBar = false; changed = true; count--; } }
        if (count > 3) { if (showHungerBar)  { showHungerBar  = false; changed = true; count--; } }
        if (count > 3) { if (showHpBar)      { showHpBar      = false; changed = true; } }
        return changed;
    }
    public boolean normalizeVisibilityLimit() {
        int prev = maxVisiblePartyHuds;
        if (maxVisiblePartyHuds < -1) {
            maxVisiblePartyHuds = -1;
        }
        return prev != maxVisiblePartyHuds;
    }
    public boolean normalizeScale() {
        float prev = hudScale;
        if (hudScale <= 0f) hudScale = 1.0f;
        if (hudScale < 0.1f) hudScale = 0.1f;
        return Float.compare(prev, hudScale) != 0;
    }
}
