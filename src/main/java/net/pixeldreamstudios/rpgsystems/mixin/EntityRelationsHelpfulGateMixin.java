package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import net.pixeldreamstudios.rpgsystems.party.PartyPersistentState;
import net.pixeldreamstudios.rpgsystems.party.PartySettings;
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
            if (target == caster) return; // Allow self-cast

            PartySettings casterSettings = null;

            if (FTBTeamsIntegration.isEnabled() && caster instanceof ServerPlayerEntity serverPlayer) {
                FTBTeamsIntegration.FTBPartyData ftbData = FTBTeamsIntegration.getPartyDataForPlayer(serverPlayer);
                if (ftbData != null) {
                    casterSettings = ftbData.settings;
                }
            } else {
                var state = PartyPersistentState.get(server);
                var party = state.getPartyByMember(caster.getUuid());
                if (party != null) {
                    casterSettings = party.settings;
                }
            }

            // If no party or allows helpful to non-members, allow the action
            if (casterSettings == null || casterSettings.allowHelpfulNonMembers) {
                return;
            }

            // Check if target is in same party
            boolean isSameParty = false;

            if (target instanceof PlayerEntity tp) {
                isSameParty = PartyAllies.sameParty(server, caster.getUuid(), tp.getUuid());
            } else {
                var ownerUuid = PartyAllies.owningPlayerUuid(target);
                if (ownerUuid != null) {
                    isSameParty = PartyAllies.sameParty(server, caster.getUuid(), ownerUuid);
                }
            }

            if (!isSameParty) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (intent == SpellTarget.Intent.HARMFUL) {
            if (target == caster) return;

            // Check if target is in same party
            boolean isSameParty = false;

            if (target instanceof PlayerEntity tp) {
                isSameParty = PartyAllies.sameParty(server, caster.getUuid(), tp.getUuid());
            } else {
                var ownerUuid = PartyAllies.owningPlayerUuid(target);
                if (ownerUuid != null) {
                    isSameParty = PartyAllies.sameParty(server, caster.getUuid(), ownerUuid);
                }
            }

            if (isSameParty) {
                cir.setReturnValue(false);
            }
        }
    }
}