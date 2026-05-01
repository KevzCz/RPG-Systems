package net.pixeldreamstudios.rpgsystems.party;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class PartyAllies {
    private PartyAllies() {}

    public static boolean sameParty(MinecraftServer server, UUID a, UUID b) {
        if (a == null || b == null) return false;
        if (server == null) return false;
        if (a.equals(b)) return true;

        // Check FTBTeams - if both players are in the same FTB party
        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {
            var pa = FTBTeamsIntegration.getPartyDataForPlayerId(server, a);
            var pb = FTBTeamsIntegration.getPartyDataForPlayerId(server, b);
            if (pa != null && pb != null && pa.partyId.equals(pb.partyId)) {
                return true;
            }
        }

        // Check PartyAddon - if both players are in the same PartyAddon group
        if (FabricLoader.getInstance().isModLoaded("partyaddon") && PartyAddonIntegration.isEnabled()) {
            var pa = PartyAddonIntegration.getPartyDataForPlayerId(server, a);
            var pb = PartyAddonIntegration.getPartyDataForPlayerId(server, b);
            if (pa != null && pb != null && pa.partyId.equals(pb.partyId)) {
                return true;
            }
        }

        // Check native party system
        var state = PartyPersistentState.get(server);
        var pa = state.getPartyByMember(a);
        var pb = state.getPartyByMember(b);
        return pa != null && pb != null && pa.id.equals(pb.id);
    }

    public static UUID owningPlayerUuid(Entity e) {
        if (e == null) return null;

        if (e instanceof PlayerEntity p) return p.getUuid();

        if (e instanceof ProjectileEntity proj) return owningPlayerUuid(proj.getOwner());
        if (e instanceof AreaEffectCloudEntity cloud) return owningPlayerUuid(cloud.getOwner());

        if (e instanceof LightningEntity lightning) {
            return owningPlayerUuid(lightning.getChanneler());
        }

        if (e instanceof Ownable ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof PlayerEntity p) return p.getUuid();
        }

        if (e instanceof TameableEntity t) {
            UUID u = t.getOwnerUuid();
            if (u != null) return u;
        }
        if (e instanceof AbstractHorseEntity h) {
            UUID u = h.getOwnerUuid();
            return u;
        }

        return null;
    }

    public static UUID owningPlayerUuidFromDamageSource(DamageSource source) {
        if (source == null) return null;
        Entity src = source.getSource();
        if (src == null) return null;
        return owningPlayerUuid(src);
    }

    public static UUID owningPlayerUuidFromAttacker(Entity attacker) { return owningPlayerUuid(attacker); }
    public static UUID owningPlayerUuidOfVictim(LivingEntity victim) { return owningPlayerUuid(victim); }
}
