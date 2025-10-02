package net.pixeldreamstudios.rpgsystems.client.party.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;

import java.util.List;
import java.util.UUID;

import static net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.*;

final class SimpleHud implements PartyHudRenderer {
    private static final int PANEL_W = 128;
    private static final float BG_SCALE = 0.75f;

    private static final int ICON_SIZE = 14;
    private static final int ICON_LEFT = 6;
    private static final int ICON_TOP  = 6;

    private static final int NAME_LEFT = 24;
    private static final int NAME_TOP  = 6;
    private static final float NAME_SCALE = 0.55f;
    private static final int NAME_COLOR = 0xFFECE0CE;

    private static final int BAR_LEFT = 24;
    private static final int BAR_RIGHT_PAD = 6;

    private static final int BAR_H = 4;
    private static final int BAR_GAP = 2;

    private static final int BAR_HP_FILL      = 0xFFDA3B44;
    private static final int BAR_HUNGER_FILL  = 0xFFE6C200;
    private static final int BAR_STAM_FILL    = 0xFFE6C200;
    private static final int BAR_MANA_FILL    = 0xFF4EA6FF;
    private static final int BAR_RPGMANA_FILL = 0xFF4EA6FF;
    private static final float VALUE_SCALE = 0.30f;
    private static final int   VALUE_COLOR = 0xFFFFFFFF;

    @Override
    public void render(DrawContext ctx, RenderTickCounter tickCounter, PartyHudClientConfig cfg) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden || mc.player == null) return;

        ensureBgNearest();
        ensureHudBarTexturesNearest();

        int xAnchor = cfg.partyHudX;
        int yAnchorStart = cfg.partyHudY;

        float hudScale = cfg.hudScale;
        if (hudScale <= 0.1f) hudScale = 0.1f;

        int rowStepUnscaled = ICON_TOP + ICON_SIZE + 4;
        int rowStep = Math.round(rowStepUnscaled * hudScale);

        List<ClientPartyHudData.Member> list = partyMembers();
        if (list.isEmpty()) return;

        int yScreen = yAnchorStart;
        for (var m : list) {
            var mtx = ctx.getMatrices();
            mtx.push();
            mtx.translate(xAnchor, yScreen, 0);
            mtx.scale(hudScale, hudScale, 1f);
            drawRow(ctx, 0, 0, m.uuid, m.name, m.totalLevel, cfg);
            mtx.pop();
            yScreen += rowStep;
        }
    }

    private void drawRow(DrawContext ctx, int x, int y, UUID uuid, String name, int level, PartyHudClientConfig cfg) {
        int scaledW = Math.round(PANEL_W * BG_SCALE);
        int bgX = x + 2;
        int bgY = y + ICON_TOP - 2;
        int bgW = scaledW - 4;
        int bgH = ICON_SIZE + 4;
        drawSoftBg(ctx, bgX, bgY, bgW, bgH);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        drawHead(ctx, x + ICON_LEFT, y + ICON_TOP, ICON_SIZE, uuid, name);

        String title = ((level >= 0) ? level : 0) + " | " + (name == null ? "Unknown" : name);
        int titleX = x + NAME_LEFT;
        int titleY = y + NAME_TOP - 1;
        drawScaledText(ctx, tr, title, titleX, titleY, NAME_COLOR, NAME_SCALE);

        int barX = x + BAR_LEFT;
        int barRight = x + scaledW - BAR_RIGHT_PAD;
        int barW = Math.max(0, barRight - barX);

        var bars = collectBars(uuid, cfg,
                BAR_HP_FILL, BAR_HUNGER_FILL, BAR_STAM_FILL, BAR_MANA_FILL, BAR_RPGMANA_FILL);
        if (bars.size() > 2) bars = bars.subList(0, 2);

        int by = y + 10;
        int drawn = 0;
        for (var b : bars) {
            if (drawn >= 2) break;
            drawHudBarSmart(ctx, barX, by, barW, BAR_H, b);

            String val = b.text;

            float tW = MinecraftClient.getInstance().textRenderer.getWidth(val) * VALUE_SCALE;
            float tH = MinecraftClient.getInstance().textRenderer.fontHeight * VALUE_SCALE;

            float tX = barX + (barW - tW) / 2f;
            float tY = by   + (BAR_H - tH) / 2f;

            drawScaledTextF(ctx, tr, val, tX, tY, VALUE_COLOR, VALUE_SCALE);
            by += BAR_H + BAR_GAP;
            drawn++;
        }

        if (isLeader(uuid)) {
            int crownW = 9;
            int crownH = 7;
            int ax = x + scaledW - crownW - 8;
            int ay = y + 4;
            drawTextureScaled(ctx, LEADER_ICON, ax, ay, crownW, crownH, crownW, crownH, 1f);
        }
    }

    private void drawHead(DrawContext ctx, int x, int y, int size, UUID uuid, String fallbackName) {
        var player = getRenderablePlayer(uuid, fallbackName);
        var mc = MinecraftClient.getInstance();
        if (player == null || mc == null) return;

        PlayerSkinProvider provider = mc.getSkinProvider();
        var textures = provider.getSkinTextures(player.getGameProfile());
        Identifier skin = textures != null ? textures.texture() : DefaultSkinHelper.getTexture();

        TextureManager tm = mc.getTextureManager();
        AbstractTexture tex = tm.getTexture(skin);
        if (tex != null) tex.setFilter(false, false);

        int texW = 64, texH = 64;
        int headU = 8, headV = 8, headSize = 8;
        int hatU  = 40, hatV = 8, hatSize = 8;

        ctx.drawTexture(skin, x, y, size, size, headU, headV, headSize, headSize, texW, texH);
        ctx.drawTexture(skin, x, y, size, size, hatU, hatV, hatSize, hatSize, texW, texH);
    }

    private void drawSoftBg(DrawContext ctx, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;

        int bg = 0xA0101010;
        ctx.fill(x, y, x + w, y + h, bg);

        drawTextureScaled(ctx, SIMPLE_OUTLINE, x, y, w, h,
                SIMPLE_OUTLINE_TEX_W, SIMPLE_OUTLINE_TEX_H, 1f);
    }
}
