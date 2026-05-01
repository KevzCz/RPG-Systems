package net.pixeldreamstudios.rpgsystems.party;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

        TeamManager ftbManager =
                FTBTeamsAPI.api().getManager();
        ftbManager.getTeamByID(teamId).ifPresent(team -> {
            if (team.isPartyTeam()) {
                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(team.getOwner());
                if (leader != null) {
                    leader.getServer().getCommandManager().executeWithPrefix(
                            leader.getCommandSource(),
                            "ftbteams party invite " + requester.getName().getString()
                    );

                    leader.sendMessage(Text.literal("Sent invite to " + requester.getName().getString()));
                    requester.sendMessage(Text.literal("Your join request was approved. Accept the invite to join!"));
                }
            }
        });

        removeRequest(teamId, requesterUuid);
    }
    private static String urlEncodeName(String name) {
        if (name == null) return "";

        try {
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);

            encoded = encoded.replace("%27", "_")
                    .replace("%20", "_")
                    .replace("+", "_")
                    .replace("%", "_");

            return encoded;
        } catch (Exception e) {
            return name.replace("'", "_")
                    .replace(" ", "_")
                    .replace("%", "_");
        }
    }
    public static void handleFTBTeamsInviteResponse(ServerPlayerEntity player, PartyInvitePayloads.InviteRespond payload) {
        try {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            Optional<Team> teamOpt = manager.getTeamByID(payload.partyId());

            if (teamOpt.isPresent()) {
                Team team = teamOpt.get();

                String shortTeamId = team.getId().toString().substring(0, 8);
                String displayName = team.getProperty(TeamProperties.DISPLAY_NAME);

                String encodedName = urlEncodeName(displayName);
                String teamIdentifier = encodedName + "#" + shortTeamId;

                if (payload.accept()) {
                    player.getServer().getCommandManager().executeWithPrefix(
                            player.getCommandSource(),
                            "ftbteams party join " + teamIdentifier
                    );
                } else {
                    player.getServer().getCommandManager().executeWithPrefix(
                            player.getCommandSource(),
                            "ftbteams party decline " + teamIdentifier
                    );
                }
            } else {
                if (payload.accept()) {
                    player.getServer().getCommandManager().executeWithPrefix(
                            player.getCommandSource(),
                            "ftbteams party join " + payload.partyId()
                    );
                } else {
                    player.getServer().getCommandManager().executeWithPrefix(
                            player.getCommandSource(),
                            "ftbteams party decline " + payload.partyId()
                    );
                }
            }

            ServerPlayNetworking.send(player, new PartyInvitePayloads.InviteRemoved(payload.partyId()));
        } catch (Exception e) {
            RPGSystems.LOGGER.error("[FTB Teams] Error handling invite response", e);
        }
    }
}