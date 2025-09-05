package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.nbt.NbtCompound;

public final class PartySettings {
    public boolean allowHelpfulNonMembers = false;

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("AllowHelpfulNonMembers", allowHelpfulNonMembers);
        return nbt;
    }
    public static PartySettings fromNbt(NbtCompound nbt) {
        PartySettings s = new PartySettings();
        if (nbt != null) {
            s.allowHelpfulNonMembers = nbt.getBoolean("AllowHelpfulNonMembers");
        }
        return s;
    }
}
