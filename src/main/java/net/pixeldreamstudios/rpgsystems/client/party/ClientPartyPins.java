package net.pixeldreamstudios.rpgsystems.client.party;

import net.minecraft.client.MinecraftClient;

import java.util.*;

public final class ClientPartyPins {

    private ClientPartyPins() {}
    private static final List<UUID> PIN_ORDER = new ArrayList<>();
    private static String lastWorldKey = null;
    private static String lastPartySignature = null;
    public static synchronized void pin(UUID uuid, int ignoredSlot, int ignoredMaxSlots) {
        if (uuid == null) return;
        if (!PIN_ORDER.contains(uuid)) {
            PIN_ORDER.add(uuid);
        }
    }

    public static synchronized void unpin(UUID uuid) {
        if (uuid == null) return;
        PIN_ORDER.remove(uuid);
    }

    public static synchronized Integer getOrder(UUID uuid) {
        if (uuid == null) return null;
        int idx = PIN_ORDER.indexOf(uuid);
        return (idx < 0) ? null : (idx + 1);
    }

    public static synchronized boolean isPinned(UUID uuid) {
        return getOrder(uuid) != null;
    }

    public static synchronized int nextOrder() {
        return PIN_ORDER.size() + 1;
    }

    public static synchronized void clearAll() {
        PIN_ORDER.clear();
        lastPartySignature = null;
    }
    public static synchronized void updateScopeAndPrune(List<ClientPartyHudData.Member> members) {

        String currentWorld = resolveWorldKey();
        if (!Objects.equals(currentWorld, lastWorldKey)) {
            lastWorldKey = currentWorld;
            clearAll();
        }

        String partySig = buildSignature(members);
        if (partySig == null || partySig.isEmpty()) {
            if (lastPartySignature != null) clearAll();
            lastPartySignature = null;
        } else {
            if (!Objects.equals(partySig, lastPartySignature)) {
                prunePinsToMembers(members);
                lastPartySignature = partySig;
            } else {
                prunePinsToMembers(members);
            }
        }
    }

    private static void prunePinsToMembers(List<ClientPartyHudData.Member> members) {
        Set<UUID> valid = new HashSet<>();
        for (ClientPartyHudData.Member m : members) {
            if (m != null && m.uuid != null) valid.add(m.uuid);
        }
        PIN_ORDER.removeIf(u -> !valid.contains(u));
    }

    private static String buildSignature(List<ClientPartyHudData.Member> members) {
        if (members == null || members.isEmpty()) return "";
        List<String> ids = new ArrayList<>();
        for (ClientPartyHudData.Member m : members) {
            if (m != null && m.uuid != null) ids.add(m.uuid.toString());
        }
        if (ids.isEmpty()) return "";
        Collections.sort(ids);
        return String.join("|", ids);
    }

    private static String resolveWorldKey() {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.world.getRegistryKey() == null || mc.world.getRegistryKey().getValue() == null)
            return "unknown";
        return mc.world.getRegistryKey().getValue().toString();
    }

    public static synchronized List<ClientPartyHudData.Member> orderedForHud(
            List<ClientPartyHudData.Member> rawMembers,
            UUID leaderUuid,
            int maxVisible
    ) {
        updateScopeAndPrune(rawMembers);

        Map<UUID, ClientPartyHudData.Member> byId = new HashMap<>();
        List<ClientPartyHudData.Member> online = new ArrayList<>();
        for (ClientPartyHudData.Member m : rawMembers) {
            if (m == null || m.uuid == null) continue;
            byId.put(m.uuid, m);
            if (m.online) online.add(m);
        }

        List<ClientPartyHudData.Member> result = new ArrayList<>();
        if (maxVisible == 0) return result;

        for (UUID u : PIN_ORDER) {
            ClientPartyHudData.Member m = byId.get(u);
            if (m != null && m.online) result.add(m);
        }

        List<ClientPartyHudData.Member> remainingOnline = new ArrayList<>();
        for (ClientPartyHudData.Member m : online) {
            if (!isPinned(m.uuid)) remainingOnline.add(m);
        }

        if (leaderUuid != null) {
            ClientPartyHudData.Member leader = null;
            for (ClientPartyHudData.Member m : remainingOnline) {
                if (leaderUuid.equals(m.uuid)) { leader = m; break; }
            }
            if (leader != null) {
                result.add(leader);
                remainingOnline.remove(leader);
            }
        }

        result.addAll(remainingOnline);

        if (maxVisible > 0 && result.size() > maxVisible) {
            result = new ArrayList<>(result.subList(0, maxVisible));
        }
        return result;
    }
}
