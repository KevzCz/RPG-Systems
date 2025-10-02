package net.pixeldreamstudios.rpgsystems.client.title;

import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;

import java.util.*;

public final class TitleClientData {
    private TitleClientData() {}

    private static final Set<Identifier> selfUnlocked = new HashSet<>();
    private static Identifier selfActive;
    private static final Map<UUID, Identifier> othersActive = new HashMap<>();
    private static Set<String> PERMA_DISABLED = new LinkedHashSet<>();
    private static final Map<Identifier, List<CondProg>> selfProgress = new HashMap<>();
    public static void setPermaDisabled(Set<String> s){ PERMA_DISABLED = s; }
    public static Set<String> getPermaDisabled(){ return PERMA_DISABLED; }
    public static void clear() {
        selfUnlocked.clear();
        selfActive = null;
        othersActive.clear();
        selfProgress.clear();
        PERMA_DISABLED.clear();
    }

    public static void setSelf(Collection<Identifier> unlocked, Identifier active) {
        selfUnlocked.clear();
        selfUnlocked.addAll(unlocked);
        selfActive = active;
    }

    public static void setActive(UUID player, Identifier active) {
        othersActive.put(player, active);
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

    public static void setProgress(List<TitlePayloads.SyncProgress.TitleProgress> list) {
        selfProgress.clear();
        for (TitlePayloads.SyncProgress.TitleProgress tp : list) {
            List<CondProg> conds = new ArrayList<>(tp.conditions().size());
            for (TitlePayloads.SyncProgress.CondProg cp : tp.conditions()) {
                conds.add(new CondProg(cp.current(), cp.done()));
            }
            selfProgress.put(tp.id(), conds);
        }
    }

    public static List<CondProg> getProgress(Identifier titleId) {
        return selfProgress.getOrDefault(titleId, java.util.Collections.emptyList());
    }

    public record CondProg(long current, boolean done) {}
}
