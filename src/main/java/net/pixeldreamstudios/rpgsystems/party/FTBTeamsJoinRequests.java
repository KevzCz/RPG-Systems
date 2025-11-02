package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.RPGSystems;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FTBTeamsJoinRequests {
    private FTBTeamsJoinRequests() {}
    private static final Map<UUID, Set<UUID>> REQUESTS = new ConcurrentHashMap<>();

    public static void addRequest(UUID teamId, UUID playerUuid) {
        REQUESTS.computeIfAbsent(teamId, k -> ConcurrentHashMap.newKeySet()).add(playerUuid);
        RPGSystems.LOGGER.debug("[FTB Teams Join Requests] Player {} requested to join team {}", playerUuid, teamId);
    }

    public static void removeRequest(UUID teamId, UUID playerUuid) {
        Set<UUID> requests = REQUESTS.get(teamId);
        if (requests != null) {
            requests.remove(playerUuid);
            if (requests.isEmpty()) {
                REQUESTS.remove(teamId);
            }
        }
    }

    public static Set<UUID> getRequests(UUID teamId) {
        return REQUESTS.getOrDefault(teamId, Collections.emptySet());
    }

    public static void clearRequestsForPlayer(UUID playerUuid) {
        REQUESTS.values().forEach(set -> set.remove(playerUuid));
    }

    public static void clearRequestsForTeam(UUID teamId) {
        REQUESTS.remove(teamId);
    }

    public static boolean hasRequest(UUID teamId, UUID playerUuid) {
        Set<UUID> requests = REQUESTS.get(teamId);
        return requests != null && requests.contains(playerUuid);
    }

    public static void acceptRequest(MinecraftServer server, UUID teamId, UUID requesterUuid) {
        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterUuid);
        if (requester == null) {
            removeRequest(teamId, requesterUuid);
            return;
        }

        dev.ftb.mods.ftbteams.api.TeamManager ftbManager = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getManager();
        ftbManager.getTeamByID(teamId).ifPresent(team -> {
            if (team.isPartyTeam()) {
                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(team.getOwner());
                if (leader != null) {
                    leader.getServer().getCommandManager().executeWithPrefix(
                            leader.getCommandSource(),
                            "ftbteams party invite " + requester.getName().getString()
                    );
                }
            }
        });

        removeRequest(teamId, requesterUuid);
    }
}