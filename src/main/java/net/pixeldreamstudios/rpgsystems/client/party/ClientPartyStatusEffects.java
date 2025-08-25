package net.pixeldreamstudios.rpgsystems.client.party;

import net.minecraft.util.Identifier;

import java.util.*;

public final class ClientPartyStatusEffects {
    private ClientPartyStatusEffects() {}

    private static final Map<UUID, List<Identifier>> MAP = new HashMap<>();

    public static synchronized void update(UUID member, List<Identifier> effectIds) {
        if (effectIds == null || effectIds.isEmpty()) {
            MAP.remove(member);
        } else {
            MAP.put(member, new ArrayList<>(effectIds));
        }
    }

    public static synchronized List<Identifier> get(UUID member) {
        List<Identifier> v = MAP.get(member);
        if (v == null) return Collections.emptyList();
        return v;
    }

    public static synchronized void clearFor(UUID member) {
        MAP.remove(member);
    }

    public static synchronized void clearAll() {
        MAP.clear();
    }
}
