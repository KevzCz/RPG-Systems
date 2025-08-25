package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.party.Party;
import net.pixeldreamstudios.rpgsystems.party.PartyPersistentState;
import net.pixeldreamstudios.rpgsystems.util.HealAttribution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealingMixin {
    @Unique private float rpgsystems$preHealHp;

    @Inject(method = "heal", at = @At("HEAD"))
    private void rpgsystems$capturePreHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        rpgsystems$preHealHp = self.getHealth();
    }

    @Inject(method = "heal", at = @At("TAIL"))
    private void rpgsystems$afterHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        World world = self.getWorld();
        if (world.isClient()) return;

        float post = self.getHealth();
        float healed = post - rpgsystems$preHealHp;
        if (healed <= 0.01f) return;

        // 1) Who healed?
        ServerPlayerEntity healer = HealAttribution.get();
        if (healer == null && self instanceof ServerPlayerEntity spSelf) healer = spSelf;

        // 2) Spell-ish detection
        boolean isSpell = HealAttribution.isSpell() || rpgsystems$inferSpellFromStack();

        // If no explicit healer but looks like a spell (splash/aoe), guess nearest player.
        if (healer == null && isSpell) {
            var nearest = world.getClosestPlayer((net.minecraft.entity.Entity)(Object)this, 12.0);
            if (nearest instanceof ServerPlayerEntity ssp) {
                healer = ssp;
            } else if (nearest != null && world instanceof net.minecraft.server.world.ServerWorld sw) {
                // Resolve to the server-side player instance using UUID (very safe on dedicated)
                healer = sw.getServer().getPlayerManager().getPlayer(nearest.getUuid());
            }
        }
        if (healer == null) return;

        // 3) Recipients: healer + party
        List<ServerPlayerEntity> viewers = new ArrayList<>();
        viewers.add(healer);

        PartyPersistentState state = PartyPersistentState.get(healer.getServer());
        Party party = state.getPartyByMember(healer.getUuid());
        if (party != null) {
            for (UUID u : party.members) {
                ServerPlayerEntity m = healer.getServer().getPlayerManager().getPlayer(u);
                if (m != null && m != healer) viewers.add(m);
            }
        }

        EnemyNet.sendHealingNumber(viewers, self, healed, isSpell);
    }

    @Unique
    private static boolean rpgsystems$inferSpellFromStack() {
        // Heuristic: catch vanilla & mod "spell-ish" frames (no compat needed)
        for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
            String n = el.getClassName().toLowerCase(Locale.ROOT);
            if (n.startsWith("net.minecraft.entity.effect.")    // vanilla effects
                    || (n.contains("status") && n.contains("effect"))
                    || n.contains("spell") || n.contains("magic") || n.contains("arcane")
                    || n.contains("wizard") || n.contains("sorcer")) {
                return true;
            }
        }
        return false;
    }
}
