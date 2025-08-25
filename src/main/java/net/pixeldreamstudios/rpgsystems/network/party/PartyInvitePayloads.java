package net.pixeldreamstudios.rpgsystems.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PartyInvitePayloads {
    private PartyInvitePayloads() {}
    public record InviteAdded(UUID partyId, UUID leaderUuid, String leaderName, String partyName) implements CustomPayload {
        public static final Id<InviteAdded> ID = new Id<>(Identifier.of("rpg-systems","invite_added"));
        public static final PacketCodec<RegistryByteBuf, InviteAdded> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, InviteAdded::partyId,
                Uuids.PACKET_CODEC, InviteAdded::leaderUuid,
                PacketCodecs.STRING, InviteAdded::leaderName,
                PacketCodecs.STRING, InviteAdded::partyName,
                InviteAdded::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record InviteRemoved(UUID partyId) implements CustomPayload {
        public static final Id<InviteRemoved> ID = new Id<>(Identifier.of("rpg-systems","invite_removed"));
        public static final PacketCodec<RegistryByteBuf, InviteRemoved> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, InviteRemoved::partyId,
                InviteRemoved::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record InviteRespond(UUID partyId, boolean accept) implements CustomPayload {
        public static final Id<InviteRespond> ID = new Id<>(Identifier.of("rpg-systems","invite_respond"));
        public static final PacketCodec<RegistryByteBuf, InviteRespond> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, InviteRespond::partyId,
                PacketCodecs.BOOL,  InviteRespond::accept,
                InviteRespond::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record InviteJoinConfirmed(String partyName) implements CustomPayload {
        public static final Id<InviteJoinConfirmed> ID =
                new Id<>(Identifier.of("rpg-systems","invite_join_confirmed"));
        public static final PacketCodec<RegistryByteBuf, InviteJoinConfirmed> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, InviteJoinConfirmed::partyName,
                InviteJoinConfirmed::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record PartyLeft(String partyName) implements CustomPayload {
        public static final Id<PartyLeft> ID =
                new Id<>(Identifier.of("rpg-systems","party_left"));
        public static final PacketCodec<RegistryByteBuf, PartyLeft> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, PartyLeft::partyName,
                PartyLeft::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record PartyKicked(String partyName) implements CustomPayload {
        public static final Id<PartyKicked> ID =
                new Id<>(Identifier.of("rpg-systems","party_kicked"));
        public static final PacketCodec<RegistryByteBuf, PartyKicked> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, PartyKicked::partyName,
                PartyKicked::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record InviteSent(String leaderName, String targetName, String partyName) implements CustomPayload {
        public static final Id<InviteSent> ID = new Id<>(Identifier.of("rpg-systems","invite_sent"));
        public static final PacketCodec<RegistryByteBuf, InviteSent> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, InviteSent::leaderName,
                PacketCodecs.STRING, InviteSent::targetName,
                PacketCodecs.STRING, InviteSent::partyName,
                InviteSent::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record InviteAccepted(String whoName, String partyName) implements CustomPayload {
        public static final Id<InviteAccepted> ID = new Id<>(Identifier.of("rpg-systems","invite_accepted"));
        public static final PacketCodec<RegistryByteBuf, InviteAccepted> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, InviteAccepted::whoName,
                PacketCodecs.STRING, InviteAccepted::partyName,
                InviteAccepted::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record InviteDeclined(String whoName, String partyName) implements CustomPayload {
        public static final Id<InviteDeclined> ID = new Id<>(Identifier.of("rpg-systems","invite_declined"));
        public static final PacketCodec<RegistryByteBuf, InviteDeclined> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, InviteDeclined::whoName,
                PacketCodecs.STRING, InviteDeclined::partyName,
                InviteDeclined::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record EligibleInviteesRequest() implements CustomPayload {
        public static final Id<EligibleInviteesRequest> ID =
                new Id<>(Identifier.of("rpg-systems","eligible_invitees_request"));
        public static final PacketCodec<RegistryByteBuf, EligibleInviteesRequest> CODEC =
                PacketCodec.unit(new EligibleInviteesRequest());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record EligibleInviteesResponse(List<UUID> uuids, List<String> names) implements CustomPayload {
        public static final Id<EligibleInviteesResponse> ID =
                new Id<>(Identifier.of("rpg-systems","eligible_invitees_response"));
        public static final PacketCodec<RegistryByteBuf, EligibleInviteesResponse> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.collection(ArrayList::new, Uuids.PACKET_CODEC), EligibleInviteesResponse::uuids,
                        PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), EligibleInviteesResponse::names,
                        EligibleInviteesResponse::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
