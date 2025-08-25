package net.pixeldreamstudios.rpgsystems.pet;

import java.util.UUID;

public interface PetOwnable {
    boolean rpgsystems$isPet();
    void rpgsystems$setPet(boolean pet);
    UUID rpgsystems$getOwnerUuid();
    void rpgsystems$setOwnerUuid(UUID owner);
    String rpgsystems$getPetName();
    void rpgsystems$setPetName(String name);
}
