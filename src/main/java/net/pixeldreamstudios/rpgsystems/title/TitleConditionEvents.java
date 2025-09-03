package net.pixeldreamstudios.rpgsystems.title;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.api.TitleApi;
import net.pixeldreamstudios.rpgsystems.network.TitleNet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TitleConditionEvents {
    private TitleConditionEvents(){}

    private static final Map<UUID, net.minecraft.util.math.Vec3d> LAST_POS = new HashMap<>();
    private static final Map<UUID, RegistryKey<World>> LAST_DIM = new HashMap<>();
    private static final int INVENTORY_CHECK_INTERVAL = 20;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(TitleConditionEvents::onServerTick);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(TitleConditionEvents::onKill);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            TitlesPersistentState state = TitlesPersistentState.get(server);
            TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

            net.minecraft.util.math.Vec3d last = LAST_POS.get(player.getUuid());
            net.minecraft.util.math.Vec3d now = player.getPos();
            if (last == null) {
                LAST_POS.put(player.getUuid(), now);
            } else {
                double dx = now.x - last.x;
                double dy = now.y - last.y;
                double dz = now.z - last.z;
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (player.isOnGround()) {
                    boolean changed = accumulateProgressWalk(state, pt, dist);
                    if (changed) TitleNet.syncProgressTo(server, player);
                }
                LAST_POS.put(player.getUuid(), now);
            }

            RegistryKey<World> curDim = player.getWorld().getRegistryKey();
            RegistryKey<World> prevDim = LAST_DIM.put(player.getUuid(), curDim);
            if (prevDim == null) {
                LAST_DIM.put(player.getUuid(), curDim);
            } else if (!prevDim.equals(curDim)) {
                boolean changed = onDimensionChanged(server, player, state, pt, curDim);
                if (changed) TitleNet.syncProgressTo(server, player);
            }

            boolean biomeChanged = updateVisitBiome(server, player, state, pt);
            if (biomeChanged) TitleNet.syncProgressTo(server, player);

            if (tickCounter % INVENTORY_CHECK_INTERVAL == 0) {
                boolean changed = checkPeriodic(server, player, state, pt);
                if (changed) TitleNet.syncProgressTo(server, player);
            }

            checkCompletionAndGrant(server, player, state, pt);
        }
        if (tickCounter > 1_000_000) tickCounter = 0;
    }

    private static void onKill(ServerWorld world, Entity killer, LivingEntity killed) {
        if (!(killer instanceof ServerPlayerEntity player)) return;
        MinecraftServer server = world.getServer();

        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

        boolean changed = false;

        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;
            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type != Title.Condition.Type.KILL_MOBS) continue;

                if (!matchesEntitySpec(killed, c)) continue;
                if (!matchesNbt(killed, c)) continue;

                incrementProgress(pt, id, idx, 1L);
                state.markDirty();
                changed = true;
            }
        }

        if (changed) TitleNet.syncProgressTo(server, player);
        checkCompletionAndGrant(server, player, state, pt);
    }

    private static boolean onDimensionChanged(MinecraftServer server, ServerPlayerEntity player, TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt, RegistryKey<World> curDim) {
        boolean changed = false;
        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type == Title.Condition.Type.ENTER_DIMENSION) {
                    if (c.dimension.isPresent()) {
                        if (curDim.getValue().equals(c.dimension.get())) {
                            markDone(pt, id, idx);
                            state.markDirty();
                            changed = true;
                        }
                    } else {
                        markDone(pt, id, idx);
                        state.markDirty();
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static boolean updateVisitBiome(MinecraftServer server, ServerPlayerEntity player, TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt) {
        boolean changed = false;
        BlockPos pos = player.getBlockPos();
        var biomeEntry = player.getWorld().getBiome(pos);
        Identifier biomeId = biomeEntry.getKey().map(k -> k.getValue()).orElse(null);

        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type != Title.Condition.Type.VISIT_BIOME) continue;

                if (c.biome.isPresent()) {
                    if (biomeId != null && biomeId.equals(c.biome.get())) {
                        markDone(pt, id, idx);
                        state.markDirty();
                        changed = true;
                    }
                } else {
                    markDone(pt, id, idx);
                    state.markDirty();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean matchesEntitySpec(LivingEntity killed, Title.Condition c) {
        if (c.entityType.isPresent()) {
            EntityType<?> wanted = Registries.ENTITY_TYPE.get(c.entityType.get());
            return killed.getType() == wanted;
        }
        if (c.entitySpec.isEmpty()) return true;
        String spec = c.entitySpec.get();
        if ("any".equalsIgnoreCase(spec)) return true;

        Identifier id = Registries.ENTITY_TYPE.getId(killed.getType());
        if (id == null) return false;

        if (spec.endsWith(":*")) {
            String ns = spec.substring(0, spec.indexOf(':'));
            return id.getNamespace().equals(ns);
        }

        Identifier target = Identifier.tryParse(spec);
        return target != null && id.equals(target);
    }

    private static boolean matchesNbt(LivingEntity killed, Title.Condition c) {
        if (c.nbtQuery.isEmpty()) return true;
        String query = c.nbtQuery.get();

        if (query.startsWith("tag:")) {
            String wanted = query.substring("tag:".length());
            return killed.getCommandTags().contains(wanted);
        }

        NbtCompound tag = new NbtCompound();
        killed.writeNbt(tag);
        return tag.toString().contains(query);
    }

    private static boolean accumulateProgressWalk(TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt, double distance) {
        boolean changed = false;
        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;
            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type == Title.Condition.Type.WALK_BLOCKS && c.distance > 0) {
                    incrementProgress(pt, id, idx, Math.round(distance));
                    state.markDirty();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean checkPeriodic(MinecraftServer server, ServerPlayerEntity player, TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt) {
        boolean changed = false;

        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);

                if (c.type == Title.Condition.Type.OBTAIN_ITEM && c.item.isPresent()) {
                    Item wanted = Registries.ITEM.get(c.item.get());
                    int haveNow = player.getInventory().count(wanted);

                    int target = Math.max(1, c.count);
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());

                    long prevBest = tag.getLong("c" + idx);
                    long newBest  = Math.max(prevBest, haveNow);
                    boolean prevDone = tag.getBoolean("done_" + idx);
                    boolean nowDone  = haveNow >= target;

                    if (newBest != prevBest) {
                        tag.putLong("c" + idx, newBest);
                        changed = true;
                    }
                    if (nowDone != prevDone) {
                        tag.putBoolean("done_" + idx, nowDone);
                        changed = true;
                    }
                    if (changed) state.markDirty();
                }

                if (c.type == Title.Condition.Type.ADVANCEMENT && c.advancement.isPresent()) {
                    Identifier advId = c.advancement.get();
                    AdvancementEntry adv = server.getAdvancementLoader().get(advId);
                    if (adv != null && player.getAdvancementTracker().getProgress(adv).isDone()) {
                        markDone(pt, id, idx);
                        state.markDirty();
                        changed = true;
                    }
                }

                if (c.type == Title.Condition.Type.REACH_LEVEL && c.level > 0) {
                    int lvl = computeTotalSkillsLevel(player);
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    tag.putLong("c" + idx, lvl);
                    if (lvl >= c.level) {
                        tag.putBoolean("done_" + idx, true);
                    }
                    state.markDirty();
                    changed = true;
                }

                if (c.type == Title.Condition.Type.CRAFT_ITEM && c.item.isPresent()) {
                    Item it = Registries.ITEM.get(c.item.get());
                    int crafted = player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(it));
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    long prev = tag.getLong("c" + idx);
                    if (crafted > prev) {
                        tag.putLong("c" + idx, crafted);
                        changed = true;
                    }
                    int target = Math.max(1, c.count);
                    if (crafted >= target) {
                        tag.putBoolean("done_" + idx, true);
                        changed = true;
                    }
                    if (changed) state.markDirty();
                }

                if (c.type == Title.Condition.Type.MINE_BLOCKS) {
                    int minedTotal = 0;
                    if (c.block.isPresent()) {
                        Block b = Registries.BLOCK.get(c.block.get());
                        minedTotal = player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(b));
                    } else {
                        minedTotal = 0;
                    }
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    long prev = tag.getLong("c" + idx);
                    if (minedTotal > prev) {
                        tag.putLong("c" + idx, minedTotal);
                        changed = true;
                    }
                    int target = Math.max(1, c.count);
                    if (minedTotal >= target) {
                        tag.putBoolean("done_" + idx, true);
                        changed = true;
                    }
                    if (changed) state.markDirty();
                }
            }
        }

        return changed;
    }

    private static void checkCompletionAndGrant(MinecraftServer server, ServerPlayerEntity player,
                                                TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt) {
        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;
            if (t.conditions.isEmpty()) continue;

            boolean allMet = true;
            for (int idx = 0; idx < t.conditions.size(); idx++) {
                if (!isConditionMet(pt, id, idx, t.conditions.get(idx))) { allMet = false; break; }
            }

            if (allMet) {
                boolean granted = TitleApi.grant(player, id);
                state.markDirty();
                TitleNet.syncSelfTo(server, player);
                TitleNet.syncProgressTo(server, player);
            }
        }
    }

    private static boolean isConditionMet(TitlesPersistentState.PlayerTitles pt, Identifier titleId, int idx, Title.Condition c) {
        NbtCompound tag = pt.progress.get(titleId.toString());
        if (tag != null && tag.getBoolean("done_" + idx)) return true;

        long current = tag == null ? 0L : tag.getLong("c" + idx);
        return switch (c.type) {
            case OBTAIN_ITEM, ADVANCEMENT, REACH_LEVEL, VISIT_BIOME, ENTER_DIMENSION -> tag != null && tag.getBoolean("done_" + idx);
            case KILL_MOBS -> current >= Math.max(1, c.count);
            case WALK_BLOCKS -> current >= Math.max(1, c.distance);
            case CRAFT_ITEM -> current >= Math.max(1, c.count);
            case MINE_BLOCKS -> current >= Math.max(1, c.count);
        };
    }

    private static void incrementProgress(TitlesPersistentState.PlayerTitles pt, Identifier titleId, int idx, long inc) {
        NbtCompound tag = pt.progress.computeIfAbsent(titleId.toString(), k -> new NbtCompound());
        long cur = tag.getLong("c" + idx);
        tag.putLong("c" + idx, cur + inc);
    }

    private static void markDone(TitlesPersistentState.PlayerTitles pt, Identifier titleId, int idx) {
        NbtCompound tag = pt.progress.computeIfAbsent(titleId.toString(), k -> new NbtCompound());
        tag.putBoolean("done_" + idx, true);
    }

    private static boolean isAlreadyUnlocked(TitlesPersistentState.PlayerTitles pt, Identifier titleId) {
        return pt.unlocked.contains(titleId.toString());
    }

    private static boolean puffishLoaded() {
        try {
            Class.forName("net.puffish.skillsmod.SkillsMod");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static int computeTotalSkillsLevel(ServerPlayerEntity player) {
        if (!puffishLoaded()) return -1;
        try {
            Class<?> skillsMod = Class.forName("net.puffish.skillsmod.SkillsMod");
            Method getInstance = skillsMod.getMethod("getInstance");
            Object instance = getInstance.invoke(null);

            Method getUnlockedCategories = skillsMod.getMethod("getUnlockedCategories", ServerPlayerEntity.class);
            java.util.Collection<Identifier> cats = (java.util.Collection<Identifier>) getUnlockedCategories.invoke(instance, player);

            Method getCurrentLevel = skillsMod.getMethod("getCurrentLevel", ServerPlayerEntity.class, Identifier.class);

            int total = 0;
            for (Identifier id : cats) {
                java.util.Optional<Integer> lvlOpt = (java.util.Optional<Integer>) getCurrentLevel.invoke(instance, player, id);
                total += lvlOpt.orElse(0);
            }
            return total;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static java.util.List<Map.Entry<Identifier, Title>> snapshotTitleEntries() {
        var map = TitleRegistry.all();
        var list = new ArrayList<Map.Entry<Identifier, Title>>(map.size());
        map.forEach((id, t) -> list.add(Map.entry(id, t)));
        return list;
    }
}
