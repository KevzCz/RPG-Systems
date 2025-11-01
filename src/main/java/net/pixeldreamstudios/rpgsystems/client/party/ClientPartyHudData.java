package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
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
        public float absorption = 0f;
        public float staminaNow = -1f, staminaMax = -1f;
        public float manaNow    = -1f, manaMax    = -1f;
        public float rpgManaNow = -1f, rpgManaMax = -1f;
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

    /** Client-side mirror of party settings. */
    public static final class PartySettingsClient {
        /** Whether non-members are allowed to receive helpful effects from party members. */
        public boolean allowHelpfulNonMembers = false;
        /** Whether collisions between party members are ignored. Default ON. */
        public boolean ignorePartyCollision   = true;
    }

    private static final PartySettingsClient SETTINGS = new PartySettingsClient();
    public static boolean allowHelpfulNonMembers() { return SETTINGS.allowHelpfulNonMembers; }
    public static boolean ignorePartyCollision()   { return SETTINGS.ignorePartyCollision; }

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

    public static Member getByUuid(UUID id) { return id == null ? null : MEMBERS.get(id); }

    public static void initClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyRosterClear.ID, (payload, ctx) -> {
            RPGSystems.LOGGER.info("[Client Party] ===== RECEIVED PARTY ROSTER CLEAR =====");
            RPGSystems.LOGGER.info("[Client Party] Party ID: {}", payload.partyId());
            RPGSystems.LOGGER.info("[Client Party] Party Name: {}", payload.partyName());
            RPGSystems.LOGGER.info("[Client Party] Leader UUID: {}", payload.leaderUuid());

            partyId = payload.partyId();
            partyName = payload.partyName();
            leaderUuid = payload.leaderUuid();
            MEMBERS.clear();

            RPGSystems.LOGGER.info("[Client Party] Client party data updated! partyId is now: {}", partyId);
            RPGSystems.LOGGER.info("[Client Party] partyName is now: {}", partyName);
            RPGSystems.LOGGER.info("[Client Party] leaderUuid is now: {}", leaderUuid);
            RPGSystems.LOGGER.info("[Client Party] ===== ROSTER CLEAR COMPLETE =====");
        });

        ClientPlayNetworking.registerGlobalReceiver(
                PartyHudPayloads.PartyMemberOnline.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    Member m = MEMBERS.get(payload.memberUuid());
                    if (m != null) {
                        m.online = payload.online();
                        RPGSystems.LOGGER.debug("[Client Party] Member {} online status: {}",
                                payload.memberUuid(), payload.online());
                    } else {
                        RPGSystems.LOGGER.warn("[Client Party] Received online status for unknown member: {}",
                                payload.memberUuid());
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyRosterAdd.ID, (payload, ctx) -> {
            RPGSystems.LOGGER.info("[Client Party] ===== RECEIVED PARTY ROSTER ADD =====");
            RPGSystems.LOGGER.info("[Client Party] Current partyId: {}", partyId);
            RPGSystems.LOGGER.info("[Client Party] Payload partyId: {}", payload.partyId());
            RPGSystems.LOGGER.info("[Client Party] Member Name: {}", payload.memberName());
            RPGSystems.LOGGER.info("[Client Party] Member UUID: {}", payload.memberUuid());

            if (!Objects.equals(partyId, payload.partyId())) {
                RPGSystems.LOGGER.warn("[Client Party] *** PARTY ID MISMATCH - MEMBER NOT ADDED ***");
                RPGSystems.LOGGER.warn("[Client Party] Expected: {}, Got: {}", partyId, payload.partyId());
                return;
            }

            MEMBERS.put(payload.memberUuid(), new Member(payload.memberUuid(), payload.memberName()));

            RPGSystems.LOGGER.info("[Client Party] Member added successfully!");
            RPGSystems.LOGGER.info("[Client Party] Total members now: {}", MEMBERS.size());
            RPGSystems.LOGGER.info("[Client Party] ===== ROSTER ADD COMPLETE =====");
        });

        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyMemberVitals.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                Member m = MEMBERS.get(payload.memberUuid());
                if (m != null) {
                    m.health = payload.health();
                    m.maxHealth = payload.maxHealth();
                    m.hunger = payload.hunger();
                    m.absorption = payload.absorption();
                    m.staminaNow = payload.staminaNow();
                    m.staminaMax = payload.staminaMax();
                    m.manaNow    = payload.manaNow();
                    m.manaMax    = payload.manaMax();
                    m.rpgManaNow = payload.rpgManaNow();
                    m.rpgManaMax = payload.rpgManaMax();
                    m.online = true;

                    RPGSystems.LOGGER.debug("[Client Party] Updated vitals for member {}: HP {}/{}",
                            payload.memberUuid(), m.health, m.maxHealth);
                } else {
                    RPGSystems.LOGGER.warn("[Client Party] Received vitals for unknown member: {}",
                            payload.memberUuid());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(
                PartyHudPayloads.PartyMemberLevel.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    ClientPartyHudData.setMemberLevel(payload.memberUuid(), payload.totalLevel());
                    RPGSystems.LOGGER.debug("[Client Party] Updated level for member {}: {}",
                            payload.memberUuid(), payload.totalLevel());
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PartySettingsPayloads.Sync.ID, (payload, ctx) ->
                        ctx.client().execute(() -> {
                            RPGSystems.LOGGER.info("[Client Party] Syncing party settings:");
                            RPGSystems.LOGGER.info("[Client Party]   Allow Helpful Non-Members: {}", payload.allowHelpfulNonMembers());
                            RPGSystems.LOGGER.info("[Client Party]   Ignore Party Collision: {}", payload.ignorePartyCollision());

                            SETTINGS.allowHelpfulNonMembers = payload.allowHelpfulNonMembers();
                            SETTINGS.ignorePartyCollision   = payload.ignorePartyCollision();
                        })
        );

        ClientPlayNetworking.registerGlobalReceiver(PartyHudPayloads.PartyRosterReset.ID, (payload, ctx) -> {
            RPGSystems.LOGGER.info("[Client Party] ===== RECEIVED PARTY ROSTER RESET =====");
            clearAll();
            RPGSystems.LOGGER.info("[Client Party] All party data cleared!");
        });
    }

    public static void clearAll() {
        partyId = null;
        partyName = "";
        leaderUuid = null;
        selectedMemberUuid = null;
        SETTINGS.allowHelpfulNonMembers = false;
        SETTINGS.ignorePartyCollision   = true;
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