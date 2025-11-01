package net.pixeldreamstudios.rpgsystems.party;

import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.network.PartyNet;

import java.util.Collection;

public final class FTBTeamsEventListener {

    public static void register() {
        if (!FTBTeamsIntegration.isEnabled()) {
            return;
        }

        TeamEvent.PLAYER_JOINED_PARTY.register(event -> {

            onPartyChanged(event.getTeam());
        });

        TeamEvent.PLAYER_LEFT_PARTY.register(event -> {
            ServerPlayerEntity leftPlayer = event.getPlayer();
            if (leftPlayer != null) {
                PartyNet.sendRosterWipeTo(leftPlayer);
            }

            onPartyChanged(event.getTeam());
        });

        TeamEvent.PROPERTIES_CHANGED.register(event -> {
            Team team = event.getTeam();
            if (!team.isPartyTeam()) return;

            onPartyChanged(team);
        });

        TeamEvent.OWNERSHIP_TRANSFERRED.register(event -> {
            Team team = event.getTeam();
            if (!team.isPartyTeam()) return;
            onPartyChanged(team);
        });

        TeamEvent.DELETED.register(event -> {
            Team team = event.getTeam();
            if (!team.isPartyTeam()) return;

            Collection<ServerPlayerEntity> online = team.getOnlineMembers();
            for (ServerPlayerEntity member : online) {
                PartyNet.sendRosterWipeTo(member);
            }
        });

    }

    private static void onPartyChanged(Team team) {
        if (!team.isPartyTeam()) {
            return;
        }

        FTBTeamsIntegration.FTBPartyData ftbData = new FTBTeamsIntegration.FTBPartyData(
                team.getId(),
                team.getProperty(dev.ftb.mods.ftbteams.api.property.TeamProperties.DISPLAY_NAME),
                team.getOwner(),
                team.getMembers(),
                new PartySettings()
        );

        int syncedPlayers = 0;
        for (ServerPlayerEntity member : team.getOnlineMembers()) {
            PartyNet.sendFTBTeamsRosterTo(member.getServer(), member, ftbData);
            syncedPlayers++;
        }

    }
}
