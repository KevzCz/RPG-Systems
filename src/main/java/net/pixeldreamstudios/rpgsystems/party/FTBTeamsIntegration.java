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


            PartySettings settings = new PartySettings();
            settings.allowHelpfulNonMembers = false;
            settings.ignorePartyCollision = true;

            FTBPartyData data = new FTBPartyData(partyId, partyName, leaderUuid, members, settings);

            return data;

        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static FTBPartyData getPartyDataForPlayerId(MinecraftServer server, UUID playerId) {
        if (!isEnabled()) return null;

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
            String partyName = team.getProperty(dev.ftb.mods.ftbteams.api.property.TeamProperties.DISPLAY_NAME);
            UUID leaderUuid = team.getOwner();
            Set<UUID> members = team.getMembers();


            PartySettings settings = new PartySettings();
            settings.allowHelpfulNonMembers = false;
            settings.ignorePartyCollision = true;

            return new FTBPartyData(partyId, partyName, leaderUuid, members, settings);

        } catch (Exception e) {

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
            return Collections.emptyList();
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
    }
}