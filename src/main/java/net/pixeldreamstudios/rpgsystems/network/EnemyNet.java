package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.rpgsystems.client.enemy.DamageNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.EnemyHealthBarRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.HealingNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.DamageTypeConfig;
import net.pixeldreamstudios.rpgsystems.network.enemy.EnemyHudPayloads;

import java.util.UUID;

public final class EnemyNet {
    private EnemyNet() {}
    private static final double BROADCAST_RANGE = 64.0;
    private static final double BROADCAST_RANGE_SQ = BROADCAST_RANGE * BROADCAST_RANGE;

    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.DamageNumber.ID, EnemyHudPayloads.DamageNumber.CODEC);
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.HealingNumber.ID, EnemyHudPayloads.HealingNumber.CODEC);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        // EnemyNet.initClient()
        ClientPlayNetworking.registerGlobalReceiver(EnemyHudPayloads.DamageNumber.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    // make sure we remember unknown types the first time we see them
                    DamageTypeConfig.seenDamageType(payload.damageTypeId());

                    var cfg = DamageTypeConfig.get();
                    // client-side filter
                    if (!cfg.shouldShowType(payload.damageTypeId())) return;

                    // optional per-type color override
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


    private static void forNearbyPlayers(Entity target, java.util.function.Consumer<ServerPlayerEntity> send) {
        MinecraftServer server = target.getServer();
        Vec3d pos = target.getPos();
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            if (sp.getWorld() != target.getWorld()) continue;
            if (sp.squaredDistanceTo(pos) <= BROADCAST_RANGE_SQ) send.accept(sp);
        }
    }
}
