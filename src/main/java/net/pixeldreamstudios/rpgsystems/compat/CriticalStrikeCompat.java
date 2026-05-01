package net.pixeldreamstudios.rpgsystems.compat;

import net.critical_strike.CriticalStrikeMod;
import net.critical_strike.api.CriticalDamageSource;
import net.critical_strike.internal.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.pixeldreamstudios.rpgsystems.util.DamageCritLinks;

public final class CriticalStrikeCompat {
    private static final boolean IS_LOADED = FabricLoader.getInstance().isModLoaded("critical_strike");

    private CriticalStrikeCompat() {}

    public static void init() {
        if (!IS_LOADED) {
        }

    }

    public static void onCriticalStrikeDamage(DamageSource source, LivingEntity target, boolean isMelee) {
        if (source == null || target == null) return;


        DamageCritLinks.link(source, DamageCritLinks.Kind.MELEE, null);
    }

    public static boolean isCriticalStrikeDamage(DamageSource source) {
        if (source == null || !IS_LOADED) return false;

        try {
            if (source instanceof CriticalDamageSource critSource) {
                return critSource.rng_isCritical();
            }
        } catch (Throwable ignored) {

        }

        return false;
    }

    public static boolean shouldAllowVanillaJumpCrits() {
        if (!IS_LOADED) return true;

        try {
            Config config = CriticalStrikeMod.config.value;

            return !config.disable_vanilla_jump_criticals;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }
}