package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.container.SpellContainerSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class TitleSpellBonusUtil {
    private static final Logger LOG = LoggerFactory.getLogger("RPGSystems-TitleSpells");

    private TitleSpellBonusUtil() {}

    /**
     * Install all title-granted spells into a single ANY-content proxy container.
     * Active spells will merge when the main-hand item has a proxy container.
     * Adds detailed debug logs showing which spells exist and what was installed.
     */
    public static void installTitleSpells(ServerPlayerEntity player, Identifier titleId, Collection<Identifier> spellIds) {
        if (spellIds == null || spellIds.isEmpty()) {
            LOG.info("Title [{}] no spells to install for player {}", titleId, player.getGameProfile().getName());
            uninstallTitleSpells(player, titleId);
            return;
        }

        World world = player.getWorld();
        List<String> install = new ArrayList<>();
        int found = 0;
        int missing = 0;

        LOG.info("Title [{}] installing {} requested spells for player {}: {}", titleId, spellIds.size(), player.getGameProfile().getName(), spellIds);

        for (Identifier id : spellIds) {
            var ref = SpellRegistry.from(world).getEntry(id).orElse(null);
            if (ref != null) {
                Spell s = ref.value();
                String type = String.valueOf(s.type);
                String content = String.valueOf(net.spell_engine.api.spell.container.SpellContainerHelper.contentTypeForSpell(s));
                LOG.info("Title [{}] spell exists: {} type={} content={}", titleId, id, type, content);
                install.add(id.toString());
                found++;
            } else {
                LOG.warn("Title [{}] spell MISSING: {}", titleId, id);
                missing++;
            }
        }

        LOG.info("Title [{}] resolved spells: found {} of {} (missing {})", titleId, found, spellIds.size(), missing);

        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String base = "title/" + titleId.toString();
        int before = serverSide.size();

        serverSide.keySet().removeIf(k -> k.startsWith(base + "/"));
        int removed = before - serverSide.size();
        if (removed > 0) {
            LOG.info("Title [{}] removed {} previous title containers for player {}", titleId, removed, player.getGameProfile().getName());
        }

        if (!install.isEmpty()) {
            SpellContainer container = new SpellContainer(SpellContainer.ContentType.ANY, true, "", 0, install);
            String key = base + "/any";
            serverSide.put(key, container);
            LOG.info("Title [{}] inserted container key={} spells={}", titleId, key, install);
        } else {
            LOG.warn("Title [{}] nothing to install after resolution; no container inserted", titleId);
        }

        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
        LOG.info("Title [{}] sync complete for player {}", titleId, player.getGameProfile().getName());
    }

    public static void uninstallTitleSpells(ServerPlayerEntity player, Identifier titleId) {
        Map<String, SpellContainer> serverSide = ((SpellContainerSource.Owner) player).serverSideSpellContainers();
        String base = "title/" + titleId.toString();
        int before = serverSide.size();
        serverSide.keySet().removeIf(k -> k.startsWith(base + "/"));
        int removed = before - serverSide.size();
        LOG.info("Title [{}] uninstalled; removed {} containers for player {}", titleId, removed, player.getGameProfile().getName());

        SpellContainerSource.setDirtyServerSide(player);
        SpellContainerSource.syncServerSideContainers(player);
    }
}
