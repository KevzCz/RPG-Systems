package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PartyDataProvider {

    public enum PartySource {
        NATIVE,
        FTB_TEAMS,
        PARTY_ADDON
    }

    public static List<PartySource> getAvailableSourcesForPlayer(ServerPlayerEntity player) {
        List<PartySource> available = new ArrayList<>();
        boolean hasExternalMod = false;
        
        if (FTBTeamsLoader.isEnabled()) {
            hasExternalMod = true;
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayer(player);
            if (ftbData != null) {
                available.add(PartySource.FTB_TEAMS);
            }
        }
        
        if (PartyAddonIntegration.isEnabled()) {
            hasExternalMod = true;
            PartyAddonIntegration.PartyAddonData partyData = PartyAddonIntegration.getPartyDataForPlayer(player);
            if (partyData != null) {
                available.add(PartySource.PARTY_ADDON);
            }
        }
        
        if (!hasExternalMod) {
            PartyPersistentState state = PartyPersistentState.get(player.getServer());
            Party party = state.getPartyByMember(player.getUuid());
            if (party != null) {
                available.add(PartySource.NATIVE);
            }
        }
        
        return available;
    }

    /**
     * Gets the effective party source for a player, respecting their preference
     */
    public static PartySource getEffectiveSourceForPlayer(ServerPlayerEntity player) {
        PartyPersistentState state = PartyPersistentState.get(player.getServer());
        PartySource preference = state.getPlayerSourcePreference(player.getUuid());
        List<PartySource> available = getAvailableSourcesForPlayer(player);
        
        if (available.isEmpty()) {
            return PartySource.NATIVE; // Default to native if nothing available
        }
        
        // If player has a preference and it's available, use it
        if (preference != null && available.contains(preference)) {
            return preference;
        }
        
        // Otherwise use the first available (priority: FTB_TEAMS > PARTY_ADDON > NATIVE)
        return available.get(0);
    }

    /**
     * Gets party data from a specific source for a player
     */
    @Nullable
    public static PartyInfo getPartyFromSource(ServerPlayerEntity player, PartySource source) {
        if (source == null) return null;
        
        return switch (source) {
            case FTB_TEAMS -> {
                if (!FTBTeamsLoader.isEnabled()) yield null;
                FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayer(player);
                if (ftbData == null) yield null;
                yield new PartyInfo(
                        ftbData.partyId,
                        ftbData.partyName,
                        ftbData.leaderUuid,
                        ftbData.members,
                        ftbData.settings,
                        PartySource.FTB_TEAMS
                );
            }
            case PARTY_ADDON -> {
                if (!PartyAddonIntegration.isEnabled()) yield null;
                PartyAddonIntegration.PartyAddonData partyData = PartyAddonIntegration.getPartyDataForPlayer(player);
                if (partyData == null) yield null;
                yield new PartyInfo(
                        partyData.partyId,
                        partyData.partyName,
                        partyData.leaderUuid,
                        partyData.members,
                        partyData.settings,
                        PartySource.PARTY_ADDON
                );
            }
            case NATIVE -> {
                PartyPersistentState state = PartyPersistentState.get(player.getServer());
                Party party = state.getPartyByMember(player.getUuid());
                if (party == null) yield null;
                yield new PartyInfo(
                        party.id,
                        party.name,
                        party.leader,
                        party.members,
                        party.settings,
                        PartySource.NATIVE
                );
            }
        };
    }

    @Nullable
    public static PartyInfo getPartyForPlayer(ServerPlayerEntity player) {
        PartySource effectiveSource = getEffectiveSourceForPlayer(player);
        return getPartyFromSource(player, effectiveSource);
    }

    @Nullable
    public static PartyInfo getPartyForPlayerId(MinecraftServer server, UUID playerId) {
        // For player ID lookups, we need to check if player is online for preferences
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player != null) {
            return getPartyForPlayer(player);
        }
        
        // Player is offline - use cascade logic without preferences
        // First check FTBTeams
        if (FTBTeamsLoader.isEnabled()) {
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayerId(server, playerId);
            if (ftbData != null) {
                return new PartyInfo(
                        ftbData.partyId,
                        ftbData.partyName,
                        ftbData.leaderUuid,
                        ftbData.members,
                        ftbData.settings,
                        PartySource.FTB_TEAMS
                );
            }
        }

        if (PartyAddonIntegration.isEnabled()) {
            PartyAddonIntegration.PartyAddonData partyData = PartyAddonIntegration.getPartyDataForPlayerId(server, playerId);
            if (partyData != null) {
                return new PartyInfo(
                        partyData.partyId,
                        partyData.partyName,
                        partyData.leaderUuid,
                        partyData.members,
                        partyData.settings,
                        PartySource.PARTY_ADDON
                );
            }
            // Player not in PartyAddon group, continue checking native
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
                PartySource.NATIVE
        );
    }

    public static boolean isInSameParty(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        // Check FTBTeams first - if both players are in the same FTB party
        if (FTBTeamsLoader.isEnabled()) {
            if (FTBTeamsIntegration.isInSameParty(player1, player2)) {
                return true;
            }
            // Not in same FTB party - but might be in same PartyAddon group
        }

        // Check PartyAddon - if both players are in the same group
        if (PartyAddonIntegration.isEnabled()) {
            if (PartyAddonIntegration.isInSameParty(player1, player2)) {
                return true;
            }
            // Not in same PartyAddon group - check native
        }

        // Check native party system
        PartyPersistentState state = PartyPersistentState.get(player1.getServer());
        return state.sameParty(player1.getUuid(), player2.getUuid());
    }

    @Nullable
    public static UUID getPartyIdForPlayer(ServerPlayerEntity player) {
        // Use the effective source based on player preference
        PartyInfo info = getPartyForPlayer(player);
        return info != null ? info.id : null;
    }

    @Nullable
    public static UUID getPartyIdForPlayerId(MinecraftServer server, UUID playerId) {
        // Check FTBTeams first
        if (FTBTeamsLoader.isEnabled()) {
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayerId(server, playerId);
            if (ftbData != null) {
                return ftbData.partyId;
            }
        }

        // Check PartyAddon
        if (PartyAddonIntegration.isEnabled()) {
            PartyAddonIntegration.PartyAddonData partyData = PartyAddonIntegration.getPartyDataForPlayerId(server, playerId);
            if (partyData != null) {
                return partyData.partyId;
            }
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        Party party = state.getPartyByMember(playerId);
        return party != null ? party.id : null;
    }

    public static List<UUID> getPartyMembers(MinecraftServer server, UUID partyId) {
        // Try FTBTeams first
        if (FTBTeamsLoader.isEnabled()) {
            List<UUID> members = FTBTeamsIntegration.getPartyMembers(server, partyId);
            if (!members.isEmpty()) {
                return members;
            }
        }

        // Try PartyAddon
        if (PartyAddonIntegration.isEnabled()) {
            List<UUID> members = PartyAddonIntegration.getPartyMembers(server, partyId);
            if (!members.isEmpty()) {
                return members;
            }
        }

        // Fall back to native
        PartyPersistentState state = PartyPersistentState.get(server);
        Party party = state.getParty(partyId);
        if (party == null) return Collections.emptyList();

        return new ArrayList<>(party.members);
    }

    @Nullable
    public static Party getPartyById(MinecraftServer server, UUID partyId) {
        // External mods don't use native Party objects
        if (FTBTeamsLoader.isEnabled() || PartyAddonIntegration.isEnabled()) {
            // Check if this partyId belongs to an external mod - if so, return null
            // since we can't convert their data to a native Party object
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        return state.getParty(partyId);
    }

    public static boolean isPlayerInParty(ServerPlayerEntity player, UUID partyId) {
        if (partyId == null) return false;

        // Check FTBTeams first
        if (FTBTeamsLoader.isEnabled()) {
            UUID playerPartyId = FTBTeamsIntegration.getPartyIdForPlayer(player);
            if (partyId.equals(playerPartyId)) {
                return true;
            }
        }

        // Check PartyAddon
        if (PartyAddonIntegration.isEnabled()) {
            UUID playerPartyId = PartyAddonIntegration.getPartyIdForPlayer(player);
            if (partyId.equals(playerPartyId)) {
                return true;
            }
        }

        // Check native
        PartyPersistentState state = PartyPersistentState.get(player.getServer());
        Party party = state.getPartyByMember(player.getUuid());
        return party != null && party.id.equals(partyId);
    }
    public static Collection<Party> getAllParties(MinecraftServer server) {
        // Return native parties only - external mods manage their own party systems
        PartyPersistentState state = PartyPersistentState.get(server);
        return state.allParties();
    }
    public static class PartyInfo {
        public final UUID id;
        public final String name;
        public final UUID leader;
        public final Set<UUID> members;
        public final PartySettings settings;
        public final PartySource source;

        /**
         * @deprecated Use {@link #PartyInfo(UUID, String, UUID, Set, PartySettings, PartySource)} instead
         */
        @Deprecated
        public PartyInfo(UUID id, String name, UUID leader, Set<UUID> members, PartySettings settings, boolean fromFTBTeams) {
            this(id, name, leader, members, settings, fromFTBTeams ? PartySource.FTB_TEAMS : PartySource.NATIVE);
        }

        public PartyInfo(UUID id, String name, UUID leader, Set<UUID> members, PartySettings settings, PartySource source) {
            this.id = id;
            this.name = name != null ? name : "";
            this.leader = leader;
            this.members = new HashSet<>(members);
            this.settings = settings != null ? settings : new PartySettings();
            this.source = source != null ? source : PartySource.NATIVE;
        }

        /**
         * @deprecated Use {@link #source} == {@link PartySource#FTB_TEAMS} instead
         */
        @Deprecated
        public boolean isFromFTBTeams() {
            return source == PartySource.FTB_TEAMS;
        }

        public boolean isFromExternalMod() {
            return source == PartySource.FTB_TEAMS || source == PartySource.PARTY_ADDON;
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