package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.partyaddon.network.PartyAddonClientPacket;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;
import net.pixeldreamstudios.rpgsystems.party.PartyAddonIntegration;
import net.pixeldreamstudios.rpgsystems.party.PartyDataProvider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class CompatCommandHelper {

    public static boolean isFTBTeamsActive() {
        return FabricLoader.getInstance().isModLoaded("ftbteams") 
                && FTBTeamsIntegration.isEnabled()
                && ClientPartyHudData.getCurrentSource() == PartyDataProvider.PartySource.FTB_TEAMS;
    }

    public static boolean isPartyAddonActive() {
        return FabricLoader.getInstance().isModLoaded("partyaddon") 
                && PartyAddonIntegration.isEnabled()
                && ClientPartyHudData.getCurrentSource() == PartyDataProvider.PartySource.PARTY_ADDON;
    }

    public static void sendLeaveCommand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (isFTBTeamsActive()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party leave");
        } else if (isPartyAddonActive()) {

            sendPartyAddonLeavePacket();
        } else {
            mc.getNetworkHandler().sendChatCommand("party leave");
        }
    }

    public static void sendInviteCommand(String targetName) {
        if (targetName == null || targetName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (isFTBTeamsActive()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party invite " + targetName);
        } else if (isPartyAddonActive()) {
            sendPartyAddonInviteByName(targetName);
        } else {
            mc.getNetworkHandler().sendChatCommand("party invite " + targetName);
        }
    }

    public static void sendInviteByUuid(UUID targetUuid) {
        if (targetUuid == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        if (isPartyAddonActive()) {
            sendPartyAddonInvitePacket(targetUuid);
        } else if (isFTBTeamsActive()) {
            // FTBTeams doesn't support UUID invite directly, would need name lookup
        } else {
            // Native doesn't support UUID invite directly either
        }
    }

    public static void sendKickCommand(String memberName) {
        if (memberName == null || memberName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (isFTBTeamsActive()) {
            mc.getNetworkHandler().sendChatCommand("ftbteams party kick " + memberName);
        } else if (isPartyAddonActive()) {
            sendPartyAddonKickByName(memberName);
        } else {
            mc.getNetworkHandler().sendChatCommand("party kick " + memberName);
        }
    }

    public static void sendKickByUuid(UUID memberUuid) {
        if (memberUuid == null) return;

        if (isPartyAddonActive()) {
            UUID leaderUuid = ClientPartyHudData.leaderUuid;
            if (leaderUuid != null) {
                sendPartyAddonKickPacket(leaderUuid, memberUuid);
            }
        }
    }

    public static void sendPromoteCommand(String memberName) {
        if (memberName == null || memberName.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        if (isFTBTeamsActive()) {
            if (ClientPartyHudData.partyId != null && ClientPartyHudData.partyName != null) {
                String shortTeamId = ClientPartyHudData.partyId.toString().substring(0, 8);
                String encodedName = urlEncodeName(ClientPartyHudData.partyName);
                String teamIdentifier = encodedName + "#" + shortTeamId;
                mc.getNetworkHandler().sendChatCommand("ftbteams party transfer_ownership_for " + teamIdentifier + " " + memberName);
            }
        } else if (isPartyAddonActive()) {
            MinecraftClient.getInstance().player.sendMessage(
                    net.minecraft.text.Text.literal("PartyAddon doesn't support leader transfer."), false);
        } else {
            mc.getNetworkHandler().sendChatCommand("party promote " + memberName);
        }
    }

    public static void openPartyScreen() {
        if (isPartyAddonActive() || (FabricLoader.getInstance().isModLoaded("partyaddon") && PartyAddonIntegration.isEnabled())) {
            sendPartyAddonOpenScreenPacket();
        } else if (isFTBTeamsActive() || (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled())) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendChatCommand("ftbteams party create");
            }
        }
    }

    private static void sendPartyAddonLeavePacket() {
        try {
            PartyAddonClientPacket.writeC2SLeaveGroupPacket();
        } catch (Exception e) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendChatCommand("party leave");
            }
        }
    }

    private static void sendPartyAddonInvitePacket(UUID targetUuid) {
        try {
            PartyAddonClientPacket.writeC2SInvitePlayerToGroupPacket(targetUuid);
        } catch (Exception e) {
            // PartyAddon invite failed
        }
    }

    private static void sendPartyAddonInviteByName(String targetName) {
        // Look up UUID from client's world
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(targetName)) {
                sendPartyAddonInvitePacket(player.getUuid());
                return;
            }
        }
    }

    private static void sendPartyAddonKickPacket(UUID leaderUuid, UUID kickUuid) {
        try {
            PartyAddonClientPacket.writeC2SKickPlayerPacket(leaderUuid, kickUuid);
        } catch (Exception e) {
            // PartyAddon kick failed
        }
    }

    private static void sendPartyAddonKickByName(String memberName) {
        ClientPartyHudData.Member member = null;
        for (ClientPartyHudData.Member m : ClientPartyHudData.members()) {
            if (m.name.equalsIgnoreCase(memberName)) {
                member = m;
                break;
            }
        }
        if (member != null) {
            sendKickByUuid(member.uuid);
        }
    }

    private static void sendPartyAddonOpenScreenPacket() {
        try {
            PartyAddonClientPacket.writeC2SOpenPartyScreenPacket(MinecraftClient.getInstance());
        } catch (Exception e) {
            // PartyAddon screen open failed
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