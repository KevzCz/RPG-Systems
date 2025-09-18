package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.pixeldreamstudios.rpgsystems.accessor.LivingEntityRawDamageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLastRawDamageMixin implements LivingEntityRawDamageAccess {
    @Unique private UUID  rpgsystems$lastRawAttacker;
    @Unique private float rpgsystems$lastRawAmount;

    @Inject(method = "damage", at = @At("HEAD"))
    private void rpgsystems$captureRawDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        Entity origin = resolveOrigin(source);
        if (origin == null) {
            origin = self;
        }

        this.rpgsystems$lastRawAttacker = origin.getUuid();
        this.rpgsystems$lastRawAmount   = amount;
    }

    @Unique
    private static Entity resolveOrigin(DamageSource src) {
        if (src == null) return null;

        Entity origin = src.getAttacker();
        if (origin == null) origin = src.getSource();
        if (origin == null) return null;

        if (origin instanceof ProjectileEntity proj && proj.getOwner() != null) {
            return proj.getOwner();
        }
        if (origin instanceof AreaEffectCloudEntity cloud && cloud.getOwner() != null) {
            return cloud.getOwner();
        }
        if (origin instanceof LightningEntity lightning && lightning.getChanneler() != null) {
            return lightning.getChanneler();
        }

        return origin;
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
