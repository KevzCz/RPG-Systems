package net.pixeldreamstudios.rpgsystems.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class PartyChatPayloads {
    private PartyChatPayloads() {}

    public record ChatSend(UUID partyId, String message) implements CustomPayload {
        public static final Id<ChatSend> ID = new Id<>(Identifier.of("rpg-systems", "party_chat_send"));
        public static final PacketCodec<RegistryByteBuf, ChatSend> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ChatSend::partyId,
                PacketCodecs.STRING, ChatSend::message,
                ChatSend::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ChatMessage(UUID partyId, UUID senderUuid, String senderName, String message, long epochMillis) implements CustomPayload {
        public static final Id<ChatMessage> ID = new Id<>(Identifier.of("rpg-systems", "party_chat_msg"));
        public static final PacketCodec<RegistryByteBuf, ChatMessage> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ChatMessage::partyId,
                Uuids.PACKET_CODEC, ChatMessage::senderUuid,
                PacketCodecs.STRING, ChatMessage::senderName,
                PacketCodecs.STRING, ChatMessage::message,
                PacketCodecs.VAR_LONG, ChatMessage::epochMillis,
                ChatMessage::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ChatNotice(UUID partyId, String message, long epochMillis) implements CustomPayload {
        public static final Id<ChatNotice> ID = new Id<>(Identifier.of("rpg-systems", "party_chat_notice"));
        public static final PacketCodec<RegistryByteBuf, ChatNotice> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ChatNotice::partyId,
                PacketCodecs.STRING, ChatNotice::message,
                PacketCodecs.VAR_LONG, ChatNotice::epochMillis,
                ChatNotice::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record ChatPinSet(UUID partyId, String pinnedText, long epochMillis) implements CustomPayload {
        public static final Id<ChatPinSet> ID = new Id<>(Identifier.of("rpg-systems", "party_chat_pin_set"));
        public static final PacketCodec<RegistryByteBuf, ChatPinSet> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ChatPinSet::partyId,
                PacketCodecs.STRING, ChatPinSet::pinnedText,
                PacketCodecs.VAR_LONG, ChatPinSet::epochMillis,
                ChatPinSet::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record ChatPinClear(UUID partyId) implements CustomPayload {
        public static final Id<ChatPinClear> ID = new Id<>(Identifier.of("rpg-systems", "party_chat_pin_clear"));
        public static final PacketCodec<RegistryByteBuf, ChatPinClear> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ChatPinClear::partyId,
                ChatPinClear::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
