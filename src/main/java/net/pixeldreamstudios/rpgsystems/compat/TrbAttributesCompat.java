package net.pixeldreamstudios.rpgsystems.compat;

import net.minecraft.entity.player.PlayerEntity;
import com.github.theredbrain.staminaattributes.entity.StaminaUsingEntity;
import com.github.theredbrain.manaattributes.entity.ManaUsingEntity;

public final class TrbAttributesCompat {
    private TrbAttributesCompat() {}

    public static float[] readStamina(PlayerEntity e) {
        if (e instanceof StaminaUsingEntity su) {
            float now = Math.max(0f, su.staminaattributes$getStamina());
            float max = Math.max(0f, su.staminaattributes$getUnreservedStamina());
            return new float[] { now, max };
        }
        return new float[] { -1f, -1f };
    }

    public static float[] readMana(PlayerEntity e) {
        if (e instanceof ManaUsingEntity mu) {
            float now = Math.max(0f, mu.manaattributes$getMana());
            float max = Math.max(0f, mu.manaattributes$getUnreservedMana());
            return new float[] { now, max };
        }
        return new float[] { -1f, -1f };
    }
}
