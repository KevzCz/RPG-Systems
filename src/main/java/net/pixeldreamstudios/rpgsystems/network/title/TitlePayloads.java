package net.pixeldreamstudios.rpgsystems.network.title;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TitlePayloads {
    private TitlePayloads() {}

    public record SyncSelf(List<Identifier> unlocked, Optional<Identifier> active) implements CustomPayload {
        public static final Id<SyncSelf> ID = new Id<>(Identifier.of("rpg-systems", "titles_self_sync"));

        public static final PacketCodec<RegistryByteBuf, SyncSelf> CODEC = PacketCodec.tuple(
                PacketCodecs.collection(ArrayList::new, PacketCodecs.registryCodec(Identifier.CODEC)),
                SyncSelf::unlocked,
                PacketCodecs.optional(PacketCodecs.registryCodec(Identifier.CODEC)),
                SyncSelf::active,
                SyncSelf::new
        );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SyncActive(UUID playerUuid, Optional<Identifier> active) implements CustomPayload {
        public static final Id<SyncActive> ID = new Id<>(Identifier.of("rpg-systems", "titles_active_sync"));

        public static final PacketCodec<RegistryByteBuf, SyncActive> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC,
                SyncActive::playerUuid,
                PacketCodecs.optional(PacketCodecs.registryCodec(Identifier.CODEC)),
                SyncActive::active,
                SyncActive::new
        );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestSetActive(Optional<Identifier> active) implements CustomPayload {
        public static final Id<RequestSetActive> ID = new Id<>(Identifier.of("rpg-systems", "titles_set_active"));

        public static final PacketCodec<RegistryByteBuf, RequestSetActive> CODEC = PacketCodec.tuple(
                PacketCodecs.optional(PacketCodecs.registryCodec(Identifier.CODEC)),
                RequestSetActive::active,
                RequestSetActive::new
        );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SyncDefinitions(List<Def> defs) implements CustomPayload {
        public static final Id<SyncDefinitions> ID = new Id<>(Identifier.of("rpg-systems", "titles_defs_sync"));

        public static final PacketCodec<RegistryByteBuf, SyncDefinitions> CODEC = PacketCodec.tuple(
                PacketCodecs.collection(ArrayList::new, Def.CODEC),
                SyncDefinitions::defs,
                SyncDefinitions::new
        );

        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public record BonusDef(Identifier attribute, double amount, EntityAttributeModifier.Operation operation) {
            private static final PacketCodec<ByteBuf, EntityAttributeModifier.Operation> OP_CODEC =
                    PacketCodecs.VAR_INT.xmap(
                            i -> EntityAttributeModifier.Operation.values()[i],
                            EntityAttributeModifier.Operation::ordinal
                    );

            public static final PacketCodec<RegistryByteBuf, BonusDef> CODEC = PacketCodec.tuple(
                    PacketCodecs.registryCodec(Identifier.CODEC), BonusDef::attribute,
                    PacketCodecs.DOUBLE,                              BonusDef::amount,
                    OP_CODEC,                                         BonusDef::operation,
                    BonusDef::new
            );
        }


        public record Def(Identifier id, String name, Optional<String> description, List<BonusDef> bonuses) {
            public static final PacketCodec<RegistryByteBuf, Def> CODEC = PacketCodec.tuple(
                    PacketCodecs.registryCodec(Identifier.CODEC), Def::id,
                    PacketCodecs.STRING, Def::name,
                    PacketCodecs.optional(PacketCodecs.STRING), Def::description,
                    PacketCodecs.collection(ArrayList::new, BonusDef.CODEC), Def::bonuses,
                    Def::new
            );
        }
    }
}
