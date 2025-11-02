package net.pixeldreamstudios.rpgsystems.party;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.network.PartyNet;
import net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads;

import java.util.List;
import java.util.UUID;

public class FTBTeamsVitalsHandler {

    public static void pushAllVitals(MinecraftServer server) {
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayer(viewer);
            if (ftbData == null) continue;

            for (UUID memberId : ftbData.members) {
                ServerPlayerEntity subject = server.getPlayerManager().getPlayer(memberId);
                if (subject == null) {
                    ServerPlayNetworking.send(viewer,
                            new PartyStatusEffectsPayloads.MemberEffects(memberId, List.of()));
                    continue;
                }

                PartyNet.pushVitalsForMember(viewer, subject, memberId);
            }
        }
    }
}