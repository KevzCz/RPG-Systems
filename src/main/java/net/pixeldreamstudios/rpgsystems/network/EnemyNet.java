// net/pixeldreamstudios/rpgsystems/network/EnemyNet.java
package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.client.enemy.DamageNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.HealingNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.network.enemy.EnemyHudPayloads;

public final class EnemyNet {
    private EnemyNet() {}

    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.DamageNumber.ID, EnemyHudPayloads.DamageNumber.CODEC);
        PayloadTypeRegistry.playS2C().register(EnemyHudPayloads.HealingNumber.ID, EnemyHudPayloads.HealingNumber.CODEC);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(EnemyHudPayloads.DamageNumber.ID,
                (payload, context) -> context.client().execute(() ->
                        DamageNumbersRenderer.spawn(payload.entityId(), payload.amount(), payload.crit(), payload.isPet())
                ));
        ClientPlayNetworking.registerGlobalReceiver(EnemyHudPayloads.HealingNumber.ID,
                (payload, context) -> context.client().execute(() ->
                        HealingNumbersRenderer.spawn(payload.entityId(), payload.amount(), payload.isSpell())
                ));
    }

    // --- server helpers ---

    // Back-compat helpers (assume non-pet)
    public static void sendDamageNumber(ServerPlayerEntity recipient, Entity target, float amount, boolean crit) {
        sendDamageNumber(recipient, target, amount, crit, false);
    }
    public static void sendDamageNumber(Iterable<ServerPlayerEntity> recipients, Entity target, float amount, boolean crit) {
        sendDamageNumber(recipients, target, amount, crit, false);
    }

    // New helpers with pet flag
    public static void sendDamageNumber(ServerPlayerEntity recipient, Entity target, float amount, boolean crit, boolean isPet) {
        if (recipient == null || target == null) return;
        ServerPlayNetworking.send(recipient, new EnemyHudPayloads.DamageNumber(target.getId(), amount, crit, isPet));
    }
    public static void sendDamageNumber(Iterable<ServerPlayerEntity> recipients, Entity target, float amount, boolean crit, boolean isPet) {
        if (recipients == null || target == null) return;
        var pkt = new EnemyHudPayloads.DamageNumber(target.getId(), amount, crit, isPet);
        for (ServerPlayerEntity sp : recipients) if (sp != null) ServerPlayNetworking.send(sp, pkt);
    }

    public static void sendHealingNumber(ServerPlayerEntity recipient, Entity target, float amount, boolean isSpell) {
        if (recipient == null || target == null) return;
        ServerPlayNetworking.send(recipient, new EnemyHudPayloads.HealingNumber(target.getId(), amount, isSpell));
    }
    public static void sendHealingNumber(Iterable<ServerPlayerEntity> recipients, Entity target, float amount, boolean isSpell) {
        if (recipients == null || target == null) return;
        var pkt = new EnemyHudPayloads.HealingNumber(target.getId(), amount, isSpell);
        for (ServerPlayerEntity sp : recipients) if (sp != null) ServerPlayNetworking.send(sp, pkt);
    }
}
