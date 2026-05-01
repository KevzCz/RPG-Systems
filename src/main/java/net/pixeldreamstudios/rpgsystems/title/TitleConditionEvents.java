package net.pixeldreamstudios.rpgsystems.title;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;
import net.pixeldreamstudios.rpgsystems.accessor.LivingEntityRawDamageAccess;
import net.pixeldreamstudios.rpgsystems.api.TitleApi;
import net.pixeldreamstudios.rpgsystems.network.TitleNet;
import net.puffish.skillsmod.SkillsMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TitleConditionEvents {
    private TitleConditionEvents() {}
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();
    private static final Map<UUID, RegistryKey<World>> LAST_DIM = new HashMap<>();
    private static final int INVENTORY_CHECK_INTERVAL = 20;
    private static int tickCounter = 0;
    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(TitleConditionEvents::onServerTick);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(TitleConditionEvents::onKill);

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity player)) return;
            if (!(entity instanceof LivingEntity victim)) return;

            MinecraftServer server = player.getServer();
            TitlesPersistentState state = TitlesPersistentState.get(server);
            TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

            boolean changed = false;

            long amt = Math.max(0L, Math.round(Math.max(baseDamageTaken, damageTaken)));

            for (var e : snapshotTitleEntries()) {
                Identifier id = e.getKey();
                Title t = e.getValue();
                if (isAlreadyUnlocked(pt, id)) continue;

                for (int idx = 0; idx < t.conditions.size(); idx++) {
                    Title.Condition c = t.conditions.get(idx);
                    if (c.type != Title.Condition.Type.DEAL_DAMAGE_TOTAL && c.type != Title.Condition.Type.DEAL_DAMAGE_MAX) continue;
                    if (!matchesEntitySpec(victim, c)) continue;
                    if (!matchesNbt(victim, c)) continue;

                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    long prev = tag.getLong("c" + idx);
                    long next = (c.type == Title.Condition.Type.DEAL_DAMAGE_TOTAL) ? prev + amt : Math.max(prev, amt);

                    if (next != prev) { tag.putLong("c" + idx, next); changed = true; }

                    int target = Math.max(1, c.count);
                    boolean nowDone = next >= target;
                    if (nowDone != tag.getBoolean("done_" + idx)) { tag.putBoolean("done_" + idx, nowDone); changed = true; }
                }
            }

            if (changed) {
                state.markDirty();
                TitleNet.syncProgressTo(server, player);
                checkCompletionAndGrant(server, player, state, pt);
            }
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> onUseBlock(player, world, hand, hit));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> onUseEntity(player, world, hand, entity, hit));
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

        MinecraftServer server = sw.getServer();
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(sp.getUuid());

        boolean changed = false;
        BlockPos pos = hit.getBlockPos();
        Block b = world.getBlockState(pos).getBlock();
        Identifier bid = Registries.BLOCK.getId(b);

        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type != Title.Condition.Type.INTERACT_BLOCK) continue;

                boolean match = c.block.map(bid::equals).orElse(true);
                if (!match) continue;

                NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                long next = tag.getLong("c" + idx) + 1;
                tag.putLong("c" + idx, next);
                if (next >= Math.max(1, c.count)) tag.putBoolean("done_" + idx, true);
                state.markDirty();
                changed = true;
            }
        }

        if (changed) {
            TitleNet.syncProgressTo(server, sp);
            checkCompletionAndGrant(server, sp, state, pt);
        }
        return ActionResult.PASS;
    }

    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hit) {
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (!(entity instanceof LivingEntity le)) return ActionResult.PASS;

        MinecraftServer server = sp.getServer();
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(sp.getUuid());

        boolean changed = false;

        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type != Title.Condition.Type.INTERACT_ENTITY) continue;

                if (!matchesEntitySpec(le, c)) continue;
                if (!matchesNbt(le, c)) continue;

                NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                long next = tag.getLong("c" + idx) + 1;
                tag.putLong("c" + idx, next);
                if (next >= Math.max(1, c.count)) tag.putBoolean("done_" + idx, true);
                state.markDirty();
                changed = true;
            }
        }

        if (changed) {
            TitleNet.syncProgressTo(server, sp);
            checkCompletionAndGrant(server, sp, state, pt);
        }
        return ActionResult.PASS;
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            TitlesPersistentState state = TitlesPersistentState.get(server);
            TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());

            Vec3d last = LAST_POS.get(player.getUuid());
            Vec3d now = player.getPos();
            if (last == null) {
                LAST_POS.put(player.getUuid(), now);
            } else {
                double dx = now.x - last.x;
                double dy = now.y - last.y;
                double dz = now.z - last.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
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

            boolean attrChanged = updateAttributeChecks(server, player, state, pt);
            if (attrChanged) TitleNet.syncProgressTo(server, player);

            boolean structureChanged = updateFindStructure(server, player, state, pt);
            if (structureChanged) TitleNet.syncProgressTo(server, player);

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

        float lastRaw = 0f;
        if (killed instanceof LivingEntityRawDamageAccess acc) {
            UUID a = acc.rpgsystems$getLastRawDamageAttacker();
            if (a != null && a.equals(player.getUuid())) {
                lastRaw = acc.rpgsystems$getLastRawDamageAmount();
            }
        }
        long lastAmt = Math.max(0L, Math.round(lastRaw));

        boolean changed = false;

        for (var e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);

                if (c.type == Title.Condition.Type.KILL_MOBS) {
                    if (!matchesEntitySpec(killed, c)) continue;
                    if (!matchesNbt(killed, c)) continue;
                    incrementProgress(pt, id, idx, 1L);
                    state.markDirty();
                    changed = true;
                }

                if ((c.type == Title.Condition.Type.DEAL_DAMAGE_TOTAL || c.type == Title.Condition.Type.DEAL_DAMAGE_MAX)
                        && lastAmt > 0
                        && matchesEntitySpec(killed, c)
                        && matchesNbt(killed, c)) {
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    long prev = tag.getLong("c" + idx);
                    long next = (c.type == Title.Condition.Type.DEAL_DAMAGE_TOTAL) ? prev + lastAmt : Math.max(prev, lastAmt);
                    if (next != prev) {
                        tag.putLong("c" + idx, next);
                        state.markDirty();
                        changed = true;
                    }
                    int target = Math.max(1, c.count);
                    boolean nowDone = next >= target;
                    if (nowDone != tag.getBoolean("done_" + idx)) {
                        tag.putBoolean("done_" + idx, nowDone);
                        state.markDirty();
                        changed = true;
                    }
                }
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

    private static boolean updateAttributeChecks(MinecraftServer server, ServerPlayerEntity player, TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt) {
        boolean changed = false;
        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type != Title.Condition.Type.CHECK_ATTRIBUTE) continue;
                if (c.attributeId.isEmpty()) continue;

                RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, c.attributeId.get());
                RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key).orElse(null);
                if (entry == null) continue;

                EntityAttributeInstance inst = player.getAttributeInstance(entry);
                if (inst == null) continue;

                double value = inst.getValue();
                NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                tag.putLong("c" + idx, Math.round(value));

                boolean meets = value >= c.minValue;
                boolean prevDone = tag.getBoolean("done_" + idx);
                if (meets != prevDone) {
                    tag.putBoolean("done_" + idx, meets);
                    state.markDirty();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean updateFindStructure(MinecraftServer server, ServerPlayerEntity player, TitlesPersistentState state, TitlesPersistentState.PlayerTitles pt) {
        boolean changed = false;
        ServerWorld sw = player.getServerWorld();
        StructureAccessor accessor = sw.getStructureAccessor();

        for (Map.Entry<Identifier, Title> e : snapshotTitleEntries()) {
            Identifier id = e.getKey();
            Title t = e.getValue();
            if (isAlreadyUnlocked(pt, id)) continue;

            for (int idx = 0; idx < t.conditions.size(); idx++) {
                Title.Condition c = t.conditions.get(idx);
                if (c.type != Title.Condition.Type.FIND_STRUCTURE) continue;

                boolean inside = false;

                if (c.structure.isPresent()) {
                    RegistryKey<Structure> skey = RegistryKey.of(RegistryKeys.STRUCTURE, c.structure.get());
                    RegistryEntry<Structure> sentry = server.getRegistryManager()
                            .get(RegistryKeys.STRUCTURE)
                            .getEntry(skey)
                            .orElse(null);

                    if (sentry != null) {
                        var start = accessor.getStructureAt(player.getBlockPos(), sentry.value());
                        inside = start != null && start.hasChildren();
                    }
                }

                if (inside || c.structure.isEmpty()) {
                    markDone(pt, id, idx);
                    state.markDirty();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean matchesEntitySpec(LivingEntity target, Title.Condition c) {

        if (c.entityTagId != null && c.entityTagId.isPresent()) {
            TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, c.entityTagId.get());
            return target.getType().isIn(tagKey);
        }

        if (c.entityType.isPresent()) {
            EntityType<?> wanted = Registries.ENTITY_TYPE.get(c.entityType.get());
            return target.getType() == wanted;
        }

        if (c.entitySpec.isEmpty()) return true;
        String spec = c.entitySpec.get().trim();
        if (spec.isEmpty() || "any".equalsIgnoreCase(spec)) return true;

        Identifier id = Registries.ENTITY_TYPE.getId(target.getType());
        if (id == null) return false;

        if (spec.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(spec.substring(1));
            if (tagId == null) return false;
            TagKey<EntityType<?>> tk = TagKey.of(RegistryKeys.ENTITY_TYPE, tagId);
            return target.getType().isIn(tk);
        }

        if (spec.endsWith(":*")) {
            int idx = spec.indexOf(':');
            String ns = (idx >= 0) ? spec.substring(0, idx) : spec;
            return id.getNamespace().equals(ns);
        }

        Identifier targetId = Identifier.tryParse(spec);
        return id.equals(targetId);
    }

    private static boolean matchesNbt(LivingEntity entity, Title.Condition c) {
        if (c.nbtQuery.isEmpty()) return true;
        String query = c.nbtQuery.get();

        if (query.startsWith("tag:")) {
            String wanted = query.substring("tag:".length());
            return entity.getCommandTags().contains(wanted);
        }

        NbtCompound tag = new NbtCompound();
        entity.writeNbt(tag);
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

                    boolean nowDone = prevDone || newBest >= target;

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

                if (c.type == Title.Condition.Type.REACH_LEVEL_XP || c.type == Title.Condition.Type.REACH_LEVEL) {
                    int lvl = player.experienceLevel;
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    tag.putLong("c" + idx, lvl);
                    if (lvl >= c.level) {
                        tag.putBoolean("done_" + idx, true);
                        state.markDirty();
                        changed = true;
                    }
                }

                if (c.type == Title.Condition.Type.REACH_LEVEL_PUFFERFISH) {
                    int lvl = computeTotalSkillsLevel(player);
                    NbtCompound tag = pt.progress.computeIfAbsent(id.toString(), k -> new NbtCompound());
                    tag.putLong("c" + idx, Math.max(0, lvl));
                    boolean meets = (lvl >= 0) && (lvl >= c.level);
                    boolean prevDone = tag.getBoolean("done_" + idx);
                    if (meets != prevDone) {
                        tag.putBoolean("done_" + idx, meets);
                        state.markDirty();
                        changed = true;
                    }
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
            case OBTAIN_ITEM, ADVANCEMENT, REACH_LEVEL, REACH_LEVEL_XP, REACH_LEVEL_PUFFERFISH,
                    VISIT_BIOME, ENTER_DIMENSION, INTERACT_BLOCK, INTERACT_ENTITY, CHECK_ATTRIBUTE, FIND_STRUCTURE
                    -> tag != null && tag.getBoolean("done_" + idx);
            case KILL_MOBS -> current >= Math.max(1, c.count);
            case WALK_BLOCKS -> current >= Math.max(1, c.distance);
            case CRAFT_ITEM -> current >= Math.max(1, c.count);
            case MINE_BLOCKS -> current >= Math.max(1, c.count);
            case DEAL_DAMAGE_TOTAL, DEAL_DAMAGE_MAX -> current >= Math.max(1, c.count);

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
        return FabricLoader.getInstance().isModLoaded("puffish_skills");
    }

    private static int computeTotalSkillsLevel(ServerPlayerEntity player) {
        if (!puffishLoaded()) return -1;
        try {
            SkillsMod mod = SkillsMod.getInstance();
            if (mod == null) return -1;
            int total = 0;
            for (Identifier cat : mod.getUnlockedCategories(player)) {
                total += mod.getCurrentLevel(player, cat).orElse(0);
            }
            return total;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static List<Map.Entry<Identifier, Title>> snapshotTitleEntries() {
        var map = TitleRegistry.all();
        var list = new ArrayList<Map.Entry<Identifier, Title>>(map.size());
        map.forEach((id, t) -> list.add(Map.entry(id, t)));
        return list;
    }
}
