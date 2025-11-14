package net.pixeldreamstudios.rpgsystems.network;


import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.party.*;
import net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads.ChatNotice;
import net.pixeldreamstudios.rpgsystems.network.party.PartyHudPayloads.*;
import net.pixeldreamstudios.rpgsystems.network.party.PartySettingsPayloads.SetAllowHelpfulNonMembers;
import net.pixeldreamstudios.rpgsystems.network.party.PartySettingsPayloads.Sync;
import net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects;
import net.pixeldreamstudios.rpgsystems.party.*;
import net.puffish.skillsmod.SkillsMod;

import java.util.*;

import static net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads.ChatMessage;
import static net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads.ChatSend;
import static net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads.*;
import static net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads.*;
import static net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration.getPartyDataForPlayer;

public final class PartyNet {
    private PartyNet() {}
    private static int tickCounter = 0;
    public static boolean PERSIST_PARTIES_ON_DISCONNECT = true;

    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(InviteAdded.ID, InviteAdded.CODEC);
        PayloadTypeRegistry.playS2C().register(InviteRemoved.ID, InviteRemoved.CODEC);
        PayloadTypeRegistry.playS2C().register(InviteSent.ID, InviteSent.CODEC);
        PayloadTypeRegistry.playS2C().register(InviteAccepted.ID, InviteAccepted.CODEC);
        PayloadTypeRegistry.playS2C().register(InviteDeclined.ID, InviteDeclined.CODEC);
        PayloadTypeRegistry.playC2S().register(InviteRespond.ID, InviteRespond.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyInvitePayloads.InviteJoinConfirmed.ID, PartyInvitePayloads.InviteJoinConfirmed.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyInvitePayloads.PartyLeft.ID, PartyInvitePayloads.PartyLeft.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyInvitePayloads.PartyKicked.ID, PartyInvitePayloads.PartyKicked.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyRosterClear.ID, PartyRosterClear.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyRosterAdd.ID, PartyRosterAdd.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyMemberVitals.ID, PartyMemberVitals.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyRosterReset.ID, PartyRosterReset.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyMemberLevel.ID, PartyMemberLevel.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyMemberOnline.ID, PartyMemberOnline.CODEC);
        PayloadTypeRegistry.playS2C().register(JoinReqAdded.ID, JoinReqAdded.CODEC);
        PayloadTypeRegistry.playS2C().register(JoinReqRemoved.ID, JoinReqRemoved.CODEC);
        PayloadTypeRegistry.playC2S().register(JoinReqRespond.ID, JoinReqRespond.CODEC);
        PayloadTypeRegistry.playS2C().register(JoinAccepted.ID, JoinAccepted.CODEC);
        PayloadTypeRegistry.playS2C().register(JoinDeclined.ID, JoinDeclined.CODEC);
        PayloadTypeRegistry.playC2S().register(ChatSend.ID, ChatSend.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatMessage.ID, ChatMessage.CODEC);
        PayloadTypeRegistry.playC2S().register(SetAllowHelpfulNonMembers.ID, PartySettingsPayloads.SetAllowHelpfulNonMembers.CODEC);
        PayloadTypeRegistry.playC2S().register(PartySettingsPayloads.SetIgnorePartyCollision.ID, PartySettingsPayloads.SetIgnorePartyCollision.CODEC);
        PayloadTypeRegistry.playS2C().register(Sync.ID, PartySettingsPayloads.Sync.CODEC);
        PayloadTypeRegistry.playC2S().register(EligibleInviteesRequest.ID, PartyInvitePayloads.EligibleInviteesRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(EligibleInviteesResponse.ID, PartyInvitePayloads.EligibleInviteesResponse.CODEC);
        PayloadTypeRegistry.playS2C().register(MemberEffects.ID, PartyStatusEffectsPayloads.MemberEffects.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatNotice.ID, PartyChatPayloads.ChatNotice.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyChatPayloads.ChatPinSet.ID, PartyChatPayloads.ChatPinSet.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyChatPayloads.ChatPinClear.ID, PartyChatPayloads.ChatPinClear.CODEC);
        PayloadTypeRegistry.playC2S().register(FTBTeamsJoinRequest.ID, FTBTeamsJoinRequest.CODEC);

        if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
            registerFTBTeamsReceivers();
        }

        ServerPlayNetworking.registerGlobalReceiver(
                PartyInvitePayloads.EligibleInviteesRequest.ID, (payload, ctx) -> {
                    ServerPlayerEntity who = ctx.player();
                    MinecraftServer server = who.getServer();
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            List<UUID> uuids = new ArrayList<>();
                            List<String> names = new ArrayList<>();
                            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                                if (sp.getUuid().equals(who.getUuid())) continue;
                                if (getPartyDataForPlayer(sp) != null) continue;
                                uuids.add(sp.getUuid());
                                names.add(sp.getGameProfile().getName());
                            }
                            ServerPlayNetworking.send(who,
                                    new PartyInvitePayloads.EligibleInviteesResponse(uuids, names));
                            return;
                        }
                    }
                    PartyPersistentState state = PartyPersistentState.get(server);
                    List<UUID> uuids = new ArrayList<>();
                    List<String> names = new ArrayList<>();
                    for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                        if (sp.getUuid().equals(who.getUuid())) continue;
                        if (state.getPartyByMember(sp.getUuid()) != null) continue;
                        uuids.add(sp.getUuid());
                        names.add(sp.getGameProfile().getName());
                    }
                    ServerPlayNetworking.send(who,
                            new PartyInvitePayloads.EligibleInviteesResponse(uuids, names));
                });

        ServerPlayNetworking.registerGlobalReceiver(
                PartySettingsPayloads.SetIgnorePartyCollision.ID, (payload, ctx) -> {
                    var player = ctx.player();
                    var server = player.getServer();
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(player);
                            if (ftbData == null) return;

                            if (!ftbData.leaderUuid.equals(player.getUuid())) {
                                long now = System.currentTimeMillis();
                                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(ftbData.leaderUuid);
                                String leaderName = leader != null ? leader.getName().getString() : ftbData.leaderUuid.toString().substring(0, 8);
                                ServerPlayNetworking.send(player,
                                        new ChatNotice(ftbData.partyId, "Only " + leaderName + " can change party settings.", now));
                                return;
                            }
                            ftbData.settings.ignorePartyCollision = payload.ignore();
                            FTBTeamsIntegration.updatePartySettings(server, ftbData.partyId, ftbData.settings);

                            long now = System.currentTimeMillis();

                            for (UUID memberId : ftbData.members) {
                                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                                if (member != null) {
                                    ServerPlayNetworking.send(member,
                                            new PartySettingsPayloads.Sync(
                                                    ftbData.settings.allowHelpfulNonMembers,
                                                    ftbData.settings.ignorePartyCollision
                                            ));
                                    ServerPlayNetworking.send(member,
                                            new ChatNotice(ftbData.partyId,
                                                    "Ignore party collision: " + (ftbData.settings.ignorePartyCollision ? "ON" : "OFF"),
                                                    now));
                                }
                            }
                            return;
                        }
                    }
                    var state  = PartyPersistentState.get(server);
                    var p      = state.getPartyByMember(player.getUuid());
                    if (p == null) return;

                    long now = System.currentTimeMillis();

                    if (!p.leader.equals(player.getUuid())) {
                        String leaderName = state.nameOf(p.leader);
                        if (leaderName == null) {
                            var l = server.getPlayerManager().getPlayer(p.leader);
                            leaderName = (l != null) ? l.getName().getString() : p.leader.toString().substring(0, 8);
                        }
                        ServerPlayNetworking.send(player,
                                new ChatNotice(p.id, "Only " + leaderName + " can change party settings.", now));
                        return;
                    }

                    p.settings.ignorePartyCollision = payload.ignore();
                    state.markDirty();

                    for (var u : p.members) {
                        var sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) {
                            ServerPlayNetworking.send(sp,
                                    new PartySettingsPayloads.Sync(
                                            p.settings.allowHelpfulNonMembers,
                                            p.settings.ignorePartyCollision
                                    ));
                            ServerPlayNetworking.send(sp,
                                    new ChatNotice(p.id,
                                            "Ignore party collision: " + (p.settings.ignorePartyCollision ? "ON" : "OFF"),
                                            now));
                        }
                    }
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PartySettingsPayloads.SetAllowHelpfulNonMembers.ID, (payload, ctx) -> {
                    var player = ctx.player();
                    var server = player.getServer();
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(player);
                            if (ftbData == null) return;

                            if (!ftbData.leaderUuid.equals(player.getUuid())) {
                                long now = System.currentTimeMillis();
                                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(ftbData.leaderUuid);
                                String leaderName = leader != null ? leader.getName().getString() : ftbData.leaderUuid.toString().substring(0, 8);
                                ServerPlayNetworking.send(player,
                                        new ChatNotice(ftbData.partyId, "Only " + leaderName + " can change party settings.", now));
                                return;
                            }

                            ftbData.settings.allowHelpfulNonMembers = payload.allow();
                            FTBTeamsIntegration.updatePartySettings(server, ftbData.partyId, ftbData.settings);

                            long now = System.currentTimeMillis();

                            for (UUID memberId : ftbData.members) {
                                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                                if (member != null) {
                                    ServerPlayNetworking.send(member,
                                            new PartySettingsPayloads.Sync(
                                                    ftbData.settings.allowHelpfulNonMembers,
                                                    ftbData.settings.ignorePartyCollision
                                            ));
                                    ServerPlayNetworking.send(member,
                                            new ChatNotice(ftbData.partyId,
                                                    "Heal/Buff non-members: " + (ftbData.settings.allowHelpfulNonMembers ? "ON" : "OFF"),
                                                    now));
                                }
                            }
                            return;
                        }
                    }
                    var state  = net.pixeldreamstudios.rpgsystems.party.PartyPersistentState.get(server);
                    var p      = state.getPartyByMember(player.getUuid());
                    if (p == null) return;

                    long now = System.currentTimeMillis();

                    if (!p.leader.equals(player.getUuid())) {
                        String leaderName = state.nameOf(p.leader);
                        if (leaderName == null) {
                            var l = server.getPlayerManager().getPlayer(p.leader);
                            leaderName = (l != null) ? l.getName().getString() : p.leader.toString().substring(0, 8);
                        }
                        ServerPlayNetworking.send(player,
                                new ChatNotice(p.id, "Only " + leaderName + " can change party settings.", now));
                        return;
                    }

                    p.settings.allowHelpfulNonMembers = payload.allow();
                    state.markDirty();

                    for (var u : p.members) {
                        var sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) {
                            ServerPlayNetworking.send(sp,
                                    new PartySettingsPayloads.Sync(p.settings.allowHelpfulNonMembers, p.settings.ignorePartyCollision));

                            ServerPlayNetworking.send(sp,
                                    new ChatNotice(p.id,
                                            "Heal/Buff non-members: " + (p.settings.allowHelpfulNonMembers ? "ON" : "OFF"),
                                            now));
                        }
                    }
                }
        );

        AttackEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            var server = world.getServer();
            if (server == null) return ActionResult.PASS;
            var a = net.pixeldreamstudios.rpgsystems.party.PartyAllies.owningPlayerUuid(player);
            var b = net.pixeldreamstudios.rpgsystems.party.PartyAllies.owningPlayerUuid(target);
            if (a != null && b != null &&
                    net.pixeldreamstudios.rpgsystems.party.PartyAllies.sameParty(server, a, b)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(player);

                            if (ftbData != null) {
                                sendFTBTeamsRosterTo(server, player, ftbData);

                                for (UUID memberId : ftbData.members) {
                                    if (!memberId.equals(player.getUuid())) {
                                        ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                                        if (member != null) {
                                            ServerPlayNetworking.send(member,
                                                    new PartyHudPayloads.PartyMemberOnline(player.getUuid(), true));
                                        }
                                    }
                                }
                            }
                            return;
                        }
                    }
            PartyPersistentState state = PartyPersistentState.get(server);
            state.rememberName(handler.player.getUuid(), handler.player.getName().getString());
            Party p = state.getPartyByMember(handler.player.getUuid());
            if (p != null) {
                sendRosterTo(server, handler.player, p);
                for (UUID u : p.members) {
                    if (!u.equals(handler.player.getUuid())) {
                        ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) {
                            ServerPlayNetworking.send(sp,
                                    new PartyHudPayloads.PartyMemberOnline(handler.player.getUuid(), true));
                        }
                    }
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity self = handler.player;
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(self);
                            if (ftbData != null) {
                                for (UUID memberId : ftbData.members) {
                                    if (!memberId.equals(self.getUuid())) {
                                        ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                                        if (member != null) {
                                            ServerPlayNetworking.send(member,
                                                    new PartyHudPayloads.PartyMemberOnline(self.getUuid(), false));
                                        }
                                    }
                                }
                            }
                            return;
                        }
                    }
            PartyPersistentState state = PartyPersistentState.get(server);
            Party p = state.getPartyByMember(self.getUuid());
            if (p == null) return;
            state.rememberName(self.getUuid(), self.getName().getString());
            if (PERSIST_PARTIES_ON_DISCONNECT) {
                for (UUID u : p.members) {
                    if (!u.equals(self.getUuid())) {
                        ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) {
                            ServerPlayNetworking.send(sp,
                                    new PartyHudPayloads.PartyMemberOnline(self.getUuid(), false));
                        }
                    }
                }
                return;
            }

            boolean isLeader = p.leader.equals(self.getUuid());
            Set<UUID> members = new HashSet<>(p.members);
            boolean ok = state.leave(self.getUuid());
            if (!ok) return;
            if (isLeader) {
                for (UUID u : members) {
                    if (!u.equals(self.getUuid())) {
                        ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) {
                            ServerPlayNetworking.send(sp, new PartyRosterReset());
                            sp.sendMessage(Text.literal("Your party disbanded (leader left)."));
                        }
                    }
                }
            } else {
                UUID anyRemaining = members.stream().filter(u -> !u.equals(self.getUuid())).findFirst().orElse(null);
                Party remaining = state.getPartyByMember(anyRemaining);
                if (remaining != null) {
                    String name = self.getName().getString();
                    for (UUID u : remaining.members) {
                        ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) sp.sendMessage(Text.literal(name + " left the party"));
                    }
                    broadcastRoster(server, remaining);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((++tickCounter % 10) == 0) pushAllPartyVitals(server);
        });

        ServerPlayNetworking.registerGlobalReceiver(InviteRespond.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsJoinRequests.handleFTBTeamsInviteResponse(player, payload);
                            return;
                        }
                    }
            PartyPersistentState state = PartyPersistentState.get(player.getServer());
            Party party = state.getParty(payload.partyId());
            String partyName = party == null || party.name.isEmpty() ? String.valueOf(payload.partyId()) : party.name;

            if (payload.accept()) {
                Set<UUID> invitesBefore = new HashSet<>(state.invitesOf(player.getUuid()));
                Set<UUID> joinReqParties = state.partiesWithJoinRequestFrom(player.getUuid());

                boolean ok = state.accept(player.getUuid(), payload.partyId());
                if (ok) {
                    state.rememberName(player.getUuid(), player.getName().getString());
                    ServerPlayerEntity leader = player.getServer().getPlayerManager().getPlayer(party.leader);
                    if (leader != null) ServerPlayNetworking.send(leader, new InviteAccepted(player.getName().getString(), partyName));

                    ServerPlayNetworking.send(player, new PartyInvitePayloads.InviteJoinConfirmed(partyName));
                    for (UUID pid : invitesBefore) {
                        if (pid.equals(payload.partyId())) continue;
                        Party p2 = state.getParty(pid);
                        if (p2 != null) {
                            ServerPlayerEntity otherLeader = player.getServer().getPlayerManager().getPlayer(p2.leader);
                            if (otherLeader != null) {
                                String otherPartyName = (p2.name == null || p2.name.isEmpty()) ? p2.id.toString() : p2.name;
                                ServerPlayNetworking.send(otherLeader,
                                        new PartyInvitePayloads.InviteDeclined(player.getName().getString(), otherPartyName));
                            }
                        }
                    }
                    for (UUID pid : invitesBefore) {
                        ServerPlayNetworking.send(player, new InviteRemoved(pid));
                    }

                    for (UUID pid : joinReqParties) {
                        Party p2 = state.getParty(pid);
                        if (p2 != null) {
                            ServerPlayerEntity leader2 = player.getServer().getPlayerManager().getPlayer(p2.leader);
                            if (leader2 != null) {
                                ServerPlayNetworking.send(leader2, new PartyJoinRequestPayloads.JoinReqRemoved(pid, player.getUuid()));
                            }
                        }
                    }
                    player.sendMessage(Text.literal("Joined party " + partyName));
                    Party updated = state.getParty(payload.partyId());
                    if (updated != null) {
                        broadcastRoster(player.getServer(), updated);

                        long now = System.currentTimeMillis();
                        String joinerName = player.getName().getString();
                        var notice = new ChatNotice(
                                updated.id, joinerName + " joined the party.", now
                        );

                        for (java.util.UUID u : updated.members) {
                            var sp = player.getServer().getPlayerManager().getPlayer(u);
                            if (sp != null) {
                                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, notice);
                            }
                        }
                    }
                } else {
                    player.sendMessage(Text.literal("Accept failed"));
                    ServerPlayNetworking.send(player, new InviteRemoved(payload.partyId()));
                }
            } else {
                var set = new HashSet<>(state.invitesOf(player.getUuid()));
                if (set.contains(payload.partyId())) state.removeInvite(player.getUuid(), payload.partyId());
                if (party != null) {
                    ServerPlayerEntity leader = player.getServer().getPlayerManager().getPlayer(party.leader);
                    if (leader != null) ServerPlayNetworking.send(leader, new InviteDeclined(player.getName().getString(), partyName));
                }
                ServerPlayNetworking.send(player, new InviteRemoved(payload.partyId()));
                player.sendMessage(Text.literal("Declined invite to " + partyName));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(PartyJoinRequestPayloads.JoinReqRespond.ID, (payload, context) -> {
            ServerPlayerEntity leader = context.player();
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            if (!payload.accept()) {
                                FTBTeamsJoinRequests.removeRequest(payload.partyId(), payload.requesterUuid());
                                ServerPlayNetworking.send(leader, new JoinReqRemoved(payload.partyId(), payload.requesterUuid()));

                                ServerPlayerEntity requester = leader.getServer().getPlayerManager().getPlayer(payload.requesterUuid());
                                if (requester != null) {
                                    FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(leader);
                                    String partyName = ftbData != null ? ftbData.partyName : "the party";
                                    ServerPlayNetworking.send(requester,
                                            new PartyJoinRequestPayloads.JoinDeclined(leader.getName().getString(), partyName));
                                }
                            } else {
                                FTBTeamsJoinRequests.acceptRequest(leader.getServer(), payload.partyId(), payload.requesterUuid());
                                ServerPlayNetworking.send(leader, new JoinReqRemoved(payload.partyId(), payload.requesterUuid()));
                            }
                            return;
                        }
                    }
            PartyPersistentState state = PartyPersistentState.get(leader.getServer());
            Party p = state.getParty(payload.partyId());
            if (p == null || !p.leader.equals(leader.getUuid())) return;

            if (payload.accept()) {
                boolean ok = state.acceptJoin(leader.getUuid(), payload.requesterUuid());
                if (!ok) return;
                ServerPlayNetworking.send(leader, new JoinReqRemoved(p.id, payload.requesterUuid()));
                String partyName = (p.name == null || p.name.isEmpty()) ? p.id.toString() : p.name;
                String leaderName = leader.getName().getString();

                ServerPlayerEntity req = leader.getServer().getPlayerManager().getPlayer(payload.requesterUuid());
                if (req != null) {
                    ServerPlayNetworking.send(req, new PartyJoinRequestPayloads.JoinAccepted(leaderName, partyName));
                }
                Party updated = state.getParty(p.id);
                broadcastRoster(leader.getServer(), updated);

                long now = System.currentTimeMillis();

                String joinerName = "A player";
                {
                    req = leader.getServer().getPlayerManager().getPlayer(payload.requesterUuid());
                    if (req != null) joinerName = req.getName().getString();
                }

                var notice = new ChatNotice(
                        updated.id, joinerName + " joined the party.", now
                );

                for (UUID u : updated.members) {
                    var sp = leader.getServer().getPlayerManager().getPlayer(u);
                    if (sp != null) {
                        ServerPlayNetworking.send(sp, notice);
                    }
                }
            } else {
                boolean ok = state.declineJoin(leader.getUuid(), payload.requesterUuid());
                if (!ok) return;
                ServerPlayNetworking.send(leader, new JoinReqRemoved(p.id, payload.requesterUuid()));

                ServerPlayerEntity req = leader.getServer().getPlayerManager().getPlayer(payload.requesterUuid());
                if (req != null) {
                    String partyName = (p.name == null || p.name.isEmpty()) ? p.id.toString() : p.name;
                    String leaderName = leader.getName().getString();
                    ServerPlayNetworking.send(req, new PartyJoinRequestPayloads.JoinDeclined(leaderName, partyName));
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ChatSend.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();

            UUID partyId = null;
            String name = sender.getName().getString();
            long now = System.currentTimeMillis();
            String raw = payload.message() == null ? "" : payload.message().trim();
            Set<UUID> partyMembers = new HashSet<>();
                        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(sender);
                            if (ftbData == null) return;
                            if (!ftbData.partyId.equals(payload.partyId())) return;

                            partyId = ftbData.partyId;
                            partyMembers.addAll(ftbData.members);
                        } else {
                            PartyPersistentState state = PartyPersistentState.get(sender.getServer());
                            Party p = state.getPartyByMember(sender.getUuid());
                            if (p == null) return;
                            if (!p.id.equals(payload.partyId())) return;

                            partyId = p.id;
                            partyMembers.addAll(p.members);
                        }

            if (raw.startsWith("/p")) {
                String[] parts = raw.split("\\s+", 3);
                String sub = (parts.length >= 2) ? parts[1].toLowerCase(java.util.Locale.ROOT) : "help";

                switch (sub) {
                    case "help" -> {
                        String help = """
            Middle click member to pin members!
            Click on the crown to change party leaders!
            Check the gear icon for settings!
            Party Chat commands:
            /p help — show this list
            /p info — party name, leader, members
            /p sharepos — share your current coords
            /p promote <member> — transfer leadership (native parties only)
            /p pin <text> — set a pinned banner 
            /p unpin - unpins a pinned banner 
            """.trim();
                        sendNoticeTo(sender, partyId, help, now);
                    }
                    case "info" -> {
                        String info;
                        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(sender);
                            if (ftbData == null) return;

                            ServerPlayerEntity leader = sender.getServer().getPlayerManager().getPlayer(ftbData.leaderUuid);
                            String leaderName = leader != null ? leader.getName().getString() : shortId(ftbData.leaderUuid);
                            int online = 0;
                            for (UUID u : ftbData.members) {
                                if (sender.getServer().getPlayerManager().getPlayer(u) != null) online++;
                            }
                            info = "Party: " + ftbData.partyName + " | Leader: " + leaderName + " | Members: " + ftbData.members.size() + " (" + online + " online)";
                        } else {
                            PartyPersistentState state = PartyPersistentState.get(sender.getServer());
                            Party p = state.getPartyByMember(sender.getUuid());
                            if (p == null) return;

                            String leaderName = state.nameOf(p.leader);
                            if (leaderName == null) {
                                ServerPlayerEntity l = sender.getServer().getPlayerManager().getPlayer(p.leader);
                                leaderName = (l != null) ? l.getName().getString() : shortId(p.leader);
                            }
                            int online = 0;
                            for (UUID u : p.members) {
                                if (sender.getServer().getPlayerManager().getPlayer(u) != null) online++;
                            }
                            info = "Party: " + displayName(p) + " | Leader: " + leaderName + " | Members: " + p.members.size() + " (" + online + " online)";
                        }
                        sendNoticeTo(sender, partyId, info, now);
                    }
                    case "sharepos" -> {
                        var pos = sender.getBlockPos();
                        String dim = sender.getWorld().getRegistryKey().getValue().toString();
                        String txt = name + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " [" + dim + "]";
                        broadcastNoticeToMembers(sender.getServer(), partyId, partyMembers, txt, now);
                    }
                    case "promote" -> {
                        if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                            if (FTBTeamsIntegration.isEnabled()) {
                                sendNoticeTo(sender, partyId, "Leadership transfer not supported with FTB Teams. Use FTB Teams commands.", now);
                                return;
                            }
                        }
                        PartyPersistentState state = PartyPersistentState.get(sender.getServer());
                        Party p = state.getPartyByMember(sender.getUuid());
                        if (p == null) return;

                        if (!p.leader.equals(sender.getUuid())) {
                            sendNoticeTo(sender, partyId, "Only the party leader can promote.", now);
                            return;
                        }

                        if (parts.length < 3 || parts[2].isBlank()) {
                            sendNoticeTo(sender, partyId, "Usage: /p promote <member>", now);
                            return;
                        }

                        UUID targetUuid = resolveMemberFromInput(sender.getServer(), state, p, parts[2]);
                        if (targetUuid == null || targetUuid.equals(sender.getUuid())) {
                            sendNoticeTo(sender, partyId, "Couldn't resolve a unique member for \"" + parts[2] + "\".", now);
                            return;
                        }

                        p.leader = targetUuid;
                        state.markDirty();

                        String targetName = state.nameOf(targetUuid);
                        if (targetName == null) {
                            ServerPlayerEntity sp = sender.getServer().getPlayerManager().getPlayer(targetUuid);
                            targetName = (sp != null) ? sp.getName().getString() : shortId(targetUuid);
                        }

                        String txt = "Leadership transferred to " + targetName + ".";

                        broadcastNoticeToMembers(sender.getServer(), partyId, partyMembers, txt, now);

                        for (UUID u : p.members) {
                            ServerPlayerEntity sp = sender.getServer().getPlayerManager().getPlayer(u);
                            if (sp != null) sp.sendMessage(net.minecraft.text.Text.literal("[Party] " + txt));
                        }

                        PartyNet.broadcastRoster(sender.getServer(), p);
                    }
                    case "pin" -> {
                        UUID leaderUuid;
                        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(sender);
                            if (ftbData == null) return;
                            leaderUuid = ftbData.leaderUuid;
                        } else {
                            PartyPersistentState state = PartyPersistentState.get(sender.getServer());
                            Party p = state.getPartyByMember(sender.getUuid());
                            if (p == null) return;
                            leaderUuid = p.leader;
                        }

                        if (!leaderUuid.equals(sender.getUuid())) {
                            sendNoticeTo(sender, partyId, "Only the party leader can pin.", now);
                            return;
                        }

                        if (parts.length < 3 || parts[2].isBlank()) {
                            sendNoticeTo(sender, partyId, "Usage: /p pin <text>", now);
                            return;
                        }

                        var pin = new PartyChatPayloads.ChatPinSet(partyId, parts[2], now);
                        for (UUID u : partyMembers) {
                            ServerPlayerEntity sp = sender.getServer().getPlayerManager().getPlayer(u);
                            if (sp != null) ServerPlayNetworking.send(sp, pin);
                        }
                    }
                    case "unpin" -> {
                        UUID leaderUuid;
                        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(sender);
                            if (ftbData == null) return;
                            leaderUuid = ftbData.leaderUuid;
                        } else {
                            PartyPersistentState state = PartyPersistentState.get(sender.getServer());
                            Party p = state.getPartyByMember(sender.getUuid());
                            if (p == null) return;
                            leaderUuid = p.leader;
                        }

                        if (!leaderUuid.equals(sender.getUuid())) {
                            sendNoticeTo(sender, partyId, "Only the party leader can unpin.", now);
                            return;
                        }

                        var clear = new PartyChatPayloads.ChatPinClear(partyId);
                        for (UUID u : partyMembers) {
                            ServerPlayerEntity sp = sender.getServer().getPlayerManager().getPlayer(u);
                            if (sp != null) ServerPlayNetworking.send(sp, clear);
                        }
                        sendNoticeTo(sender, partyId, "Pinned banner cleared.", now);
                    }
                    default -> sendNoticeTo(sender, partyId, "Unknown subcommand. Try /p help", now);
                }
                return;
            }
                    if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
                        if (FTBTeamsIntegration.isEnabled()) {
                            FTBTeamsChatBridge.sendToFTBTeams(sender, payload.message());
                        }
                    }
            ChatMessage msg = new ChatMessage(partyId, sender.getUuid(), name, payload.message(), now);

            if (RPGSystemsConfig.get().party.logChatToConsole) {
                String partyName;
                if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
                    FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(sender);
                    partyName = ftbData != null ? ftbData.partyName : partyId.toString();
                } else {
                    PartyPersistentState state = PartyPersistentState.get(sender.getServer());
                    Party p = state.getPartyByMember(sender.getUuid());
                    partyName = (p != null && p.name != null && !p.name.isBlank()) ? p.name : partyId.toString();
                }
            }

            net.pixeldreamstudios.rpgsystems.api.PartyChatEvent.fire(
                    new net.pixeldreamstudios.rpgsystems.api.PartyChatEvent.Message(partyId, sender, payload.message())
            );
            if (!FabricLoader.getInstance().isModLoaded("ftbteams") || !FTBTeamsIntegration.isEnabled()) {
                for (UUID u : partyMembers) {
                    ServerPlayerEntity sp = sender.getServer().getPlayerManager().getPlayer(u);
                    if (sp != null) {
                        ServerPlayNetworking.send(sp, msg);
                    }
                }
            }
        });
    }

    private static void registerFTBTeamsReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(FTBTeamsJoinRequest.ID, (payload, ctx) -> {
            ServerPlayerEntity requester = ctx.player();
            ServerPlayerEntity target = requester.getServer().getPlayerManager().getPlayer(payload.targetPlayerUuid());

            if (target == null) return;

            FTBTeamsIntegration.FTBPartyData ftbData = getPartyDataForPlayer(target);
            if (ftbData == null) return;

            FTBTeamsJoinRequests.addRequest(ftbData.partyId, requester.getUuid());

            ServerPlayerEntity leader = requester.getServer().getPlayerManager().getPlayer(ftbData.leaderUuid);
            if (leader != null) {
                PartyNet.sendJoinReqAdded(leader, ftbData.partyId, requester.getUuid(), requester.getName().getString());
            }

            requester.sendMessage(Text.literal("Sent join request to " + ftbData.partyName));
        });
    }


    private static void pushAllPartyVitals(MinecraftServer server) {
        if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
            if (FTBTeamsIntegration.isEnabled()) {
                try {
                    FTBTeamsVitalsHandler.pushAllVitals(server);
                } catch (Throwable ignored) {
                }
                return;
            }
        }
        PartyPersistentState state = PartyPersistentState.get(server);
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            Party p = state.getPartyByMember(viewer.getUuid());
            if (p == null) continue;

            for (UUID memberId : p.members) {
                ServerPlayerEntity subject = server.getPlayerManager().getPlayer(memberId);
                if (subject == null) {
                    ServerPlayNetworking.send(viewer,
                            new net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects(
                                    memberId, java.util.List.of()
                            ));
                    continue;
                }

                pushVitalsForMember(viewer, subject, memberId);
            }
        }
    }
    private static UUID lastLoggedPartyId = null;

    public static void pushVitalsForMember(ServerPlayerEntity viewer, ServerPlayerEntity subject, UUID memberId) {
        float hp = subject.getHealth();
        float max = subject.getMaxHealth();
        int hunger = subject.getHungerManager().getFoodLevel();
        float absorption = subject.getAbsorptionAmount();

        float staminaNow = -1f, staminaMax = -1f;
        float manaNow    = -1f, manaMax    = -1f;
        float rpgNow     = -1f, rpgMax     = -1f;

        try {
            float[] st = net.pixeldreamstudios.rpgsystems.compat.TrbAttributesCompat.readStamina(subject);
            staminaNow = st[0]; staminaMax = st[1];
        } catch (Throwable ignored) { }
        try {
            float[] ma = net.pixeldreamstudios.rpgsystems.compat.TrbAttributesCompat.readMana(subject);
            manaNow = ma[0]; manaMax = ma[1];
        } catch (Throwable ignored) { }
        try {
            float[] rm = net.pixeldreamstudios.rpgsystems.compat.RpgManaCompat.readMana(subject);
            rpgNow = rm[0]; rpgMax = rm[1];
        } catch (Throwable ignored) { }

        ServerPlayNetworking.send(viewer,
                new PartyHudPayloads.PartyMemberVitals(
                        memberId, hp, max, hunger, absorption,
                        staminaNow, staminaMax,
                        manaNow, manaMax,
                        rpgNow, rpgMax
                ));

        int lvl = net.pixeldreamstudios.rpgsystems.compat.LevelZCompat.getLevel(subject);
        if (lvl < 0) {
            lvl = computeTotalSkillsLevel(subject);
        }

        if (lvl >= 0) {
            ServerPlayNetworking.send(viewer, new PartyHudPayloads.PartyMemberLevel(memberId, lvl));
        }

        java.util.List<String> effIds =
                net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsSync.snapshotEffectIds(subject);
        ServerPlayNetworking.send(viewer,
                new net.pixeldreamstudios.rpgsystems.network.party.
                        PartyStatusEffectsPayloads.MemberEffects(
                        memberId, effIds
                ));
    }

    public static void broadcastNotice(MinecraftServer server, Party p, String text, long when) {
        var pay = new PartyChatPayloads.ChatNotice(p.id, text, when);
        for (UUID u : p.members) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
            if (sp != null) ServerPlayNetworking.send(sp, pay);
        }
    }

    private static void broadcastNoticeToMembers(MinecraftServer server, UUID partyId, Set<UUID> members, String text, long when) {
        var pay = new PartyChatPayloads.ChatNotice(partyId, text, when);
        for (UUID u : members) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
            if (sp != null) ServerPlayNetworking.send(sp, pay);
        }
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static UUID resolveMemberFromInput(MinecraftServer server, PartyPersistentState state, Party p, String input) {
        if (input == null || input.isBlank()) return null;

        try { return UUID.fromString(input.trim()); } catch (IllegalArgumentException ignored) { }

        String qi = input.trim();
        String qiLower = qi.toLowerCase(java.util.Locale.ROOT);

        UUID match = null; int matches = 0;

        for (UUID u : p.members) {
            String name = state.nameOf(u);
            if (name == null) {
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                name = (sp != null) ? sp.getName().getString() : shortId(u);
            }
            String label1 = name;
            String label2 = name + " #" + shortId(u);

            if (label1.equalsIgnoreCase(qi) || label2.equalsIgnoreCase(qi)) {
                match = u; matches++; continue;
            }
            if (label1.toLowerCase(java.util.Locale.ROOT).contains(qiLower) ||
                    label2.toLowerCase(java.util.Locale.ROOT).contains(qiLower)) {
                match = u; matches++;
            }
            if (shortId(u).equalsIgnoreCase(qi)) { match = u; matches++; }
        }
        return (matches == 1) ? match : null;
    }

    private static String displayName(Party p) {
        return (p.name == null || p.name.isBlank()) ? p.id.toString() : p.name;
    }

    public static void sendNoticeTo(ServerPlayerEntity to, UUID partyId, String text, long when) {
        ServerPlayNetworking.send(to, new net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads.ChatNotice(partyId, text, when));
    }

    private static void sendNoticeTo(ServerPlayerEntity to, Party p, String text, long when) {
        ServerPlayNetworking.send(to, new net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads.ChatNotice(p.id, text, when));
    }

    public static void broadcastRoster(MinecraftServer server, Party party) {
        String partyName = (party.name == null || party.name.isEmpty()) ? party.id.toString() : party.name;
        for (UUID u : party.members) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
            if (sp != null) sendRosterTo(server, sp, partyName, party);
        }
    }

    public static void sendRosterTo(MinecraftServer server, ServerPlayerEntity recipient, Party party) {
        String partyName = (party.name == null || party.name.isEmpty()) ? party.id.toString() : party.name;
        sendRosterTo(server, recipient, partyName, party);
    }

    private static void sendRosterTo(MinecraftServer server, ServerPlayerEntity recipient, String partyName, Party party) {
        PartyPersistentState state = PartyPersistentState.get(server);
        ServerPlayNetworking.send(recipient, new PartyRosterClear(party.id, partyName, party.leader));

        for (UUID memberId : party.members) {
            String name = getName(server, state, memberId);
            ServerPlayNetworking.send(recipient, new PartyRosterAdd(party.id, memberId, name));

            ServerPlayerEntity subject = server.getPlayerManager().getPlayer(memberId);
            boolean isOnline = (subject != null);
            ServerPlayNetworking.send(recipient, new PartyHudPayloads.PartyMemberOnline(memberId, isOnline));
            ServerPlayNetworking.send(recipient,
                    new PartySettingsPayloads.Sync(
                            party.settings.allowHelpfulNonMembers,
                            party.settings.ignorePartyCollision
                    ));

            if (subject != null) {
                int lvl = net.pixeldreamstudios.rpgsystems.compat.LevelZCompat.getLevel(subject);
                if (lvl < 0) {
                    lvl = computeTotalSkillsLevel(subject);
                }

                if (lvl >= 0) {
                    ServerPlayNetworking.send(recipient, new PartyHudPayloads.PartyMemberLevel(memberId, lvl));
                }

                java.util.List<String> effIds =
                        net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsSync.snapshotEffectIds(subject);
                ServerPlayNetworking.send(recipient,
                        new net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects(
                                memberId, effIds
                        ));
            } else {
                ServerPlayNetworking.send(recipient,
                        new net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects(
                                memberId, java.util.List.of()
                        ));
            }
        }
    }

    public static void sendFTBTeamsRosterTo(MinecraftServer server, ServerPlayerEntity recipient, FTBTeamsIntegration.FTBPartyData ftbData) {
        ServerPlayNetworking.send(recipient, new PartyRosterClear(ftbData.partyId, ftbData.partyName, ftbData.leaderUuid));

        for (UUID memberId : ftbData.members) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
            String name = FTBTeamsEventListener.getFTBTeamMemberName(server, memberId, member);

            ServerPlayNetworking.send(recipient, new PartyRosterAdd(ftbData.partyId, memberId, name));

            boolean isOnline = (member != null);
            ServerPlayNetworking.send(recipient, new PartyHudPayloads.PartyMemberOnline(memberId, isOnline));
            ServerPlayNetworking.send(recipient,
                    new PartySettingsPayloads.Sync(
                            ftbData.settings.allowHelpfulNonMembers,
                            ftbData.settings.ignorePartyCollision
                    ));

            if (member != null) {
                int lvl = net.pixeldreamstudios.rpgsystems.compat.LevelZCompat.getLevel(member);
                if (lvl < 0) {
                    lvl = computeTotalSkillsLevel(member);
                }

                if (lvl >= 0) {
                    ServerPlayNetworking.send(recipient, new PartyHudPayloads.PartyMemberLevel(memberId, lvl));
                }

                java.util.List<String> effIds =
                        net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsSync.snapshotEffectIds(member);
                ServerPlayNetworking.send(recipient,
                        new net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects(
                                memberId, effIds
                        ));
            } else {
                ServerPlayNetworking.send(recipient,
                        new net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects(
                                memberId, java.util.List.of()
                        ));
            }
        }
    }

    public static void sendRosterWipeTo(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new PartyRosterReset());
    }

    private static String getName(MinecraftServer server, PartyPersistentState state, UUID u) {
        ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
        if (sp != null) {
            String n = sp.getName().getString();
            state.rememberName(u, n);
            return n;
        }
        String cached = state.nameOf(u);
        return cached != null ? cached : u.toString();
    }

    public static void sendJoinReqAdded(ServerPlayerEntity leader, UUID partyId, UUID requester, String requesterName) {
        ServerPlayNetworking.send(leader, new JoinReqAdded(partyId, requester, requesterName));
    }

    public static void sendJoinReqRemoved(ServerPlayerEntity leader, UUID partyId, UUID requester) {
        ServerPlayNetworking.send(leader, new JoinReqRemoved(partyId, requester));
    }

    private static boolean puffishLoaded() {
        return FabricLoader.getInstance().isModLoaded("puffish_skills");
    }

    private static int computeTotalSkillsLevel(ServerPlayerEntity player) {
        if (!puffishLoaded()) return -1;

        try {
            SkillsMod skillsMod = SkillsMod.getInstance();
            if (skillsMod == null) {
                return -1;
            }

            Collection<Identifier> cats = skillsMod.getUnlockedCategories(player);
            int total = 0;

            for (Identifier id : cats) {
                Optional<Integer> lvlOpt = skillsMod.getCurrentLevel(player, id);
                total += lvlOpt.orElse(0);
            }

            return total;
        } catch (Throwable t) {
            return -1;
        }
    }

}