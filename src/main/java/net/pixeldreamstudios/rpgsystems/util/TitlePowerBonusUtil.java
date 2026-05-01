package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.title.PermaGroupKey;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;
import net.pixeldreamstudios.rpgsystems.title.power.PowerRegistry;

import java.util.*;
import java.util.stream.Collectors;

public final class TitlePowerBonusUtil {

    private TitlePowerBonusUtil() {}

    private static final Map<UUID, Set<Identifier>> PERMA_INSTALLED = new HashMap<>();

    public static void installTitlePowers(ServerPlayerEntity player, Identifier titleId, List<Identifier> powers) {
        if (powers == null || powers.isEmpty()) {
            uninstallEquippedTitlePowers(player, titleId, Collections.emptyList());
            return;
        }
        for (Identifier pid : powers) {
            PowerRegistry.activate(pid, player);
        }
    }
    public static void uninstallEquippedTitlePowers(ServerPlayerEntity player, Identifier titleId, List<Identifier> equippedPowerIds) {
        for (Identifier pid : equippedPowerIds) {
            PowerRegistry.deactivate(pid, player);
        }
    }
    public static void uninstallTitlePowers(ServerPlayerEntity player, Title t) {
        for (Title.Bonus b : t.bonuses) {
            if (b.powerId != null && b.powerId.isPresent()) {
                PowerRegistry.deactivate(b.powerId.get(), player);
            }
        }
    }

    public static void rebuildPermaTitlePowers(ServerPlayerEntity player, Collection<Title> unlockedTitles) {
        UUID key = player.getUuid();
        Set<Identifier> previous = PERMA_INSTALLED.getOrDefault(key, Collections.emptySet());

        TitlesPersistentState state =
                TitlesPersistentState.get(player.getServer());
        var pt = state.getOrCreate(player.getUuid());
        Set<String> disabled = pt.permaDisabledGroups;

        Set<Identifier> desired = new HashSet<>();

        if (unlockedTitles != null) {
            for (Title t : unlockedTitles) {
                for (Title.Bonus b : t.permaBonuses) {
                    if (b.powerId != null && b.powerId.isPresent()) {
                        Identifier pid = b.powerId.get();

                        String gk = PermaGroupKey.power(pid);
                        if (disabled.contains(gk)) continue;

                        desired.add(pid);
                    }
                }
            }
        }

        for (Identifier pid : previous) {
            if (!desired.contains(pid)) {
                PowerRegistry.deactivate(pid, player);
            }
        }
        for (Identifier pid : desired) {
            if (!previous.contains(pid)) {
                PowerRegistry.activate(pid, player);
            }
        }

        PERMA_INSTALLED.put(key, desired);
    }

    public static void rebuildAllTitlePowers(ServerPlayerEntity player, Title equipped, Collection<Title> unlockedTitles) {
        if (equipped != null) {
            List<Identifier> equippedPowerIds = equipped.bonuses.stream()
                    .filter(b -> b.powerId != null && b.powerId.isPresent())
                    .map(b -> b.powerId.get())
                    .collect(Collectors.toList());
            installTitlePowers(player, equipped.id, equippedPowerIds);
        }
        rebuildPermaTitlePowers(player, unlockedTitles);
    }
}
