package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.network.party.PartyHudPayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartySettingsPayloads;

import java.util.*;
@Environment(EnvType.CLIENT)

public final class ClientPartyHudData {

    public static final class Member {
        public final UUID uuid;
        public String name;
        public float health = -1f;
        public float maxHealth = 20f;
        public int hunger = -1;
        public int totalLevel = -1;
        public boolean online = false;
        public Member(UUID uuid, String name) { this.uuid = uuid; this.name = name; }
        public float hpPct() { return maxHealth <= 0 ? 0 : Math.max(0f, Math.min(1f, health / maxHealth)); }
        public float hungerPct() { return Math.max(0f, Math.min(1f, hunger / 20f)); }

    }

    public static UUID partyId = null;
    public static String partyName = "";
    public static UUID leaderUuid = null;
    public static final Set<UUID> memberUuids = new HashSet<>();

    public static boolean isInMyParty(UUID id) {
        if (id == null) return false;
        var mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null && id.equals(mc.player.getUuid())) return true;
        return MEMBERS.containsKey(id);
    }

    private static final Map<UUID, Member> MEMBERS = new LinkedHashMap<>();

    public static Collection<Member> members() { return MEMBERS.values(); }
    public static UUID selectedMemberUuid = null;

    public static void setSelectedMember(UUID uuid) {
        selectedMemberUuid = uuid;
    }
    public static final class PartySettingsClient {
        public boolean allowHelpfulNonMembers = false;
    }
    private static final PartySettingsClient SETTINGS = new PartySettingsClient();
    public static boolean allowHelpfulNonMembers() { return SETTINGS.allowHelpfulNonMembers; }
    public static Member getSelectedOrDefault() {

        if (selectedMemberUuid != null) {
            Member m = MEMBERS.get(selectedMemberUuid);
            if (m != null) return m;
        }

        if (leaderUuid != null) {
            Member m = MEMBERS.get(leaderUuid);
            if (m != null) return m;
        }

        return MEMBERS.values().stream().findFirst().orElse(null);
    }

    public static Member getByUuid(UUID id) {
        return id == null ? null : MEMBERS.get(id);
    }
    public static void initClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyRosterClear.ID, (payload, ctx) -> {
            partyId = payload.partyId();
            partyName = payload.partyName();
            leaderUuid = payload.leaderUuid();
            MEMBERS.clear();
        });
        ClientPlayNetworking.registerGlobalReceiver(
                PartyHudPayloads.PartyMemberOnline.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    Member m = MEMBERS.get(payload.memberUuid());
                    if (m != null) m.online = payload.online();
                })
        );
        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyRosterAdd.ID, (payload, ctx) -> {
            if (!Objects.equals(partyId, payload.partyId())) return;
            MEMBERS.put(payload.memberUuid(), new Member(payload.memberUuid(), payload.memberName()));
        });

        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyMemberVitals.ID, (payload, ctx) -> {
            Member m = MEMBERS.get(payload.memberUuid());
            if (m != null) {
                m.health = payload.health();
                m.maxHealth = payload.maxHealth();
                m.hunger = payload.hunger();
                m.online = true;
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(
                PartyHudPayloads.PartyMemberLevel.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        ClientPartyHudData.setMemberLevel(payload.memberUuid(), payload.totalLevel()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                PartySettingsPayloads.Sync.ID, (payload, ctx) ->
                        ctx.client().execute(() ->
                                SETTINGS.allowHelpfulNonMembers = payload.allowHelpfulNonMembers()));
        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyRosterReset.ID, (payload, ctx) -> clearAll());
    }

    public static void clearAll() {
        partyId = null;
        partyName = "";
        leaderUuid = null;
        selectedMemberUuid = null;
        SETTINGS.allowHelpfulNonMembers = false;
        MEMBERS.clear();
    }

    public static List<Member> membersSortedExcludingSelf() {
        var mc = MinecraftClient.getInstance();
        UUID self = mc != null && mc.player != null ? mc.player.getUuid() : null;
        return MEMBERS.values().stream()
                .filter(m -> m.online)
                .filter(m -> self == null || !m.uuid.equals(self))
                .sorted(Comparator.comparing(m -> m.name.toLowerCase(Locale.ROOT)))
                .toList();
    }
    public static void setMemberLevel(UUID uuid, int level) {
        Member m = MEMBERS.get(uuid);
        if (m != null) m.totalLevel = level;
    }
    public static boolean isMember(UUID uuid) {
        if (uuid == null) return false;
        return MEMBERS.containsKey(uuid);
    }

    public static boolean isSameParty(UUID a, UUID b) {
        if (partyId == null || a == null || b == null) return false;
        return MEMBERS.containsKey(a) && MEMBERS.containsKey(b);
    }
}
