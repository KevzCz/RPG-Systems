package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class FriendlyFireMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void rpgsystems$blockFriendly(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        MinecraftServer server = victim.getServer();
        if (server == null) return;

        UUID attackerUuid = PartyAllies.owningPlayerUuid(source.getAttacker());
        if (attackerUuid == null) {
            attackerUuid = PartyAllies.owningPlayerUuidFromDamageSource(source);
        }

        UUID victimUuid = PartyAllies.owningPlayerUuid(victim);

        if (victimUuid != null && PartyAllies.sameParty(server, attackerUuid, victimUuid)) {
            cir.setReturnValue(false);
        }
    }
}
