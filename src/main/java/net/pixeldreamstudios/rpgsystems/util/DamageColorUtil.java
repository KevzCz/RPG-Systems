package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.world.World;
import net.spell_engine.entity.SpellProjectile;

public final class DamageColorUtil {
    private DamageColorUtil(){}

    public static final int WHITE         = 0xFFFFFF;
    public static final int LIGHT_BLUE    = 0x82D4FF;
    public static final int DARK_BLUE     = 0x2F49B8;
    public static final int RED           = 0xFF3A3A;
    public static final int DARK_BROWNRED = 0x7A2E20;
    public static final int BLACK         = 0x000000;
    public static final int DARK_GREEN    = 0x2E8B3A;
    public static final int LIGHT_RED     = 0xFF8A80;
    public static final int DARK_PURPLE   = 0x6A3DBA;
    public static final int CRIT_ORANGE   = 0xFFC84A;

    public static int colorOf(World world, DamageSource source, LivingEntity victim) {
        if (source == null) return DARK_BLUE;

        if (source.isIn(DamageTypeTags.IS_FIRE)) return RED;
        if (isWither(source)) return BLACK;
        if (isPoison(source, victim)) return DARK_GREEN;

        Entity srcEnt = source.getSource();
        if (srcEnt instanceof SpellProjectile) return DARK_BLUE;
        Integer schoolColor = trySpellSchoolColorFromDamageSource(source);
        if (schoolColor != null) return schoolColor;

        Integer projSchoolColor = trySpellSchoolColorFromSourceEntity(srcEnt);
        if (projSchoolColor != null) return projSchoolColor;

        if (srcEnt instanceof AreaEffectCloudEntity) return DARK_BROWNRED;

        if (source.isIn(DamageTypeTags.IS_PROJECTILE) ||
                srcEnt instanceof TridentEntity ||
                srcEnt instanceof ProjectileEntity) {
            return LIGHT_BLUE;
        }

        Entity attacker = source.getAttacker();
        if (attacker != null && !(attacker instanceof ProjectileEntity)) return WHITE;

        return DARK_BLUE;
    }


    private static boolean isWither(DamageSource src) { return src.isOf(DamageTypes.WITHER); }

    private static boolean isPoison(DamageSource src, LivingEntity victim) {
        boolean magic = src.isOf(DamageTypes.MAGIC) || src.isOf(DamageTypes.INDIRECT_MAGIC);
        return magic && victim != null && victim.hasStatusEffect(StatusEffects.POISON);
    }

    private static Integer trySpellSchoolColorFromDamageSource(DamageSource src) { /* … unchanged … */ return null; }
    private static Integer trySpellSchoolColorFromSourceEntity(Entity sourceEntity) { /* … unchanged … */ return null; }
}
