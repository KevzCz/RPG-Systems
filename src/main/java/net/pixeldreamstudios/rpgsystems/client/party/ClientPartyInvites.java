package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads;

import java.util.*;
@Environment(EnvType.CLIENT)
public final class ClientPartyInvites {
    public enum NoticeKind {
        INVITE_SENT,
        INVITE_RECEIVED,
        INVITE_ACCEPT,
        INVITE_DECLINE,
        INVITE_EXPIRED,
        JOIN_VIA_INVITE,
        PARTY_LEFT,
        PARTY_KICK,
        PARTY_JOIN_REQ_ACC,
        PARTY_JOIN_REQ_DEC
    }

    public static final class Invite {
        public final UUID partyId;
        public final UUID leaderUuid;
        public final String leaderName;
        public final String partyName;
        public final long createdAt = System.currentTimeMillis();
        public Invite(UUID partyId, UUID leaderUuid, String leaderName, String partyName) {
            this.partyId = partyId; this.leaderUuid = leaderUuid; this.leaderName = leaderName; this.partyName = partyName;
        }
    }

    public static final class Sent {
        public final UUID partyId;
        public final String targetName;
        public final String partyName;
        public final long createdAt = System.currentTimeMillis();
        public Sent(UUID partyId, String targetName, String partyName) {
            this.partyId = partyId; this.targetName = targetName; this.partyName = partyName;
        }
    }

    public static final class Notice {
        public final String text;
        public final long createdAt = System.currentTimeMillis();
        public final long ttlMs;
        public final NoticeKind kind;
        public Notice(String text, long ttlMs, NoticeKind kind) {
            this.text = text; this.ttlMs = ttlMs; this.kind = kind;
        }
        public boolean expired() { return System.currentTimeMillis() - createdAt > ttlMs; }
    }

    private static final long INVITE_TTL_MS = 60_000L;

    private static final Map<UUID, Invite> INBOUND = new LinkedHashMap<>();
    private static final Deque<Notice> NOTICES = new ArrayDeque<>();
    private static final List<Sent> OUTBOUND = new ArrayList<>();

    public static Collection<Invite> invites() { pruneExpired(); return INBOUND.values(); }
    public static Invite firstOrNull() { pruneExpired(); return INBOUND.values().stream().findFirst().orElse(null); }

    public static Deque<Notice> notices() { pruneExpired(); return NOTICES; }
    public static void pushNotice(String s) { pushNotice(s, 4000L, NoticeKind.INVITE_SENT); }
    public static void pushNotice(String s, NoticeKind kind) { pushNotice(s, 4000L, kind); }
    public static void pushNotice(String s, long ttlMs, NoticeKind kind) {
        NOTICES.addLast(new Notice(s, ttlMs, kind));
        while (NOTICES.size() > 5) NOTICES.removeFirst();
    }

    public static List<Sent> sent() { pruneExpired(); return new ArrayList<>(OUTBOUND); }
    public static void removeSentByTarget(String name) {
        if (name == null) return;
        OUTBOUND.removeIf(s -> s.targetName.equalsIgnoreCase(name));
    }
    public static void clearAll() {
        INBOUND.clear(); OUTBOUND.clear(); NOTICES.clear(); ELIGIBLE_NAMES.clear();

    }
    private static final Set<String> ELIGIBLE_NAMES = new HashSet<>();

    public static Set<String> eligibleNames() { return new HashSet<>(ELIGIBLE_NAMES); }
    private static void pruneExpired() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Invite>> ii = INBOUND.entrySet().iterator();
        while (ii.hasNext()) {
            var e = ii.next();
            if (now - e.getValue().createdAt > INVITE_TTL_MS) {
                pushNotice("Invite from " + e.getValue().leaderName + " expired.", NoticeKind.INVITE_EXPIRED);
                ii.remove();
            }
        }

        Iterator<Sent> so = OUTBOUND.iterator();
        while (so.hasNext()) {
            var s = so.next();
            if (now - s.createdAt > INVITE_TTL_MS) {
                pushNotice("Invite to " + s.targetName + " expired.", NoticeKind.INVITE_EXPIRED);
                so.remove();
            }
        }
    }

    public static void initClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyInvitePayloads.InviteAdded.ID, (payload, context) -> {
            INBOUND.put(payload.partyId(), new Invite(payload.partyId(), payload.leaderUuid(), payload.leaderName(), payload.partyName()));
            pushNotice(payload.leaderName() + " invited you to " + payload.partyName(), NoticeKind.INVITE_RECEIVED);
        });
        ClientPlayNetworking.registerGlobalReceiver(
                PartyInvitePayloads.InviteJoinConfirmed.ID, (payload, ctx) ->
                        pushNotice("Joined " + payload.partyName(), NoticeKind.JOIN_VIA_INVITE)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                PartyInvitePayloads.PartyLeft.ID, (payload, ctx) ->
                        pushNotice("Left " + payload.partyName(), NoticeKind.PARTY_LEFT)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                PartyInvitePayloads.PartyKicked.ID, (payload, ctx) ->
                        pushNotice("Kicked from " + payload.partyName(), NoticeKind.PARTY_KICK)
        );
        ClientPlayNetworking.registerGlobalReceiver(PartyInvitePayloads.InviteRemoved.ID, (payload, context) -> {
            INBOUND.remove(payload.partyId());
        });
        ClientPlayNetworking.registerGlobalReceiver(PartyInvitePayloads.InviteSent.ID, (payload, context) -> {
            OUTBOUND.add(new Sent(/*party*/null, payload.targetName(), payload.partyName()));
        });
        ClientPlayNetworking.registerGlobalReceiver(PartyInvitePayloads.InviteAccepted.ID, (payload, context) -> {
            removeSentByTarget(payload.whoName());
            pushNotice(payload.whoName() + " has joined " + payload.partyName(), NoticeKind.INVITE_ACCEPT);
        });
        ClientPlayNetworking.registerGlobalReceiver(PartyInvitePayloads.InviteDeclined.ID, (payload, context) -> {
            removeSentByTarget(payload.whoName());
            pushNotice(payload.whoName() + " declined " + payload.partyName(), NoticeKind.INVITE_DECLINE);
        });
        ClientPlayNetworking.registerGlobalReceiver(
                PartyJoinRequestPayloads.JoinAccepted.ID, (payload, ctx) ->
                        pushNotice("Join request accepted by " + payload.leaderName()
                                + " (" + payload.partyName() + ")", NoticeKind.PARTY_JOIN_REQ_ACC)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                PartyJoinRequestPayloads.JoinDeclined.ID, (payload, ctx) ->
                        pushNotice("Join request declined by " + payload.leaderName()
                                + " (" + payload.partyName() + ")", NoticeKind.PARTY_JOIN_REQ_DEC)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                PartyInvitePayloads.EligibleInviteesResponse.ID, (payload, ctx) -> {
                    ELIGIBLE_NAMES.clear();
                    for (String n : payload.names()) if (n != null && !n.isBlank())
                        ELIGIBLE_NAMES.add(n);
                });
    }

    public static void sendResponse(UUID partyId, boolean accept) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new PartyInvitePayloads.InviteRespond(partyId, accept));
        }
    }

    public static long inviteTtlMs() { return INVITE_TTL_MS; }
}
