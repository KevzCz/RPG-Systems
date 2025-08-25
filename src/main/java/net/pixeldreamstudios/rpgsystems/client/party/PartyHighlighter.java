package net.pixeldreamstudios.rpgsystems.client.party;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class PartyHighlighter {
    private PartyHighlighter() {}

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static boolean enabled = false;

    private static final Map<UUID, Integer> COLOR_CACHE = new HashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled) return;
            ClientPlayerEntity self = MC.player;
            if (self == null) return;
            COLOR_CACHE.keySet().removeIf(u -> !ClientPartyHudData.isMember(u));
        });
    }

    public static void toggle() {
        enabled = !enabled;
        var p = MC.player;
        if (p != null) {
            p.sendMessage(net.minecraft.text.Text.literal(
                    enabled ? "Party highlight: ON" : "Party highlight: OFF"), true);
        }
    }

    public static void disable() { enabled = false; }

    public static boolean isEnabled() { return enabled; }

    public static boolean shouldGlow(Entity e) {
        if (!enabled) return false;
        if (!(e instanceof PlayerEntity)) return false;
        if (MC == null || MC.player == null) return false;
        if (e.getId() == MC.player.getId()) return false;
        return ClientPartyHudData.isMember(e.getUuid());
    }

    /** Color to use when we’re glowing them. */
    public static int getColor(Entity e) {
        UUID id = e.getUuid();
        return COLOR_CACHE.computeIfAbsent(id, PartyHighlighter::colorForUuid);
    }

    private static int colorForUuid(UUID u) {
        long hbits = (u.getMostSignificantBits() ^ u.getLeastSignificantBits());
        float hue = ((hbits & 0xFFFFFFFFL) % 360L) / 360f;
        float sat = 0.80f;
        float lit = 0.55f;
        return hslToRgb(hue, sat, lit);
    }
    private static int hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? (l * (1 + s)) : (l + s - l * s);
            float p = 2 * l - q;
            r = hue2rgb(p, q, h + 1f/3f);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1f/3f);
        }
        int R = Math.min(255, Math.max(0, Math.round(r * 255f)));
        int G = Math.min(255, Math.max(0, Math.round(g * 255f)));
        int B = Math.min(255, Math.max(0, Math.round(b * 255f)));
        return (R << 16) | (G << 8) | B;
    }
    private static float hue2rgb(float p, float q, float t) {
        if (t < 0) t += 1f;
        if (t > 1) t -= 1f;
        if (t < 1f/6f) return p + (q - p) * 6f * t;
        if (t < 1f/2f) return q;
        if (t < 2f/3f) return p + (q - p) * (2f/3f - t) * 6f;
        return p;
    }
}
