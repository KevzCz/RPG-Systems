package net.pixeldreamstudios.rpgsystems.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.network.PartyNet;
import net.pixeldreamstudios.rpgsystems.network.party.PartyHudPayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class PartyCommands {
    private PartyCommands() {}

    private static String getActiveExternalSystem() {
        if (FTBTeamsLoader.isEnabled()) {
            return "FTB Teams";
        }
        if (PartyAddonLoader.isEnabled()) {
            return "Party Addon";
        }
        return null;
    }

    private static boolean vanillaMode() {
        return VanillaTeamsLoader.isEnabled();
    }

    private static int showExternalSystemMessage(CommandContext<ServerCommandSource> ctx) {
        String system = getActiveExternalSystem();
        if (system != null) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                player.sendMessage(Text.literal("Party management is handled by " + system + ". Use their commands/GUI instead.").styled(s -> s.withColor(0xFFAA00)));
            }
            return 0;
        }
        return 1;
    }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        String externalSystem = getActiveExternalSystem();
        
        if (externalSystem != null) {
            RPGSystems.LOGGER.info("[PartyCommands] Native party commands disabled - using {}", externalSystem);
            d.register(literal("party")
                    .requires(src -> src.hasPermissionLevel(0))
                    .executes(PartyCommands::showExternalSystemMessage)
                    .then(literal("create").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("invite").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("leave").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("disband").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("kick").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("rename").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("promote").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("accept").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("decline").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("request").executes(PartyCommands::showExternalSystemMessage))
                    .then(literal("requests").executes(PartyCommands::showExternalSystemMessage))
            );
            return;
        }

        RPGSystems.LOGGER.info("[PartyCommands] Registering native party commands");
        d.register(literal("party")
                .requires(src -> src.hasPermissionLevel(0))
                .then(literal("create")
                        .executes(ctx -> create(ctx, null))
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(literal("rename")
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> rename(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(literal("promote")
                        .then(argument("member", StringArgumentType.greedyString())
                                .suggests(PartyCommands::suggestPartyMembers)
                                .executes(ctx -> promoteByLabel(ctx, StringArgumentType.getString(ctx, "member")))))
                .then(literal("invite")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(ctx -> invite(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                .then(literal("accept")
                        .then(argument("party", StringArgumentType.greedyString())
                                .suggests(PartyCommands::suggestInvitedParties)
                                .executes(ctx -> acceptByName(ctx, StringArgumentType.getString(ctx, "party")))))
                .then(literal("decline")
                        .then(argument("party", StringArgumentType.greedyString())
                                .suggests(PartyCommands::suggestInvitedParties)
                                .executes(ctx -> declineByName(ctx, StringArgumentType.getString(ctx, "party")))))
                .then(literal("leave")
                        .executes(PartyCommands::leave))
                .then(literal("disband")
                        .executes(PartyCommands::disband))
                .then(literal("request")
                        .then(argument("party", StringArgumentType.greedyString())
                                .suggests(PartyCommands::suggestParties)
                                .executes(ctx -> requestJoinByName(ctx, StringArgumentType.getString(ctx, "party"))))
                        .then(argument("partyId", UuidArgumentType.uuid())
                                .executes(ctx -> requestJoin(ctx, UuidArgumentType.getUuid(ctx, "partyId"))))
                        .then(literal("player")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> requestJoinByPlayer(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                )
                .then(literal("requests")
                        .then(literal("accept")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> acceptJoin(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                        .then(literal("decline")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> declineJoin(ctx, EntityArgumentType.getPlayer(ctx, "player"))))))
                .then(literal("kick")
                        .then(argument("member", StringArgumentType.greedyString())
                                .suggests(PartyCommands::suggestPartyMembers)
                                .executes(ctx -> kickByLabel(ctx, StringArgumentType.getString(ctx, "member")))))
        );
    }
    private static int promoteByLabel(CommandContext<ServerCommandSource> ctx, String label) {
        ServerPlayerEntity leader = ctx.getSource().getPlayer();
        if (leader == null) return 0;

        var server = leader.getServer();
        var state  = PartyPersistentState.get(server);
        Party p    = state.getPartyByMember(leader.getUuid());

        if (p == null) {
            leader.sendMessage(Text.literal("You are not in a party."));
            return 0;
        }
        if (!p.leader.equals(leader.getUuid())) {
            leader.sendMessage(Text.literal("Only the party leader can promote."));
            return 0;
        }

        UUID targetUuid = resolveMemberFromInput(server, state, p, label);
        if (targetUuid == null || targetUuid.equals(leader.getUuid())) {
            leader.sendMessage(Text.literal("Couldn’t resolve a unique member for \"" + label + "\"."));
            return 0;
        }

        String targetName = state.nameOf(targetUuid);
        if (targetName == null) {
            var sp = server.getPlayerManager().getPlayer(targetUuid);
            targetName = (sp != null) ? sp.getName().getString() : shortId(targetUuid);
        }

        p.leader = targetUuid;
        state.markDirty();
        VanillaTeamSync.reconcile(server, p);

        String txt = "Leadership transferred to " + targetName + ".";
        long now = System.currentTimeMillis();

        for (UUID u : p.members) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
            if (sp != null) sp.sendMessage(Text.literal(txt));
        }

        PartyNet.broadcastNotice(server, p, txt, now);
        PartyNet.broadcastRoster(server, p);

        leader.sendMessage(Text.literal("Promoted " + targetName + " to leader."));
        return 1;
    }


    private static int kickByLabel(CommandContext<ServerCommandSource> ctx, String label) {
        ServerPlayerEntity leader = ctx.getSource().getPlayer();
        if (leader == null) return 0;

        var server = leader.getServer();
        var state  = PartyPersistentState.get(server);
        Party p    = state.getPartyByMember(leader.getUuid());

        if (p == null) {
            leader.sendMessage(Text.literal("You are not in a party."));
            return 0;
        }
        if (!p.leader.equals(leader.getUuid())) {
            leader.sendMessage(Text.literal("Only the party leader can kick members."));
            return 0;
        }

        UUID targetUuid = resolveMemberFromInput(server, state, p, label);
        if (targetUuid == null || targetUuid.equals(leader.getUuid())) {
            leader.sendMessage(Text.literal("Couldn’t resolve a unique member for \"" + label + "\"."));
            return 0;
        }

        String targetName = state.nameOf(targetUuid);
        if (targetName == null) {
            var sp = server.getPlayerManager().getPlayer(targetUuid);
            targetName = (sp != null) ? sp.getName().getString() : shortId(targetUuid);
        }

        if (!state.kick(leader.getUuid(), targetUuid)) {
            leader.sendMessage(Text.literal(targetName + " is not in your party."));
            return 0;
        }

        VanillaTeamSync.removeMember(server, p.id, targetUuid);
        VanillaTeamSync.reconcile(server, p);

        ServerPlayerEntity targetOnline = server.getPlayerManager().getPlayer(targetUuid);
        if (targetOnline != null) {
            PartyNet.sendRosterWipeTo(targetOnline);
            targetOnline.sendMessage(Text.literal("You were kicked from " + displayName(p) + "."));
            ServerPlayNetworking.send(targetOnline, new PartyInvitePayloads.PartyKicked(displayName(p)));
        }

        String notice = targetName + " was kicked from the party.";
        long now = System.currentTimeMillis();

        for (UUID u : p.members) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
            if (sp != null) sp.sendMessage(Text.literal(notice));
        }

        PartyNet.broadcastNotice(server, p, notice, now);
        PartyNet.broadcastRoster(server, p);

        leader.sendMessage(Text.literal("Kicked " + targetName + "."));
        return 1;
    }

    private static int create(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        Party p = state.getPartyByMember(self.getUuid());
        if (p != null) {
            self.sendMessage(Text.literal("You are already in a party."));
            return 1;
        }

        String finalName = (name == null || name.isBlank()) ? defaultPartyName(self) : name;
        Party created = state.createParty(self.getUuid(), finalName);
        state.rememberName(self.getUuid(), self.getName().getString());
        VanillaTeamSync.reconcile(self.getServer(), created);
        self.sendMessage(Text.literal("Party created: " + displayName(created)));
        PartyNet.sendRosterTo(self.getServer(), self, created);
        return 1;
    }


    private static int rename(CommandContext<ServerCommandSource> ctx, String newName) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;

        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        Party p = state.getPartyByMember(self.getUuid());
        if (p == null || !p.leader.equals(self.getUuid())) {
            self.sendMessage(Text.literal("You must be the party leader to rename."));
            return 0;
        }

        String desired = (newName == null) ? "" : newName.trim();

        for (Party other : state.allParties()) {
            if (!other.id.equals(p.id)) {
                String on = (other.name == null) ? "" : other.name.trim();
                if (!on.isEmpty() && on.equalsIgnoreCase(desired)) {
                    self.sendMessage(Text.literal("That party name is already in use."));
                    return 0;
                }
            }
        }

        boolean ok = state.renameParty(self.getUuid(), desired);
        if (!ok) {
            self.sendMessage(Text.literal("You must be the party leader to rename."));
            return 0;
        }

        VanillaTeamSync.updateDisplayName(self.getServer(), state.getPartyByMember(self.getUuid()));
        PartyNet.broadcastRoster(self.getServer(), state.getPartyByMember(self.getUuid()));
        self.sendMessage(Text.literal("Renamed party to " + desired + "."));
        return 1;
    }

    private static int invite(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        if (Objects.equals(self.getUuid(), target.getUuid())) {
            self.sendMessage(Text.literal("You cannot invite yourself."));
            return 0;
        }

        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        Party p = state.getPartyByMember(self.getUuid());
        if (p == null) {
            p = state.createParty(self.getUuid(), defaultPartyName(self));
            state.rememberName(self.getUuid(), self.getName().getString());
            VanillaTeamSync.reconcile(self.getServer(), p);
            PartyNet.sendRosterTo(self.getServer(), self, p);
        }
        if (!p.leader.equals(self.getUuid())) {
            self.sendMessage(Text.literal("Only the party leader can invite."));
            return 0;
        }

        boolean ok = state.invite(self.getUuid(), target.getUuid());
        if (!ok) {
            self.sendMessage(Text.literal("Unable to send invite."));
            return 0;
        }

        state.rememberName(self.getUuid(), self.getName().getString());
        state.rememberName(target.getUuid(), target.getName().getString());

        String partyName = displayName(p);
        ServerPlayNetworking.send(target, new PartyInvitePayloads.InviteAdded(p.id, p.leader, self.getName().getString(), partyName));
        ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteSent(self.getName().getString(), target.getName().getString(), partyName));
        self.sendMessage(Text.literal("Invited " + target.getName().getString() + " to " + partyName + "."));
        return 1;
    }

    private static int acceptAny(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());

        Set<UUID> invitesBefore = Set.copyOf(state.invitesOf(self.getUuid()));
        Set<UUID> joinReqParties = state.partiesWithJoinRequestFrom(self.getUuid());

        boolean ok = state.acceptAny(self.getUuid());
        if (!ok) {
            self.sendMessage(Text.literal("No valid invites to accept (they may have expired)."));
            ServerPlayNetworking.send(self, new PartyHudPayloads.PartyRosterReset());
            return 0;
        }

        Party p = state.getPartyByMember(self.getUuid());
        if (p != null) {
            state.rememberName(self.getUuid(), self.getName().getString());
            VanillaTeamSync.reconcile(self.getServer(), p);
            ServerPlayerEntity leader = self.getServer().getPlayerManager().getPlayer(p.leader);
            if (leader != null) {
                String partyName = displayName(p);
                ServerPlayNetworking.send(leader, new PartyInvitePayloads.InviteAccepted(self.getName().getString(), partyName));
            }
            ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteJoinConfirmed(displayName(p)));
            self.sendMessage(Text.literal("Joined " + displayName(p)));

            for (UUID pid : invitesBefore) {
                ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteRemoved(pid));
            }
            for (UUID pid : joinReqParties) {
                Party p2 = state.getParty(pid);
                if (p2 != null) {
                    ServerPlayerEntity leader2 = self.getServer().getPlayerManager().getPlayer(p2.leader);
                    if (leader2 != null) {
                        ServerPlayNetworking.send(leader2, new PartyJoinRequestPayloads.JoinReqRemoved(pid, self.getUuid()));
                    }
                }
            }

            PartyNet.broadcastRoster(self.getServer(), p);
        }
        return 1;
    }

    private static int acceptSpecific(CommandContext<ServerCommandSource> ctx, UUID partyId) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());

        Set<UUID> invitesBefore = Set.copyOf(state.invitesOf(self.getUuid()));
        Set<UUID> joinReqParties = state.partiesWithJoinRequestFrom(self.getUuid());

        boolean ok = state.accept(self.getUuid(), partyId);
        if (!ok) {
            self.sendMessage(Text.literal("Invite not found or expired."));
            ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteRemoved(partyId));
            return 0;
        }
        Party p = state.getPartyByMember(self.getUuid());
        if (p != null) {
            state.rememberName(self.getUuid(), self.getName().getString());
            VanillaTeamSync.reconcile(self.getServer(), p);
            ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteJoinConfirmed(displayName(p)));
            self.sendMessage(Text.literal("Joined " + displayName(p)));

            for (UUID pid : invitesBefore) {
                ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteRemoved(pid));
            }
            for (UUID pid : joinReqParties) {
                Party p2 = state.getParty(pid);
                if (p2 != null) {
                    ServerPlayerEntity leader2 = self.getServer().getPlayerManager().getPlayer(p2.leader);
                    if (leader2 != null) {
                        ServerPlayNetworking.send(leader2, new PartyJoinRequestPayloads.JoinReqRemoved(pid, self.getUuid()));
                    }
                }
            }

            PartyNet.broadcastRoster(self.getServer(), p);
        }
        return 1;
    }

    private static int declineAny(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        Set<UUID> invites = state.invitesOf(self.getUuid());
        if (invites.isEmpty()) {
            self.sendMessage(Text.literal("You have no invites."));
            return 0;
        }
        for (UUID pid : invites) {
            declineInternal(self, state, pid);
        }
        return 1;
    }

    private static int declineSpecific(CommandContext<ServerCommandSource> ctx, UUID partyId) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        if (!state.removeInvite(self.getUuid(), partyId)) {
            self.sendMessage(Text.literal("Invite not found or expired."));
            return 0;
        }
        declineNotify(self, state, partyId);
        return 1;
    }

    private static void declineInternal(ServerPlayerEntity self, PartyPersistentState state, UUID partyId) {
        if (state.removeInvite(self.getUuid(), partyId)) {
            declineNotify(self, state, partyId);
        }
    }

    private static void declineNotify(ServerPlayerEntity self, PartyPersistentState state, UUID partyId) {
        Party p = state.getParty(partyId);
        if (p != null) {
            String partyName = displayName(p);
            ServerPlayerEntity leader = self.getServer().getPlayerManager().getPlayer(p.leader);
            if (leader != null) {
                ServerPlayNetworking.send(leader, new PartyInvitePayloads.InviteDeclined(self.getName().getString(), partyName));
            }
            ServerPlayNetworking.send(self, new PartyInvitePayloads.InviteRemoved(partyId));
            self.sendMessage(Text.literal("Declined invite to " + partyName + "."));
        }
    }

    private static int leave(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;

        MinecraftServer server = self.getServer();
        PartyPersistentState state = PartyPersistentState.get(server);
        Party p = state.getPartyByMember(self.getUuid());
        if (p == null) { self.sendMessage(Text.literal("You are not in a party.")); return 0; }
        String partyName = displayName(p);
        boolean isLeader = p.leader.equals(self.getUuid());
        Set<UUID> membersSnapshot = Set.copyOf(p.members);
        Set<UUID> otherMembers = new HashSet<>(membersSnapshot);

        otherMembers.remove(self.getUuid());

        UUID partyId = p.id;
        boolean ok = state.leave(self.getUuid());
        if (!ok) return 0;

        VanillaTeamSync.removeMember(server, partyId, self.getUuid());

        if (isLeader && otherMembers.isEmpty()) {
            // Was a solo leader — party is now disbanded
            VanillaTeamSync.remove(server, partyId);
            self.sendMessage(Text.literal("You left and the party was disbanded."));
        } else if (isLeader) {
            // Leadership was transferred to another member
            UUID anyRemaining = otherMembers.iterator().next();
            Party remaining = state.getPartyByMember(anyRemaining);
            String newLeaderName = null;
            if (remaining != null) {
                newLeaderName = state.nameOf(remaining.leader);
                if (newLeaderName == null) {
                    ServerPlayerEntity sp = server.getPlayerManager().getPlayer(remaining.leader);
                    if (sp != null) newLeaderName = sp.getName().getString();
                }
            }
            String leftMsg = self.getName().getString() + " left the party.";
            String promoteMsg = (newLeaderName != null ? newLeaderName : "Someone") + " is now the party leader.";
            for (UUID u : otherMembers) {
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                if (sp != null) {
                    sp.sendMessage(Text.literal(leftMsg));
                    sp.sendMessage(Text.literal(promoteMsg));
                }
            }
            if (remaining != null) {
                VanillaTeamSync.reconcile(server, remaining);
                PartyNet.broadcastRoster(server, remaining);
            }
            self.sendMessage(Text.literal("You left the party."));
        } else {
            UUID anyRemaining = otherMembers.stream().findFirst().orElse(null);
            Party remaining = state.getPartyByMember(anyRemaining);
            if (remaining != null) {
                VanillaTeamSync.reconcile(server, remaining);
                String name = self.getName().getString();
                for (UUID u : remaining.members) {
                    ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                    if (sp != null) sp.sendMessage(Text.literal(name + " left the party."));
                }
                PartyNet.broadcastRoster(server, remaining);
            }
            self.sendMessage(Text.literal("You left the party."));
        }
        ServerPlayNetworking.send(self, new PartyInvitePayloads.PartyLeft(partyName));
        PartyNet.sendRosterWipeTo(self);
        return 1;
    }

    private static int disband(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        MinecraftServer server = self.getServer();
        PartyPersistentState state = PartyPersistentState.get(server);
        Party p = state.getPartyByMember(self.getUuid());
        if (p == null) {
            self.sendMessage(Text.literal("You are not in a party."));
            return 0;
        }
        if (!p.leader.equals(self.getUuid())) {
            self.sendMessage(Text.literal("Only the party leader can disband."));
            return 0;
        }
        Set<UUID> inviteTargets = state.targetsInvitedBy(p.id);
        Set<UUID> reqs = state.joinRequestsOf(p.id);
        Set<UUID> members = Set.copyOf(p.members);
        UUID partyId = p.id;
        boolean ok = state.disband(self.getUuid());
        if (!ok) return 0;

        VanillaTeamSync.remove(server, partyId);
        for (UUID t : inviteTargets) {
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(t);
            if (target != null) {
                ServerPlayNetworking.send(target, new PartyInvitePayloads.InviteRemoved(p.id));
            }
        }
        for (UUID requester : reqs) {
            ServerPlayNetworking.send(self, new PartyJoinRequestPayloads.JoinReqRemoved(p.id, requester));
        }
        for (UUID u : members) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
            if (sp != null && !u.equals(self.getUuid())) {
                ServerPlayNetworking.send(sp, new PartyHudPayloads.PartyRosterReset());
                sp.sendMessage(Text.literal("Your party disbanded."));
            }
        }
        self.sendMessage(Text.literal("Party disbanded."));
        PartyNet.sendRosterWipeTo(self);
        return 1;
    }

    private static int requestJoin(CommandContext<ServerCommandSource> ctx, UUID partyId) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        boolean ok = state.requestJoin(self.getUuid(), partyId);
        if (!ok) {
            self.sendMessage(Text.literal("Unable to send join request."));
            return 0;
        }
        Party p = state.getParty(partyId);
        if (p == null) return 0;
        ServerPlayerEntity leader = self.getServer().getPlayerManager().getPlayer(p.leader);
        if (leader != null) {
            PartyNet.sendJoinReqAdded(leader, p.id, self.getUuid(), self.getName().getString());
            leader.sendMessage(Text.literal(self.getName().getString() + " requested to join " + displayName(p) + "."));
        }
        self.sendMessage(Text.literal("Join request sent."));
        return 1;
    }

    private static int requestJoinByPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(self.getServer());
        Party targetParty = state.getPartyByMember(target.getUuid());
        if (targetParty == null) {
            self.sendMessage(Text.literal(target.getName().getString() + " is not in a party."));
            return 0;
        }
        return requestJoin(ctx, targetParty.id);
    }

    private static int acceptJoin(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity requester) {
        ServerPlayerEntity leader = ctx.getSource().getPlayer();
        if (leader == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(leader.getServer());
        boolean ok = state.acceptJoin(leader.getUuid(), requester.getUuid());
        if (!ok) {
            leader.sendMessage(Text.literal("No matching join request."));
            return 0;
        }
        Party p = state.getPartyByMember(leader.getUuid());
        if (p != null) {
            state.rememberName(requester.getUuid(), requester.getName().getString());
            VanillaTeamSync.reconcile(leader.getServer(), p);
            PartyNet.sendJoinReqRemoved(leader, p.id, requester.getUuid());
            ServerPlayerEntity req = leader.getServer().getPlayerManager().getPlayer(requester.getUuid());
            if (req != null) {
                ServerPlayNetworking.send(req, new PartyJoinRequestPayloads.JoinAccepted(
                        leader.getName().getString(), displayName(p)));
                req.sendMessage(Text.literal("Your join request was accepted. You joined " + displayName(p) + "."));
            }
            PartyNet.broadcastRoster(leader.getServer(), p);
            leader.sendMessage(Text.literal("Accepted join request from " + requester.getName().getString() + "."));
        }
        return 1;
    }

    private static int declineJoin(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity requester) {
        ServerPlayerEntity leader = ctx.getSource().getPlayer();
        if (leader == null) return 0;
        PartyPersistentState state = PartyPersistentState.get(leader.getServer());
        boolean ok = state.declineJoin(leader.getUuid(), requester.getUuid());
        if (!ok) {
            leader.sendMessage(Text.literal("No matching join request."));
            return 0;
        }
        Party p = state.getPartyByMember(leader.getUuid());
        if (p != null) {
            PartyNet.sendJoinReqRemoved(leader, p.id, requester.getUuid());
            ServerPlayerEntity req = leader.getServer().getPlayerManager().getPlayer(requester.getUuid());
            if (req != null) {
                ServerPlayNetworking.send(req, new PartyJoinRequestPayloads.JoinDeclined(
                        leader.getName().getString(), displayName(p)));
                req.sendMessage(Text.literal("Your join request was declined."));
            }
            leader.sendMessage(Text.literal("Declined join request from " + requester.getName().getString() + "."));
        }
        return 1;
    }

    private static int kick(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerPlayerEntity leader = ctx.getSource().getPlayer();
        if (leader == null) return 0;

        var server = leader.getServer();
        var state  = PartyPersistentState.get(server);
        var p      = state.getPartyByMember(leader.getUuid());

        if (p == null) {
            leader.sendMessage(Text.literal("You are not in a party."));
            return 0;
        }
        if (!p.leader.equals(leader.getUuid())) {
            leader.sendMessage(Text.literal("Only the party leader can kick members."));
            return 0;
        }
        if (target.getUuid().equals(leader.getUuid())) {
            leader.sendMessage(Text.literal("You cannot kick yourself (use /party leave)."));
            return 0;
        }

        if (!state.kick(leader.getUuid(), target.getUuid())) {
            leader.sendMessage(Text.literal(target.getName().getString() + " is not in your party."));
            return 0;
        }

        VanillaTeamSync.removeMember(server, p.id, target.getUuid());
        VanillaTeamSync.reconcile(server, p);

        PartyNet.sendRosterWipeTo(target);
        target.sendMessage(Text.literal("You were kicked from " + displayName(p) + "."));

        for (UUID u : p.members) {
            var sp = server.getPlayerManager().getPlayer(u);
            if (sp != null) sp.sendMessage(Text.literal(target.getName().getString() + " was kicked from the party."));
        }
        PartyNet.broadcastRoster(server, p);

        return 1;
    }

    private static String defaultPartyName(ServerPlayerEntity self) {
        String base = self.getName().getString();
        return base.endsWith("s") ? (base + "' Party") : (base + "'s Party");
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static CompletableFuture<Suggestions> suggestParties(CommandContext<ServerCommandSource> c,
                                                                 SuggestionsBuilder b) {
        var server = c.getSource().getServer();
        var state  = PartyPersistentState.get(server);
        ServerPlayerEntity self = c.getSource().getPlayer();
        UUID selfPartyId = null;
        if (self != null) {
            var sp = state.getPartyByMember(self.getUuid());
            if (sp != null) selfPartyId = sp.id;
        }

        Set<String> labels = new LinkedHashSet<>();
        for (Party p : state.allParties()) {
            if (p.id.equals(selfPartyId)) continue;

            String leader = state.nameOf(p.leader);
            if (leader == null) {
                var sp = server.getPlayerManager().getPlayer(p.leader);
                leader = (sp != null) ? sp.getName().getString() : shortId(p.leader);
            }
            String name = displayName(p);
            labels.add(name);
            labels.add(name + " (" + leader + ")");
            labels.add(name + " #" + shortId(p.id));
        }

        return CommandSource.suggestMatching(labels, b);
    }

    private static UUID resolvePartyFromInput(MinecraftServer server, PartyPersistentState state, String input) {
        if (input == null || input.isBlank()) return null;

        try { return UUID.fromString(input.trim()); } catch (IllegalArgumentException ignored) {}

        String qi = input.trim();
        String qiLower = qi.toLowerCase(Locale.ROOT);

        UUID match = null;
        int matches = 0;

        for (Party p : state.allParties()) {
            String name = displayName(p);
            String leader = state.nameOf(p.leader);
            if (leader == null) {
                var sp = server.getPlayerManager().getPlayer(p.leader);
                leader = (sp != null) ? sp.getName().getString() : shortId(p.leader);
            }
            String labelPlain  = name;
            String labelLeader = name + " (" + leader + ")";
            String labelShort  = name + " #" + shortId(p.id);

            if (labelPlain.equalsIgnoreCase(qi) ||
                    labelLeader.equalsIgnoreCase(qi) ||
                    labelShort.equalsIgnoreCase(qi)) {
                match = p.id; matches++; continue;
            }

            if (labelLeader.toLowerCase(Locale.ROOT).contains(qiLower)) {
                match = p.id; matches++;
            }
        }
        return (matches == 1) ? match : null;
    }

    private static CompletableFuture<Suggestions> suggestInvitedParties(CommandContext<ServerCommandSource> c,
                                                                        SuggestionsBuilder b) {
        ServerPlayerEntity self = c.getSource().getPlayer();
        if (self == null) return Suggestions.empty();
        var server = self.getServer();
        var state  = PartyPersistentState.get(server);

        for (UUID pid : state.invitesOf(self.getUuid())) {
            Party p = state.getParty(pid);
            if (p == null) continue;
            String name = displayName(p);
            String leader = state.nameOf(p.leader);
            if (leader == null) {
                var sp = server.getPlayerManager().getPlayer(p.leader);
                leader = (sp != null) ? sp.getName().getString() : shortId(p.leader);
            }
            b.suggest(name);
            b.suggest(name + " (" + leader + ")");
            b.suggest(name + " #" + shortId(p.id));
        }
        return b.buildFuture();
    }

    private static UUID resolveInvitedPartyFromInput(MinecraftServer server, PartyPersistentState state,
                                                     ServerPlayerEntity self, String input) {
        if (input == null || input.isBlank()) return null;

        try { return UUID.fromString(input.trim()); } catch (IllegalArgumentException ignored) {}

        String qi = input.trim();
        String qiLower = qi.toLowerCase(Locale.ROOT);

        UUID match = null; int matches = 0;
        for (UUID pid : state.invitesOf(self.getUuid())) {
            Party p = state.getParty(pid);
            if (p == null) continue;
            String name   = displayName(p);
            String leader = state.nameOf(p.leader);
            if (leader == null) {
                var sp = server.getPlayerManager().getPlayer(p.leader);
                leader = (sp != null) ? sp.getName().getString() : shortId(p.leader);
            }
            String labelPlain  = name;
            String labelLeader = name + " (" + leader + ")";
            String labelShort  = name + " #" + shortId(p.id);

            if (labelPlain.equalsIgnoreCase(qi) || labelLeader.equalsIgnoreCase(qi) || labelShort.equalsIgnoreCase(qi)) {
                match = p.id; matches++; continue;
            }
            if (labelLeader.toLowerCase(Locale.ROOT).contains(qiLower)) { match = p.id; matches++; }
        }
        return (matches == 1) ? match : null;
    }

    private static int requestJoinByName(CommandContext<ServerCommandSource> ctx, String userInput) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        var server = self.getServer();
        var state  = PartyPersistentState.get(server);

        UUID partyId = resolvePartyFromInput(server, state, userInput);
        if (partyId == null) {
            self.sendMessage(Text.literal("Couldn’t find a unique party for \"" + userInput +
                    "\". Use Tab to pick from the suggestions."));
            return 0;
        }
        return requestJoin(ctx, partyId);
    }

    private static String displayName(Party p) {
        return (p.name == null || p.name.isBlank()) ? p.id.toString() : p.name;
    }

    private static CompletableFuture<Suggestions> suggestPartyMembers(CommandContext<ServerCommandSource> c,
                                                                      SuggestionsBuilder b) {
        ServerPlayerEntity leader = c.getSource().getPlayer();
        if (leader == null) return Suggestions.empty();

        var server = leader.getServer();
        var state  = PartyPersistentState.get(server);
        Party p    = state.getPartyByMember(leader.getUuid());
        if (p == null || !p.leader.equals(leader.getUuid())) return Suggestions.empty();

        for (UUID u : p.members) {
            if (u.equals(leader.getUuid())) continue;
            String name = state.nameOf(u);
            if (name == null) {
                var sp = server.getPlayerManager().getPlayer(u);
                name = (sp != null) ? sp.getName().getString() : shortId(u);
            }
            b.suggest(name);
            b.suggest(name + " #" + shortId(u));
            b.suggest(u.toString());
        }
        return b.buildFuture();
    }

    private static UUID resolveMemberFromInput(MinecraftServer server, PartyPersistentState state, Party p, String input) {
        if (input == null || input.isBlank()) return null;

        try { return UUID.fromString(input.trim()); } catch (IllegalArgumentException ignored) {}

        String qi = input.trim();
        String qiLower = qi.toLowerCase(Locale.ROOT);

        UUID match = null; int matches = 0;

        for (UUID u : p.members) {
            String name = state.nameOf(u);
            if (name == null) {
                var sp = server.getPlayerManager().getPlayer(u);
                name = (sp != null) ? sp.getName().getString() : shortId(u);
            }
            String label1 = name;
            String label2 = name + " #" + shortId(u);

            if (label1.equalsIgnoreCase(qi) || label2.equalsIgnoreCase(qi)) {
                match = u; matches++; continue;
            }
            if (label1.toLowerCase(Locale.ROOT).contains(qiLower) ||
                    label2.toLowerCase(Locale.ROOT).contains(qiLower)) {
                match = u; matches++;
            }
            if (shortId(u).equalsIgnoreCase(qi)) { match = u; matches++; }
        }
        return (matches == 1) ? match : null;
    }

    private static int acceptByName(CommandContext<ServerCommandSource> ctx, String userInput) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        var server = self.getServer();
        var state  = PartyPersistentState.get(server);
        UUID partyId = resolveInvitedPartyFromInput(server, state, self, userInput);
        if (partyId == null) {
            self.sendMessage(Text.literal("Couldn’t find a unique invite for \"" + userInput +
                    "\". Use Tab to pick from your invite list."));
            return 0;
        }
        return acceptSpecific(ctx, partyId);
    }

    private static int declineByName(CommandContext<ServerCommandSource> ctx, String userInput) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        if (self == null) return 0;
        var server = self.getServer();
        var state  = PartyPersistentState.get(server);
        UUID partyId = resolveInvitedPartyFromInput(server, state, self, userInput);
        if (partyId == null) {
            self.sendMessage(Text.literal("Couldn’t find a unique invite for \"" + userInput +
                    "\". Use Tab to pick from your invite list."));
            return 0;
        }
        return declineSpecific(ctx, partyId);
    }
}
