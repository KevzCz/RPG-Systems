package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DamageCritPending {
    private static final class Pending {
        final UUID attackerUuid;
        final Integer colorOverride;
        final long expiresAtTick;
        Pending(UUID attackerUuid, Integer colorOverride, long expiresAtTick) {
            this.attackerUuid = attackerUuid;
            this.colorOverride = colorOverride;
            this.expiresAtTick = expiresAtTick;
        }
    }

    private static final Map<LivingEntity, Pending> PENDING = new WeakHashMap<>();
    private static final int TTL_TICKS = 2;

    private DamageCritPending() {}

    public static void addMagic(LivingEntity target, Entity attacker, Integer colorOverride, long nowTick) {
        if (target == null || attacker == null) return;
        PENDING.put(target, new Pending(attacker.getUuid(), colorOverride, nowTick + TTL_TICKS));
    }

    public static DamageCritLinks.Info consumeIfMatches(DamageSource source, LivingEntity target, long nowTick) {
        if (source == null || target == null) return DamageCritLinks.Info.none();
        Pending p = PENDING.get(target);
        if (p == null) return DamageCritLinks.Info.none();
        if (nowTick > p.expiresAtTick) {
            PENDING.remove(target);
            return DamageCritLinks.Info.none();
        }
        Entity atk = source.getAttacker();
        if (atk == null || !atk.getUuid().equals(p.attackerUuid)) {
            return DamageCritLinks.Info.none();
        }
        PENDING.remove(target);
        return new DamageCritLinks.Info(DamageCritLinks.Kind.MAGIC, p.colorOverride);
    }

    public static void clearFor(LivingEntity target) {
        if (target != null) PENDING.remove(target);
    }
}
