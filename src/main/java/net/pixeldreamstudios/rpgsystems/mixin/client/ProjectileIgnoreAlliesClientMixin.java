package net.pixeldreamstudios.rpgsystems.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ProjectileEntity.class)
public abstract class ProjectileIgnoreAlliesClientMixin {
    @Shadow public abstract Entity getOwner();

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void rpgsystems$clientIgnoreAllies(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!(target instanceof LivingEntity)) return;

        Entity owner = this.getOwner();
        if (owner == null) return;

        if (owner.getUuid() != null && target.getUuid() != null &&
                ClientPartyHudData.isSameParty(owner.getUuid(), target.getUuid())) {
            cir.setReturnValue(false);
        }
    }
}
