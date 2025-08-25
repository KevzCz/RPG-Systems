package net.pixeldreamstudios.rpgsystems.api;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PartyChatEvent {

    public static final class Message {
        public final UUID partyId;
        public final ServerPlayerEntity sender;
        public final String content;

        public Message(UUID partyId, ServerPlayerEntity sender, String content) {
            this.partyId = partyId;
            this.sender = sender;
            this.content = content;
        }
    }
    public interface Listener {

        void onPartyChat(Message msg);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();

    private PartyChatEvent() {}

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }
    public static void fire(Message msg) {
        for (Listener l : LISTENERS) {
            try {
                l.onPartyChat(msg);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
