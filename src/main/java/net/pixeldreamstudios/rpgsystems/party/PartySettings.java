package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.nbt.NbtCompound;

public final class PartySettings {
    public boolean allowHelpfulNonMembers = false;
    public boolean ignorePartyCollision   = true;

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("AllowHelpfulNonMembers", allowHelpfulNonMembers);
        nbt.putBoolean("IgnorePartyCollision",   ignorePartyCollision);
        return nbt;
    }
    public static PartySettings fromNbt(NbtCompound nbt) {
        PartySettings s = new PartySettings();
        if (nbt != null) {
            s.allowHelpfulNonMembers = nbt.getBoolean("AllowHelpfulNonMembers");

            if (nbt.contains("IgnorePartyCollision")) {
                s.ignorePartyCollision = nbt.getBoolean("IgnorePartyCollision");
            } else {
                s.ignorePartyCollision = true;
            }
        }
        return s;
    }
}
