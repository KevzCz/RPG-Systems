package net.pixeldreamstudios.rpgsystems.network.title;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.RPGSystems;

import java.util.List;

public record TitleListSyncPayload(List<Entry> entries) implements CustomPayload {
    public static final CustomPayload.Id<TitleListSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGSystems.MOD_ID, "title_list_sync"));

    public static final PacketCodec<RegistryByteBuf, TitleListSyncPayload> CODEC =
            Entry.PACKET_CODEC.collect(PacketCodecs.toList())
                    .xmap(TitleListSyncPayload::new, TitleListSyncPayload::entries);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record Entry(Identifier id, String name, boolean hidden) {
        public static final PacketCodec<RegistryByteBuf, Entry> PACKET_CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.xmap(Identifier::of, Identifier::toString), Entry::id,
                        PacketCodecs.STRING, Entry::name,
                        PacketCodecs.BOOL, Entry::hidden,
                        Entry::new
                );
    }
}
