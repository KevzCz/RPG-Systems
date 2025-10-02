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

    public record DamageNumber(
            int entityId,
            float amount,
            boolean crit,
            boolean isPet,
            int rgb,
            UUID sourceUuid,
            Identifier damageTypeId
    ) implements CustomPayload {
        public static final Id<DamageNumber> ID =
                new Id<>(Identifier.of("rpg-systems", "enemy_damage_number"));

        public static final PacketCodec<RegistryByteBuf, DamageNumber> CODEC =
                new PacketCodec<>() {
                    @Override
                    public DamageNumber decode(RegistryByteBuf buf) {
                        int entityId   = PacketCodecs.VAR_INT.decode(buf);
                        float amount   = PacketCodecs.FLOAT.decode(buf);
                        boolean crit   = PacketCodecs.BOOL.decode(buf);
                        boolean isPet  = PacketCodecs.BOOL.decode(buf);
                        int rgb        = PacketCodecs.INTEGER.decode(buf);
                        UUID srcUuid   = Uuids.PACKET_CODEC.decode(buf);
                        Identifier dmgId = Identifier.PACKET_CODEC.decode(buf);

                        return new DamageNumber(entityId, amount, crit, isPet, rgb, srcUuid, dmgId);
                    }

                    @Override
                    public void encode(RegistryByteBuf buf, DamageNumber v) {
                        PacketCodecs.VAR_INT.encode(buf, v.entityId());
                        PacketCodecs.FLOAT.encode(buf,   v.amount());
                        PacketCodecs.BOOL.encode(buf,    v.crit());
                        PacketCodecs.BOOL.encode(buf,    v.isPet());
                        PacketCodecs.INTEGER.encode(buf, v.rgb());
                        Uuids.PACKET_CODEC.encode(buf,   v.sourceUuid());
                        Identifier.PACKET_CODEC.encode(buf, v.damageTypeId());
                    }
                };

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record HealingNumber(int entityId, float attempted, float applied, boolean isSpell, UUID sourceUuid)
            implements CustomPayload {
        public static final Id<HealingNumber> ID =
                new Id<>(Identifier.of("rpg-systems", "ally_heal_number"));

        public static final PacketCodec<RegistryByteBuf, HealingNumber> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, HealingNumber::entityId,
                        PacketCodecs.FLOAT,   HealingNumber::attempted,
                        PacketCodecs.FLOAT,   HealingNumber::applied,
                        PacketCodecs.BOOL,    HealingNumber::isSpell,
                        Uuids.PACKET_CODEC,   HealingNumber::sourceUuid,
                        HealingNumber::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record AbsorptionSync(int entityId, float absorption)
            implements CustomPayload {
        public static final Id<AbsorptionSync> ID =
                new Id<>(Identifier.of("rpg-systems", "enemy_absorption_sync"));

        public static final PacketCodec<RegistryByteBuf, AbsorptionSync> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, AbsorptionSync::entityId,
                        PacketCodecs.FLOAT,   AbsorptionSync::absorption,
                        AbsorptionSync::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
