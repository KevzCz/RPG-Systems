package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
public final class CompatCommandHelper {

    public static void sendLeaveCommand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party leave");
        } else {
            mc.getNetworkHandler().sendChatCommand("party leave");
        }
    }

    public static void sendInviteCommand(String targetName) {
        if (targetName == null || targetName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party invite " + targetName);
        } else {
            mc.getNetworkHandler().sendChatCommand("party invite " + targetName);
        }
    }

    public static void sendKickCommand(String memberName) {
        if (memberName == null || memberName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party kick " + memberName);
        } else {
            mc.getNetworkHandler().sendChatCommand("party kick " + memberName);
        }
    }

    public static void sendPromoteCommand(String memberName) {
        if (memberName == null || memberName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
            if (ClientPartyHudData.partyId != null && ClientPartyHudData.partyName != null) {
                String shortTeamId = ClientPartyHudData.partyId.toString().substring(0, 8);

                String encodedName = urlEncodeName(ClientPartyHudData.partyName);
                String teamIdentifier = encodedName + "#" + shortTeamId;

                mc.getNetworkHandler().sendChatCommand("ftbteams party transfer_ownership_for " + teamIdentifier + " " + memberName);
            }
        } else {
            mc.getNetworkHandler().sendChatCommand("party promote " + memberName);
        }
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
}