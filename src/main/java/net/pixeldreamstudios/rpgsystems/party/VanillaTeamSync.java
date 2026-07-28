package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VanillaTeamSync {
    private VanillaTeamSync() {}

    public static boolean active() {
        return VanillaTeamsLoader.isEnabled();
    }

    private static String scoreHolderName(MinecraftServer server, UUID memberId) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(memberId);
        if (online != null) return online.getNameForScoreboard();
        return PartyPersistentState.get(server).nameOf(memberId);
    }

    public static void reconcile(MinecraftServer server, Party party) {
        if (!active() || server == null || party == null) return;

        PartyPersistentState state = PartyPersistentState.get(server);
        Scoreboard scoreboard = server.getScoreboard();
        String displayName = (party.name == null || party.name.isBlank()) ? party.id.toString() : party.name;
        Team team = VanillaTeamsIntegration.getOrCreateTeam(server, party.id, displayName);

        Set<String> desired = new HashSet<>();
        for (UUID memberId : party.members) {
            String name = scoreHolderName(server, memberId);
            if (name != null && !name.isBlank()) {
                desired.add(name);
                state.recordIdentity(memberId, name);
            }
        }

        for (String existing : new HashSet<>(team.getPlayerList())) {
            if (!desired.contains(existing)) {
                scoreboard.removeScoreHolderFromTeam(existing, team);
            }
        }
        for (String name : desired) {
            scoreboard.addScoreHolderToTeam(name, team);
        }

        state.setVanillaLeader(party.id, party.leader);
    }

    public static void updateDisplayName(MinecraftServer server, Party party) {
        if (!active() || server == null || party == null) return;
        Team team = VanillaTeamsIntegration.getTeamById(server, party.id);
        if (team == null) return;
        String displayName = (party.name == null || party.name.isBlank()) ? party.id.toString() : party.name;
        team.setDisplayName(Text.literal(displayName));
    }

    public static void removeMember(MinecraftServer server, UUID partyId, UUID memberId) {
        if (!active() || server == null || partyId == null) return;
        String name = scoreHolderName(server, memberId);
        if (name != null) VanillaTeamsIntegration.removeMember(server, partyId, name);
    }

    public static void remove(MinecraftServer server, UUID partyId) {
        if (!active() || server == null || partyId == null) return;
        VanillaTeamsIntegration.removeTeam(server, partyId);
    }
}
