package net.pixeldreamstudios.rpgsystems.client.party.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.config.MiscClientConfig;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyJoinRequests;

import java.util.*;

@Environment(EnvType.CLIENT)
public final class PartyJoinRequestHud implements HudRenderCallback {
    public static void init() { HudRenderCallback.EVENT.register(new PartyJoinRequestHud()); }

    private static final Identifier TOAST_TEX = Identifier.of("rpg-systems","textures/gui/party_join_request_notification.png");

    private static final int TEX_W = 64, TEX_H = 32;

    private static final float SCALE = 0.5f;
    private static final int VIS_W = Math.round(TEX_W * SCALE);
    private static final int VIS_H = Math.round(TEX_H * SCALE);

    private static final int STACK_GAP = 6;

    private static final long SHOW_MS  = 5000L;
    private static final long SLIDE_MS = 300L;

    private static boolean filtered = false;

    private static final class Toast { final String key; final long startAtMs; Toast(String k, long t){key=k;startAtMs=t;} }
    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final Set<String> KNOWN_REQUESTERS = new HashSet<>();

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden) return;

        ensureNearest();

        Set<String> current = new HashSet<>();
        for (var r : new ArrayList<>(ClientPartyJoinRequests.all())) {
            String key = "join:" + (r.requesterName == null ? "Unknown" : r.requesterName);
            current.add(key);
            if (KNOWN_REQUESTERS.add(key)) TOASTS.add(new Toast(key, System.currentTimeMillis()));
        }
        KNOWN_REQUESTERS.removeIf(k -> !current.contains(k));

        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> now - t.startAtMs >= SHOW_MS + SLIDE_MS);

        var cfg = MiscClientConfig.get();

        int baseLeft = cfg.joinRequestHudX;
        int baseBottom = ctx.getScaledWindowHeight() - cfg.joinRequestHudY;

        int nextBottom = baseBottom;

        ListIterator<Toast> it = TOASTS.listIterator(TOASTS.size());
        while (it.hasPrevious()) {
            Toast t = it.previous();

            float slide = slideProgress(now - t.startAtMs);
            int offX    = -VIS_W - baseLeft;
            int targetX = baseLeft;
            int drawX   = Math.round(lerp(offX, targetX, slide));
            int drawY   = nextBottom - VIS_H;
            nextBottom  = drawY - STACK_GAP;

            var m = ctx.getMatrices();
            m.push();
            m.translate(drawX, drawY, 0);
            m.scale(SCALE, SCALE, 1f);
            ctx.drawTexture(TOAST_TEX, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
            m.pop();
        }
    }

    private static float slideProgress(long elapsedMs) {
        if (elapsedMs <= SLIDE_MS) return elapsedMs / (float) SLIDE_MS;
        if (elapsedMs >= SHOW_MS)  return 1f - Math.min(SLIDE_MS, elapsedMs - SHOW_MS) / (float) SLIDE_MS;
        return 1f;
    }

    private static float lerp(float a, float b, float t){ return a + (b - a) * Math.max(0f, Math.min(1f, t)); }

    private static void ensureNearest() {
        if (filtered) return;
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        AbstractTexture tex = tm.getTexture(TOAST_TEX);
        if (tex != null) tex.setFilter(false, false);
        filtered = true;
    }
}
