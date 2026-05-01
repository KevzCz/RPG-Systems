package net.pixeldreamstudios.rpgsystems.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(EntityRelations.class)
public abstract class EntityRelationsHelpfulGateClientMixin {
    @Inject(method = "actionAllowed", at = @At("HEAD"), cancellable = true)
    private static void rpgsystems$clientGateHelpful(SpellTarget.FocusMode focusMode,
                                                     SpellTarget.Intent intent,
                                                     LivingEntity caster,
                                                     Entity target,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (caster == null || target == null) return;

        if (intent == SpellTarget.Intent.HELPFUL) {
            if (target == caster) return;
            if (ClientPartyHudData.partyId == null) return;
            if (ClientPartyHudData.allowHelpfulNonMembers()) return;

            if (target instanceof PlayerEntity tp) {
                boolean same = ClientPartyHudData.isSameParty(caster.getUuid(), tp.getUuid());
                if (!same) { cir.setReturnValue(false); }
                return;
            }

            UUID owner = PartyAllies.owningPlayerUuid(target);
            if (owner != null && !ClientPartyHudData.isSameParty(caster.getUuid(), owner)) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (intent == SpellTarget.Intent.HARMFUL) {
            if (target == caster) return;

            if (target instanceof PlayerEntity tp) {
                boolean same = ClientPartyHudData.isSameParty(caster.getUuid(), tp.getUuid());
                if (same) { cir.setReturnValue(false); }
                return;
            }

            UUID owner = PartyAllies.owningPlayerUuid(target);
            if (owner != null && ClientPartyHudData.isSameParty(caster.getUuid(), owner)) {
                cir.setReturnValue(false);
            }
        }
    }
}
