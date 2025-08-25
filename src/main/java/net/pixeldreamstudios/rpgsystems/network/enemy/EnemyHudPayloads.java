package net.pixeldreamstudios.rpgsystems.network.enemy;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class EnemyHudPayloads {
    private EnemyHudPayloads() {}

    public record DamageNumber(int entityId, float amount, boolean crit, boolean isPet, int rgb, UUID sourceUuid)
            implements CustomPayload {
        public static final Id<DamageNumber> ID = new Id<>(Identifier.of("rpg-systems", "enemy_damage_number"));
        public static final PacketCodec<RegistryByteBuf, DamageNumber> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, DamageNumber::entityId,
                PacketCodecs.FLOAT,   DamageNumber::amount,
                PacketCodecs.BOOL,    DamageNumber::crit,
                PacketCodecs.BOOL,    DamageNumber::isPet,
                PacketCodecs.INTEGER, DamageNumber::rgb,
                Uuids.PACKET_CODEC,   DamageNumber::sourceUuid,
                DamageNumber::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record HealingNumber(int entityId, float amount, boolean isSpell, UUID sourceUuid) implements CustomPayload {
        public static final Id<HealingNumber> ID = new Id<>(Identifier.of("rpg-systems", "ally_heal_number"));
        public static final PacketCodec<RegistryByteBuf, HealingNumber> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, HealingNumber::entityId,
                PacketCodecs.FLOAT,   HealingNumber::amount,
                PacketCodecs.BOOL,    HealingNumber::isSpell,
                Uuids.PACKET_CODEC,   HealingNumber::sourceUuid,
                HealingNumber::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
