package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads;

import java.util.*;
@Environment(EnvType.CLIENT)
public final class ClientPartyJoinRequests {
    public static final class Req {
        public final UUID partyId;
        public final UUID requesterUuid;
        public String requesterName;
        public Req(UUID partyId, UUID requesterUuid, String name) {
            this.partyId = partyId; this.requesterUuid = requesterUuid; this.requesterName = name;
        }
    }

    private static final LinkedHashMap<UUID, Req> BY_REQUESTER = new LinkedHashMap<>();

    public static Collection<Req> all() { return BY_REQUESTER.values(); }
    public static void clearAll() { BY_REQUESTER.clear(); }

    public static void initClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyJoinRequestPayloads.JoinReqAdded.ID, (payload, ctx) -> {
            BY_REQUESTER.put(payload.requesterUuid(), new Req(payload.partyId(), payload.requesterUuid(), payload.requesterName()));
        });
        ClientPlayNetworking.registerGlobalReceiver(PartyJoinRequestPayloads.JoinReqRemoved.ID, (payload, ctx) -> {
            BY_REQUESTER.remove(payload.requesterUuid());
        });
    }

    public static void sendResponse(UUID partyId, UUID requester, boolean accept) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new PartyJoinRequestPayloads.JoinReqRespond(partyId, requester, accept));
        }
    }

    public static List<Req> pending() {
        return new ArrayList<>(BY_REQUESTER.values());
    }
    public static boolean isEmpty() {
        return BY_REQUESTER.isEmpty();
    }
    public static int size() {
        return BY_REQUESTER.size();
    }
    public static void removeLocal(UUID requesterUuid) {
        if (requesterUuid != null) {
            BY_REQUESTER.remove(requesterUuid);
        }
    }
    public static void pruneByPartyMembers(Set<UUID> currentMemberIds) {
        if (currentMemberIds == null || currentMemberIds.isEmpty()) return;
        BY_REQUESTER.entrySet().removeIf(e ->
                e.getValue() != null && currentMemberIds.contains(e.getValue().requesterUuid));
    }

}
