package net.pixeldreamstudios.rpgsystems.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;

import java.util.Optional;

public final class TitleApi {
    private TitleApi() {}

    public static boolean grant(ServerPlayerEntity player, Identifier titleId) {
        Title t = TitleRegistry.get(titleId);
        if (t == null) return false;
        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        boolean added = pt.unlocked.add(titleId.toString());
        if (added) {
            state.markDirty();
            return true;
        }
        return false;
    }

    public static boolean revoke(ServerPlayerEntity player, Identifier titleId) {
        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        boolean removed = pt.unlocked.remove(titleId.toString());
        if (removed) {
            if (titleId.toString().equals(pt.active)) {
                applyActiveTitle(player, Optional.ofNullable(Identifier.of(pt.active)), Optional.empty());
                pt.active = null;
            }
            state.markDirty();
            return true;
        }
        return false;
    }

    public static boolean setActive(ServerPlayerEntity player, Optional<Identifier> newActiveOpt) {
        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        Optional<Identifier> old = Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));

        if (newActiveOpt.isPresent()) {
            Identifier id = newActiveOpt.get();
            if (!pt.unlocked.contains(id.toString())) return false;
            pt.active = id.toString();
        } else {
            pt.active = null;
        }

        applyActiveTitle(player, old, newActiveOpt);
        state.markDirty();
        return true;
    }

    private static void applyActiveTitle(ServerPlayerEntity player, Optional<Identifier> oldActive, Optional<Identifier> newActive) {
        oldActive.ifPresent(id -> {
            Title t = TitleRegistry.get(id);
            if (t != null) removeBonuses(player, t);
        });
        newActive.ifPresent(id -> {
            Title t = TitleRegistry.get(id);
            if (t != null) applyBonuses(player, t);
        });

        float max = player.getMaxHealth();
        if (player.getHealth() > max) player.setHealth(max);
    }

    private static void applyBonuses(ServerPlayerEntity player, Title t) {
        java.util.List<Identifier> spells = new java.util.ArrayList<>();

        for (Title.Bonus b : t.bonuses) {
            if (b.spellId != null && b.spellId.isPresent()) {
                spells.add(b.spellId.get());
                continue;
            }
            if (b.attribute == null) {
                continue;
            }
            var inst = player.getAttributeInstance(b.attribute);
            if (inst == null) {
                continue;
            }
            Identifier mid = modifierId(t, b);
            inst.removeModifier(mid);
            inst.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(mid, b.amount, b.operation));
        }
        if (!spells.isEmpty()) {
            net.pixeldreamstudios.rpgsystems.util.TitleSpellBonusUtil.installTitleSpells(player, t.id, spells);
        }
    }

    private static void removeBonuses(ServerPlayerEntity player, Title t) {
        for (Title.Bonus b : t.bonuses) {
            if (b.spellId != null && b.spellId.isPresent()) {
                continue;
            }
            if (b.attribute == null) continue;
            var inst = player.getAttributeInstance(b.attribute);
            if (inst == null) continue;
            Identifier mid = modifierId(t, b);
            inst.removeModifier(mid);
        }
        net.pixeldreamstudios.rpgsystems.util.TitleSpellBonusUtil.uninstallTitleSpells(player, t.id);
    }

    private static Identifier modifierId(Title t, Title.Bonus b) {
        Identifier attrId = b.attribute.getKey()
                .map(k -> k.getValue())
                .orElse(Identifier.of("minecraft", "unknown"));
        String path = "title/" + t.id.getNamespace() + "/" + t.id.getPath()
                + "/" + attrId.getNamespace() + "/" + attrId.getPath();
        return Identifier.of("rpg-systems", path);
    }
}
