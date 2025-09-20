package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.pixeldreamstudios.rpgsystems.accessor.LivingEntityRawDamageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityLastRawDamageMixin implements LivingEntityRawDamageAccess {
    @Unique private UUID  rpgsystems$lastRawAttackerP;
    @Unique private float rpgsystems$lastRawAmountP;

    @Redirect(
            method = "applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"
            )
    )
    private float rpgsystems$capturePostMitigationPlayer(PlayerEntity self, DamageSource source, float amount) {
        float post = ((LivingEntityDamageAccessor) self)
                .rpgsystems$invokeModifyAppliedDamage(source, amount);

        Entity origin = resolveOrigin(source);
        if (origin == null) origin = self;

        this.rpgsystems$lastRawAttackerP = origin.getUuid();
        this.rpgsystems$lastRawAmountP   = post;
        return post;
    }

    @Override public UUID  rpgsystems$getLastRawDamageAttacker() { return rpgsystems$lastRawAttackerP; }
    @Override public float rpgsystems$getLastRawDamageAmount()   { return rpgsystems$lastRawAmountP; }

    @Unique
    private static Entity resolveOrigin(DamageSource src) {
        if (src == null) return null;
        Entity origin = src.getAttacker();
        if (origin == null) origin = src.getSource();
        if (origin instanceof ProjectileEntity p && p.getOwner() != null) return p.getOwner();
        if (origin instanceof AreaEffectCloudEntity c && c.getOwner() != null) return c.getOwner();
        if (origin instanceof LightningEntity l && l.getChanneler() != null) return l.getChanneler();
        return origin;
    }
}
