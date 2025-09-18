package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.spell_engine.internals.SpellHelper;
import net.pixeldreamstudios.rpgsystems.util.DamageCritLinks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = SpellHelper.class, remap = false)
public abstract class SpellHelperCritLinkMixin {
    @Unique private static final ThreadLocal<Boolean> RPG_CRIT_FLAG =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @ModifyArg(
            method = "performImpact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/spell_engine/internals/SpellTriggers;onSpellImpactSpecific(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/spell_engine/api/spell/Spell$Impact;ZLnet/spell_engine/api/spell/Spell$Trigger$Stage;)V",
                    remap = true
            ),
            index = 4
    )
    private static boolean rpgsystems$captureCriticalFlag(boolean critical) {
        RPG_CRIT_FLAG.set(critical);
        return critical;
    }

    @ModifyArg(
            method = "performImpact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
                    remap = true
            ),
            index = 0
    )
    private static DamageSource rpgsystems$linkMagicCritToSource(DamageSource source) {
        if (Boolean.TRUE.equals(RPG_CRIT_FLAG.get())) {
            DamageCritLinks.link(source, DamageCritLinks.Kind.MAGIC, null);
        }
        RPG_CRIT_FLAG.set(Boolean.FALSE);
        return source;
    }

    @Inject(method = "performImpact", at = @At("RETURN"))
    private static void rpgsystems$clearFlag(CallbackInfoReturnable<Boolean> cir) {
        RPG_CRIT_FLAG.set(Boolean.FALSE);
    }
}
