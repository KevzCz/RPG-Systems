package net.pixeldreamstudios.rpgsystems.title.power;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

public interface TitlePower {
    Identifier id();
    default void onActivate(ServerPlayerEntity player) {}
    default void onDeactivate(ServerPlayerEntity player) {}
    default void onServerTick(MinecraftServer server, Set<ServerPlayerEntity> players) {}
    default void onClientTrigger(ServerPlayerEntity player) {}
}
