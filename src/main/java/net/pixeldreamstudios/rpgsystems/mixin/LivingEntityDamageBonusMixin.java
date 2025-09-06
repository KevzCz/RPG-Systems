package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.api.TitleApi;
import net.pixeldreamstudios.rpgsystems.title.Title;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityDamageBonusMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float rpgsystems$applyTitleDamageBonus(float amount, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) return amount;

        Title title = TitleApi.getActive(player).orElse(null);
        if (title == null) return amount;

        Identifier victimType = Registries.ENTITY_TYPE.getId(((Entity) (Object) this).getType());

        double add = 0.0;
        double mul = 0.0;

        for (Title.Bonus b : title.bonuses) {
            if (b.damageTarget != null
                    && b.damageTarget.isPresent()
                    && b.damageTarget.get().equals(victimType)) {
                if (b.damageOp == Title.DamageOp.ADDED) {
                    add += b.damageAmount;
                } else if (b.damageOp == Title.DamageOp.MULTIPLIED) {
                    mul += b.damageAmount;
                }
            }
        }

        if (add == 0.0 && mul == 0.0) return amount;

        double result = (amount + add) * (1.0 + mul);
        return (float) Math.max(0.0, result);
    }
}
