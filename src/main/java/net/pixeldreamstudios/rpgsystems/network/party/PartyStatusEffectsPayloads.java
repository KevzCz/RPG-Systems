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

public final class PartyStatusEffectsPayloads {
    private PartyStatusEffectsPayloads() {}

    public record MemberEffects(UUID memberId, List<String> effectIds) implements CustomPayload {
        public static final Id<MemberEffects> ID = new Id<>(Identifier.of("rpg-systems", "party_member_effects"));
        public static final PacketCodec<RegistryByteBuf, MemberEffects> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, MemberEffects::memberId,
                PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), MemberEffects::effectIds,
                MemberEffects::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
