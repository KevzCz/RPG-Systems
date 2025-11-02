package net.pixeldreamstudios.rpgsystems.party;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads;

import java.util.Optional;
import java.util.UUID;

import static net.minecraft.util.Util.NIL_UUID;

public final class FTBTeamsChatBridge {

    public static void register() {
        if (!FTBTeamsIntegration.isEnabled()) {
            return;
        }

        // Listen to FTB Teams messages and forward to our chat system
        // Note: FTB Teams doesn't have a direct event for this, so we'll need to
        // hook into their sendMessage method via our event listener
    }

    /**
     * Send a message from your party chat system to FTB Teams
     */
    public static void sendToFTBTeams(ServerPlayerEntity sender, String message) {
        if (!FTBTeamsIntegration.isEnabled()) {
            return;
        }

        Optional<Team> teamOpt = FTBTeamsAPI.api().getManager().getTeamForPlayer(sender);
        if (teamOpt.isPresent() && teamOpt.get().isPartyTeam()) {
            Team team = teamOpt.get();
            // FTB Teams will handle broadcasting to all members
            team.sendMessage(sender.getUuid(), message);
        }
    }

    /**
     * Forward FTB Teams message to our party chat HUD
     */
    public static void forwardToRPGSystems(MinecraftServer server, Team team, UUID senderId, String message) {
        if (!team.isPartyTeam()) {
            return;
        }

        long now = System.currentTimeMillis();
        String senderName = getSenderName(server, senderId);

        // Send to all online party members using our chat payload
        for (ServerPlayerEntity member : team.getOnlineMembers()) {
            PartyChatPayloads.ChatMessage payload = new PartyChatPayloads.ChatMessage(
                    team.getId(),
                    senderId,
                    senderName,
                    message,
                    now
            );
            ServerPlayNetworking.send(member, payload);
        }
    }

    private static String getSenderName(MinecraftServer server, UUID senderId) {
        if (senderId == null || senderId.equals(NIL_UUID)) {
            return "Party";
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(senderId);
        if (player != null) {
            return player.getName().getString();
        }

        return "Unknown";
    }
}