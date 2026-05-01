package net.pixeldreamstudios.rpgsystems.title;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.api.TitleApi;
import net.pixeldreamstudios.rpgsystems.network.TitleNet;
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class TitleCommands {
    private TitleCommands() {}
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("titles")
                .then(literal("give")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("id", IdentifierArgumentType.identifier())
                                        .suggests(TitleCommands::suggestTitles)
                                        .executes(ctx -> give(ctx,
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                IdentifierArgumentType.getIdentifier(ctx, "id"))))))
                .then(literal("remove")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("id", IdentifierArgumentType.identifier())
                                        .suggests(TitleCommands::suggestTitles)
                                        .executes(ctx -> remove(ctx,
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                IdentifierArgumentType.getIdentifier(ctx, "id"))))))
                .then(literal("reset")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("id", IdentifierArgumentType.identifier())
                                        .suggests(TitleCommands::suggestTitles)
                                        .executes(ctx -> reset(ctx,
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                IdentifierArgumentType.getIdentifier(ctx, "id"))))))
                .then(literal("apply")
                        .then(argument("id", IdentifierArgumentType.identifier())
                                .suggests(TitleCommands::suggestTitles)
                                .executes(ctx -> applySelf(ctx, IdentifierArgumentType.getIdentifier(ctx, "id"))))
                        .then(literal("none").executes(TitleCommands::clearSelf)))
                .then(literal("condition_check")
                        .then(argument("id", IdentifierArgumentType.identifier())
                                .suggests(TitleCommands::suggestTitles)
                                .executes(ctx -> conditionCheck(ctx, IdentifierArgumentType.getIdentifier(ctx, "id")))))
        );
    }

    private static int conditionCheck(CommandContext<ServerCommandSource> ctx, Identifier id) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        MinecraftServer server = ctx.getSource().getServer();

        Title t = TitleRegistry.get(id);
        if (t == null) {
            ctx.getSource().sendError(Text.literal("Unknown title: " + id));
            return 0;
        }

        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(self.getUuid());
        var tag = pt.progress.get(id.toString());

        ctx.getSource().sendFeedback(() -> Text.literal("§7[ titles ] Checking §f" + id), false);

        boolean allMet = true;
        for (int i = 0; i < t.conditions.size(); i++) {
            Title.Condition c = t.conditions.get(i);

            long cur   = tag == null ? 0L : tag.getLong("c" + i);
            boolean dn = tag != null && tag.getBoolean("done_" + i);

            boolean met = false;
            switch (c.type) {
                case OBTAIN_ITEM, ADVANCEMENT, REACH_LEVEL, REACH_LEVEL_XP, REACH_LEVEL_PUFFERFISH,
                        VISIT_BIOME, ENTER_DIMENSION, INTERACT_BLOCK, INTERACT_ENTITY, CHECK_ATTRIBUTE, FIND_STRUCTURE -> {
                    met = dn;
                }
                case KILL_MOBS ->      met = cur >= Math.max(1, c.count);
                case WALK_BLOCKS ->    met = cur >= Math.max(1, c.distance);
                case CRAFT_ITEM ->     met = cur >= Math.max(1, c.count);
                case MINE_BLOCKS ->    met = cur >= Math.max(1, c.count);
                case DEAL_DAMAGE_TOTAL, DEAL_DAMAGE_MAX -> met = cur >= Math.max(1, c.count);
            }

            allMet &= met;

            String targetTxt = switch (c.type) {
                case WALK_BLOCKS -> String.valueOf(Math.max(1, c.distance));
                case REACH_LEVEL, REACH_LEVEL_XP, REACH_LEVEL_PUFFERFISH -> String.valueOf(Math.max(1, c.level));
                case CHECK_ATTRIBUTE -> "≥ " + c.minValue;
                case ADVANCEMENT, VISIT_BIOME, ENTER_DIMENSION, FIND_STRUCTURE,
                        INTERACT_BLOCK, INTERACT_ENTITY, OBTAIN_ITEM, CRAFT_ITEM,
                        MINE_BLOCKS, KILL_MOBS, DEAL_DAMAGE_TOTAL, DEAL_DAMAGE_MAX -> String.valueOf(Math.max(1, c.count));
            };

            String line = String.format(
                    "§7 %2d) §f%-22s §7cur=%s  done=%s  target=%s  %s",
                    i + 1,
                    c.type.name().toLowerCase(Locale.ROOT),
                    cur,
                    dn,
                    targetTxt,
                    met ? "§a[OK]" : "§c[NO]"
            );
            ctx.getSource().sendFeedback(() -> Text.literal(line), false);
        }

        boolean hasTitle = pt.unlocked.contains(id.toString());
        if (allMet && !hasTitle) {
            boolean granted = TitleApi.grant(self, id);
            if (granted) {
                state.markDirty();
                syncSelfTo(server, self);
                TitleNet.syncProgressTo(server, self);
                ctx.getSource().sendFeedback(() -> Text.literal("§aAll conditions met. Granted " + id), false);
                return 1;
            }
        }

        boolean finalAllMet = allMet;
        ctx.getSource().sendFeedback(
                () -> Text.literal(finalAllMet ? "§aAll conditions met. (already unlocked)" : "§eNot all conditions are met."),
                false
        );
        return allMet ? 1 : 0;
    }
    private static int give(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, Identifier id) {
        boolean ok = TitleApi.grant(target, id);
        if (!ok) {
            ctx.getSource().sendError(Text.literal("Unknown title or already unlocked: " + id));
            return 0;
        }
        syncSelfTo(ctx.getSource().getServer(), target);
        ctx.getSource().sendFeedback(() -> Text.literal("Granted title " + id + " to " + target.getName().getString()), true);
        return 1;
    }
    private static int remove(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, Identifier id) {
        boolean ok = TitleApi.revoke(target, id);
        if (!ok) {
            ctx.getSource().sendError(Text.literal("Title not found or not unlocked: " + id));
            return 0;
        }

        TitleApi.clearProgress(target, id);

        TitleNet.syncSelfTo(ctx.getSource().getServer(), target);
        broadcastActiveToAll(ctx.getSource().getServer(), target.getUuid(), activeOf(ctx.getSource().getServer(), target.getUuid()));

        ctx.getSource().sendFeedback(
                () -> Text.literal("Removed and wiped progress for title " + id + " from " + target.getName().getString()),
                true
        );
        return 1;
    }
    private static int reset(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, Identifier id) {
        boolean had = TitleApi.clearProgress(target, id);

        TitleNet.syncSelfTo(ctx.getSource().getServer(), target);

        if (had) {
            ctx.getSource().sendFeedback(
                    () -> Text.literal("Reset progress for title " + id + " on " + target.getName().getString()),
                    true
            );
            return 1;
        } else {
            ctx.getSource().sendError(Text.literal("No progress found for title: " + id));
            return 0;
        }
    }

    private static int applySelf(CommandContext<ServerCommandSource> ctx, Identifier id) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        boolean ok = TitleApi.setActive(self, Optional.of(id));
        if (!ok) {
            ctx.getSource().sendError(Text.literal("You have not unlocked: " + id));
            return 0;
        }
        syncSelfTo(ctx.getSource().getServer(), self);
        broadcastActiveToAll(ctx.getSource().getServer(), self.getUuid(), Optional.of(id));
        ctx.getSource().sendFeedback(() -> Text.literal("Applied title " + id), false);
        return 1;
    }
    private static int clearSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        boolean ok = TitleApi.setActive(self, Optional.empty());
        if (!ok) {
            ctx.getSource().sendError(Text.literal("Failed to clear active title"));
            return 0;
        }
        syncSelfTo(ctx.getSource().getServer(), self);
        broadcastActiveToAll(ctx.getSource().getServer(), self.getUuid(), Optional.empty());
        ctx.getSource().sendFeedback(() -> Text.literal("Cleared active title"), false);
        return 1;
    }
    private static CompletableFuture<Suggestions> suggestTitles(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder b) {
        for (Identifier id : TitleRegistry.all().keySet()) b.suggest(id.toString());
        return b.buildFuture();
    }
    private static Optional<Identifier> activeOf(MinecraftServer server, UUID uuid) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(uuid);
        return Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));
    }
    private static void syncSelfTo(MinecraftServer server, ServerPlayerEntity player) {
        TitlesPersistentState state = TitlesPersistentState.get(server);
        TitlesPersistentState.PlayerTitles pt = state.getOrCreate(player.getUuid());
        List<Identifier> unlocked = new ArrayList<>();
        for (String s : pt.unlocked) unlocked.add(Identifier.of(s));
        Optional<Identifier> active = Optional.ofNullable(pt.active == null ? null : Identifier.of(pt.active));
        ServerPlayNetworking.send(player, new TitlePayloads.SyncSelf(unlocked, active));
    }
    private static void broadcastActiveToAll(MinecraftServer server, UUID playerUuid, Optional<Identifier> active) {
        TitlePayloads.SyncActive pkt = new TitlePayloads.SyncActive(playerUuid, active);
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(sp, pkt);
        }
    }
}
