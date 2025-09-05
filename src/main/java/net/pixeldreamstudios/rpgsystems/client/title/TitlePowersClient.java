package net.pixeldreamstudios.rpgsystems.client.title;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public final class TitlePowersClient {
    private TitlePowersClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

        });
    }
}
