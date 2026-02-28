package net.pixeldreamstudios.rpgsystems.network.party;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.party.PartyDataProvider;

import java.util.ArrayList;
import java.util.List;

public final class PartySourcePayloads {
    private PartySourcePayloads() {}
    public record SwitchSource(String source) implements CustomPayload {
        public static final Id<SwitchSource> ID =
                new Id<>(Identifier.of("rpg-systems", "party_switch_source"));
        public static final PacketCodec<RegistryByteBuf, SwitchSource> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, SwitchSource::source, SwitchSource::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public PartyDataProvider.PartySource toPartySource() {
            try {
                return PartyDataProvider.PartySource.valueOf(source);
            } catch (IllegalArgumentException ignored) {}
            
            return switch (source.toLowerCase()) {
                case "ftb_teams", "ftbteams" -> PartyDataProvider.PartySource.FTB_TEAMS;
                case "party_addon", "partyaddon" -> PartyDataProvider.PartySource.PARTY_ADDON;
                default -> PartyDataProvider.PartySource.NATIVE;
            };
        }
    }

    public record AvailableSources(List<String> sources, String activeSource) implements CustomPayload {
        public static final Id<AvailableSources> ID =
                new Id<>(Identifier.of("rpg-systems", "party_available_sources"));
        public static final PacketCodec<RegistryByteBuf, AvailableSources> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.collect(PacketCodecs.toList()),
                        AvailableSources::sources,
                        PacketCodecs.STRING,
                        AvailableSources::activeSource,
                        AvailableSources::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public static AvailableSources create(List<PartyDataProvider.PartySource> sources, PartyDataProvider.PartySource active) {
            List<String> sourceStrings = new ArrayList<>();
            for (PartyDataProvider.PartySource src : sources) {
                sourceStrings.add(src.name());
            }
            String activeStr = (active != null) ? active.name() : PartyDataProvider.PartySource.NATIVE.name();
            return new AvailableSources(sourceStrings, activeStr);
        }

        public List<PartyDataProvider.PartySource> getSourcesAsEnum() {
            List<PartyDataProvider.PartySource> result = new ArrayList<>();
            for (String s : sources) {
                try {
                    result.add(PartyDataProvider.PartySource.valueOf(s));
                } catch (IllegalArgumentException ignored) {}
            }
            return result;
        }

        public PartyDataProvider.PartySource getActiveAsEnum() {
            try {
                return PartyDataProvider.PartySource.valueOf(activeSource);
            } catch (IllegalArgumentException e) {
                return PartyDataProvider.PartySource.NATIVE;
            }
        }
    }


    public record SourceSwitched(String newSource) implements CustomPayload {
        public static final Id<SourceSwitched> ID =
                new Id<>(Identifier.of("rpg-systems", "party_source_switched"));
        public static final PacketCodec<RegistryByteBuf, SourceSwitched> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, SourceSwitched::newSource, SourceSwitched::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public static SourceSwitched create(PartyDataProvider.PartySource source) {
            return new SourceSwitched(source.name());
        }

        public PartyDataProvider.PartySource getSourceAsEnum() {
            try {
                return PartyDataProvider.PartySource.valueOf(newSource);
            } catch (IllegalArgumentException e) {
                return PartyDataProvider.PartySource.NATIVE;
            }
        }
    }
}
