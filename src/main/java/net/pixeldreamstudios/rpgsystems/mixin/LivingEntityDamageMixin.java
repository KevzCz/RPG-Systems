package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import net.pixeldreamstudios.rpgsystems.util.DamageColorUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Unique private float rpgsystems$preHp;

    @Inject(method = "damage", at = @At("HEAD"))
    private void rpgsystems$capturePreHp(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        rpgsystems$preHp = self.getHealth();
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void rpgsystems$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        LivingEntity self = (LivingEntity)(Object)this;
        World world = self.getWorld();
        if (world.isClient()) return;

        float post = self.getHealth();
        float taken = rpgsystems$preHp - post;
        if (taken <= 0.01f) return;

        int rgb = DamageColorUtil.colorOf(world, source, self);

        Entity attacker = source.getAttacker();
        boolean crit = false;


        boolean isPet = attacker instanceof Tameable;

        UUID owner = attacker != null ? PartyAllies.owningPlayerUuidFromAttacker(attacker) : null;
        UUID srcUuid = owner != null ? owner : (attacker != null ? attacker.getUuid() : new UUID(0L, 0L));

        EnemyNet.broadcastDamageNumber(self, taken, crit, isPet, rgb, srcUuid);
    }
}
