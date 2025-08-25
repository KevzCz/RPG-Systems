package net.pixeldreamstudios.rpgsystems.network.party;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class PartyStatusEffectsSync {
    private PartyStatusEffectsSync() {}

    public static void sendMemberEffects(ServerPlayerEntity subject, Collection<ServerPlayerEntity> targets) {
        if (subject == null || targets == null || targets.isEmpty()) return;
        List<String> ids = snapshotEffectIds(subject);
        PartyStatusEffectsPayloads.MemberEffects payload =
                new PartyStatusEffectsPayloads.MemberEffects(subject.getUuid(), ids);
        for (ServerPlayerEntity to : targets) {
            if (to == null) continue;
            ServerPlayNetworking.send(to, payload);
        }
    }

    public static List<String> snapshotEffectIds(ServerPlayerEntity player) {
        List<String> out = new ArrayList<>();
        for (StatusEffectInstance inst : player.getStatusEffects()) {
            var entry = inst.getEffectType();
            var id = Registries.STATUS_EFFECT.getId(entry.value());
            if (id != null) out.add(id.toString());
        }
        return out;
    }

    public static void sendEmpty(UUID subject, Collection<ServerPlayerEntity> targets) {
        PartyStatusEffectsPayloads.MemberEffects payload =
                new PartyStatusEffectsPayloads.MemberEffects(subject, List.of());
        for (ServerPlayerEntity to : targets) {
            if (to == null) continue;
            ServerPlayNetworking.send(to, payload);
        }
    }
}
