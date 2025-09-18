package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.accessor.LivingEntityRawDamageAccess;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import net.pixeldreamstudios.rpgsystems.util.DamageColorUtil;
import net.pixeldreamstudios.rpgsystems.util.DamageCritLinks;
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

        Entity attacker = source.getAttacker();
        Entity origin = attacker != null ? attacker : source.getSource();
        boolean isPet = origin instanceof Tameable;

        UUID ownerUuid = PartyAllies.owningPlayerUuidFromAttacker(origin);
        UUID srcUuid = ownerUuid != null ? ownerUuid : (origin != null ? origin.getUuid() : new UUID(0L, 0L));

        float displayDamage = taken;

        boolean killedNow = post <= 0.0f;
        boolean rawMatchesAttacker = false;
        float rawAmount = 0.0f;

        if (self instanceof LivingEntityRawDamageAccess acc) {
            UUID rawAttacker = acc.rpgsystems$getLastRawDamageAttacker();
            if (rawAttacker != null) {
                UUID cmp = ownerUuid != null ? ownerUuid
                        : (origin != null ? origin.getUuid() : self.getUuid());
                if (cmp.equals(rawAttacker)) {
                    rawMatchesAttacker = true;
                    rawAmount = acc.rpgsystems$getLastRawDamageAmount();
                }
            }
        }
        if (killedNow && rawMatchesAttacker && rawAmount > 0.0f) {
            displayDamage = Math.max(displayDamage, rawAmount);
        }

        DamageCritLinks.Info link = DamageCritLinks.consume(source);
        boolean crit = link.isCrit();
        boolean magicCrit = link.isMagic();

        boolean sourceIsMagic = source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC);
        if (magicCrit && !sourceIsMagic) {
            magicCrit = true;
        }

        int rgb = DamageColorUtil.colorOf(world, source, self, crit, magicCrit, link.colorOverride);

        Identifier dmgId = Identifier.of("minecraft", "unknown");
        if (source != null) {
            RegistryEntry<DamageType> entry = source.getTypeRegistryEntry();
            dmgId = entry.getKey().map(RegistryKey::getValue).orElse(Identifier.of("minecraft", "unknown"));
        }

        EnemyNet.broadcastDamageNumber(self, displayDamage, crit, isPet, rgb, srcUuid, dmgId);
    }
}
