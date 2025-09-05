package net.pixeldreamstudios.rpgsystems.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.title.power.PowerRegistry;
import net.pixeldreamstudios.rpgsystems.title.power.TitlePower;

public final class TitlePowerNet {
    private TitlePowerNet() {}

    public record Trigger(Identifier powerId) implements CustomPayload {
        public static final Id<Trigger> ID = new Id<>(Identifier.of("rpg-systems", "power_trigger"));
        public static final PacketCodec<RegistryByteBuf, Trigger> CODEC =
                PacketCodec.tuple(Identifier.PACKET_CODEC, Trigger::powerId, Trigger::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(Trigger.ID, Trigger.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Trigger.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            Identifier pid = payload.powerId();
            if (PowerRegistry.get(pid).isPresent()) {
                if (PowerRegistry.isActive(pid, player.getUuid())) {
                    TitlePower p = PowerRegistry.get(pid).get();
                    p.onClientTrigger(player);
                }
            }
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClient() {
    }

    @Environment(EnvType.CLIENT)
    public static void sendTrigger(Identifier id) {
        ClientPlayNetworking.send(new Trigger(id));
    }
}
