package net.pixeldreamstudios.rpgsystems.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(SpellTarget.class)
public abstract class SpellTargetHelpfulFilterMixin {

    @Inject(method = "findTargets", at = @At("RETURN"), cancellable = true)
    private static void rpgsystems$restrictHelpfulTargets(LivingEntity caster,
                                                          RegistryEntry<Spell> spellEntry,
                                                          SpellTarget.SearchResult prev,
                                                          boolean filterInvalidTargets,
                                                          CallbackInfoReturnable<SpellTarget.SearchResult> cir) {
        Spell spell = (spellEntry == null) ? null : spellEntry.value();
        if (spell == null || spell.impacts == null) return;

        boolean helpful = SpellHelper.deliveryIntent(spell).map(i -> i == SpellTarget.Intent.HELPFUL).orElse(false);
        if (!helpful) {
            for (Spell.Impact impact : spell.impacts) {
                if (SpellHelper.impactIntent(impact.action) == SpellTarget.Intent.HELPFUL) { helpful = true; break; }
            }
        }
        if (!helpful) return;

        if (ClientPartyHudData.partyId == null) return;
        if (ClientPartyHudData.allowHelpfulNonMembers()) return;

        SpellTarget.SearchResult result = cir.getReturnValue();
        if (result == null || result.entities() == null || result.entities().isEmpty()) return;

        UUID casterUuid;
        if (caster instanceof PlayerEntity) {
            casterUuid = caster.getUuid();
        } else {
            casterUuid = PartyAllies.owningPlayerUuid(caster);
            if (casterUuid == null) return;
        }

        List<Entity> filtered = new ArrayList<>(result.entities().size());

        for (Entity e : result.entities()) {
            if (e == null) continue;
            if (e == caster) { filtered.add(e); continue; }

            boolean keep;
            if (e instanceof PlayerEntity tp) {
                keep = ClientPartyHudData.isSameParty(casterUuid, tp.getUuid());
            } else {
                UUID owner = PartyAllies.owningPlayerUuid(e);
                keep = (owner == null) || ClientPartyHudData.isSameParty(casterUuid, owner);
            }
            if (keep) filtered.add(e);
        }

        if (filtered.size() != result.entities().size()) {
            cir.setReturnValue(new SpellTarget.SearchResult(List.copyOf(filtered), result.location()));
        }
    }
}
