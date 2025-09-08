package net.pixeldreamstudios.rpgsystems.network.system;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class SystemPayloads {
    private SystemPayloads() {}

    public record SystemsSync(boolean party, boolean pet, boolean title) implements CustomPayload {
        public static final Id<SystemsSync> ID =
                new Id<>(Identifier.of("rpg-systems", "systems_sync"));

        public static final PacketCodec<RegistryByteBuf, SystemsSync> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL,  SystemsSync::party,
                PacketCodecs.BOOL,  SystemsSync::pet,
                PacketCodecs.BOOL,  SystemsSync::title,
                SystemsSync::new
        );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
