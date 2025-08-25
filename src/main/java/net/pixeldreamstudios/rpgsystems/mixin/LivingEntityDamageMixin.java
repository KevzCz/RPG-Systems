// net/pixeldreamstudios/rpgsystems/mixin/LivingEntityDamageMixin.java
package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable; // yarn 1.21.x (interface). If your mappings use TameableEntity, adjust import & code.
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.party.Party;
import net.pixeldreamstudios.rpgsystems.party.PartyPersistentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
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
        if (!cir.getReturnValue()) return; // cancelled
        LivingEntity self = (LivingEntity)(Object)this;
        World world = self.getWorld();
        if (world.isClient()) return;

        float post = self.getHealth();
        float taken = rpgsystems$preHp - post;
        if (taken <= 0.01f) return;

        Entity attacker = source.getAttacker();
        if (attacker == null) return;

        // Case A: direct player attacker (unchanged)
        if (attacker instanceof ServerPlayerEntity sp) {
            List<ServerPlayerEntity> viewers = new ArrayList<>();
            viewers.add(sp);

            PartyPersistentState state = PartyPersistentState.get(sp.getServer());
            Party p = state.getPartyByMember(sp.getUuid());
            if (p != null) {
                for (UUID u : p.members) {
                    ServerPlayerEntity m = sp.getServer().getPlayerManager().getPlayer(u);
                    if (m != null && m != sp) viewers.add(m);
                }
            }

            boolean crit = sp.fallDistance > 0.0f && !sp.isOnGround() && !sp.isClimbing() && !sp.isTouchingWater();
            EnemyNet.sendDamageNumber(viewers, self, taken, crit, false);
            return;
        }

        // Case B: pet attacker – show to the owner + owner party
        // (Interface name may differ in your mappings; adjust if needed.)
        if (attacker instanceof Tameable tame) {
            Entity owner = tame.getOwner();
            if (owner instanceof ServerPlayerEntity ownerSp) {
                List<ServerPlayerEntity> viewers = new ArrayList<>();
                viewers.add(ownerSp);

                PartyPersistentState state = PartyPersistentState.get(ownerSp.getServer());
                Party p = state.getPartyByMember(ownerSp.getUuid());
                if (p != null) {
                    for (UUID u : p.members) {
                        ServerPlayerEntity m = ownerSp.getServer().getPlayerManager().getPlayer(u);
                        if (m != null && m != ownerSp) viewers.add(m);
                    }
                }

                // Simple crit heuristic for pets: reuse owner's fall crit if present; otherwise false
                boolean crit = false;
                EnemyNet.sendDamageNumber(viewers, self, taken, crit, true);
            }
        }
    }
}
