package net.pixeldreamstudios.rpgsystems.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class PartyHudPayloads {
    private PartyHudPayloads() {}

    public record PartyRosterClear(UUID partyId, String partyName, UUID leaderUuid) implements CustomPayload {
        public static final Id<PartyRosterClear> ID = new Id<>(Identifier.of("rpg-systems","party_roster_clear"));
        public static final PacketCodec<RegistryByteBuf, PartyRosterClear> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, PartyRosterClear::partyId,
                PacketCodecs.STRING, PartyRosterClear::partyName,
                Uuids.PACKET_CODEC, PartyRosterClear::leaderUuid,
                PartyRosterClear::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PartyRosterAdd(UUID partyId, UUID memberUuid, String memberName) implements CustomPayload {
        public static final Id<PartyRosterAdd> ID = new Id<>(Identifier.of("rpg-systems","party_roster_add"));
        public static final PacketCodec<RegistryByteBuf, PartyRosterAdd> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, PartyRosterAdd::partyId,
                Uuids.PACKET_CODEC, PartyRosterAdd::memberUuid,
                PacketCodecs.STRING, PartyRosterAdd::memberName,
                PartyRosterAdd::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PartyMemberVitals(UUID memberUuid, float health, float maxHealth, int hunger) implements CustomPayload {
        public static final Id<PartyMemberVitals> ID = new Id<>(Identifier.of("rpg-systems","party_member_vitals"));
        public static final PacketCodec<RegistryByteBuf, PartyMemberVitals> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, PartyMemberVitals::memberUuid,
                PacketCodecs.FLOAT, PartyMemberVitals::health,
                PacketCodecs.FLOAT, PartyMemberVitals::maxHealth,
                PacketCodecs.VAR_INT, PartyMemberVitals::hunger,
                PartyMemberVitals::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PartyRosterReset() implements CustomPayload {
        public static final Id<PartyRosterReset> ID =
                new Id<>(Identifier.of("rpg-systems", "party_roster_reset"));

        public static final PacketCodec<RegistryByteBuf, PartyRosterReset> CODEC =
                new PacketCodec<RegistryByteBuf, PartyRosterReset>() {
                    @Override
                    public PartyRosterReset decode(RegistryByteBuf buf) {
                        return new PartyRosterReset();
                    }
                    @Override
                    public void encode(RegistryByteBuf buf, PartyRosterReset value) {

                    }
                };

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record PartyMemberOnline(UUID memberUuid, boolean online) implements CustomPayload {
        public static final Id<PartyMemberOnline> ID =
                new Id<>(Identifier.of("rpg-systems","party_member_online"));

        public static final PacketCodec<RegistryByteBuf, PartyMemberOnline> CODEC =
                PacketCodec.tuple(
                        Uuids.PACKET_CODEC, PartyMemberOnline::memberUuid,
                        PacketCodecs.BOOL,  PartyMemberOnline::online,
                        PartyMemberOnline::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PartyMemberLevel(UUID memberUuid, int totalLevel) implements CustomPayload {
        public static final Id<PartyMemberLevel> ID =
                new Id<>(Identifier.of("rpg-systems","party_member_level"));

        public static final PacketCodec<RegistryByteBuf, PartyMemberLevel> CODEC =
                PacketCodec.tuple(
                        Uuids.PACKET_CODEC, PartyMemberLevel::memberUuid,
                        PacketCodecs.VAR_INT, PartyMemberLevel::totalLevel,
                        PartyMemberLevel::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
