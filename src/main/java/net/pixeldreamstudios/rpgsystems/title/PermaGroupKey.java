package net.pixeldreamstudios.rpgsystems.title;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public final class PermaGroupKey {
    private PermaGroupKey() {}

    public static String attr(Identifier attrId, EntityAttributeModifier.Operation op) {
        return "attr:" + attrId + "|op:" + op.name();
    }

    public static String dmgTarget(Identifier id, Title.DamageOp op) {
        return "dmg:target:" + id + "|op:" + op.name();
    }

    public static String dmgTag(Identifier tag, Title.DamageOp op) {
        return "dmg:tag:" + tag + "|op:" + op.name();
    }

    public static String spell(Identifier spellId) {
        return "spell:" + spellId;
    }

    public static String power(Identifier powerId) {
        return "power:" + powerId;
    }
}
