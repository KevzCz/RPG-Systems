package net.pixeldreamstudios.rpgsystems.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.pixeldreamstudios.kevslibrary.api.CritEvents;
import net.pixeldreamstudios.rpgsystems.util.DamageCritLinks;

public final class KevsLibraryCritCompat {

    private KevsLibraryCritCompat() {}

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("kevslibrary")) return;

        CritEvents.CRIT.register(ctx -> {
            if (ctx == null || !ctx.isCrit()) return;
            LivingEntity target = ctx.target();
            if (target == null) return;
            DamageCritLinks.link(ctx.source(), DamageCritLinks.Kind.MELEE, null);
        });
    }
}
