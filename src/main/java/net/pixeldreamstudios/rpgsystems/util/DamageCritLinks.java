package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.entity.damage.DamageSource;

import java.util.Map;
import java.util.WeakHashMap;

public final class DamageCritLinks {
    public enum Kind { NONE, MELEE, MAGIC }

    public static final class Info {
        public final Kind kind;
        public final Integer colorOverride;
        public Info(Kind kind, Integer colorOverride) {
            this.kind = kind;
            this.colorOverride = colorOverride;
        }
        public boolean isCrit() { return kind != Kind.NONE; }
        public boolean isMagic() { return kind == Kind.MAGIC; }
        public static Info none() { return new Info(Kind.NONE, null); }
    }

    private static final Map<DamageSource, Info> MAP = new WeakHashMap<>();

    private DamageCritLinks() {}

    public static void link(DamageSource src, Kind kind, Integer colorOverride) {
        if (src == null || kind == null) return;
        MAP.put(src, new Info(kind, colorOverride));
    }

    public static Info consume(DamageSource src) {
        if (src == null) return Info.none();
        Info i = MAP.remove(src);
        return i == null ? Info.none() : i;
    }

    public static void clear(DamageSource src) {
        if (src != null) MAP.remove(src);
    }
}
