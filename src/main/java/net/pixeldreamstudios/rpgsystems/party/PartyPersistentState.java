package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

public final class PartyPersistentState extends PersistentState {
    public static final String KEY = "rpgsystems_parties";
    public static final PersistentState.Type<PartyPersistentState> TYPE = new PersistentState.Type<>(
            PartyPersistentState::new,
            PartyPersistentState::fromNbt,
            null
    );
    public static final long INVITE_TTL_MS = 60_000L;

    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> membership = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingInvites = new HashMap<>();
    private final Map<UUID, String> lastKnownNames = new HashMap<>();
    private final Map<UUID, Set<UUID>> joinRequests = new HashMap<>();
    private final Map<String, Long> inviteTimes = new HashMap<>();

    private final Map<UUID, PartySettings> ftbPartySettings = new HashMap<>();
    private final Map<UUID, PartySettings> partyAddonSettings = new HashMap<>();
    private final Map<UUID, PartyDataProvider.PartySource> playerSourcePreferences = new HashMap<>();

    public static PartyPersistentState get(MinecraftServer server) {
        PersistentStateManager mgr = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return mgr.getOrCreate(TYPE, KEY);
    }

    private static PartyPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PartyPersistentState s = new PartyPersistentState();

        NbtList partyList = nbt.getList("Parties", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < partyList.size(); i++) {
            Party p = Party.fromNbt(partyList.getCompound(i));
            s.parties.put(p.id, p);
            for (UUID u : p.members) s.membership.put(u, p.id);
        }

        NbtList invitesList = nbt.getList("Invites", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < invitesList.size(); i++) {
            NbtCompound tag = invitesList.getCompound(i);
            UUID target = tag.getUuid("Target");
            Set<UUID> set = new HashSet<>();
            NbtList ids = tag.getList("Ids", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < ids.size(); j++) set.add(ids.getCompound(j).getUuid("V"));
            if (!set.isEmpty()) s.pendingInvites.put(target, set);
        }

        NbtList names = nbt.getList("Names", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < names.size(); i++) {
            NbtCompound row = names.getCompound(i);
            s.lastKnownNames.put(row.getUuid("U"), row.getString("N"));
        }

        NbtList jr = nbt.getList("JoinRequests", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < jr.size(); i++) {
            NbtCompound row = jr.getCompound(i);
            UUID partyId = row.getUuid("Party");
            Set<UUID> reqs = new HashSet<>();
            NbtList rs = row.getList("Reqs", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < rs.size(); j++) reqs.add(rs.getCompound(j).getUuid("U"));
            if (!reqs.isEmpty()) s.joinRequests.put(partyId, reqs);
        }

        NbtList times = nbt.getList("InviteTimes", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < times.size(); i++) {
            NbtCompound row = times.getCompound(i);
            UUID target = row.getUuid("Target");
            UUID party = row.getUuid("Party");
            long when = row.getLong("Time");
            s.inviteTimes.put(inviteKey(target, party), when);
        }

        NbtList ftbSettings = nbt.getList("FTBPartySettings", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < ftbSettings.size(); i++) {
            NbtCompound row = ftbSettings.getCompound(i);
            UUID partyId = row.getUuid("PartyId");
            PartySettings settings = PartySettings.fromNbt(row.getCompound("Settings"));
            s.ftbPartySettings.put(partyId, settings);
        }

        NbtList partyAddonSettingsList = nbt.getList("PartyAddonSettings", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < partyAddonSettingsList.size(); i++) {
            NbtCompound row = partyAddonSettingsList.getCompound(i);
            UUID partyId = row.getUuid("PartyId");
            PartySettings settings = PartySettings.fromNbt(row.getCompound("Settings"));
            s.partyAddonSettings.put(partyId, settings);
        }

        NbtList sourcePrefs = nbt.getList("PlayerSourcePreferences", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < sourcePrefs.size(); i++) {
            NbtCompound row = sourcePrefs.getCompound(i);
            UUID playerId = row.getUuid("PlayerId");
            String sourceName = row.getString("Source");
            try {
                PartyDataProvider.PartySource source = PartyDataProvider.PartySource.valueOf(sourceName);
                s.playerSourcePreferences.put(playerId, source);
            } catch (IllegalArgumentException ignored) {}
        }

        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {

        NbtList partyList = new NbtList();
        for (Party p : parties.values()) partyList.add(p.toNbt());
        nbt.put("Parties", partyList);

        NbtList invitesList = new NbtList();
        for (Map.Entry<UUID, Set<UUID>> e : pendingInvites.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("Target", e.getKey());
            NbtList ids = new NbtList();
            for (UUID v : e.getValue()) {
                NbtCompound c = new NbtCompound();
                c.putUuid("V", v);
                ids.add(c);
            }
            row.put("Ids", ids);
            invitesList.add(row);
        }
        nbt.put("Invites", invitesList);

        NbtList names = new NbtList();
        for (Map.Entry<UUID, String> e : lastKnownNames.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("U", e.getKey());
            row.putString("N", e.getValue());
            names.add(row);
        }
        nbt.put("Names", names);

        NbtList jr = new NbtList();
        for (Map.Entry<UUID, Set<UUID>> e : joinRequests.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("Party", e.getKey());
            NbtList rs = new NbtList();
            for (UUID u : e.getValue()) {
                NbtCompound c = new NbtCompound();
                c.putUuid("U", u);
                rs.add(c);
            }
            row.put("Reqs", rs);
            jr.add(row);
        }
        nbt.put("JoinRequests", jr);

        NbtList times = new NbtList();
        for (Map.Entry<String, Long> e : inviteTimes.entrySet()) {
            String[] parts = e.getKey().split("#", 2);
            if (parts.length != 2) continue;
            try {
                UUID target = UUID.fromString(parts[0]);
                UUID party = UUID.fromString(parts[1]);
                NbtCompound row = new NbtCompound();
                row.putUuid("Target", target);
                row.putUuid("Party", party);
                row.putLong("Time", e.getValue());
                times.add(row);
            } catch (IllegalArgumentException ignored) {}
        }
        nbt.put("InviteTimes", times);

        NbtList ftbSettings = new NbtList();
        for (Map.Entry<UUID, PartySettings> e : ftbPartySettings.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("PartyId", e.getKey());
            row.put("Settings", e.getValue().toNbt());
            ftbSettings.add(row);
        }
        nbt.put("FTBPartySettings", ftbSettings);

        NbtList partyAddonSettingsList = new NbtList();
        for (Map.Entry<UUID, PartySettings> e : partyAddonSettings.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("PartyId", e.getKey());
            row.put("Settings", e.getValue().toNbt());
            partyAddonSettingsList.add(row);
        }
        nbt.put("PartyAddonSettings", partyAddonSettingsList);

        NbtList sourcePrefs = new NbtList();
        for (Map.Entry<UUID, PartyDataProvider.PartySource> e : playerSourcePreferences.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("PlayerId", e.getKey());
            row.putString("Source", e.getValue().name());
            sourcePrefs.add(row);
        }
        nbt.put("PlayerSourcePreferences", sourcePrefs);

        return nbt;
    }


    public boolean sameParty(UUID a, UUID b) {
        UUID pa = membership.get(a);
        return pa != null && pa.equals(membership.get(b));
    }

    public Party createParty(UUID leader, String name) {
        if (membership.containsKey(leader)) return getPartyByMember(leader);
        Party p = new Party(UUID.randomUUID(), leader, name == null ? "" : name);
        parties.put(p.id, p);
        membership.put(leader, p.id);
        markDirty();
        return p;
    }

    public Party getParty(UUID id) {
        return parties.get(id);
    }

    public Party getPartyByMember(UUID player) {
        UUID pid = membership.get(player);
        return pid == null ? null : parties.get(pid);
    }

    public boolean kick(UUID leader, UUID target) {
        Party p = getPartyByMember(leader);
        if (p == null || !p.leader.equals(leader)) return false;
        if (leader.equals(target)) return false;
        if (!p.members.remove(target)) return false;
        membership.remove(target);

        purgeRequesterFromJoinRequests(target);

        markDirty();
        return true;
    }

    public Collection<Party> allParties() {
        return Collections.unmodifiableCollection(parties.values());
    }

    public boolean invite(UUID inviter, UUID target) {
        Party p = getPartyByMember(inviter);
        if (p == null) return false;
        if (!p.isMember(inviter)) return false;
        if (inviter.equals(target)) return false;
        if (p.isMember(target)) return false;
        if (membership.containsKey(target)) return false;

        long now = System.currentTimeMillis();
        purgeExpiredInvitesFor(target, now);

        Set<UUID> set = pendingInvites.computeIfAbsent(target, k -> new HashSet<>());
        if (set.contains(p.id)) return false;
        set.add(p.id);
        inviteTimes.put(inviteKey(target, p.id), now);
        markDirty();
        return true;
    }

    public Set<UUID> targetsInvitedBy(UUID partyId) {
        Set<UUID> out = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> e : pendingInvites.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(partyId)) out.add(e.getKey());
        }
        return out;
    }

    public Set<UUID> partiesWithJoinRequestFrom(UUID requester) {
        Set<UUID> out = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> e : joinRequests.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(requester)) out.add(e.getKey());
        }
        return out;
    }

    public boolean acceptAny(UUID target) {
        purgeExpiredInvitesFor(target, System.currentTimeMillis());
        Set<UUID> invites = pendingInvites.getOrDefault(target, Collections.emptySet());
        UUID chosen = invites.stream().findFirst().orElse(null);
        if (chosen == null) return false;
        return accept(target, chosen);
    }

    public boolean accept(UUID target, UUID partyId) {
        if (membership.containsKey(target)) return false;
        Party p = parties.get(partyId);
        if (p == null) return false;

        Set<UUID> set = pendingInvites.get(target);
        if (set == null || !set.contains(partyId)) return false;

        Long when = inviteTimes.get(inviteKey(target, partyId));
        if (when != null && (System.currentTimeMillis() - when) > INVITE_TTL_MS) {
            set.remove(partyId);
            if (set.isEmpty()) pendingInvites.remove(target);
            inviteTimes.remove(inviteKey(target, partyId));
            markDirty();
            return false;
        }

        p.members.add(target);
        membership.put(target, p.id);
        pendingInvites.remove(target);
        inviteTimes.remove(inviteKey(target, partyId));

        purgeRequesterFromJoinRequests(target);

        markDirty();
        return true;
    }

    public boolean leave(UUID player) {
        Party p = getPartyByMember(player);
        if (p == null) return false;

        if (p.leader.equals(player)) {
            parties.remove(p.id);
            for (UUID u : new HashSet<>(p.members)) membership.remove(u);
            purgePartyFromInvites(p.id);
            purgePartyFromJoinRequests(p.id);
        } else {
            p.members.remove(player);
            membership.remove(player);
        }
        markDirty();
        return true;
    }

    public boolean renameParty(UUID requester, String newName) {
        Party p = getPartyByMember(requester);
        if (p == null || !p.leader.equals(requester)) return false;
        p.name = newName;
        markDirty();
        return true;
    }

    public boolean disband(UUID requester) {
        Party p = getPartyByMember(requester);
        if (p == null) return false;
        if (!p.leader.equals(requester)) return false;

        parties.remove(p.id);
        for (UUID u : new HashSet<>(p.members)) membership.remove(u);
        purgePartyFromInvites(p.id);
        purgePartyFromJoinRequests(p.id);

        markDirty();
        return true;
    }

    public boolean removeInvite(UUID target, UUID partyId) {
        Set<UUID> set = pendingInvites.get(target);
        if (set == null) return false;
        boolean removed = set.remove(partyId);
        if (set.isEmpty()) pendingInvites.remove(target);
        inviteTimes.remove(inviteKey(target, partyId));
        if (removed) markDirty();
        return removed;
    }

    public Set<UUID> invitesOf(UUID target) {
        purgeExpiredInvitesFor(target, System.currentTimeMillis());
        Set<UUID> s = pendingInvites.get(target);
        return s == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(s));
    }

    public void rememberName(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) return;
        String prev = lastKnownNames.put(uuid, name);
        if (!name.equals(prev)) markDirty();
    }

    public String nameOf(UUID uuid) {
        return lastKnownNames.get(uuid);
    }

    public boolean requestJoin(UUID requester, UUID partyId) {
        if (membership.containsKey(requester)) return false;
        Party p = parties.get(partyId);
        if (p == null) return false;
        if (p.isMember(requester)) return false;

        Set<UUID> set = joinRequests.computeIfAbsent(partyId, k -> new HashSet<>());
        if (!set.add(requester)) return false;

        markDirty();
        return true;
    }

    public Set<UUID> joinRequestsOf(UUID partyId) {
        Set<UUID> s = joinRequests.get(partyId);
        return s == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(s));
    }

    public boolean acceptJoin(UUID leader, UUID requester) {
        Party p = getPartyByMember(leader);
        if (p == null || !p.leader.equals(leader)) return false;
        Set<UUID> set = joinRequests.get(p.id);
        if (set == null || !set.contains(requester)) return false;
        if (membership.containsKey(requester)) {
            set.remove(requester);
            cleanJoinRequestsBucket(p.id);
            markDirty();
            return false;
        }

        set.remove(requester);
        cleanJoinRequestsBucket(p.id);

        p.members.add(requester);
        membership.put(requester, p.id);

        pendingInvites.remove(requester);
        purgeRequesterFromJoinRequests(requester);

        markDirty();
        return true;
    }

    public boolean declineJoin(UUID leader, UUID requester) {
        Party p = getPartyByMember(leader);
        if (p == null || !p.leader.equals(leader)) return false;
        Set<UUID> set = joinRequests.get(p.id);
        if (set == null || !set.remove(requester)) return false;
        cleanJoinRequestsBucket(p.id);
        markDirty();
        return true;
    }

    public PartySettings getFTBPartySettings(UUID ftbPartyId) {
        return ftbPartySettings.computeIfAbsent(ftbPartyId, id -> {
            PartySettings s = new PartySettings();
            s.allowHelpfulNonMembers = false;
            s.ignorePartyCollision = true;
            markDirty();
            return s;
        });
    }

    public void updateFTBPartySettings(UUID ftbPartyId, PartySettings settings) {
        ftbPartySettings.put(ftbPartyId, settings);
        markDirty();
    }

    public void removeFTBPartySettings(UUID ftbPartyId) {
        if (ftbPartySettings.remove(ftbPartyId) != null) {
            markDirty();
        }
    }
    public boolean hasFTBPartySettings(UUID ftbPartyId) {
        return ftbPartySettings.containsKey(ftbPartyId);
    }

    public PartySettings getPartyAddonPartySettings(UUID partyId) {
        return partyAddonSettings.computeIfAbsent(partyId, id -> {
            PartySettings s = new PartySettings();
            s.allowHelpfulNonMembers = false;
            s.ignorePartyCollision = true;
            markDirty();
            return s;
        });
    }

    public void updatePartyAddonPartySettings(UUID partyId, PartySettings settings) {
        partyAddonSettings.put(partyId, settings);
        markDirty();
    }

    public void removePartyAddonPartySettings(UUID partyId) {
        if (partyAddonSettings.remove(partyId) != null) {
            markDirty();
        }
    }

    public boolean hasPartyAddonPartySettings(UUID partyId) {
        return partyAddonSettings.containsKey(partyId);
    }

    public PartyDataProvider.PartySource getPlayerSourcePreference(UUID playerId) {
        return playerSourcePreferences.get(playerId);
    }

    public void setPlayerSourcePreference(UUID playerId, PartyDataProvider.PartySource source) {
        if (source == null) {
            playerSourcePreferences.remove(playerId);
        } else {
            playerSourcePreferences.put(playerId, source);
        }
        markDirty();
    }

    public void clearPlayerSourcePreference(UUID playerId) {
        if (playerSourcePreferences.remove(playerId) != null) {
            markDirty();
        }
    }


    private void purgePartyFromInvites(UUID partyId) {
        Iterator<Map.Entry<UUID, Set<UUID>>> it = pendingInvites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Set<UUID>> e = it.next();
            Set<UUID> set = e.getValue();
            set.remove(partyId);
            inviteTimes.remove(inviteKey(e.getKey(), partyId));
            if (set.isEmpty()) it.remove();
        }
    }

    private void purgePartyFromJoinRequests(UUID partyId) {
        joinRequests.remove(partyId);
    }

    private void purgeRequesterFromJoinRequests(UUID requester) {
        for (Set<UUID> set : joinRequests.values()) set.remove(requester);
        cleanJoinRequestsBuckets();
    }

    private void cleanJoinRequestsBucket(UUID partyId) {
        Set<UUID> set = joinRequests.get(partyId);
        if (set != null && set.isEmpty()) joinRequests.remove(partyId);
    }

    private void cleanJoinRequestsBuckets() {
        joinRequests.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
    }

    private void purgeExpiredInvitesFor(UUID target, long now) {
        Set<UUID> set = pendingInvites.get(target);
        if (set == null || set.isEmpty()) return;
        boolean changed = false;
        Iterator<UUID> it = set.iterator();
        while (it.hasNext()) {
            UUID partyId = it.next();
            Long when = inviteTimes.get(inviteKey(target, partyId));
            if (when != null && (now - when) > INVITE_TTL_MS) {
                it.remove();
                inviteTimes.remove(inviteKey(target, partyId));
                changed = true;
            }
        }
        if (changed) {
            if (set.isEmpty()) pendingInvites.remove(target);
            markDirty();
        }
    }

    private static String inviteKey(UUID target, UUID partyId) {
        return target + "#" + partyId;
    }
}