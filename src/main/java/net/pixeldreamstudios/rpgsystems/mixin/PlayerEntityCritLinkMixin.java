package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.pixeldreamstudios.rpgsystems.util.DamageCritLinks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityCritLinkMixin {
    @Unique private boolean rpgsystems$pendingVanillaCrit;

    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;resetLastAttackedTicks()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void rpgsystems$computeCritFlag(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        rpgsystems$pendingVanillaCrit = rpgsystems$isVanillaCrit(self, target);
    }

    @ModifyArg(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
                    ordinal = 0
            ),
            index = 0
    )
    private DamageSource rpgsystems$linkCritToDamageSource(DamageSource source) {
        if (rpgsystems$pendingVanillaCrit) {
            DamageCritLinks.link(source, DamageCritLinks.Kind.MELEE, null);
            rpgsystems$pendingVanillaCrit = false;
        }
        return source;
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void rpgsystems$clearFlag(Entity target, CallbackInfo ci) {
        rpgsystems$pendingVanillaCrit = false;
    }

    private static boolean rpgsystems$isVanillaCrit(PlayerEntity player, Entity target) {
        if (!(target instanceof LivingEntity)) return false;
        if (player.isSprinting()) return false;
        boolean strong = player.getAttackCooldownProgress(0.5F) > 0.9F;
        if (!strong) return false;
        return player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle();
    }
}
