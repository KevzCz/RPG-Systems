package net.pixeldreamstudios.rpgsystems.accessor;

import java.util.UUID;

public interface LivingEntityRawDamageAccess {
    UUID rpgsystems$getLastRawDamageAttacker();
    float rpgsystems$getLastRawDamageAmount();
}
