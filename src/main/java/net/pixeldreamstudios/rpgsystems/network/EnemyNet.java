package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.rpgsystems.client.enemy.DamageNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.EnemyHealthBarRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.HealingNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.DamageTypeConfig;
import net.pixeldreamstudios.rpgsystems.network.enemy.EnemyHudPayloads;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class EnemyNet {
    private EnemyNet() {}
    private static final double BROADCAST_RANGE = 64.0;
    private static final double BROADCAST_RANGE_SQ = BROADCAST_RANGE * BROADCAST_RANGE;
    private static final long ABS_SYNC_INTERVAL_TICKS = 20;
    private static final float ABS_EPS = 0.02f;

    private static final Map<String, Float> lastAbsSent = new HashMap<>();
    private static final Map<String, Long>  lastAbsAt   = new HashMap<>();

    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.DamageNumber.ID, EnemyHudPayloads.DamageNumber.CODEC);
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.HealingNumber.ID, EnemyHudPayloads.HealingNumber.CODEC);
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.AbsorptionSync.ID, EnemyHudPayloads.AbsorptionSync.CODEC);

        ServerTickEvents.END_SERVER_TICK.register(server -> pushNearbyAbsorption(server));
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(EnemyHudPayloads.DamageNumber.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    DamageTypeConfig.seenDamageType(payload.damageTypeId());

                    var cfg = DamageTypeConfig.get();
                    if (!cfg.shouldShowType(payload.damageTypeId())) return;

                    Integer override = cfg.colorOverride(payload.damageTypeId());
                    int rgb = override != null ? override : payload.rgb();

                    DamageNumbersRenderer.spawn(
                            payload.entityId(),
                            payload.amount(),
                            payload.crit(),
                            payload.isPet(),
                            rgb,
                            payload.sourceUuid(),
                            payload.damageTypeId()
                    );
                    EnemyHealthBarRenderer.pokeOnHpChange(payload.entityId());
                }));

        ClientPlayNetworking.registerGlobalReceiver(EnemyHudPayloads.HealingNumber.ID,
                (payload, context) -> context.client().execute(() -> {
                    var cfg = net.pixeldreamstudios.rpgsystems.client.enemy.config.HealingNumbersClientConfig.get();
                    float display = (cfg.amountMode == net.pixeldreamstudios.rpgsystems.client.enemy.config.HealingNumbersClientConfig.AmountMode.APPLIED)
                            ? payload.applied()
                            : payload.attempted();

                    HealingNumbersRenderer.spawn(
                            payload.entityId(),
                            display,
                            payload.isSpell(),
                            payload.sourceUuid()
                    );
                    EnemyHealthBarRenderer.pokeOnHpChange(payload.entityId());
                }));

        ClientPlayNetworking.registerGlobalReceiver(EnemyHudPayloads.AbsorptionSync.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        EnemyHealthBarRenderer.onAbsorptionSync(payload.entityId(), payload.absorption())
                ));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var rm = handler.getRegistryManager();
            DamageTypeConfig.maybePopulateFromRegistry(rm);
        });
    }
    public static void broadcastDamageNumber(Entity target, float amount, boolean crit, boolean isPet, int rgb, UUID sourceUuid, Identifier damageTypeId) {
        if (target == null || target.getServer() == null) return;
        var pkt = new EnemyHudPayloads.DamageNumber(target.getId(), amount, crit, isPet, rgb, sourceUuid, damageTypeId);
        forNearbyPlayers(target, sp -> ServerPlayNetworking.send(sp, pkt));
    }
    public static void broadcastHealingNumber(Entity target, float attempted, float applied, boolean isSpell, UUID sourceUuid) {
        if (target == null || target.getServer() == null) return;
        var pkt = new EnemyHudPayloads.HealingNumber(target.getId(), attempted, applied, isSpell, sourceUuid);
        forNearbyPlayers(target, sp -> ServerPlayNetworking.send(sp, pkt));
    }
    private static void broadcastAbsorptionToViewer(ServerPlayerEntity viewer, LivingEntity target, float absorption) {
        ServerPlayNetworking.send(viewer,
                new EnemyHudPayloads.AbsorptionSync(target.getId(), absorption));
    }
    private static void forNearbyPlayers(Entity target, Consumer<ServerPlayerEntity> send) {
        MinecraftServer server = target.getServer();
        Vec3d pos = target.getPos();
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            if (sp.getWorld() != target.getWorld()) continue;
            if (sp.squaredDistanceTo(pos) <= BROADCAST_RANGE_SQ) send.accept(sp);
        }
    }
    private static void pushNearbyAbsorption(MinecraftServer server) {
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            var world = viewer.getWorld();
            Box box = viewer.getBoundingBox().expand(BROADCAST_RANGE);
            for (LivingEntity le : world.getEntitiesByClass(LivingEntity.class, box, e -> true)) {
                if (le == viewer) continue;
                if (le.isRemoved()) continue;

                double dSq = viewer.squaredDistanceTo(le);
                if (dSq > BROADCAST_RANGE_SQ) continue;

                float abs = le.getAbsorptionAmount();
                String key = viewer.getUuid() + ":" + le.getId();
                float prev = lastAbsSent.getOrDefault(key, Float.NaN);
                long  at   = lastAbsAt.getOrDefault(key, 0L);
                long  now  = server.getOverworld().getTime();

                boolean changed = Float.isNaN(prev) || Math.abs(abs - prev) > ABS_EPS;
                boolean timed   = (now - at) >= ABS_SYNC_INTERVAL_TICKS;

                if (changed || timed) {
                    broadcastAbsorptionToViewer(viewer, le, abs);
                    lastAbsSent.put(key, abs);
                    lastAbsAt.put(key, now);
                }
            }
        }
    }
}
