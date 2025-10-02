package net.pixeldreamstudios.rpgsystems.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class PartySettingsPayloads {
    private PartySettingsPayloads() {}

    public record SetAllowHelpfulNonMembers(boolean allow) implements CustomPayload {
        public static final Id<SetAllowHelpfulNonMembers> ID =
                new Id<>(Identifier.of("rpg-systems","party_set_allow_helpful_non_members"));
        public static final PacketCodec<RegistryByteBuf, SetAllowHelpfulNonMembers> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOL, SetAllowHelpfulNonMembers::allow, SetAllowHelpfulNonMembers::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SetIgnorePartyCollision(boolean ignore) implements CustomPayload {
        public static final Id<SetIgnorePartyCollision> ID =
                new Id<>(Identifier.of("rpg-systems","party_set_ignore_collision"));
        public static final PacketCodec<RegistryByteBuf, SetIgnorePartyCollision> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOL, SetIgnorePartyCollision::ignore, SetIgnorePartyCollision::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record Sync(boolean allowHelpfulNonMembers, boolean ignorePartyCollision) implements CustomPayload {
        public static final Id<Sync> ID =
                new Id<>(Identifier.of("rpg-systems","party_settings_sync"));
        public static final PacketCodec<RegistryByteBuf, Sync> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.BOOL, Sync::allowHelpfulNonMembers,
                        PacketCodecs.BOOL, Sync::ignorePartyCollision,
                        Sync::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
