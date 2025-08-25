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
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyInvites;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyInvites.NoticeKind;

import java.util.*;
@Environment(EnvType.CLIENT)
public final class PartyInviteHud implements HudRenderCallback {
    public static void init() { HudRenderCallback.EVENT.register(new PartyInviteHud()); }

    private static final Identifier TEX_NOTIFICATION = Identifier.of("rpg-systems","textures/gui/party_invite_notification.png");
    private static final Identifier TEX_ACCEPT       = Identifier.of("rpg-systems","textures/gui/party_invite_accept.png");
    private static final Identifier TEX_DECLINE      = Identifier.of("rpg-systems","textures/gui/party_invite_decline.png");
    private static final Identifier TEX_EXPIRED      = Identifier.of("rpg-systems","textures/gui/party_invite_expired.png");
    private static final Identifier TEX_JOIN_REQ     = Identifier.of("rpg-systems","textures/gui/party_join.png");
    private static final Identifier TEX_JOIN_REQ_ACC     = Identifier.of("rpg-systems","textures/gui/party_join_accept.png");
    private static final Identifier TEX_JOIN_REQ_DEC     = Identifier.of("rpg-systems","textures/gui/party_join_decline.png");
    private static final Identifier TEX_LEAVE        = Identifier.of("rpg-systems","textures/gui/party_leave.png");
    private static final Identifier TEX_KICK         = Identifier.of("rpg-systems","textures/gui/party_kick.png");

    private static final Identifier[] ALL_TEX = {
            TEX_NOTIFICATION, TEX_ACCEPT, TEX_DECLINE, TEX_EXPIRED, TEX_JOIN_REQ, TEX_LEAVE, TEX_KICK, TEX_JOIN_REQ_ACC, TEX_JOIN_REQ_DEC
    };
    private static final int TEX_W = 64, TEX_H = 32;

    private static final float SCALE = 0.5f;
    private static final int VIS_W = Math.round(TEX_W * SCALE);
    private static final int VIS_H = Math.round(TEX_H * SCALE);

    private static final int MARGIN_X = 0;
    private static final int MARGIN_Y = 10;
    private static final int STACK_GAP = 6;

    private static final long SHOW_MS  = 5000L;
    private static final long SLIDE_MS = 300L;

    private static boolean filtered = false;

    private static final class Toast {
        final String key; final long startAtMs; final Identifier tex;
        Toast(String key, long startAtMs, Identifier tex) { this.key = key; this.startAtMs = startAtMs; this.tex = tex; }
    }

    private static Identifier texForKind(NoticeKind kind) {
        return switch (kind) {
            case INVITE_ACCEPT  -> TEX_ACCEPT;
            case INVITE_DECLINE -> TEX_DECLINE;
            case INVITE_EXPIRED -> TEX_EXPIRED;
            case JOIN_VIA_INVITE -> TEX_JOIN_REQ;
            case PARTY_LEFT -> TEX_LEAVE;
            case PARTY_KICK -> TEX_KICK;
            case PARTY_JOIN_REQ_ACC -> TEX_JOIN_REQ_ACC;
            case PARTY_JOIN_REQ_DEC -> TEX_JOIN_REQ_DEC;
            case INVITE_SENT, INVITE_RECEIVED -> TEX_NOTIFICATION;
        };
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final Set<String> SEEN   = new HashSet<>();

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden) return;

        ensureNearest();

        for (var n : new ArrayList<>(ClientPartyInvites.notices())) {
            String key = n.createdAt + ":" + n.text;
            if (SEEN.add(key)) TOASTS.add(new Toast(key, System.currentTimeMillis(), texForKind(n.kind)));
        }

        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> now - t.startAtMs >= SHOW_MS + SLIDE_MS);

        int sh = ctx.getScaledWindowHeight();
        int nextBottom = sh - MARGIN_Y;

        ListIterator<Toast> it = TOASTS.listIterator(TOASTS.size());
        while (it.hasPrevious()) {
            Toast t = it.previous();

            float slide = slideProgress(now - t.startAtMs);
            int offX    = -VIS_W - MARGIN_X;
            int targetX = MARGIN_X;
            int drawX   = Math.round(lerp(offX, targetX, slide));
            int drawY   = nextBottom - VIS_H;
            nextBottom  = drawY - STACK_GAP;

            var m = ctx.getMatrices();
            m.push();
            m.translate(drawX, drawY, 0);
            m.scale(SCALE, SCALE, 1f);
            ctx.drawTexture(t.tex, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
            m.pop();
        }
    }

    private static float slideProgress(long elapsedMs) {
        if (elapsedMs <= SLIDE_MS) return elapsedMs / (float) SLIDE_MS;
        if (elapsedMs >= SHOW_MS)  return 1f - Math.min(SLIDE_MS, elapsedMs - SHOW_MS) / (float) SLIDE_MS;
        return 1f;
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * Math.max(0f, Math.min(1f, t)); }

    private static void ensureNearest() {
        if (filtered) return;
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        for (Identifier id : ALL_TEX) {
            AbstractTexture tex = tm.getTexture(id);
            if (tex != null) tex.setFilter(false, false);
        }
        filtered = true;
    }
}