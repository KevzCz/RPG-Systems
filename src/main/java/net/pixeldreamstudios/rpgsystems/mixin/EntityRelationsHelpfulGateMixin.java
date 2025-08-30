package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRelations.class)
public abstract class EntityRelationsHelpfulGateMixin {
    @Inject(method = "actionAllowed", at = @At("HEAD"), cancellable = true)
    private static void rpgsystems$gateHelpfulToParty(
            SpellTarget.FocusMode focusMode,
            SpellTarget.Intent intent,
            LivingEntity caster,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (caster == null || target == null) return;

        var server = caster.getWorld().getServer();
        if (server == null) return;

        if (intent == SpellTarget.Intent.HELPFUL) {
            if (target == caster) return;

            var state = net.pixeldreamstudios.rpgsystems.party.PartyPersistentState.get(server);
            var p = state.getPartyByMember(caster.getUuid());

            if (p == null) return;
            if (p.settings.allowHelpfulNonMembers) return;

            if (target instanceof PlayerEntity tp) {
                boolean same = net.pixeldreamstudios.rpgsystems.party.PartyAllies
                        .sameParty(server, caster.getUuid(), tp.getUuid());
                if (!same) { cir.setReturnValue(false); return; }
                return;
            }

            var ownerUuid = net.pixeldreamstudios.rpgsystems.party.PartyAllies.owningPlayerUuid(target);
            if (ownerUuid != null) {
                boolean same = net.pixeldreamstudios.rpgsystems.party.PartyAllies
                        .sameParty(server, caster.getUuid(), ownerUuid);
                if (!same) { cir.setReturnValue(false); return; }
            }
            return;
        }

        if (intent == SpellTarget.Intent.HARMFUL) {
            if (target == caster) return;

            if (target instanceof PlayerEntity tp) {
                boolean same = net.pixeldreamstudios.rpgsystems.party.PartyAllies
                        .sameParty(server, caster.getUuid(), tp.getUuid());
                if (same) { cir.setReturnValue(false); return; }
                return;
            }

            var ownerUuid = net.pixeldreamstudios.rpgsystems.party.PartyAllies.owningPlayerUuid(target);
            if (ownerUuid != null) {
                boolean same = net.pixeldreamstudios.rpgsystems.party.PartyAllies
                        .sameParty(server, caster.getUuid(), ownerUuid);
                if (same) { cir.setReturnValue(false);
                }
            }
        }
    }
}
