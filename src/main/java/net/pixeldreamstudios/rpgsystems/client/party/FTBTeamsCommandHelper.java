package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;

@Environment(EnvType.CLIENT)
public final class FTBTeamsCommandHelper {
    public static void sendLeaveCommand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FTBTeamsIntegration.isEnabled()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party leave");
        } else {
            mc.getNetworkHandler().sendChatCommand("party leave");
        }
    }

    public static void sendInviteCommand(String targetName) {
        if (targetName == null || targetName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FTBTeamsIntegration.isEnabled()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party invite " + targetName);
        } else {
            mc.getNetworkHandler().sendChatCommand("party invite " + targetName);
        }
    }

    public static void sendKickCommand(String memberName) {
        if (memberName == null || memberName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FTBTeamsIntegration.isEnabled()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party kick " + memberName);
        } else {
            mc.getNetworkHandler().sendChatCommand("party kick " + memberName);
        }
    }

    public static void sendPromoteCommand(String memberName) {
        if (memberName == null || memberName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FTBTeamsIntegration.isEnabled()) {
            if (ClientPartyHudData.partyId != null && ClientPartyHudData.partyName != null) {
                String shortTeamId = ClientPartyHudData.partyId.toString().substring(0, 8);
                String teamShortName = ClientPartyHudData.partyName + "#" + shortTeamId;
                mc.getNetworkHandler().sendChatCommand("ftbteams party transfer_ownership_for " + teamShortName + " " + memberName);
            }
        } else {
            mc.getNetworkHandler().sendChatCommand("party promote " + memberName);
        }
    }
}