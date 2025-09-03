package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;

import java.util.*;

public final class TitleNet {
    private TitleNet() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(TitlePayloads.RequestSetActive.ID, TitlePayloads.RequestSetActive.CODEC);

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
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncSelf.ID, TitlePayloads.SyncSelf.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncActive.ID, TitlePayloads.SyncActive.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncDefinitions.ID, TitlePayloads.SyncDefinitions.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncProgress.ID, TitlePayloads.SyncProgress.CODEC);

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

        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncDefinitions.ID, (payload, context) ->
                context.client().execute(() -> {
                    Map<Identifier, Title> map = new LinkedHashMap<>();
                    for (TitlePayloads.SyncDefinitions.Def d : payload.defs()) {
                        Identifier id = d.id();
                        Text name = Text.literal(d.name());
                        Text desc = d.description().map(Text::literal).orElse(Text.empty());

                        Title.Builder b = Title.builder(id, name).description(desc);

                        // attributes (unchanged)
                        for (TitlePayloads.SyncDefinitions.BonusDef jb : d.bonuses()) {
                            RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, jb.attribute());
                            RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                                    .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + jb.attribute()));
                            b.add(entry, jb.amount(), jb.operation());
                        }

                        // NEW: spells from payload
                        for (Identifier sid : d.spells()) {
                            b.addSpell(sid);
                        }

                        // conditions (unchanged)
                        for (TitlePayloads.SyncDefinitions.ConditionDef cd : d.conditions()) {
                            Title.Condition.Type t = switch (cd.type()) {
                                case OBTAIN_ITEM     -> Title.Condition.Type.OBTAIN_ITEM;
                                case KILL_MOBS       -> Title.Condition.Type.KILL_MOBS;
                                case ADVANCEMENT     -> Title.Condition.Type.ADVANCEMENT;
                                case WALK_BLOCKS     -> Title.Condition.Type.WALK_BLOCKS;
                                case REACH_LEVEL     -> Title.Condition.Type.REACH_LEVEL;
                                case CRAFT_ITEM      -> Title.Condition.Type.CRAFT_ITEM;
                                case MINE_BLOCKS     -> Title.Condition.Type.MINE_BLOCKS;
                                case VISIT_BIOME     -> Title.Condition.Type.VISIT_BIOME;
                                case ENTER_DIMENSION -> Title.Condition.Type.ENTER_DIMENSION;
                            };
                            b.addCondition(new Title.Condition(
                                    t, cd.item(), cd.entityType(), cd.advancement(),
                                    cd.distance(), cd.count(), cd.hint(), cd.hidden(),
                                    cd.entitySpec(), cd.nbtQuery(), cd.level(),
                                    cd.block(), cd.biome(), cd.dimension()
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

    public static void syncProgressTo(MinecraftServer server, ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

        List<net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads.SyncProgress.TitleProgress> out = new ArrayList<>();

        for (Map.Entry<Identifier, Title> e : TitleRegistry.all().entrySet()) {
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

            // Collect attribute bonuses and spell bonuses separately
            List<TitlePayloads.SyncDefinitions.BonusDef> bdefs = new ArrayList<>();
            List<Identifier> sdefs = new ArrayList<>();
            for (Title.Bonus b : t.bonuses) {
                if (b == null) continue;

                // Spell bonus?
                if (b.spellId != null && b.spellId.isPresent()) {
                    sdefs.add(b.spellId.get());
                    continue;
                }

                // Attribute bonus?
                if (b.attribute != null) {
                    Identifier attrId = Registries.ATTRIBUTE.getId(b.attribute.value());
                    if (attrId != null) {
                        bdefs.add(new TitlePayloads.SyncDefinitions.BonusDef(attrId, b.amount, b.operation));
                    }
                }
            }

            List<TitlePayloads.SyncDefinitions.ConditionDef> cdefs = new ArrayList<>();
            for (Title.Condition c : t.conditions) {
                TitlePayloads.SyncDefinitions.CondType type = switch (c.type) {
                    case OBTAIN_ITEM     -> TitlePayloads.SyncDefinitions.CondType.OBTAIN_ITEM;
                    case KILL_MOBS       -> TitlePayloads.SyncDefinitions.CondType.KILL_MOBS;
                    case ADVANCEMENT     -> TitlePayloads.SyncDefinitions.CondType.ADVANCEMENT;
                    case WALK_BLOCKS     -> TitlePayloads.SyncDefinitions.CondType.WALK_BLOCKS;
                    case REACH_LEVEL     -> TitlePayloads.SyncDefinitions.CondType.REACH_LEVEL;
                    case CRAFT_ITEM      -> TitlePayloads.SyncDefinitions.CondType.CRAFT_ITEM;
                    case MINE_BLOCKS     -> TitlePayloads.SyncDefinitions.CondType.MINE_BLOCKS;
                    case VISIT_BIOME     -> TitlePayloads.SyncDefinitions.CondType.VISIT_BIOME;
                    case ENTER_DIMENSION -> TitlePayloads.SyncDefinitions.CondType.ENTER_DIMENSION;
                };
                cdefs.add(new TitlePayloads.SyncDefinitions.ConditionDef(
                        type, c.item, c.entityType, c.advancement,
                        c.distance, c.count, c.hint, c.hidden,
                        c.entitySpec, c.nbtQuery, c.level, c.block, c.biome, c.dimension
                ));
            }

            defs.add(new TitlePayloads.SyncDefinitions.Def(
                    id,
                    name,
                    Optional.ofNullable(desc.isEmpty() ? null : desc),
                    bdefs,
                    sdefs,
                    cdefs
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
