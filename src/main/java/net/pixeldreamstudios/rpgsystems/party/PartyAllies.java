package net.pixeldreamstudios.rpgsystems.party;

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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PartyAllies {
    private PartyAllies() {}

    private static final int MAX_OWNER_DEPTH = 16;

    public static boolean sameParty(MinecraftServer server, UUID a, UUID b) {
        if (a == null || b == null) return false;
        if (server == null) return false;
        if (a.equals(b)) return true;

        // Check FTBTeams - if both players are in the same FTB party
        if (FTBTeamsLoader.isEnabled()) {
            PartyDataProvider.PartyInfo pa = FTBTeamsIntegration.getPartyInfoForPlayerId(server, a);
            PartyDataProvider.PartyInfo pb = FTBTeamsIntegration.getPartyInfoForPlayerId(server, b);
            if (pa != null && pb != null && pa.id.equals(pb.id)) {
                return true;
            }
        }

        // Check PartyAddon - if both players are in the same PartyAddon group
        if (PartyAddonLoader.isEnabled()) {
            PartyDataProvider.PartyInfo pa = PartyAddonIntegration.getPartyInfoForPlayerId(server, a);
            PartyDataProvider.PartyInfo pb = PartyAddonIntegration.getPartyInfoForPlayerId(server, b);
            if (pa != null && pb != null && pa.id.equals(pb.id)) {
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
        return owningPlayerUuid(e, new HashSet<>(), 0);
    }

    private static UUID owningPlayerUuid(Entity e, Set<Entity> visited, int depth) {
        if (e == null) return null;
        if (depth >= MAX_OWNER_DEPTH) return null;
        if (!visited.add(e)) return null;

        if (e instanceof PlayerEntity p) return p.getUuid();

        if (e instanceof ProjectileEntity proj) return owningPlayerUuid(proj.getOwner(), visited, depth + 1);
        if (e instanceof AreaEffectCloudEntity cloud) return owningPlayerUuid(cloud.getOwner(), visited, depth + 1);

        if (e instanceof LightningEntity lightning) {
            return owningPlayerUuid(lightning.getChanneler(), visited, depth + 1);
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
