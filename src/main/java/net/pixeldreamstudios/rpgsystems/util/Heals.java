package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Helper to attribute and apply healing without extra mixins. */
public final class Heals {
    private Heals() {}

    public static void healAs(ServerPlayerEntity healer, LivingEntity target, float amount) {
        if (healer == null || target == null || amount <= 0f) return;
        try (HealAttribution.Scope ignored = HealAttribution.scope(healer)) {
            target.heal(amount);
        }
    }

    public static void healAsSpell(ServerPlayerEntity healer, LivingEntity target, float amount) {
        if (healer == null || target == null || amount <= 0f) return;
        try (HealAttribution.Scope ignored = HealAttribution.spell(healer)) {
            target.heal(amount);
        }
    }
}
