// net/pixeldreamstudios/rpgsystems/network/enemy/EnemyHudPayloads.java
package net.pixeldreamstudios.rpgsystems.network.enemy;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class EnemyHudPayloads {
    private EnemyHudPayloads() {}

    /** S2C: show a floating damage number above a target entity. */
    public record DamageNumber(int entityId, float amount, boolean crit, boolean isPet) implements CustomPayload {
        public static final Id<DamageNumber> ID =
                new Id<>(Identifier.of("rpg-systems", "enemy_damage_number"));
        public static final PacketCodec<RegistryByteBuf, DamageNumber> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, DamageNumber::entityId,
                        PacketCodecs.FLOAT,   DamageNumber::amount,
                        PacketCodecs.BOOL,    DamageNumber::crit,
                        PacketCodecs.BOOL,    DamageNumber::isPet,
                        DamageNumber::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: show a floating healing number above a target entity. (unchanged) */
    public record HealingNumber(int entityId, float amount, boolean isSpell) implements CustomPayload {
        public static final Id<HealingNumber> ID =
                new Id<>(Identifier.of("rpg-systems", "enemy_healing_number"));
        public static final PacketCodec<RegistryByteBuf, HealingNumber> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, HealingNumber::entityId,
                        PacketCodecs.FLOAT,   HealingNumber::amount,
                        PacketCodecs.BOOL,    HealingNumber::isSpell,
                        HealingNumber::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
