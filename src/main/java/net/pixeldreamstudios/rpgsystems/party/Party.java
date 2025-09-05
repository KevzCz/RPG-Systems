package net.pixeldreamstudios.rpgsystems.party;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    public final UUID id;
    public UUID leader;
    public String name;
    public final Set<UUID> members = new HashSet<>();
    public final PartySettings settings = new PartySettings();
    public Party(UUID id, UUID leader, String name) {
        this.id = id;
        this.leader = leader;
        this.name = name == null ? "" : name;
        this.members.add(leader);
    }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Id", id);
        nbt.putUuid("Leader", leader);
        nbt.putString("Name", name);

        NbtList list = new NbtList();
        for (UUID u : members) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("U", u);
            list.add(tag);
        }
        nbt.put("Members", list);
        nbt.put("Settings", settings.toNbt());
        return nbt;
    }

    public static Party fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("Id");
        UUID leader = nbt.getUuid("Leader");
        String name = nbt.contains("Name") ? nbt.getString("Name") : "";
        Party p = new Party(id, leader, name);
        NbtList list = nbt.getList("Members", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) p.members.add(list.getCompound(i).getUuid("U"));
        if (nbt.contains("Settings")) {
            p.settings.allowHelpfulNonMembers =
                    net.pixeldreamstudios.rpgsystems.party.PartySettings.fromNbt(nbt.getCompound("Settings"))
                            .allowHelpfulNonMembers;
        }
        return p;
    }
}
