package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class NoFriendlyTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void rpgsystems$noFriendlyTarget(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;
        MobEntity self = (MobEntity)(Object)this;
        var server = self.getWorld().getServer();
        if (server == null) return;

        var a = PartyAllies.owningPlayerUuid(self);
        var b = PartyAllies.owningPlayerUuid(target);

        if (a != null && b != null && PartyAllies.sameParty(server, a, b)) {
            ci.cancel();
        }
    }
}
