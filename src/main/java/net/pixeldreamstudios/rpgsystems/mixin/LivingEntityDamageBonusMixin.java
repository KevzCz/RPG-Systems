package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
import net.pixeldreamstudios.rpgsystems.title.TitlesPersistentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityDamageBonusMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float rpgsystems$applyTitleDamageBonus(float amount, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) return amount;

        Entity victim = (Entity)(Object)this;
        Identifier victimTypeId = Registries.ENTITY_TYPE.getId(victim.getType());
        RegistryEntry<?> victimEntry = Registries.ENTITY_TYPE.getEntry(victim.getType());

        double add = 0.0;
        double mul = 0.0;

        Title equipped = net.pixeldreamstudios.rpgsystems.api.TitleApi.getActive(player).orElse(null);
        if (equipped != null) {
            for (Title.Bonus b : equipped.bonuses) {
                if (b.damageOp == null) continue;

                if (b.damageTarget != null && b.damageTarget.isPresent()
                        && b.damageTarget.get().equals(victimTypeId)) {
                    if (b.damageOp == Title.DamageOp.ADDED) add += b.damageAmount;
                    else mul += b.damageAmount;
                }

                if (b.damageTag != null && b.damageTag.isPresent()) {
                    TagKey tag = TagKey.of(RegistryKeys.ENTITY_TYPE, b.damageTag.get());
                    if (victimEntry != null && victimEntry.isIn(tag)) {
                        if (b.damageOp == Title.DamageOp.ADDED) add += b.damageAmount;
                        else mul += b.damageAmount;
                    }
                }
            }
        }

        TitlesPersistentState state = TitlesPersistentState.get(player.getServer());
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        var disabledGroups = pt.permaDisabledGroups;

        for (String s : pt.unlocked) {
            try {
                Title t = TitleRegistry.get(Identifier.of(s));
                if (t == null) continue;

                for (Title.Bonus b : t.permaBonuses) {
                    if (b.damageOp == null) continue;

                    if (b.damageTarget != null && b.damageTarget.isPresent()) {
                        Identifier targetId = b.damageTarget.get();

                        String key = net.pixeldreamstudios.rpgsystems.title.PermaGroupKey
                                .dmgTarget(targetId, b.damageOp);
                        if (disabledGroups.contains(key)) continue;

                        if (targetId.equals(victimTypeId)) {
                            if (b.damageOp == Title.DamageOp.ADDED) add += b.damageAmount;
                            else mul += b.damageAmount;
                        }
                    }

                    if (b.damageTag != null && b.damageTag.isPresent()) {
                        Identifier tagId = b.damageTag.get();

                        String key = net.pixeldreamstudios.rpgsystems.title.PermaGroupKey
                                .dmgTag(tagId, b.damageOp);
                        if (disabledGroups.contains(key)) continue;

                        TagKey tag = TagKey.of(RegistryKeys.ENTITY_TYPE, tagId);
                        if (victimEntry != null && victimEntry.isIn(tag)) {
                            if (b.damageOp == Title.DamageOp.ADDED) add += b.damageAmount;
                            else mul += b.damageAmount;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }


        if (add == 0.0 && mul == 0.0) return amount;
        double result = (amount + add) * (1.0 + mul);
        return (float) Math.max(0.0, result);
    }
}
