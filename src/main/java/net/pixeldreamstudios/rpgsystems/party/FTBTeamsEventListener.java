package net.pixeldreamstudios.rpgsystems.party;

import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.network.PartyNet;
import net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads;

import java.util.Collection;
import java.util.UUID;

public final class FTBTeamsEventListener {

    public static void register() {
        if (!FTBTeamsIntegration.isEnabled()) {
            return;
        }

        TeamEvent.PLAYER_JOINED_PARTY.register(event -> {
            ServerPlayerEntity player = event.getPlayer();
            Team team = event.getTeam();

            if (player != null && team != null) {
                String partyName = team.getProperty(TeamProperties.DISPLAY_NAME);

                ServerPlayNetworking.send(player,
                        new PartyInvitePayloads.InviteJoinConfirmed(partyName));

                for (ServerPlayerEntity member : team.getOnlineMembers()) {
                    if (!member.getUuid().equals(player.getUuid())) {
                        PartyNet.sendNoticeTo(member, team.getId(),
                                player.getName().getString() + " joined the party.",
                                System.currentTimeMillis());
                    }
                }
            }

            onPartyChanged(event.getTeam());
        });

        TeamEvent.PLAYER_LEFT_PARTY.register(event -> {
            ServerPlayerEntity leftPlayer = event.getPlayer();
            Team team = event.getTeam();

            if (leftPlayer != null) {
                String partyName = team != null
                        ? team.getProperty(TeamProperties.DISPLAY_NAME)
                        : "the party";

                ServerPlayNetworking.send(leftPlayer,
                        new PartyInvitePayloads.PartyLeft(partyName));

                PartyNet.sendRosterWipeTo(leftPlayer);
            }

            if (team != null && leftPlayer != null) {
                String leftPlayerName = leftPlayer.getName().getString();
                for (ServerPlayerEntity member : team.getOnlineMembers()) {
                    PartyNet.sendNoticeTo(member, team.getId(),
                            leftPlayerName + " left the party.",
                            System.currentTimeMillis());
                }
            }

            onPartyChanged(event.getTeam());
        });

        TeamEvent.PROPERTIES_CHANGED.register(event -> {
            Team team = event.getTeam();
            if (!team.isPartyTeam()) return;

            for (ServerPlayerEntity member : team.getOnlineMembers()) {
                PartyNet.sendNoticeTo(member, team.getId(),
                        "Party settings updated.",
                        System.currentTimeMillis());
            }

            onPartyChanged(team);
        });

        TeamEvent.OWNERSHIP_TRANSFERRED.register(event -> {
            Team team = event.getTeam();
            if (!team.isPartyTeam()) return;

            ServerPlayerEntity newLeader = null;
            UUID newOwnerId = team.getOwner();
            if (newOwnerId != null) {
                for (ServerPlayerEntity m : team.getOnlineMembers()) {
                    if (m.getUuid().equals(newOwnerId)) {
                        newLeader = m;
                        break;
                    }
                }
            }

            String leaderName = newLeader != null ? newLeader.getName().getString() : "the new leader";
            for (ServerPlayerEntity member : team.getOnlineMembers()) {
                PartyNet.sendNoticeTo(member, team.getId(),
                        "Leadership transferred to " + leaderName + ".",
                        System.currentTimeMillis());
            }

            onPartyChanged(team);
        });

        TeamEvent.DELETED.register(event -> {
            Team team = event.getTeam();
            if (!team.isPartyTeam()) return;

            Collection<ServerPlayerEntity> online = team.getOnlineMembers();

            for (ServerPlayerEntity member : online) {
                PartyNet.sendNoticeTo(member, team.getId(),
                        "Party disbanded.",
                        System.currentTimeMillis());

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

        for (ServerPlayerEntity member : team.getOnlineMembers()) {
            PartyNet.sendFTBTeamsRosterTo(member.getServer(), member, ftbData);
        }
    }
}
