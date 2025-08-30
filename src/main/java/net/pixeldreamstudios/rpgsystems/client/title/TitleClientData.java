package net.pixeldreamstudios.rpgsystems.client.title;

import net.minecraft.util.Identifier;

import java.util.*;

public final class TitleClientData {
    private TitleClientData() {}

    private static final Set<Identifier> selfUnlocked = new HashSet<>();
    private static Identifier selfActive;
    private static final Map<UUID, Identifier> othersActive = new HashMap<>();

    public static void clear() {
        selfUnlocked.clear();
        selfActive = null;
        othersActive.clear();
    }

    public static void setSelf(Collection<Identifier> unlocked, Identifier active) {
        selfUnlocked.clear();
        selfUnlocked.addAll(unlocked);
        selfActive = active;
    }

    public static void setActive(UUID player, Identifier active) {
        if (active == null) othersActive.remove(player);
        else othersActive.put(player, active);
    }

    public static Set<Identifier> getSelfUnlocked() {
        return java.util.Collections.unmodifiableSet(selfUnlocked);
    }

    public static Identifier getSelfActive() {
        return selfActive;
    }

    public static Identifier getActive(UUID player) {
        return othersActive.get(player);
    }
}
