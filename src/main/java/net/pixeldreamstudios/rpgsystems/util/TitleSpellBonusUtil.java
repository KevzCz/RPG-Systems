package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.container.SpellContainerSource;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;
import net.pixeldreamstudios.rpgsystems.title.PermaGroupKey;

import java.util.*;
import java.util.stream.Collectors;

public final class TitleSpellBonusUtil {
    private TitleSpellBonusUtil() {}

    public static void installTitleSpells(ServerPlayerEntity player, Identifier titleId, Collection<Identifier> spellIds) {
        if (spellIds == null || spellIds.isEmpty()) {
            uninstallTitleSpells(player, titleId);
            return;
        }

        World world = player.getWorld();
        List<String> install = new ArrayList<>();

        for (Identifier id : spellIds) {
            var ref = SpellRegistry.from(world).getEntry(id).orElse(null);
            if (ref != null) {
                install.add(id.toString());
            }
        }

        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String base = "title/" + titleId.toString();

        serverSide.keySet().removeIf(k -> k.startsWith(base + "/"));

        if (!install.isEmpty()) {
            SpellContainer container = new SpellContainer(SpellContainer.ContentType.ANY, "", "", 0, install);
            serverSide.put(base + "/any", container);
        }

        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
    }

    public static void uninstallTitleSpells(ServerPlayerEntity player, Identifier titleId) {
        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String base = "title/" + titleId.toString();
        serverSide.keySet().removeIf(k -> k.startsWith(base + "/"));
        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
    }

    public static void rebuildPermaTitleSpells(ServerPlayerEntity player, Collection<Title> unlockedTitles) {
        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String permaKey = "title/perma/any";

        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        var pt = state.getOrCreate(player.getUuid());
        Set<String> disabled = pt.permaDisabledGroups;

        Set<String> install = new HashSet<>();
        if (unlockedTitles != null) {
            World world = player.getWorld();
            for (Title t : unlockedTitles) {
                for (Title.Bonus b : t.permaBonuses) {
                    if (b.spellId != null && b.spellId.isPresent()) {
                        Identifier id = b.spellId.get();

                        String key = PermaGroupKey.spell(id);
                        if (disabled.contains(key)) continue;

                        var ref = SpellRegistry.from(world).getEntry(id).orElse(null);
                        if (ref != null) {
                            install.add(id.toString());
                        }
                    }
                }
            }
        }

        serverSide.remove(permaKey);
        if (!install.isEmpty()) {
            SpellContainer container = new SpellContainer(SpellContainer.ContentType.ANY, "", "", 0, new ArrayList<>(install));
            serverSide.put(permaKey, container);
        }

        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
    }



    public static void rebuildAllTitleSpells(ServerPlayerEntity player, Title equipped, Collection<Title> unlockedTitles) {

        if (equipped != null) {
            List<Identifier> equippedSpellIds = equipped.bonuses.stream()
                    .filter(b -> b.spellId != null && b.spellId.isPresent())
                    .map(b -> b.spellId.get())
                    .collect(Collectors.toList());
            installTitleSpells(player, equipped.id, equippedSpellIds);
        }

        rebuildPermaTitleSpells(player, unlockedTitles);
    }
}
