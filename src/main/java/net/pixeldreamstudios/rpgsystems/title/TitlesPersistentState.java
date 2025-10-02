package net.pixeldreamstudios.rpgsystems.title;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

public final class TitlesPersistentState extends PersistentState {
    public static final String KEY = "rpgsystems_titles";
    public static final PersistentState.Type<TitlesPersistentState> TYPE = new PersistentState.Type<>(TitlesPersistentState::new, TitlesPersistentState::fromNbt, null);
    private final Map<UUID, PlayerTitles> data = new HashMap<>();
    public static final class PlayerTitles {
        public final Set<String> unlocked = new HashSet<>();
        public final Map<String, NbtCompound> progress = new HashMap<>();
        public String active;
        public final Set<String> permaDisabledGroups = new HashSet<>();
    }
    public static TitlesPersistentState get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE, KEY);
    }
    private static TitlesPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        TitlesPersistentState s = new TitlesPersistentState();

        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < players.size(); i++) {
            NbtCompound row = players.getCompound(i);
            UUID u = row.getUuid("Uuid");
            PlayerTitles pt = new PlayerTitles();
            NbtList unlocked = row.getList("Unlocked", NbtElement.STRING_TYPE);
            for (int j = 0; j < unlocked.size(); j++) {
                pt.unlocked.add(unlocked.getString(j));
            }
            if (row.contains("Active", NbtElement.STRING_TYPE)) {
                pt.active = row.getString("Active");
            }
            if (row.contains("Progress", NbtElement.COMPOUND_TYPE)) {
                NbtCompound prog = row.getCompound("Progress");
                for (String key : prog.getKeys()) {
                    pt.progress.put(key, prog.getCompound(key));
                }
            }
            if (row.contains("PermaDisabled", NbtElement.LIST_TYPE)) {
                NbtList dl = row.getList("PermaDisabled", NbtElement.STRING_TYPE);
                for (int j = 0; j < dl.size(); j++) pt.permaDisabledGroups.add(dl.getString(j));
            }
            s.data.put(u, pt);
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList players = new NbtList();
        for (Map.Entry<UUID, PlayerTitles> e : data.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("Uuid", e.getKey());
            NbtList unlocked = new NbtList();
            for (String id : e.getValue().unlocked) {
                unlocked.add(net.minecraft.nbt.NbtString.of(id));
            }
            row.put("Unlocked", unlocked);
            if (e.getValue().active != null) row.putString("Active", e.getValue().active);

            if (!e.getValue().progress.isEmpty()) {
                NbtCompound prog = new NbtCompound();
                for (Map.Entry<String, NbtCompound> p : e.getValue().progress.entrySet()) {
                    prog.put(p.getKey(), p.getValue());
                }
                row.put("Progress", prog);
            }
            if (!e.getValue().permaDisabledGroups.isEmpty()) {
                NbtList dl = new NbtList();
                for (String k : e.getValue().permaDisabledGroups) dl.add(net.minecraft.nbt.NbtString.of(k));
                row.put("PermaDisabled", dl);
            }
            players.add(row);
        }
        nbt.put("Players", players);
        return nbt;
    }

    public PlayerTitles getOrCreate(UUID uuid) {
        return data.computeIfAbsent(uuid, u -> new PlayerTitles());
    }
}
