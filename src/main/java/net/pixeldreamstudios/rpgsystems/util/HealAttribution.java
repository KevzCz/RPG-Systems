package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Thread-local attribution for healing sources, with a "spell" flag.
 * Prefer {@link #scope(ServerPlayerEntity)} and {@link #spell(ServerPlayerEntity)} for safety.
 */
public final class HealAttribution {
    private HealAttribution() {}

    private static final ThreadLocal<ServerPlayerEntity> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_SPELL = new ThreadLocal<>();

    public static ServerPlayerEntity get() { return CURRENT.get(); }
    public static boolean isSpell() { return Boolean.TRUE.equals(IS_SPELL.get()); }

    public static void set(ServerPlayerEntity healer, boolean spell) {
        CURRENT.set(healer);
        IS_SPELL.set(spell);
    }
    public static void clear() {
        CURRENT.remove();
        IS_SPELL.remove();
    }

    /** Scoped generic heal attribution (non-spell). */
    public static Scope scope(ServerPlayerEntity healer) { return new Scope(healer, false); }
    /** Scoped spell heal attribution. */
    public static Scope spell(ServerPlayerEntity healer) { return new Scope(healer, true); }

    public static final class Scope implements AutoCloseable {
        private final ServerPlayerEntity prevHealer;
        private final Boolean prevSpell;

        private Scope(ServerPlayerEntity healer, boolean spell) {
            this.prevHealer = CURRENT.get();
            this.prevSpell  = IS_SPELL.get();
            CURRENT.set(healer);
            IS_SPELL.set(spell);
        }

        @Override public void close() {
            if (prevHealer == null) CURRENT.remove(); else CURRENT.set(prevHealer);
            if (prevSpell  == null) IS_SPELL.remove(); else IS_SPELL.set(prevSpell);
        }
    }
}
