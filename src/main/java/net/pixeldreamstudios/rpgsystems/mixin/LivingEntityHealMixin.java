package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {
    @Unique private float rpgsystems$preHealHp;

    @Inject(method = "heal", at = @At("HEAD"))
    private void rpgsystems$capturePreHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        rpgsystems$preHealHp = self.getHealth();
    }

    @Inject(method = "heal", at = @At("TAIL"))
    private void rpgsystems$afterHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;

        float post = self.getHealth();
        float healed = post - rpgsystems$preHealHp;
        if (healed <= 0.01f) return;

        UUID source = null;
        if (self instanceof ServerPlayerEntity sp) {
            source = sp.getUuid();
        }

        EnemyNet.broadcastHealingNumber(self, healed, false, source != null ? source : self.getUuid());

    }
}
