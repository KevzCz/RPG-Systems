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
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;

import java.util.ArrayList;
import java.util.List;
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
                .then(literal("apply")
                        .then(argument("id", IdentifierArgumentType.identifier())
                                .suggests(TitleCommands::suggestTitles)
                                .executes(ctx -> applySelf(ctx, IdentifierArgumentType.getIdentifier(ctx, "id"))))
                        .then(literal("none").executes(TitleCommands::clearSelf)))
        );
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
        syncSelfTo(ctx.getSource().getServer(), target);
        broadcastActiveToAll(ctx.getSource().getServer(), target.getUuid(), activeOf(ctx.getSource().getServer(), target.getUuid()));
        ctx.getSource().sendFeedback(() -> Text.literal("Removed title " + id + " from " + target.getName().getString()), true);
        return 1;
    }

    private static int applySelf(CommandContext<ServerCommandSource> ctx, Identifier id) {
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        boolean ok = net.pixeldreamstudios.rpgsystems.api.TitleApi.setActive(self, Optional.of(id));
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
        boolean ok = net.pixeldreamstudios.rpgsystems.api.TitleApi.setActive(self, Optional.empty());
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
