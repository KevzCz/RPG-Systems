package net.pixeldreamstudios.rpgsystems.title.power;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;

public final class PowerRegistry {
    private static final Map<Identifier, TitlePower> POWERS = new HashMap<>();
    private static final Map<Identifier, Set<UUID>> ACTIVE = new HashMap<>();
    private static boolean registeredTick = false;

    private PowerRegistry() {}

    public static void register(TitlePower power) {
        Objects.requireNonNull(power, "power");
        Identifier id = power.id();
        POWERS.put(id, power);
        ACTIVE.computeIfAbsent(id, k -> new HashSet<>());
    }

    public static Optional<TitlePower> get(Identifier id) {
        return Optional.ofNullable(POWERS.get(id));
    }

    public static void activate(Identifier powerId, ServerPlayerEntity player) {
        TitlePower power = POWERS.get(powerId);
        if (power == null) {
            return;
        }
        Set<UUID> set = ACTIVE.computeIfAbsent(powerId, k -> new HashSet<>());
        if (set.add(player.getUuid())) {
            power.onActivate(player);
            }
    }

    public static void deactivate(Identifier powerId, ServerPlayerEntity player) {
        TitlePower power = POWERS.get(powerId);
        if (power == null) return;
        Set<UUID> set = ACTIVE.get(powerId);
        if (set != null && set.remove(player.getUuid())) {
            power.onDeactivate(player);
            }
    }

    public static void registerServerTick() {
        if (registeredTick) return;
        registeredTick = true;

        ServerTickEvents.START_SERVER_TICK.register(PowerRegistry::onServerTick);

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.player;
            UUID uuid = player.getUuid();
            for (Map.Entry<Identifier, Set<UUID>> e : ACTIVE.entrySet()) {
                if (e.getValue().remove(uuid)) {
                    TitlePower p = POWERS.get(e.getKey());
                    if (p != null) p.onDeactivate(player);
                }
            }
        });
    }

    private static void onServerTick(MinecraftServer server) {
        for (Map.Entry<Identifier, Set<UUID>> e : ACTIVE.entrySet()) {
            Identifier id = e.getKey();
            TitlePower power = POWERS.get(id);
            if (power == null) continue;
            Set<ServerPlayerEntity> players = new HashSet<>();
            Iterator<UUID> it = e.getValue().iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(uuid);
                if (sp == null) {
                    it.remove();
                    continue;
                }
                players.add(sp);
            }
            if (!players.isEmpty()) {
                power.onServerTick(server, players);
            }
        }
    }
    public static boolean isActive(Identifier powerId, java.util.UUID uuid) {
        var set = ACTIVE.get(powerId);
        return set != null && set.contains(uuid);
    }

}
