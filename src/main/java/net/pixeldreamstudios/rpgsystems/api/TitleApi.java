package net.pixeldreamstudios.rpgsystems.api;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;
import net.pixeldreamstudios.rpgsystems.util.TitlePowerBonusUtil;
import net.pixeldreamstudios.rpgsystems.util.TitleSpellBonusUtil;

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

            String rawName = extractRawName(t);
            String plainName = stripStyleTokens(rawName);
            player.sendMessage(Text.literal("You have unlocked the title " + plainName));

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
            inst.addPersistentModifier(new EntityAttributeModifier(mid, b.amount, b.operation));
        }
        if (!spells.isEmpty()) {
            TitleSpellBonusUtil.installTitleSpells(player, t.id, spells);
        }

        java.util.List<Identifier> powers = new java.util.ArrayList<>();
        for (Title.Bonus b : t.bonuses) {
            if (b.powerId != null && b.powerId.isPresent()) {
                powers.add(b.powerId.get());
            }
        }
        if (!powers.isEmpty()) {
            TitlePowerBonusUtil.installTitlePowers(player, t.id, powers);
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
        TitleSpellBonusUtil.uninstallTitleSpells(player, t.id);
        TitlePowerBonusUtil.uninstallTitlePowers(player, t);
    }

    private static Identifier modifierId(Title t, Title.Bonus b) {
        Identifier attrId = b.attribute.getKey()
                .map(k -> k.getValue())
                .orElse(Identifier.of("minecraft", "unknown"));
        String path = "title/" + t.id.getNamespace() + "/" + t.id.getPath()
                + "/" + attrId.getNamespace() + "/" + attrId.getPath();
        return Identifier.of("rpg-systems", path);
    }

    public static void refreshActiveOnLogin(ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        if (pt.active != null) {
            Identifier id = Identifier.of(pt.active);
            Title t = TitleRegistry.get(id);
            if (t != null) {
                removeBonuses(player, t);
                applyBonuses(player, t);
            }
        }
    }

    public static boolean clearProgress(ServerPlayerEntity player, Identifier titleId) {
        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        net.minecraft.nbt.NbtCompound removed = pt.progress.remove(titleId.toString());
        state.markDirty();
        return removed != null;
    }

    public static Optional<Identifier> getActiveId(ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        return Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));
    }

    public static Optional<Title> getActive(ServerPlayerEntity player) {
        return getActiveId(player).map(TitleRegistry::get);
    }

    public static boolean isActive(ServerPlayerEntity player, Identifier titleId) {
        return getActiveId(player).map(id -> id.equals(titleId)).orElse(false);
    }

    private static String extractRawName(Title t) {
        try {
            Object dn = t.displayName; // Text or String in your codebase
            if (dn instanceof Text txt) return txt.getString();
            return String.valueOf(dn);
        } catch (Throwable ignored) {
            return t.id.getPath();
        }
    }

    private static String stripStyleTokens(String s) {
        if (s == null || s.isEmpty()) return "";
        String out = s;

        out = out.replaceAll("(?i)\\{\\s*(?:rainbow|wiggle|gradient|pulse|shake|color|bounce|wave|glitch|clear|reset)(?:[^}]*)\\}", "");
        out = out.replaceAll("(?i)\\{\\s*/\\s*(?:rainbow|wiggle|gradient|pulse|shake|color|bounce|wave|glitch|clear|reset)\\s*\\}", "");

        out = out.replaceAll("(?i)&[0-9a-frk-o]", "");

        out = out.replace('\u00A7', ' ');
        out = out.replaceAll("\\s+", " ").trim();

        return out;
    }
}
