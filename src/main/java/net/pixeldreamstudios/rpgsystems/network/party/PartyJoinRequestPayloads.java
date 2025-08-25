package net.pixeldreamstudios.rpgsystems.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class PartyJoinRequestPayloads {
    private PartyJoinRequestPayloads() {}

    public record JoinReqAdded(UUID partyId, UUID requesterUuid, String requesterName) implements CustomPayload {
        public static final Id<JoinReqAdded> ID = new Id<>(Identifier.of("rpg-systems","join_req_added"));
        public static final PacketCodec<RegistryByteBuf, JoinReqAdded> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, JoinReqAdded::partyId,
                Uuids.PACKET_CODEC, JoinReqAdded::requesterUuid,
                PacketCodecs.STRING, JoinReqAdded::requesterName,
                JoinReqAdded::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record JoinReqRemoved(UUID partyId, UUID requesterUuid) implements CustomPayload {
        public static final Id<JoinReqRemoved> ID = new Id<>(Identifier.of("rpg-systems","join_req_removed"));
        public static final PacketCodec<RegistryByteBuf, JoinReqRemoved> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, JoinReqRemoved::partyId,
                Uuids.PACKET_CODEC, JoinReqRemoved::requesterUuid,
                JoinReqRemoved::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record JoinAccepted(String leaderName, String partyName) implements CustomPayload {
        public static final Id<JoinAccepted> ID =
                new Id<>(Identifier.of("rpg-systems", "party_join_accept"));
        public static final PacketCodec<RegistryByteBuf, JoinAccepted> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, JoinAccepted::leaderName,
                PacketCodecs.STRING, JoinAccepted::partyName,
                JoinAccepted::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record JoinDeclined(String leaderName, String partyName) implements CustomPayload {
        public static final Id<JoinDeclined> ID =
                new Id<>(Identifier.of("rpg-systems", "party_join_decline"));
        public static final PacketCodec<RegistryByteBuf, JoinDeclined> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, JoinDeclined::leaderName,
                PacketCodecs.STRING, JoinDeclined::partyName,
                JoinDeclined::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record JoinReqRespond(UUID partyId, UUID requesterUuid, boolean accept) implements CustomPayload {
        public static final Id<JoinReqRespond> ID = new Id<>(Identifier.of("rpg-systems","join_req_respond"));
        public static final PacketCodec<RegistryByteBuf, JoinReqRespond> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, JoinReqRespond::partyId,
                Uuids.PACKET_CODEC, JoinReqRespond::requesterUuid,
                PacketCodecs.BOOL, JoinReqRespond::accept,
                JoinReqRespond::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
