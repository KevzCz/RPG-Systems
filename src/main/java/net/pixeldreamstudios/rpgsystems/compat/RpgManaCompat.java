package net.pixeldreamstudios.rpgsystems.compat;

import net.minecraft.entity.player.PlayerEntity;
import com.cleannrooster.rpgmana.api.ManaInterface;

public final class RpgManaCompat {
    private RpgManaCompat() {}

    public static float[] readMana(PlayerEntity e) {
        if (e instanceof ManaInterface mi) {
            double nowD = mi.getMana();
            double maxD = mi.getMaxMana();
            float now = (float) Math.max(0.0, nowD);
            float max = (float) Math.max(0.0, maxD);
            return new float[] { now, max };
        }
        return new float[] { -1f, -1f };
    }
}
