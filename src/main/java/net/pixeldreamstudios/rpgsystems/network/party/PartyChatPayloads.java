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
        public static final Id<ChatSend> ID = new Id<>(Identifier.of("rpg-systems","party_chat_send"));
        public static final PacketCodec<RegistryByteBuf, ChatSend> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ChatSend::partyId,
                PacketCodecs.STRING, ChatSend::message,
                ChatSend::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ChatMessage(UUID partyId, UUID senderUuid, String senderName, String message, long epochMillis) implements CustomPayload {
        public static final Id<ChatMessage> ID = new Id<>(Identifier.of("rpg-systems","party_chat_msg"));
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
}
