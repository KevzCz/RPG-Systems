package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.system.SystemPayloads;

public final class SystemNet {
    private SystemNet() {}

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(
                SystemPayloads.SystemsSync.ID, SystemPayloads.SystemsSync.CODEC
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendSystemsSyncTo(server, handler.player);
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClient() {

        ClientPlayNetworking.registerGlobalReceiver(SystemPayloads.SystemsSync.ID, (payload, ctx) ->
                ctx.client().execute(() -> SystemsClientState.accept(payload.party(), payload.pet(), payload.title()))
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SystemsClientState.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SystemsClientState.clear());
    }

    public static void sendSystemsSyncTo(MinecraftServer server, ServerPlayerEntity player) {
        var sys = RPGSystemsConfig.get().systems;
        ServerPlayNetworking.send(player, new SystemPayloads.SystemsSync(sys.party, sys.pet, sys.title));
    }

    @Environment(EnvType.CLIENT)
    public static final class SystemsClientState {
        private static boolean gotSync = false;
        private static boolean party = true, pet = false, title = false;

        static void accept(boolean p, boolean pe, boolean t) {
            gotSync = true;
            party = p; pet = pe; title = t;
        }

        public static boolean hasServerSync() { return gotSync; }

        public static boolean partyEnabled() {
            return gotSync ? party : RPGSystemsConfig.get().systems.party;
        }
        public static boolean petEnabled() {
            return gotSync ? pet : RPGSystemsConfig.get().systems.pet;
        }
        public static boolean titleEnabled() {
            return gotSync ? title : RPGSystemsConfig.get().systems.title;
        }

        static void clear() {
            gotSync = false;
            party = true; pet = false; title = false;
        }
    }
}
