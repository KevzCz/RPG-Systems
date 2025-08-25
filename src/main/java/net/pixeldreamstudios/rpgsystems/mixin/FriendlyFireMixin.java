package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class FriendlyFireMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void rpgsystems$blockFriendly(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        var world = victim.getWorld();
        MinecraftServer server = world.getServer();
        if (server == null) return;

        Entity attacker = source.getAttacker();
        var atkUuid = PartyAllies.owningPlayerUuidFromAttacker(attacker);
        var vicUuid = PartyAllies.owningPlayerUuidOfVictim(victim);

        if (atkUuid != null && vicUuid != null && PartyAllies.sameParty(server, atkUuid, vicUuid)) {

            cir.setReturnValue(false);
        }
    }
}
