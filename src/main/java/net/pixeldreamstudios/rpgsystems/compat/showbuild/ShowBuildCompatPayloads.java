package net.pixeldreamstudios.rpgsystems.compat.showbuild;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ShowBuildCompatPayloads {
    private ShowBuildCompatPayloads() {}

    public record OpenBuildRequest(String targetName) implements CustomPayload {
        public static final Id<OpenBuildRequest> ID =
                new Id<>(Identifier.of("rpg-systems", "compat_open_build_req"));
        public static final PacketCodec<RegistryByteBuf, OpenBuildRequest> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, OpenBuildRequest::targetName, OpenBuildRequest::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
public record OpenBuildData(String playerName, NbtCompound data) implements CustomPayload {
        public static final Id<OpenBuildData> ID =
                new Id<>(Identifier.of("rpg-systems", "compat_open_build_data"));
        public static final PacketCodec<RegistryByteBuf, OpenBuildData> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, OpenBuildData::playerName,
                        PacketCodecs.NBT_COMPOUND, OpenBuildData::data,
                        OpenBuildData::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
