package net.pixeldreamstudios.rpgsystems.party;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.PartyNet;
import net.pixeldreamstudios.rpgsystems.network.party.PartyHudPayloads;
import net.pixeldreamstudios.rpgsystems.network.party.PartySourcePayloads;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PartyAddonIntegration {

    private static boolean partyAddonLoaded = false;

    public static void init() {
        partyAddonLoaded = FabricLoader.getInstance().isModLoaded("partyaddon");
        if (partyAddonLoaded) {
            RPGSystems.LOGGER.info("[PartyAddon Integration] PartyAddon mod detected!");
        } else {
            RPGSystems.LOGGER.info("[PartyAddon Integration] PartyAddon mod not found.");
        }
    }

    public static boolean isEnabled() {
        boolean enabled = partyAddonLoaded && RPGSystemsConfig.get().party.usePartyAddon;
        if (partyAddonLoaded && !enabled) {
            RPGSystems.LOGGER.debug("[PartyAddon Integration] PartyAddon is loaded but integration is disabled in config.");
        }
        return enabled;
    }

    public static void onPartyAddonSync(ServerPlayerEntity player) {
        if (!isEnabled()) return;
        
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        RPGSystems.LOGGER.info("[PartyAddon Integration] onPartyAddonSync triggered for {}", player.getName().getString());
        
        try {
            PartyDataProvider.PartyInfo partyInfo = PartyDataProvider.getPartyForPlayer(player);
            PartyDataProvider.PartySource currentSource = PartyDataProvider.getEffectiveSourceForPlayer(player);
            List<PartyDataProvider.PartySource> availableSources = PartyDataProvider.getAvailableSourcesForPlayer(player);
            
            ServerPlayNetworking.send(player,
                    PartySourcePayloads.AvailableSources.create(availableSources, currentSource));
            
            ServerPlayNetworking.send(player,
                    new PartyHudPayloads.PartyRosterReset());
            
            if (partyInfo != null) {
                PartyNet.sendPartyInfoRosterTo(server, player, partyInfo);
            }
        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error during sync callback for {}", player.getName().getString(), e);
        }
    }

    @Nullable
    public static PartyAddonData getPartyDataForPlayer(ServerPlayerEntity player) {
        RPGSystems.LOGGER.info("[PartyAddon Integration] getPartyDataForPlayer called for {}", player.getName().getString());
        
        if (!isEnabled()) {
            RPGSystems.LOGGER.info("[PartyAddon Integration] isEnabled() returned false for player {}", player.getName().getString());
            return null;
        }

        try {
            RPGSystems.LOGGER.info("[PartyAddon Integration] Attempting to get GroupManager for {}", player.getName().getString());
            GroupManager groupManager = ((GroupManagerAccess) player).getGroupManager();
            RPGSystems.LOGGER.info("[PartyAddon Integration] Got GroupManager: {}", groupManager);
            
            UUID leaderId = groupManager.getGroupLeaderId();
            List<UUID> memberList = groupManager.getGroupPlayerIdList();
            RPGSystems.LOGGER.info("[PartyAddon Integration] leaderId: {}, memberList size: {}, memberList: {}", 
                    leaderId, memberList != null ? memberList.size() : "null", memberList);
            
            if (leaderId == null) {
                RPGSystems.LOGGER.info("[PartyAddon Integration] Player {} is not in a PartyAddon group (leaderId is null)", player.getName().getString());
                return null;
            }

            if (memberList == null || memberList.isEmpty()) {
                RPGSystems.LOGGER.info("[PartyAddon Integration] Player {} has leaderId but empty member list", player.getName().getString());
                return null;
            }

            UUID partyId = leaderId;
            String partyName = getPartyNameFromLeader(player.getServer(), leaderId);

            MinecraftServer server = player.getServer();
            if (server == null) {
                RPGSystems.LOGGER.warn("[PartyAddon Integration] Server is null for player {}", player.getName().getString());
                return null;
            }

            PartyPersistentState state = PartyPersistentState.get(server);
            PartySettings settings = state.getPartyAddonPartySettings(partyId);

            RPGSystems.LOGGER.info("[PartyAddon Integration] Found party for {}: id={}, name={}, leader={}, members={}", 
                    player.getName().getString(), partyId, partyName, leaderId, memberList.size());
            return new PartyAddonData(partyId, partyName, leaderId, new HashSet<>(memberList), settings);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error getting party data for player {}",
                    player.getName().getString(), e);
            return null;
        }
    }

    @Nullable
    public static PartyAddonData getPartyDataForPlayerId(MinecraftServer server, UUID playerId) {
        if (!isEnabled()) return null;
        if (server == null) return null;

        try {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                return null;
            }

            return getPartyDataForPlayer(player);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error getting party data for player ID {}",
                    playerId, e);
            return null;
        }
    }

    public static boolean isInSameParty(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        if (!isEnabled()) return false;

        try {
            GroupManager gm1 = ((GroupManagerAccess) player1).getGroupManager();
            GroupManager gm2 = ((GroupManagerAccess) player2).getGroupManager();

            UUID leader1 = gm1.getGroupLeaderId();
            UUID leader2 = gm2.getGroupLeaderId();

            if (leader1 == null || leader2 == null) {
                return false;
            }

            return leader1.equals(leader2);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error checking if players are in same party", e);
            return false;
        }
    }

    @Nullable
    public static UUID getPartyIdForPlayer(ServerPlayerEntity player) {
        if (!isEnabled()) return null;

        try {
            GroupManager groupManager = ((GroupManagerAccess) player).getGroupManager();
            UUID leaderId = groupManager.getGroupLeaderId();
            
            if (leaderId == null) {
                return null;
            }

            List<UUID> memberList = groupManager.getGroupPlayerIdList();
            if (memberList == null || memberList.isEmpty()) {
                return null;
            }

            return leaderId;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error getting party ID for player {}",
                    player.getName().getString(), e);
            return null;
        }
    }

    public static List<UUID> getPartyMembers(MinecraftServer server, UUID partyId) {
        if (!isEnabled()) return Collections.emptyList();

        try {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(partyId);
            if (leader == null) {
                return Collections.emptyList();
            }

            GroupManager groupManager = ((GroupManagerAccess) leader).getGroupManager();
            List<UUID> memberList = groupManager.getGroupPlayerIdList();
            
            if (memberList == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(memberList);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error getting party members for party {}",
                    partyId, e);
            return Collections.emptyList();
        }
    }

    public static boolean updatePartySettings(MinecraftServer server, UUID partyId, PartySettings settings) {
        if (!isEnabled()) return false;
        if (server == null || partyId == null || settings == null) return false;

        try {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(partyId);
            if (leader == null) {
                RPGSystems.LOGGER.warn("[PartyAddon Integration] Party leader {} not found", partyId);
                return false;
            }

            GroupManager groupManager = ((GroupManagerAccess) leader).getGroupManager();
            if (groupManager.getGroupLeaderId() == null || groupManager.getGroupPlayerIdList().isEmpty()) {
                RPGSystems.LOGGER.warn("[PartyAddon Integration] Party {} does not exist", partyId);
                return false;
            }

            PartyPersistentState state = PartyPersistentState.get(server);
            state.updatePartyAddonPartySettings(partyId, settings);

            RPGSystems.LOGGER.info("[PartyAddon Integration] Updated settings for party {}", partyId);
            return true;

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error updating party settings for {}",
                    partyId, e);
            return false;
        }
    }

    public static boolean updatePartySettingsForPlayer(ServerPlayerEntity player, PartySettings settings) {
        if (!isEnabled()) return false;
        if (player == null || settings == null) return false;

        UUID partyId = getPartyIdForPlayer(player);
        if (partyId == null) {
            RPGSystems.LOGGER.warn("[PartyAddon Integration] Player {} is not in a party",
                    player.getName().getString());
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        return updatePartySettings(server, partyId, settings);
    }

    @Nullable
    public static PartySettings getPartySettings(MinecraftServer server, UUID partyId) {
        if (!isEnabled()) return null;
        if (server == null || partyId == null) return null;

        try {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(partyId);
            if (leader == null) return null;

            GroupManager groupManager = ((GroupManagerAccess) leader).getGroupManager();
            if (groupManager.getGroupLeaderId() == null) return null;

            PartyPersistentState state = PartyPersistentState.get(server);
            return state.getPartyAddonPartySettings(partyId);

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error getting party settings for {}",
                    partyId, e);
            return null;
        }
    }

    public static boolean isPartyLeader(ServerPlayerEntity player) {
        if (!isEnabled()) return false;

        try {
            GroupManager groupManager = ((GroupManagerAccess) player).getGroupManager();
            return groupManager.isGroupLeader();

        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error checking if player is leader", e);
            return false;
        }
    }

    public static void cleanupPartySettings(MinecraftServer server, UUID partyId) {
        if (!isEnabled()) return;
        if (server == null || partyId == null) return;

        try {
            PartyPersistentState state = PartyPersistentState.get(server);
            state.removePartyAddonPartySettings(partyId);
            RPGSystems.LOGGER.info("[PartyAddon Integration] Cleaned up settings for disbanded party {}", partyId);
        } catch (Exception e) {
            RPGSystems.LOGGER.error("[PartyAddon Integration] Error cleaning up party settings for {}",
                    partyId, e);
        }
    }

    private static String getPartyNameFromLeader(MinecraftServer server, UUID leaderId) {
        if (server == null || leaderId == null) {
            return "Party";
        }
        
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderId);
        if (leader != null) {
            return leader.getName().getString() + "'s Party";
        }
        return "Party";
    }

    public static class PartyAddonData {
        public final UUID partyId;
        public final String partyName;
        public final UUID leaderUuid;
        public final Set<UUID> members;
        public final PartySettings settings;

        public PartyAddonData(UUID partyId, String partyName, UUID leaderUuid, Set<UUID> members, PartySettings settings) {
            this.partyId = partyId;
            this.partyName = partyName;
            this.leaderUuid = leaderUuid;
            this.members = new HashSet<>(members);
            this.settings = settings;
        }

        @Override
        public String toString() {
            return "PartyAddonData{" +
                    "partyId=" + partyId +
                    ", partyName='" + partyName + '\'' +
                    ", leaderUuid=" + leaderUuid +
                    ", memberCount=" + members.size() +
                    ", allowHelpfulNonMembers=" + settings.allowHelpfulNonMembers +
                    ", ignorePartyCollision=" + settings.ignorePartyCollision +
                    '}';
        }
    }
}
