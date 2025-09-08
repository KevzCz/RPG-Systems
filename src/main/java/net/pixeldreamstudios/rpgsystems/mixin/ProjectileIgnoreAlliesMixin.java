package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileIgnoreAlliesMixin {
    @Shadow public abstract Entity getOwner();

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void rpgsystems$ignoreAllies(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!(target instanceof LivingEntity living)) return;

        Entity owner = this.getOwner();
        if (owner == null) return;

        MinecraftServer server = target.getServer();
        if (server == null) return;

        UUID shooterUuid = PartyAllies.owningPlayerUuidFromAttacker(owner);
        UUID targetUuid  = PartyAllies.owningPlayerUuidOfVictim(living);

        if (shooterUuid != null && targetUuid != null &&
                PartyAllies.sameParty(server, shooterUuid, targetUuid)) {
            cir.setReturnValue(false);
        }
    }
}
