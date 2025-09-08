package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.accessor.LivingEntityRawDamageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLastRawDamageMixin implements LivingEntityRawDamageAccess {
    @Unique private UUID rpgsystems$lastRawAttacker;
    @Unique private float rpgsystems$lastRawAmount;

    @Inject(method = "damage", at = @At("HEAD"))
    private void rpgsystems$captureRawDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getAttacker();
        UUID attackerUuid = null;
        if (attacker instanceof ServerPlayerEntity p) {
            attackerUuid = p.getUuid();
        } else if (attacker instanceof ProjectileEntity proj) {
            Entity owner = proj.getOwner();
            if (owner instanceof ServerPlayerEntity p2) {
                attackerUuid = p2.getUuid();
            }
        }
        if (attackerUuid != null) {
            this.rpgsystems$lastRawAttacker = attackerUuid;
            this.rpgsystems$lastRawAmount = amount;
        }
    }

    @Override
    public UUID rpgsystems$getLastRawDamageAttacker() {
        return rpgsystems$lastRawAttacker;
    }

    @Override
    public float rpgsystems$getLastRawDamageAmount() {
        return rpgsystems$lastRawAmount;
    }
}
