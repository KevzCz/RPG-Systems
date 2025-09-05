package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.container.SpellContainerSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class TitleSpellBonusUtil {
    private TitleSpellBonusUtil() {}
    public static void installTitleSpells(ServerPlayerEntity player, Identifier titleId, Collection<Identifier> spellIds) {
        if (spellIds == null || spellIds.isEmpty()) {
            uninstallTitleSpells(player, titleId);
            return;
        }
        World world = player.getWorld();
        List<String> install = new ArrayList<>();
        int found = 0;
        int missing = 0;

        for (Identifier id : spellIds) {
            var ref = SpellRegistry.from(world).getEntry(id).orElse(null);
            if (ref != null) {
                Spell s = ref.value();
                String type = String.valueOf(s.type);
                String content = String.valueOf(net.spell_engine.api.spell.container.SpellContainerHelper.contentTypeForSpell(s));
                install.add(id.toString());
                found++;
            } else {
                missing++;
            }
        }


        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String base = "title/" + titleId.toString();
        int before = serverSide.size();

        serverSide.keySet().removeIf(k -> k.startsWith(base + "/"));


        if (!install.isEmpty()) {
            SpellContainer container = new SpellContainer(SpellContainer.ContentType.ANY, true, "", 0, install);
            String key = base + "/any";
            serverSide.put(key, container);
            }

        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
    }

    public static void uninstallTitleSpells(ServerPlayerEntity player, Identifier titleId) {
        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String base = "title/" + titleId.toString();
        int before = serverSide.size();
        serverSide.keySet().removeIf(k -> k.startsWith(base + "/"));
        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
    }
}
