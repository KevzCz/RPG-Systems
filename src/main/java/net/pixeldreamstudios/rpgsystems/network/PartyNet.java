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
import net.pixeldreamstudios.rpgsystems.network.party.PartyHudPayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartySettingsPayloads;
import net.pixeldreamstudios.rpgsystems.party.Party;
import net.pixeldreamstudios.rpgsystems.party.PartyPersistentState;
import net.pixeldreamstudios.rpgsystems.network.party.PartyHudPayloads.*;

import java.lang.reflect.Method;
import java.util.*;

import static net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads.*;
import static net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads.*;
import static net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads.*;

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
        PayloadTypeRegistry.playS2C().register(PartyInvitePayloads.InviteJoinConfirmed.ID,
                PartyInvitePayloads.InviteJoinConfirmed.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyInvitePayloads.PartyLeft.ID,
                PartyInvitePayloads.PartyLeft.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyInvitePayloads.PartyKicked.ID,
                PartyInvitePayloads.PartyKicked.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyRosterClear.ID, PartyRosterClear.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyRosterAdd.ID, PartyRosterAdd.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyMemberVitals.ID, PartyMemberVitals.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyRosterReset.ID, PartyRosterReset.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyMemberLevel.ID, PartyMemberLevel.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyMemberOnline.ID, PartyMemberOnline.CODEC);

        PayloadTypeRegistry.playS2C().register(JoinReqAdded.ID, JoinReqAdded.CODEC);
        PayloadTypeRegistry.playS2C().register(JoinReqRemoved.ID, JoinReqRemoved.CODEC);
        PayloadTypeRegistry.playC2S().register(JoinReqRespond.ID, JoinReqRespond.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyJoinRequestPayloads.JoinAccepted.ID,
                PartyJoinRequestPayloads.JoinAccepted.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyJoinRequestPayloads.JoinDeclined.ID,
                PartyJoinRequestPayloads.JoinDeclined.CODEC);

        PayloadTypeRegistry.playC2S().register(ChatSend.ID, ChatSend.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatMessage.ID, ChatMessage.CODEC);

        PayloadTypeRegistry.playC2S().register(
                PartySettingsPayloads.SetAllowHelpfulNonMembers.ID,
                PartySettingsPayloads.SetAllowHelpfulNonMembers.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                PartySettingsPayloads.Sync.ID,
                PartySettingsPayloads.Sync.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                PartyInvitePayloads.EligibleInviteesRequest.ID,
                PartyInvitePayloads.EligibleInviteesRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(
                PartyInvitePayloads.EligibleInviteesResponse.ID,
                PartyInvitePayloads.EligibleInviteesResponse.CODEC);


        ServerPlayNetworking.registerGlobalReceiver(
                PartyInvitePayloads.EligibleInviteesRequest.ID, (payload, ctx) -> {
                    ServerPlayerEntity who = ctx.player();
                    MinecraftServer server = who.getServer();
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
                PartySettingsPayloads.SetAllowHelpfulNonMembers.ID, (payload, ctx) -> {
                    var player = ctx.player();
                    var server = player.getServer();
                    var state  = net.pixeldreamstudios.rpgsystems.party.PartyPersistentState.get(server);
                    var p      = state.getPartyByMember(player.getUuid());
                    if (p == null) return;
                    if (!p.leader.equals(player.getUuid())) return;

                    p.settings.allowHelpfulNonMembers = payload.allow();
                    state.markDirty();

                    for (var u : p.members) {
                        var sp = server.getPlayerManager().getPlayer(u);
                        if (sp != null) {
                            ServerPlayNetworking.send(sp,
                                    new PartySettingsPayloads.Sync(p.settings.allowHelpfulNonMembers));
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
            PartyPersistentState state = PartyPersistentState.get(server);
            ServerPlayerEntity self = handler.player;
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
            if ((++tickCounter % 20) == 0) pushAllPartyVitals(server);
        });

        ServerPlayNetworking.registerGlobalReceiver(InviteRespond.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
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
                    if (updated != null) broadcastRoster(player.getServer(), updated);
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


        ServerPlayNetworking.registerGlobalReceiver(JoinReqRespond.ID, (payload, context) -> {
            ServerPlayerEntity leader = context.player();
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
                broadcastRoster(leader.getServer(), state.getParty(p.id));
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
            PartyPersistentState state = PartyPersistentState.get(sender.getServer());
            Party p = state.getPartyByMember(sender.getUuid());
            if (p == null) return;
            if (!p.id.equals(payload.partyId())) return;

            String name = sender.getName().getString();
            long now = System.currentTimeMillis();
            ChatMessage msg = new ChatMessage(p.id, sender.getUuid(), name, payload.message(), now);

            if (net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig.get().party.logChatToConsole) {
                String partyName = (p.name != null && !p.name.isBlank()) ? p.name : p.id.toString();
                net.pixeldreamstudios.rpgsystems.RPGSystems.LOGGER.info("{}: {} > {}", name, partyName, payload.message());
            }

            net.pixeldreamstudios.rpgsystems.api.PartyChatEvent.fire(
                    new net.pixeldreamstudios.rpgsystems.api.PartyChatEvent.Message(p.id, sender, payload.message())
            );

            for (java.util.UUID u : p.members) {
                ServerPlayerEntity sp = sender.getServer().getPlayerManager().getPlayer(u);
                if (sp != null) {
                    ServerPlayNetworking.send(sp, msg);
                }
            }
        });



    }

    private static void pushAllPartyVitals(MinecraftServer server) {
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

                float hp = subject.getHealth();
                float max = subject.getMaxHealth();
                int hunger = subject.getHungerManager().getFoodLevel();
                ServerPlayNetworking.send(viewer, new PartyHudPayloads.PartyMemberVitals(memberId, hp, max, hunger));

                int lvl = computeTotalSkillsLevel(subject);
                if (lvl >= 0) {
                    ServerPlayNetworking.send(viewer, new PartyHudPayloads.PartyMemberLevel(memberId, lvl));
                }

                java.util.List<String> effIds =
                        net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsSync.snapshotEffectIds(subject);
                ServerPlayNetworking.send(viewer,
                        new net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads.MemberEffects(
                                memberId, effIds
                        ));
            }
        }
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
                    new PartySettingsPayloads.Sync(party.settings.allowHelpfulNonMembers));
            if (subject != null) {
                int lvl = computeTotalSkillsLevel(subject);
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

    /** Returns -1 if puffish_skills is missing or any reflection fails. */
    private static int computeTotalSkillsLevel(ServerPlayerEntity player) {
        if (!puffishLoaded()) return -1;
        try {
            Class<?> skillsMod = Class.forName("net.puffish.skillsmod.SkillsMod");
            Method getInstance = skillsMod.getMethod("getInstance");
            Object instance = getInstance.invoke(null);

            Method getUnlockedCategories = skillsMod.getMethod("getUnlockedCategories", ServerPlayerEntity.class);
            @SuppressWarnings("unchecked")
            Collection<Identifier> cats = (Collection<Identifier>) getUnlockedCategories.invoke(instance, player);

            Method getCurrentLevel = skillsMod.getMethod("getCurrentLevel", ServerPlayerEntity.class, Identifier.class);

            int total = 0;
            for (Identifier id : cats) {
                @SuppressWarnings("unchecked")
                Optional<Integer> lvlOpt = (Optional<Integer>) getCurrentLevel.invoke(instance, player, id);
                total += lvlOpt.orElse(0);
            }
            return total;
        } catch (Throwable t) {
            return -1;
        }
    }

}
