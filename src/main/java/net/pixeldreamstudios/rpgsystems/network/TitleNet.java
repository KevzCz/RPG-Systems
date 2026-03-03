package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.api.TitleApi;
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.network.title.TitleListSyncPayload;
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;
import net.pixeldreamstudios.rpgsystems.util.TitlePowerBonusUtil;
import net.pixeldreamstudios.rpgsystems.util.TitleSpellBonusUtil;

import java.util.*;

public final class TitleNet {
    private TitleNet() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(TitlePayloads.RequestSetActive.ID, TitlePayloads.RequestSetActive.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncSelf.ID, TitlePayloads.SyncSelf.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncActive.ID, TitlePayloads.SyncActive.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncDefinitions.ID, TitlePayloads.SyncDefinitions.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncProgress.ID, TitlePayloads.SyncProgress.CODEC);
        PayloadTypeRegistry.playS2C().register(TitleListSyncPayload.ID, TitleListSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TitlePayloads.RequestSetPermaToggles.ID, TitlePayloads.RequestSetPermaToggles.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncPermaToggles.ID, TitlePayloads.SyncPermaToggles.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TitlePayloads.RequestSetPermaToggles.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
            var pt = state.getOrCreate(player.getUuid());

            pt.permaDisabledGroups.clear();
            pt.permaDisabledGroups.addAll(payload.disabled());
            state.markDirty();

            // Rebuild perma effects immediately
            var unlocked = net.pixeldreamstudios.rpgsystems.api.TitleApi
                    .getActive(player) // not needed for perma, but we’ll rebuild everything
                    .map(a -> a) // noop
                    ;

            // Use your existing code paths:
            var list = new ArrayList<Title>(pt.unlocked.size());
            for (String s : pt.unlocked) {
                try { var id = Identifier.of(s); var t = TitleRegistry.get(id); if (t != null) list.add(t); } catch (Exception ignored) {}
            }
            TitleApi.rebuildPermaAttributes(player, list);
            TitleSpellBonusUtil.rebuildAllTitleSpells(player, TitleApi.getActive(player).orElse(null), list);
            TitlePowerBonusUtil.rebuildAllTitlePowers(player, TitleApi.getActive(player).orElse(null), list);

            // Sync new toggle state back to client
            ServerPlayNetworking.send(player, new TitlePayloads.SyncPermaToggles(new ArrayList<>(pt.permaDisabledGroups)));
        });
        ServerPlayNetworking.registerGlobalReceiver(TitlePayloads.RequestSetActive.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            Optional<Identifier> requested = payload.active();
            boolean ok = net.pixeldreamstudios.rpgsystems.api.TitleApi.setActive(player, requested);
            if (ok) {
                broadcastActiveToAll(player.getServer(), player.getUuid(), requested);
                syncSelfTo(player.getServer(), player);
                syncProgressTo(player.getServer(), player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendDefinitionsTo(server, handler.player);
            syncSelfTo(server, handler.player);
            syncProgressTo(server, handler.player);
            broadcastActiveToAll(server, handler.player.getUuid(), activeOf(server, handler.player.getUuid()));
            broadcastAllActivesTo(server, handler.player);
            TitleApi.refreshActiveOnLogin(handler.player);
            ServerPlayNetworking.send(handler.player,
                    new TitlePayloads.SyncPermaToggles(new ArrayList<>(TitlesPersistentState.get(server).getOrCreate(handler.player.getUuid()).permaDisabledGroups)));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendAllTitles(handler.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            TitleApi.refreshActiveOnLogin(newPlayer);

            syncSelfTo(newPlayer.getServer(), newPlayer);
            syncProgressTo(newPlayer.getServer(), newPlayer);
            broadcastActiveToAll(newPlayer.getServer(),
                    newPlayer.getUuid(),
                    activeOf(newPlayer.getServer(), newPlayer.getUuid()));
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncSelf.ID, (payload, context) ->
                context.client().execute(() ->
                        TitleClientData.setSelf(payload.unlocked(), payload.active().orElse(null))
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncActive.ID, (payload, context) ->
                context.client().execute(() ->
                        TitleClientData.setActive(payload.playerUuid(), payload.active().orElse(null))
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncPermaToggles.ID, (payload, ctx) ->
                ctx.client().execute(() -> TitleClientData.setPermaDisabled(new java.util.LinkedHashSet<>(payload.disabled())))
        );
        ClientPlayNetworking.registerGlobalReceiver(TitleListSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Map<Identifier, Title> merged = new LinkedHashMap<>(TitleRegistry.all());
                for (TitleListSyncPayload.Entry e : payload.entries()) {
                    Identifier id = e.id();
                    if (!merged.containsKey(id)) {
                        Title t = Title.builder(id, Text.literal(e.name()))
                                .hidden(e.hidden())
                                .build();
                        merged.put(id, t);
                    }
                }
                TitleRegistry.replaceAll(merged);
                TitleRegistry.bootstrapFallback();
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncDefinitions.ID, (payload, context) ->
                context.client().execute(() -> {
                    Map<Identifier, Title> map = new LinkedHashMap<>();
                    for (TitlePayloads.SyncDefinitions.Def d : payload.defs()) {
                        Identifier id = d.id();
                        Text name = Text.literal(d.name());
                        Text desc = d.description().map(Text::literal).orElse(Text.empty());
                        Title.Builder b = Title.builder(id, name).description(desc).hidden(d.hidden());

                        for (TitlePayloads.SyncDefinitions.BonusDef jb : d.bonuses()) {
                            RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, jb.attribute());
                            RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                                    .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + jb.attribute()));
                            b.add(entry, jb.amount(), jb.operation());
                        }
                        for (TitlePayloads.SyncDefinitions.DamageBonusDef db : d.damage()) {
                            if (db.target().isPresent()) {
                                b.addDamageBonus(db.target().get(), db.amount(),
                                        db.op() == TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED
                                                ? Title.DamageOp.MULTIPLIED : Title.DamageOp.ADDED);
                            } else if (db.tag().isPresent()) {
                                b.addDamageBonusTag(db.tag().get(), db.amount(),
                                        db.op() == TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED
                                                ? Title.DamageOp.MULTIPLIED : Title.DamageOp.ADDED);
                            }
                        }

                        for (Identifier sid : d.spells()) b.addSpell(sid);
                        for (Identifier pid : d.powers()) b.addPower(pid);

                        for (var jb : d.permaBonuses()) {
                            var key = RegistryKey.of(RegistryKeys.ATTRIBUTE, jb.attribute());
                            RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                                    .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + jb.attribute()));
                            b.addPerma(entry, jb.amount(), jb.operation());
                        }
                        for (Identifier sid : d.permaSpells()) b.addPermaSpell(sid);
                        for (Identifier pid : d.permaPowers()) b.addPermaPower(pid);
                        for (var db : d.permaDamage()) {
                            if (db.target().isPresent()) {
                                b.addPermaDamageBonus(db.target().get(), db.amount(),
                                        db.op() == TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED ? Title.DamageOp.MULTIPLIED : Title.DamageOp.ADDED);
                            } else if (db.tag().isPresent()) {
                                b.addPermaDamageBonusTag(db.tag().get(), db.amount(),
                                        db.op() == TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED ? Title.DamageOp.MULTIPLIED : Title.DamageOp.ADDED);
                            }
                        }
                        for (TitlePayloads.SyncDefinitions.ConditionDef cd : d.conditions()) {
                            Title.Condition.Type t = switch (cd.type()) {
                                case OBTAIN_ITEM            -> Title.Condition.Type.OBTAIN_ITEM;
                                case KILL_MOBS              -> Title.Condition.Type.KILL_MOBS;
                                case ADVANCEMENT            -> Title.Condition.Type.ADVANCEMENT;
                                case WALK_BLOCKS            -> Title.Condition.Type.WALK_BLOCKS;
                                case REACH_LEVEL            -> Title.Condition.Type.REACH_LEVEL;
                                case REACH_LEVEL_XP         -> Title.Condition.Type.REACH_LEVEL_XP;
                                case REACH_LEVEL_PUFFERFISH -> Title.Condition.Type.REACH_LEVEL_PUFFERFISH;
                                case CRAFT_ITEM             -> Title.Condition.Type.CRAFT_ITEM;
                                case MINE_BLOCKS            -> Title.Condition.Type.MINE_BLOCKS;
                                case VISIT_BIOME            -> Title.Condition.Type.VISIT_BIOME;
                                case ENTER_DIMENSION        -> Title.Condition.Type.ENTER_DIMENSION;
                                case INTERACT_BLOCK         -> Title.Condition.Type.INTERACT_BLOCK;
                                case INTERACT_ENTITY        -> Title.Condition.Type.INTERACT_ENTITY;
                                case FIND_STRUCTURE         -> Title.Condition.Type.FIND_STRUCTURE;
                                case DEAL_DAMAGE_TOTAL      -> Title.Condition.Type.DEAL_DAMAGE_TOTAL;
                                case DEAL_DAMAGE_MAX        -> Title.Condition.Type.DEAL_DAMAGE_MAX;
                                case CHECK_ATTRIBUTE        -> Title.Condition.Type.CHECK_ATTRIBUTE;
                            };

                            b.addCondition(new Title.Condition(
                                    t,
                                    cd.item(), cd.entityType(), cd.advancement(),
                                    cd.distance(), cd.count(), cd.hint(), cd.hidden(),
                                    cd.entitySpec(), cd.nbtQuery(), cd.level(),
                                    cd.block(), cd.biome(), cd.dimension(),
                                    cd.structure(), cd.attribute(), cd.min(),
                                    cd.entityTag()
                            ));
                        }

                        map.put(id, b.build());
                    }
                    TitleRegistry.replaceAll(map);
                    TitleRegistry.bootstrapFallback();
                })
        );
        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncProgress.ID, (payload, context) ->
                context.client().execute(() -> TitleClientData.setProgress(payload.progresses()))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TitleClientData.clear());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> TitleClientData.clear());
    }

    private static Optional<Identifier> activeOf(MinecraftServer server, UUID uuid) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(uuid);
        return Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));
    }

    public static void syncSelfTo(MinecraftServer server, ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

        List<Identifier> unlocked = new ArrayList<>();
        for (String s : pt.unlocked) unlocked.add(Identifier.of(s));
        Optional<Identifier> active = Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));

        ServerPlayNetworking.send(player, new net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads.SyncSelf(unlocked, active));
    }

    public static void sendAllTitles(ServerPlayerEntity player) {
        List<TitleListSyncPayload.Entry> entries = new ArrayList<>();
        for (Title t : TitleRegistry.all().values()) {
            String name = t.displayName != null ? t.displayName.getString() : t.id.toString();
            boolean hidden = t.hidden;
            entries.add(new TitleListSyncPayload.Entry(t.id, name, hidden));
        }
        ServerPlayNetworking.send(player, new TitleListSyncPayload(entries));
    }

    public static void syncProgressTo(MinecraftServer server, ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

        Map<Identifier, Title> titles = TitleRegistry.all();

        List<TitlePayloads.SyncProgress.TitleProgress> out = new ArrayList<>();
        for (Map.Entry<Identifier, Title> e : titles.entrySet()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (t.conditions.isEmpty()) continue;

            List<net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads.SyncProgress.CondProg> conds = new ArrayList<>();
            for (int i = 0; i < t.conditions.size(); i++) {
                net.minecraft.nbt.NbtCompound tag = pt.progress.get(id.toString());
                long cur = tag == null ? 0L : tag.getLong("c" + i);
                boolean done = tag != null && tag.getBoolean("done_" + i);
                conds.add(new net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads.SyncProgress.CondProg(cur, done));
            }
            out.add(new net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads.SyncProgress.TitleProgress(id, conds));
        }

        ServerPlayNetworking.send(player, new net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads.SyncProgress(out));
    }

    private static void sendDefinitionsTo(MinecraftServer server, ServerPlayerEntity player) {
        List<TitlePayloads.SyncDefinitions.Def> defs = new ArrayList<>();

        for (Map.Entry<Identifier, Title> e : TitleRegistry.all().entrySet()) {
            Identifier id = e.getKey();
            Title t = e.getValue();

            String name = t.displayName == null ? id.toString() : t.displayName.getString();
            String desc = t.description == null ? "" : t.description.getString();

            List<TitlePayloads.SyncDefinitions.BonusDef> bdefs = new ArrayList<>();
            List<TitlePayloads.SyncDefinitions.DamageBonusDef> ddefs = new ArrayList<>();
            List<Identifier> sdefs = new ArrayList<>();
            List<Identifier> pdefs = new ArrayList<>();
            for (Title.Bonus b : t.bonuses) {
                if (b == null) continue;

                if (b.spellId != null && b.spellId.isPresent()) {
                    sdefs.add(b.spellId.get());
                    continue;
                }
                if (b.powerId != null && b.powerId.isPresent()) {
                    pdefs.add(b.powerId.get());
                    continue;
                }
                if (b.damageTarget != null && b.damageTarget.isPresent()) {
                    var op = (b.damageOp == Title.DamageOp.MULTIPLIED)
                            ? TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED
                            : TitlePayloads.SyncDefinitions.DmgOp.ADDED;
                    ddefs.add(new TitlePayloads.SyncDefinitions.DamageBonusDef(b.damageTarget, Optional.empty(), b.damageAmount, op));
                } else if (b.damageTag != null && b.damageTag.isPresent()) {
                    var op = (b.damageOp == Title.DamageOp.MULTIPLIED)
                            ? TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED
                            : TitlePayloads.SyncDefinitions.DmgOp.ADDED;
                    ddefs.add(new TitlePayloads.SyncDefinitions.DamageBonusDef(Optional.empty(), b.damageTag, b.damageAmount, op));
                }
                if (b.attribute != null) {
                    Identifier attrId = Registries.ATTRIBUTE.getId(b.attribute.value());
                    if (attrId != null) {
                        bdefs.add(new TitlePayloads.SyncDefinitions.BonusDef(attrId, b.amount, b.operation));
                    }
                }
            }

            List<TitlePayloads.SyncDefinitions.BonusDef> p_bdefs = new ArrayList<>();
            List<TitlePayloads.SyncDefinitions.DamageBonusDef> p_ddefs = new ArrayList<>();
            List<Identifier> p_sdefs = new ArrayList<>();
            List<Identifier> p_pdefs = new ArrayList<>();

            for (Title.Bonus b : t.permaBonuses) {
                if (b.spellId != null && b.spellId.isPresent()) { p_sdefs.add(b.spellId.get()); continue; }
                if (b.powerId != null && b.powerId.isPresent()) { p_pdefs.add(b.powerId.get()); continue; }
                if (b.damageTarget != null && b.damageTarget.isPresent()) {
                    var op = (b.damageOp == Title.DamageOp.MULTIPLIED)
                            ? TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED
                            : TitlePayloads.SyncDefinitions.DmgOp.ADDED;
                    p_ddefs.add(new TitlePayloads.SyncDefinitions.DamageBonusDef(b.damageTarget, Optional.empty(), b.damageAmount, op));
                } else if (b.damageTag != null && b.damageTag.isPresent()) {
                    var op = (b.damageOp == Title.DamageOp.MULTIPLIED)
                            ? TitlePayloads.SyncDefinitions.DmgOp.MULTIPLIED
                            : TitlePayloads.SyncDefinitions.DmgOp.ADDED;
                    p_ddefs.add(new TitlePayloads.SyncDefinitions.DamageBonusDef(Optional.empty(), b.damageTag, b.damageAmount, op));
                }
                if (b.attribute != null) {
                    Identifier attrId = Registries.ATTRIBUTE.getId(b.attribute.value());
                    if (attrId != null) p_bdefs.add(new TitlePayloads.SyncDefinitions.BonusDef(attrId, b.amount, b.operation));
                }
            }

            List<TitlePayloads.SyncDefinitions.ConditionDef> cdefs = new ArrayList<>();
            for (Title.Condition c : t.conditions) {
                TitlePayloads.SyncDefinitions.CondType type = switch (c.type) {
                    case OBTAIN_ITEM           -> TitlePayloads.SyncDefinitions.CondType.OBTAIN_ITEM;
                    case KILL_MOBS             -> TitlePayloads.SyncDefinitions.CondType.KILL_MOBS;
                    case ADVANCEMENT           -> TitlePayloads.SyncDefinitions.CondType.ADVANCEMENT;
                    case WALK_BLOCKS           -> TitlePayloads.SyncDefinitions.CondType.WALK_BLOCKS;
                    case REACH_LEVEL           -> TitlePayloads.SyncDefinitions.CondType.REACH_LEVEL;
                    case REACH_LEVEL_XP        -> TitlePayloads.SyncDefinitions.CondType.REACH_LEVEL_XP;
                    case REACH_LEVEL_PUFFERFISH-> TitlePayloads.SyncDefinitions.CondType.REACH_LEVEL_PUFFERFISH;
                    case CRAFT_ITEM            -> TitlePayloads.SyncDefinitions.CondType.CRAFT_ITEM;
                    case MINE_BLOCKS           -> TitlePayloads.SyncDefinitions.CondType.MINE_BLOCKS;
                    case VISIT_BIOME           -> TitlePayloads.SyncDefinitions.CondType.VISIT_BIOME;
                    case ENTER_DIMENSION       -> TitlePayloads.SyncDefinitions.CondType.ENTER_DIMENSION;
                    case INTERACT_BLOCK        -> TitlePayloads.SyncDefinitions.CondType.INTERACT_BLOCK;
                    case INTERACT_ENTITY       -> TitlePayloads.SyncDefinitions.CondType.INTERACT_ENTITY;
                    case FIND_STRUCTURE        -> TitlePayloads.SyncDefinitions.CondType.FIND_STRUCTURE;
                    case DEAL_DAMAGE_TOTAL     -> TitlePayloads.SyncDefinitions.CondType.DEAL_DAMAGE_TOTAL;
                    case DEAL_DAMAGE_MAX       -> TitlePayloads.SyncDefinitions.CondType.DEAL_DAMAGE_MAX;
                    case CHECK_ATTRIBUTE       -> TitlePayloads.SyncDefinitions.CondType.CHECK_ATTRIBUTE;
                };

                cdefs.add(new TitlePayloads.SyncDefinitions.ConditionDef(
                        type, c.item, c.entityType, c.advancement,
                        c.distance, c.count, c.hint, c.hidden,
                        c.entitySpec, c.nbtQuery, c.level, c.block, c.biome, c.dimension,
                        c.structure, c.attributeId, c.minValue,
                        c.entityTagId
                ));
            }

            defs.add(new TitlePayloads.SyncDefinitions.Def(
                    id, name, Optional.ofNullable(desc.isEmpty() ? null : desc),
                    bdefs, sdefs, pdefs, ddefs,
                    p_bdefs, p_sdefs, p_pdefs, p_ddefs,
                    cdefs, t.hidden
            ));
        }
        ServerPlayNetworking.send(player, new TitlePayloads.SyncDefinitions(defs));
    }

    private static void broadcastActiveToAll(MinecraftServer server, UUID playerUuid, Optional<Identifier> active) {
        TitlePayloads.SyncActive pkt = new TitlePayloads.SyncActive(playerUuid, active);
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(sp, pkt);
        }
    }

    private static void broadcastAllActivesTo(MinecraftServer server, ServerPlayerEntity targetPlayer) {
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            Optional<Identifier> active = activeOf(server, sp.getUuid());
            ServerPlayNetworking.send(targetPlayer, new TitlePayloads.SyncActive(sp.getUuid(), active));
        }
    }
}
