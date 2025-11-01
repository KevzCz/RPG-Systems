package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PartyDataProvider {

    @Nullable
    public static PartyInfo getPartyForPlayer(ServerPlayerEntity player) {

        if (FTBTeamsIntegration.isEnabled()) {
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayer(player);
            if (ftbData != null) {
                return new PartyInfo(
                        ftbData.partyId,
                        ftbData.partyName,
                        ftbData.leaderUuid,
                        ftbData.members,
                        ftbData.settings,
                        true
                );
            }
            return null;
        }

        PartyPersistentState state = PartyPersistentState.get(player.getServer());
        Party party = state.getPartyByMember(player.getUuid());
        if (party == null) return null;

        return new PartyInfo(
                party.id,
                party.name,
                party.leader,
                party.members,
                party.settings,
                false
        );
    }

    @Nullable
    public static PartyInfo getPartyForPlayerId(MinecraftServer server, UUID playerId) {
        if (FTBTeamsIntegration.isEnabled()) {
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayerId(server, playerId);
            if (ftbData != null) {
                return new PartyInfo(
                        ftbData.partyId,
                        ftbData.partyName,
                        ftbData.leaderUuid,
                        ftbData.members,
                        ftbData.settings,
                        true
                );
            }
            return null;
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        Party party = state.getPartyByMember(playerId);
        if (party == null) return null;

        return new PartyInfo(
                party.id,
                party.name,
                party.leader,
                party.members,
                party.settings,
                false
        );
    }

    public static boolean isInSameParty(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        if (FTBTeamsIntegration.isEnabled()) {
            return FTBTeamsIntegration.isInSameParty(player1, player2);
        }

        PartyPersistentState state = PartyPersistentState.get(player1.getServer());
        return state.sameParty(player1.getUuid(), player2.getUuid());
    }

    @Nullable
    public static UUID getPartyIdForPlayer(ServerPlayerEntity player) {
        if (FTBTeamsIntegration.isEnabled()) {
            return FTBTeamsIntegration.getPartyIdForPlayer(player);
        }

        PartyPersistentState state = PartyPersistentState.get(player.getServer());
        Party party = state.getPartyByMember(player.getUuid());
        return party != null ? party.id : null;
    }

    @Nullable
    public static UUID getPartyIdForPlayerId(MinecraftServer server, UUID playerId) {
        if (FTBTeamsIntegration.isEnabled()) {
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayerId(server, playerId);
            return ftbData != null ? ftbData.partyId : null;
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        Party party = state.getPartyByMember(playerId);
        return party != null ? party.id : null;
    }

    public static List<UUID> getPartyMembers(MinecraftServer server, UUID partyId) {
        if (FTBTeamsIntegration.isEnabled()) {
            return FTBTeamsIntegration.getPartyMembers(server, partyId);
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        Party party = state.getParty(partyId);
        if (party == null) return Collections.emptyList();

        return new ArrayList<>(party.members);
    }

    @Nullable
    public static Party getPartyById(MinecraftServer server, UUID partyId) {
        if (FTBTeamsIntegration.isEnabled()) {
            return null;
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        return state.getParty(partyId);
    }

    public static boolean isPlayerInParty(ServerPlayerEntity player, UUID partyId) {
        if (partyId == null) return false;

        if (FTBTeamsIntegration.isEnabled()) {
            UUID playerPartyId = FTBTeamsIntegration.getPartyIdForPlayer(player);
            return partyId.equals(playerPartyId);
        }

        PartyPersistentState state = PartyPersistentState.get(player.getServer());
        Party party = state.getPartyByMember(player.getUuid());
        return party != null && party.id.equals(partyId);
    }
    public static Collection<Party> getAllParties(MinecraftServer server) {
        if (FTBTeamsIntegration.isEnabled()) {
            return Collections.emptyList();
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        return state.allParties();
    }
    public static class PartyInfo {
        public final UUID id;
        public final String name;
        public final UUID leader;
        public final Set<UUID> members;
        public final PartySettings settings;
        public final boolean fromFTBTeams;

        public PartyInfo(UUID id, String name, UUID leader, Set<UUID> members, PartySettings settings, boolean fromFTBTeams) {
            this.id = id;
            this.name = name;
            this.leader = leader;
            this.members = new HashSet<>(members);
            this.settings = settings;
            this.fromFTBTeams = fromFTBTeams;
        }

        public boolean isMember(UUID playerId) {
            return members.contains(playerId);
        }

        public boolean isLeader(UUID playerId) {
            return leader.equals(playerId);
        }

        public int getMemberCount() {
            return members.size();
        }
    }
}