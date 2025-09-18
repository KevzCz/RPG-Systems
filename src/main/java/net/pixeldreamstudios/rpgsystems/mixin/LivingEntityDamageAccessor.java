package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityDamageAccessor {
    @Invoker("modifyAppliedDamage")
    float rpgsystems$invokeModifyAppliedDamage(DamageSource source, float amount);
}
