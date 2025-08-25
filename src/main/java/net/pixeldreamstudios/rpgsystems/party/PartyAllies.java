package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class PartyAllies {
    private PartyAllies() {}

    public static boolean sameParty(MinecraftServer server, UUID a, UUID b) {
        if (a == null || b == null) return false;
        if (server == null) return false;
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
            if (u != null) return u;
        }

        return null;
    }

    public static UUID owningPlayerUuidFromAttacker(Entity attacker) { return owningPlayerUuid(attacker); }
    public static UUID owningPlayerUuidOfVictim(LivingEntity victim)   { return owningPlayerUuid(victim); }
}
