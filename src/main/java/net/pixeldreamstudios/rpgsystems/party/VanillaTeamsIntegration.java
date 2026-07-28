package net.pixeldreamstudios.rpgsystems.party;

import com.mojang.authlib.GameProfile;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class VanillaTeamsIntegration {

    private static final String TEAM_PREFIX = "rpgp_";
    private static final Set<String> WARNED_NAMES = Collections.synchronizedSet(new HashSet<>());

    public static boolean isEnabled() {
        return VanillaTeamsLoader.isEnabled();
    }

    public static String teamNameFor(UUID partyId) {
        return TEAM_PREFIX + partyId.toString().replace("-", "");
    }

    @Nullable
    public static UUID partyIdFromTeamName(String teamName) {
        if (teamName == null || !teamName.startsWith(TEAM_PREFIX)) return null;
        String hex = teamName.substring(TEAM_PREFIX.length());
        if (hex.length() != 32) return null;
        try {
            String dashed = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-"
                    + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
            return UUID.fromString(dashed);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isPartyTeam(Team team) {
        return team != null && team.getName().startsWith(TEAM_PREFIX);
    }

    @Nullable
    public static Team getTeamForPlayer(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null) return null;
        Team team = server.getScoreboard().getScoreHolderTeam(player.getNameForScoreboard());
        return isPartyTeam(team) ? team : null;
    }

    @Nullable
    public static Team getTeamById(MinecraftServer server, UUID partyId) {
        if (server == null || partyId == null) return null;
        Team team = server.getScoreboard().getTeam(teamNameFor(partyId));
        return isPartyTeam(team) ? team : null;
    }

    public static Team getOrCreateTeam(MinecraftServer server, UUID partyId, String displayName) {
        Scoreboard scoreboard = server.getScoreboard();
        String teamName = teamNameFor(partyId);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
        }
        if (displayName != null && !displayName.isBlank()) {
            team.setDisplayName(Text.literal(displayName));
        }
        return team;
    }

    public static void addMember(MinecraftServer server, UUID partyId, ServerPlayerEntity player, String displayName) {
        Team team = getOrCreateTeam(server, partyId, displayName);
        server.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(), team);
        recordIdentity(server, player);
    }

    public static void removeMember(MinecraftServer server, UUID partyId, String scoreHolderName) {
        Team team = getTeamById(server, partyId);
        if (team == null) return;
        server.getScoreboard().removeScoreHolderFromTeam(scoreHolderName, team);
    }

    public static void removeMember(MinecraftServer server, UUID partyId, ServerPlayerEntity player) {
        removeMember(server, partyId, player.getNameForScoreboard());
    }

    public static void removeTeam(MinecraftServer server, UUID partyId) {
        Team team = getTeamById(server, partyId);
        if (team != null) {
            server.getScoreboard().removeTeam(team);
        }
        PartyPersistentState state = PartyPersistentState.get(server);
        state.removeVanillaTeamSettings(partyId);
    }

    public static void recordIdentity(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null) return;
        PartyPersistentState.get(server).recordIdentity(player.getUuid(), player.getGameProfile().getName());
    }

    @Nullable
    public static UUID resolveName(MinecraftServer server, String name) {
        if (server == null || name == null || name.isBlank()) return null;

        ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
        if (online != null) {
            PartyPersistentState.get(server).recordIdentity(online.getUuid(), online.getGameProfile().getName());
            return online.getUuid();
        }

        UUID cached = PartyPersistentState.get(server).resolveCachedName(name);
        if (cached != null) return cached;

        Optional<GameProfile> profile = server.getUserCache() != null
                ? server.getUserCache().findByName(name)
                : Optional.empty();
        if (profile.isPresent() && profile.get().getId() != null) {
            UUID id = profile.get().getId();
            PartyPersistentState.get(server).recordIdentity(id, profile.get().getName());
            return id;
        }

        if (WARNED_NAMES.add(name.toLowerCase(Locale.ROOT))) {
            RPGSystems.LOGGER.debug("[Vanilla Teams] Could not resolve team member name '{}' to a UUID yet", name);
        }
        return null;
    }

    public static Set<UUID> resolveTeamMembers(MinecraftServer server, Team team) {
        Set<UUID> members = new LinkedHashSet<>();
        if (team == null) return members;
        for (String name : team.getPlayerList()) {
            UUID id = resolveName(server, name);
            if (id != null) members.add(id);
        }
        return members;
    }

    public static void refreshIdentitiesFor(MinecraftServer server, Team team) {
        if (server == null || team == null) return;
        for (String name : team.getPlayerList()) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
            if (online != null) {
                PartyPersistentState.get(server).recordIdentity(online.getUuid(), online.getGameProfile().getName());
            }
        }
    }

    @Nullable
    public static UUID getLeader(MinecraftServer server, UUID partyId) {
        return PartyPersistentState.get(server).getVanillaLeader(partyId);
    }

    public static void setLeader(MinecraftServer server, UUID partyId, UUID leader) {
        PartyPersistentState.get(server).setVanillaLeader(partyId, leader);
    }

    @Nullable
    public static PartyDataProvider.PartyInfo getPartyInfoForPlayer(ServerPlayerEntity player) {
        if (!isEnabled()) return null;
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        Team team = getTeamForPlayer(server, player);
        if (team == null) return null;
        return buildInfo(server, team);
    }

    @Nullable
    public static PartyDataProvider.PartyInfo getPartyInfoForPlayerId(MinecraftServer server, UUID playerId) {
        if (!isEnabled() || server == null) return null;

        String name = PartyPersistentState.get(server).nameOf(playerId);
        if (name == null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) name = player.getNameForScoreboard();
        }
        if (name == null) return null;

        Team team = server.getScoreboard().getScoreHolderTeam(name);
        if (!isPartyTeam(team)) return null;
        return buildInfo(server, team);
    }

    @Nullable
    private static PartyDataProvider.PartyInfo buildInfo(MinecraftServer server, Team team) {
        UUID partyId = partyIdFromTeamName(team.getName());
        if (partyId == null) return null;

        Set<UUID> members = resolveTeamMembers(server, team);
        if (members.isEmpty()) return null;

        PartyPersistentState state = PartyPersistentState.get(server);
        UUID leader = state.getVanillaLeader(partyId);
        if (leader == null || !members.contains(leader)) {
            leader = members.iterator().next();
            state.setVanillaLeader(partyId, leader);
        }

        String partyName = team.getDisplayName() != null ? team.getDisplayName().getString() : "";
        PartySettings settings = state.getVanillaTeamSettings(partyId);

        return new PartyDataProvider.PartyInfo(
                partyId, partyName, leader, members, settings, PartyDataProvider.PartySource.VANILLA);
    }

    public static boolean isInSameParty(ServerPlayerEntity a, ServerPlayerEntity b) {
        if (!isEnabled()) return false;
        MinecraftServer server = a.getServer();
        if (server == null) return false;
        Team ta = getTeamForPlayer(server, a);
        Team tb = getTeamForPlayer(server, b);
        return ta != null && tb != null && ta.getName().equals(tb.getName());
    }

    @Nullable
    public static UUID getPartyIdForPlayer(ServerPlayerEntity player) {
        if (!isEnabled()) return null;
        MinecraftServer server = player.getServer();
        if (server == null) return null;
        Team team = getTeamForPlayer(server, player);
        if (team == null) return null;
        return partyIdFromTeamName(team.getName());
    }

    public static List<UUID> getPartyMembers(MinecraftServer server, UUID partyId) {
        if (!isEnabled() || server == null || partyId == null) return Collections.emptyList();
        Team team = getTeamById(server, partyId);
        if (team == null) return Collections.emptyList();
        return new ArrayList<>(resolveTeamMembers(server, team));
    }
}
