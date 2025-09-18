package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Optional;

public final class DamageColorUtil {
    private DamageColorUtil(){}

    private static final TagKey<DamageType> C_MAGIC =
            TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("c", "magic"));

    public static final int COLOR_GENERIC    = 0xFFFFFF;
    public static final int COLOR_CRIT_MELEE = 0xFFF7CC;
    public static final int COLOR_CRIT_MAGIC = 0xB8E6FF;
    public static final int COLOR_DRAGON     = 0xE600FF;
    public static final int COLOR_WITHER     = 0x666666;
    public static final int COLOR_EXPLOSION  = 0xFFBB29;
    public static final int COLOR_WATER      = 0x1898E3;
    public static final int COLOR_FREEZING   = 0x09D2FF;
    public static final int COLOR_LIGHTNING  = 0xFFF200;
    public static final int COLOR_CACTUS     = 0x0FA209;
    public static final int COLOR_WARDEN     = 0x074550;
    public static final int COLOR_FALL       = 0xC8A16B;
    public static final int COLOR_IMPACT     = 0xAAAAAA;
    public static final int WHITE      = 0xFFFFFF;
    public static final int LIGHT_BLUE = 0x82D4FF;
    public static final int DARK_BLUE  = 0x2F49B8;
    public static final int RED        = 0xFF3A3A;
    public static final int DARK_BROWN = 0x7A2E20;

    // New: “holy” gold, air sky, and earthy brown tones
    public static final int COLOR_HOLY   = 0xFFE27D; // warm golden/yellow
    public static final int COLOR_AIR    = 0xCDEBFF; // pale sky blue
    public static final int COLOR_EARTH  = 0x8C5E3C; // earthy brown

    public static int colorOf(World world, DamageSource source, LivingEntity victim) {
        return baseColorOf(world, source, victim);
    }

    public static int colorOf(World world, DamageSource source, LivingEntity victim, boolean crit, boolean magicCrit, Integer overrideColor) {
        int base = baseColorOf(world, source, victim);
        if (!crit) return base;
        if (magicCrit) {
            if (overrideColor != null) return overrideColor;
            return exaggerate(base);
        }
        return COLOR_CRIT_MELEE;
    }

    public static int baseColorOf(World world, DamageSource source, LivingEntity victim) {
        if (source == null) return DARK_BLUE;

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) return COLOR_EXPLOSION;
        if (source.isOf(DamageTypes.WITHER)) return COLOR_WITHER;
        if (source.isOf(DamageTypes.DRAGON_BREATH)) return COLOR_DRAGON;
        if (source.isOf(DamageTypes.DROWN)) return COLOR_WATER;
        if (source.isOf(DamageTypes.FREEZE)) return COLOR_FREEZING;
        if (source.isOf(DamageTypes.LIGHTNING_BOLT)) return COLOR_LIGHTNING;
        if (source.isOf(DamageTypes.CACTUS)) return COLOR_CACTUS;
        if (source.isOf(DamageTypes.SONIC_BOOM)) return COLOR_WARDEN;
        if (source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.STALAGMITE)) return COLOR_FALL;
        if (source.isOf(DamageTypes.FLY_INTO_WALL) || source.isOf(DamageTypes.FALLING_BLOCK)) return COLOR_IMPACT;

        if (isTypeId(source, "spell_power:arcane"))    return LIGHT_BLUE;
        if (isTypeId(source, "spell_power:fire"))      return RED;
        if (isTypeId(source, "spell_power:frost"))     return COLOR_FREEZING;
        if (isTypeId(source, "spell_power:generic"))   return DARK_BLUE;
        if (isTypeId(source, "spell_power:healing"))   return COLOR_HOLY;       // changed to yellowish “holy”
        if (isTypeId(source, "spell_power:lightning")) return COLOR_LIGHTNING;
        if (isTypeId(source, "spell_power:soul"))      return COLOR_DRAGON;

        if (isTypeId(source, "spell_power:air"))       return COLOR_AIR;
        if (isTypeId(source, "spell_power:earth"))     return COLOR_EARTH;
        if (isTypeId(source, "spell_power:water"))     return COLOR_WATER;

        if (source.isIn(C_MAGIC) || source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC)) {
            return DARK_BLUE;
        }

        Entity srcEnt = source.getSource();
        if (srcEnt instanceof AreaEffectCloudEntity) return DARK_BROWN;

        if (source.isIn(DamageTypeTags.IS_PROJECTILE) ||
                srcEnt instanceof TridentEntity ||
                srcEnt instanceof ProjectileEntity) {
            return LIGHT_BLUE;
        }

        if (source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK)) return WHITE;
        if (source.isIn(DamageTypeTags.IS_FIRE)) return RED;

        return DARK_BLUE;
    }

    public static int critColorPreview(boolean magicCrit, int baseRgb, Integer overrideColor) {
        if (magicCrit) return overrideColor != null ? overrideColor : exaggerate(baseRgb);
        return COLOR_CRIT_MELEE;
    }

    private static int exaggerate(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        int outR = clamp255((int)(r * 0.75f + 255 * 0.25f));
        int outG = clamp255((int)(g * 0.75f + 255 * 0.25f));
        int outB = clamp255((int)(b * 0.75f + 255 * 0.25f));
        return (outR << 16) | (outG << 8) | outB;
    }

    private static int clamp255(int v) { return v < 0 ? 0 : Math.min(v, 255); }

    private static boolean isTypeId(DamageSource src, String fullId) {
        Optional<RegistryKey<DamageType>> key = src.getTypeRegistryEntry().getKey();
        return key.isPresent() && key.get().getValue().toString().equals(fullId);
    }
}
