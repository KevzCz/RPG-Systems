package net.pixeldreamstudios.rpgsystems.party;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FTBTeamsIntegration {

    private static boolean ftbTeamsLoaded = false;

    public static void init() {
        ftbTeamsLoaded = FabricLoader.getInstance().isModLoaded("ftbteams");
        if (ftbTeamsLoaded) {
            RPGSystems.LOGGER.info("[FTB Teams Integration] FTB Teams mod detected!");
        } else {
            RPGSystems.LOGGER.info("[FTB Teams Integration] FTB Teams mod not found, using native party system.");
        }
    }

    public static boolean isEnabled() {
        boolean enabled = ftbTeamsLoaded && RPGSystemsConfig.get().party.useFTBTeams;
        if (ftbTeamsLoaded && !enabled) {
            RPGSystems.LOGGER.debug("[FTB Teams Integration] FTB Teams is loaded but integration is disabled in config.");
        }
        return enabled;
    }

    @Nullable
    public static FTBPartyData getPartyDataForPlayer(ServerPlayerEntity player) {
        if (!isEnabled()) return null;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamForPlayer(player);

            if (teamOpt.isEmpty()) {
                return null;
            }

            Team team = teamOpt.get();

            if (!team.isPartyTeam()) {
                return null;
            }

            UUID partyId = team.getId();
            String partyName = team.getProperty(TeamProperties.DISPLAY_NAME);
            UUID leaderUuid = team.getOwner();
            Set<UUID> members = team.getMembers();

            // FIXED: Get persisted settings instead of creating new ones
            MinecraftServer server = player.getServer();
            if (server == null) {
                RPGSystems.LOGGER.warn("[FTB Teams Integration] Server is null for player {}", player.getName().getString());
                return null;
            }

            PartyPersistentState state = PartyPersistentState.get(server);
            PartySettings settings = state.getFTBPartySettings(partyId);

            FTBPartyData data = new FTBPartyData(partyId, partyName, leaderUuid, members, settings);

            return data;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error getting party data for player {}",
                    player.getName().getString(), e);
            return null;
        }
    }

    @Nullable
    public static FTBPartyData getPartyDataForPlayerId(MinecraftServer server, UUID playerId) {
        if (!isEnabled()) return null;
        if (server == null) return null;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamForPlayerID(playerId);

            if (teamOpt.isEmpty()) {
                return null;
            }

            Team team = teamOpt.get();

            if (!team.isPartyTeam()) {
                return null;
            }

            UUID partyId = team.getId();
            String partyName = team.getProperty(TeamProperties.DISPLAY_NAME);
            UUID leaderUuid = team.getOwner();
            Set<UUID> members = team.getMembers();

            // FIXED: Get persisted settings instead of creating new ones
            PartyPersistentState state = PartyPersistentState.get(server);
            PartySettings settings = state.getFTBPartySettings(partyId);

            return new FTBPartyData(partyId, partyName, leaderUuid, members, settings);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error getting party data for player ID {}",
                    playerId, e);
            return null;
        }
    }

    public static boolean isInSameParty(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        if (!isEnabled()) return false;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> team1 = manager.getTeamForPlayer(player1);
            Optional<Team> team2 = manager.getTeamForPlayer(player2);

            if (team1.isEmpty() || team2.isEmpty()) {
                return false;
            }

            Team t1 = team1.get();
            Team t2 = team2.get();

            if (!t1.isPartyTeam() || !t2.isPartyTeam()) {
                return false;
            }

            boolean sameParty = t1.getId().equals(t2.getId());

            return sameParty;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error checking if players are in same party", e);
            return false;
        }
    }

    @Nullable
    public static UUID getPartyIdForPlayer(ServerPlayerEntity player) {
        if (!isEnabled()) return null;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamForPlayer(player);

            if (teamOpt.isEmpty()) {
                return null;
            }

            Team team = teamOpt.get();
            if (!team.isPartyTeam()) {
                return null;
            }

            UUID partyId = team.getId();

            return partyId;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error getting party ID for player {}",
                    player.getName().getString(), e);
            return null;
        }
    }

    public static List<UUID> getPartyMembers(MinecraftServer server, UUID partyId) {
        if (!isEnabled()) return Collections.emptyList();

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamByID(partyId);

            if (teamOpt.isEmpty()) {
                return Collections.emptyList();
            }

            Team team = teamOpt.get();
            if (!team.isPartyTeam()) {
                return Collections.emptyList();
            }

            List<UUID> members = new ArrayList<>(team.getMembers());

            return members;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error getting party members for party {}",
                    partyId, e);
            return Collections.emptyList();
        }
    }

    // =========================
    // NEW: Settings Management
    // =========================

    /**
     * Update party settings for an FTB Teams party.
     * This persists the settings across server restarts.
     *
     * @param server The server instance
     * @param partyId The FTB Teams party UUID
     * @param settings The new settings to apply
     * @return true if successful, false otherwise
     */
    public static boolean updatePartySettings(MinecraftServer server, UUID partyId, PartySettings settings) {
        if (!isEnabled()) return false;
        if (server == null || partyId == null || settings == null) return false;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamByID(partyId);

            if (teamOpt.isEmpty()) {
                RPGSystems.LOGGER.warn("[FTB Teams Integration] Party {} not found", partyId);
                return false;
            }

            Team team = teamOpt.get();
            if (!team.isPartyTeam()) {
                RPGSystems.LOGGER.warn("[FTB Teams Integration] Team {} is not a party", partyId);
                return false;
            }

            // Save settings to persistent state
            PartyPersistentState state = PartyPersistentState.get(server);
            state.updateFTBPartySettings(partyId, settings);

            RPGSystems.LOGGER.info("[FTB Teams Integration] Updated settings for party {}", partyId);
            return true;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error updating party settings for {}",
                    partyId, e);
            return false;
        }
    }

    /**
     * Update party settings for the party that a player is in.
     *
     * @param player The player whose party settings to update
     * @param settings The new settings to apply
     * @return true if successful, false otherwise
     */
    public static boolean updatePartySettingsForPlayer(ServerPlayerEntity player, PartySettings settings) {
        if (!isEnabled()) return false;
        if (player == null || settings == null) return false;

        UUID partyId = getPartyIdForPlayer(player);
        if (partyId == null) {
            RPGSystems.LOGGER.warn("[FTB Teams Integration] Player {} is not in a party",
                    player.getName().getString());
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        return updatePartySettings(server, partyId, settings);
    }

    /**
     * Get party settings for an FTB Teams party.
     *
     * @param server The server instance
     * @param partyId The FTB Teams party UUID
     * @return The party settings, or null if not found
     */
    @Nullable
    public static PartySettings getPartySettings(MinecraftServer server, UUID partyId) {
        if (!isEnabled()) return null;
        if (server == null || partyId == null) return null;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamByID(partyId);

            if (teamOpt.isEmpty()) return null;

            Team team = teamOpt.get();
            if (!team.isPartyTeam()) return null;

            PartyPersistentState state = PartyPersistentState.get(server);
            return state.getFTBPartySettings(partyId);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error getting party settings for {}",
                    partyId, e);
            return null;
        }
    }

    /**
     * Check if a player is the leader of their FTB Teams party.
     *
     * @param player The player to check
     * @return true if the player is the party leader, false otherwise
     */
    public static boolean isPartyLeader(ServerPlayerEntity player) {
        if (!isEnabled()) return false;

        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamForPlayer(player);

            if (teamOpt.isEmpty()) return false;

            Team team = teamOpt.get();
            if (!team.isPartyTeam()) return false;

            return team.getOwner().equals(player.getUuid());

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error checking if player is leader", e);
            return false;
        }
    }

    /**
     * Cleanup settings for a disbanded FTB Teams party.
     * This should be called when an FTB Teams party is deleted.
     *
     * @param server The server instance
     * @param partyId The FTB Teams party UUID
     */
    public static void cleanupPartySettings(MinecraftServer server, UUID partyId) {
        if (!isEnabled()) return;
        if (server == null || partyId == null) return;

        try {
            PartyPersistentState state = PartyPersistentState.get(server);
            state.removeFTBPartySettings(partyId);
            RPGSystems.LOGGER.info("[FTB Teams Integration] Cleaned up settings for disbanded party {}", partyId);
        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams Integration] Error cleaning up party settings for {}",
                    partyId, e);
        }
    }


    public static class FTBPartyData {
        public final UUID partyId;
        public final String partyName;
        public final UUID leaderUuid;
        public final Set<UUID> members;
        public final PartySettings settings;

        public FTBPartyData(UUID partyId, String partyName, UUID leaderUuid, Set<UUID> members, PartySettings settings) {
            this.partyId = partyId;
            this.partyName = partyName;
            this.leaderUuid = leaderUuid;
            this.members = new HashSet<>(members);
            this.settings = settings;
        }

        @Override
        public String toString() {
            return "FTBPartyData{" +
                    "partyId=" + partyId +
                    ", partyName='" + partyName + '\'' +
                    ", leaderUuid=" + leaderUuid +
                    ", memberCount=" + members.size() +
                    ", allowHelpfulNonMembers=" + settings.allowHelpfulNonMembers +
                    ", ignorePartyCollision=" + settings.ignorePartyCollision +
                    '}';
        }
    }
}