package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.network.party.PartyChatPayloads;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
@Environment(EnvType.CLIENT)
public final class ClientPartyChat {
    public static final class Msg {
        public final UUID sender;
        public final String name;
        public final String text;
        public final long time;
        public Msg(UUID sender, String name, String text, long time) {
            this.sender = sender; this.name = name; this.text = text; this.time = time;
        }
    }

    private static final Deque<Msg> MESSAGES = new ArrayDeque<>();
    private static final int MAX = 200;

    public static List<Msg> all() { return new ArrayList<>(MESSAGES); }
    public static void clearAll() { MESSAGES.clear(); }

    public static void initClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyChatPayloads.ChatMessage.ID, (payload, ctx) -> {
            push(new Msg(payload.senderUuid(), payload.senderName(), payload.message(), payload.epochMillis()));
        });
    }

    private static void push(Msg m) {
        MESSAGES.addLast(m);
        while (MESSAGES.size() > MAX) MESSAGES.removeFirst();
    }

    public static void send(UUID partyId, String text) {
        if (text == null || text.isBlank()) return;
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;
        ClientPlayNetworking.send(new PartyChatPayloads.ChatSend(partyId, text));
    }
}
