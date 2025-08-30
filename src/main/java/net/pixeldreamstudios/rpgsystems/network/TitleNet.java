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
import net.pixeldreamstudios.rpgsystems.api.TitleApi;
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
            boolean ok = TitleApi.setActive(player, requested);
            if (ok) {
                broadcastActiveToAll(player.getServer(), player.getUuid(), requested);
                syncSelfTo(player.getServer(), player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendDefinitionsTo(server, handler.player);
            syncSelfTo(server, handler.player);
            broadcastActiveToAll(server, handler.player.getUuid(), activeOf(server, handler.player.getUuid()));
            broadcastAllActivesTo(server, handler.player);
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncSelf.ID, TitlePayloads.SyncSelf.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncActive.ID, TitlePayloads.SyncActive.CODEC);
        PayloadTypeRegistry.playS2C().register(TitlePayloads.SyncDefinitions.ID, TitlePayloads.SyncDefinitions.CODEC);

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

        ClientPlayNetworking.registerGlobalReceiver(TitlePayloads.SyncDefinitions.ID, (payload, context) -> {
            context.client().execute(() -> {
                Map<Identifier, Title> map = new HashMap<>();
                for (TitlePayloads.SyncDefinitions.Def d : payload.defs()) {
                    Text name = Text.literal(d.name());
                    Text desc = d.description().map(Text::literal)
                            .orElseGet(() -> Text.translatable("title." + d.id().getNamespace() + "." + d.id().getPath() + ".desc"));

                    Title.Builder b = Title.builder(d.id(), name).description(desc);

                    for (TitlePayloads.SyncDefinitions.BonusDef jb : d.bonuses()) {
                        RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, jb.attribute());
                        RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + jb.attribute()));
                        b.add(entry, jb.amount(), jb.operation());
                    }

                    map.put(d.id(), b.build());
                }
                TitleRegistry.replaceAll(map);
                TitleRegistry.bootstrapFallback();
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TitleClientData.clear());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> TitleClientData.clear());
    }

    private static Optional<Identifier> activeOf(MinecraftServer server, UUID uuid) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(uuid);
        return Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));
    }

    private static void syncSelfTo(MinecraftServer server, ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

        List<Identifier> unlocked = new ArrayList<>();
        for (String s : pt.unlocked) unlocked.add(Identifier.of(s));
        Optional<Identifier> active = Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));

        ServerPlayNetworking.send(player, new TitlePayloads.SyncSelf(unlocked, active));
    }

    private static void sendDefinitionsTo(MinecraftServer server, ServerPlayerEntity player) {
        List<TitlePayloads.SyncDefinitions.Def> defs = new ArrayList<>();
        for (Map.Entry<Identifier, Title> e : TitleRegistry.all().entrySet()) {
            Identifier id = e.getKey();
            Title t = e.getValue();

            String name = t.displayName.getString();
            String desc = t.description.getString();

            List<TitlePayloads.SyncDefinitions.BonusDef> bdefs = new ArrayList<>();
            for (Title.Bonus b : t.bonuses) {
                Identifier attrId = b.attribute.getKey()
                        .map(RegistryKey::getValue)
                        .orElseThrow(() -> new IllegalStateException("Unregistered attribute on title " + id));
                bdefs.add(new TitlePayloads.SyncDefinitions.BonusDef(attrId, b.amount, b.operation));
            }

            defs.add(new TitlePayloads.SyncDefinitions.Def(
                    id,
                    name,
                    Optional.ofNullable(desc.isEmpty() ? null : desc),
                    bdefs
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
