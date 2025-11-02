package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.FTBTeamsCommandHelper;
import net.pixeldreamstudios.rpgsystems.network.party.PartySettingsPayloads;

import java.util.LinkedHashMap;
import java.util.Map;
@Environment(EnvType.CLIENT)
public final class PartyInfoBox implements PartyBox {

    private static final Identifier DIVIDER       = Identifier.of("rpg-systems", "textures/gui/divider.png");
    private static final Identifier LEAVE_NORMAL  = Identifier.of("rpg-systems", "textures/gui/button/leave_normal.png");
    private static final Identifier LEAVE_HOVER   = Identifier.of("rpg-systems", "textures/gui/button/leave_hover.png");
    private static final Identifier SETTINGS_BUTTON = Identifier.of("rpg-systems", "textures/gui/button/settings_button.png");

    private static final boolean DEBUG_BOXES = false;
    private static final boolean DEBUG_GEAR_SLOT = false;

    private static final int TL_BOX_W = 66;
    private static final int TL_BOX_H = 40;
    private static final int TL_BOX_MARGIN = 8;
    private static final int TL_BOX_OFFSET_Y = 15;
    private static final float TEXT_SCALE = 0.5f;
    private static final int BTN_W = 8;
    private static final int BTN_H = 8;
    private static final int BTN_GAP = 2;
    private static final int GEAR_W = 16;
    private static final int GEAR_H = 16;
    private static final int GEAR_PAD = 2;
    private int gearX = -1, gearY = -1, gearW = 0, gearH = 0;
    private static boolean filterSet = false;
    private static boolean showSettings = false;
    private static final float SETTINGS_TEXT_SCALE = 0.55f;
    private static final int SETTINGS_BG = 0xA0000000;
    private static final int SETTINGS_BORDER = 0x80FFFFFF;
    private static final int SETTINGS_CHECK_BG = 0xFF2A2A2A;
    private static final int SETTINGS_CHECK_TICK = 0xFF21C35E;
    private static final int SETTINGS_ROW_H = 12;

    private static final String OPT_HELPFUL_NONMEMBERS = "Heal/Buff non-members";
    private static final String OPT_IGNORE_COLLISION   = "Ignore party collision";
    private static final LinkedHashMap<String, Boolean> SETTINGS = new LinkedHashMap<>();
    static {
        SETTINGS.put(OPT_HELPFUL_NONMEMBERS, ClientPartyHudData.allowHelpfulNonMembers());
        SETTINGS.put(OPT_IGNORE_COLLISION,   ClientPartyHudData.ignorePartyCollision());
    }
    private int overlayX = 0, overlayY = 0, overlayW = 0, overlayH = 0;
    private static final int GEAR_SPIN_DURATION_MS = 300;
    private static long gearAnimStartMs = 0L;
    private static int gearAnimDir = 0;
    private static float gearAnimAngle = 0f;

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH) {
        ensureFilters();

        int x = canvasX + TL_BOX_MARGIN + 18;
        int y = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int w = TL_BOX_W;
        int h = TL_BOX_H;

        if (DEBUG_BOXES) {
            ctx.fill(x, y, x + w, y + h, 0x402196F3);
            drawBorder(ctx, x, y, w, h, 0xFF2196F3);
        }

        String partyNameRaw = (ClientPartyHudData.partyName != null && !ClientPartyHudData.partyName.isBlank())
                ? ClientPartyHudData.partyName
                : "Party";
        String leaderNameRaw = getLeaderName();

        float s = TEXT_SCALE;
        int fontH = tr.fontHeight;
        int scaledFontH = Math.round(fontH * s);
        int dividerH = 2;

        String partyName = ellipsizeScaled(tr, partyNameRaw, w - 4, s);
        int partyX = centeredScaledTextX(tr, partyName, x, w, s);

        int topLineY   = y + (h - (scaledFontH + 2 + dividerH + 2 + scaledFontH)) / 2;
        int partyY     = topLineY;

        drawScaledText(ctx, tr, partyName, partyX, partyY - 5, 0xFFFFFFFF, s);

        int divW = w - 16;
        int divX = x + (w - divW) / 2;
        int divY = partyY + scaledFontH + 2;
        ctx.drawTexture(DIVIDER, divX, divY, 0, 0, divW, dividerH + 3, divW, dividerH + 3);

        int leaderTextAreaW = w - (BTN_GAP + BTN_W);
        String leaderName = ellipsizeScaled(tr, leaderNameRaw, leaderTextAreaW - 4, s);
        int leaderTextX = centeredScaledTextXWithinWidth(tr, leaderName, x, leaderTextAreaW, s);
        int leaderTextY = divY + dividerH + 2 + 5;

        drawScaledText(ctx, tr, leaderName, leaderTextX, leaderTextY, 0xFFEFEFEF, s);

        int leaderTextWpx = Math.round(tr.getWidth(leaderName) * s);
        int btnX = leaderTextX + leaderTextWpx + BTN_GAP;
        int btnTopBaseline = leaderTextY;
        int btnY = btnTopBaseline + (scaledFontH - BTN_H) / 2;

        if (btnX + BTN_W > x + w) btnX = x + w - BTN_W;
        boolean hoverLeave = isMouseOver(btnX, btnY, BTN_W, BTN_H);
        ctx.drawTexture(hoverLeave ? LEAVE_HOVER : LEAVE_NORMAL, btnX, btnY, 0, 0, 4, 7, 4, 7);

        if (gearAnimDir != 0) {
            long now = System.currentTimeMillis();
            float t = (now - gearAnimStartMs) / (float) GEAR_SPIN_DURATION_MS;
            if (t >= 1f) {
                gearAnimAngle = 0f;
                gearAnimDir = 0;
            } else {
                gearAnimAngle = gearAnimDir * 360f * t;
            }
        }

        gearW = GEAR_W; gearH = GEAR_H;
        gearX = x + w - GEAR_PAD - gearW;
        int desiredGearY = btnY + (BTN_H - gearH) / 2;
        gearY = Math.max(y + GEAR_PAD, Math.min(y + h - GEAR_PAD - gearH, desiredGearY));

        if (DEBUG_GEAR_SLOT) {
            ctx.fill(gearX, gearY, gearX + gearW, gearY + gearH, 0x4000FF00);
            drawBorder(ctx, gearX, gearY, gearW, gearH, 0xFF00FF00);
        }

        var m = ctx.getMatrices();
        m.push();
        m.translate(gearX + gearW / 2f, gearY + gearH / 2f, 0f);
        if (gearAnimAngle != 0f) {
            m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(gearAnimAngle));
        }
        m.scale(0.75f, 0.75f, 1f);
        ctx.drawTexture(SETTINGS_BUTTON, -gearW / 2, -gearH / 2, 0, 0, gearW, gearH, 16, 16);
        m.pop();

        SETTINGS.put(OPT_HELPFUL_NONMEMBERS, ClientPartyHudData.allowHelpfulNonMembers());
        SETTINGS.put(OPT_IGNORE_COLLISION,   ClientPartyHudData.ignorePartyCollision());

        if (showSettings) {
            int rows = Math.max(1, SETTINGS.size());
            int innerPad = 6;
            int rowGap = 2;
            int listH = rows * SETTINGS_ROW_H + (rows - 1) * rowGap;
            int titleH = 12;
            int boxW = 158;
            int boxH = innerPad + titleH + 4 + listH + innerPad;

            int bx = Math.min(canvasX + canvasW - TL_BOX_MARGIN - boxW, Math.max(x, gearX + gearW + 4));
            int by = Math.max(canvasY + TL_BOX_MARGIN, Math.min(y, canvasY + canvasH - TL_BOX_MARGIN - boxH));

            by = Math.min(by + 40, canvasY + canvasH - TL_BOX_MARGIN - boxH);

            overlayX = bx; overlayY = by; overlayW = boxW; overlayH = boxH;

            ctx.fill(bx, by, bx + boxW, by + boxH, SETTINGS_BG);
            drawBorder(ctx, bx, by, boxW, boxH, SETTINGS_BORDER);

            drawScaledText(ctx, tr, "Settings", bx + innerPad, by + innerPad, 0xFFFFFFFF, SETTINGS_TEXT_SCALE);

            int listTop = by + innerPad + titleH + 4;
            int cx0 = bx + innerPad;
            int tx0 = cx0 + 12 + 6;
            int yRow = listTop;

            for (Map.Entry<String, Boolean> e : SETTINGS.entrySet()) {
                int cbX = cx0, cbY = yRow + (SETTINGS_ROW_H - 10) / 2, cbS = 10;
                ctx.fill(cbX, cbY, cbX + cbS, cbY + cbS, SETTINGS_CHECK_BG);
                drawBorder(ctx, cbX, cbY, cbS, cbS, 0xFFFFFFFF);

                if (Boolean.TRUE.equals(e.getValue())) {
                    ctx.fill(cbX + 2, cbY + 2, cbX + cbS - 2, cbY + cbS - 2, SETTINGS_CHECK_TICK);
                }

                drawScaledText(ctx, tr, e.getKey(), tx0, yRow + 3, 0xFFEFEFEF, SETTINGS_TEXT_SCALE);
                yRow += SETTINGS_ROW_H + 2;
            }
        } else {
            overlayW = overlayH = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {

        if (showSettings) {
            boolean clickedInside = (mouseX >= overlayX && mouseX < overlayX + overlayW && mouseY >= overlayY && mouseY < overlayY + overlayH);

            if (!clickedInside) {
                showSettings = false;
                startGearSpin(-1);
                return true;
            }

            int innerPad = 6;
            int titleH = 12;
            int rowGap = 2;
            int listTop = overlayY + innerPad + titleH + 4;
            int yRow = listTop;

            for (Map.Entry<String, Boolean> e : SETTINGS.entrySet()) {
                int rowTop = yRow;
                int rowBottom = yRow + SETTINGS_ROW_H;
                if (mouseY >= rowTop && mouseY < rowBottom) {
                    boolean currentVal = Boolean.TRUE.equals(e.getValue());
                    boolean newVal = !currentVal;

                    boolean leader = isLocalPlayerLeader();

                    if (OPT_HELPFUL_NONMEMBERS.equals(e.getKey())) {
                        if (ClientPartyHudData.partyId != null) {
                            ClientPlayNetworking.send(new PartySettingsPayloads.SetAllowHelpfulNonMembers(newVal));
                        }
                    } else if (OPT_IGNORE_COLLISION.equals(e.getKey())) {
                        if (ClientPartyHudData.partyId != null) {
                            ClientPlayNetworking.send(new PartySettingsPayloads.SetIgnorePartyCollision(newVal));
                        }
                    }

                    if (leader) {
                        SETTINGS.put(e.getKey(), newVal);
                    }

                    return true;
                }
                yRow += SETTINGS_ROW_H + rowGap;
            }
            return true;
        }

        int x = canvasX + TL_BOX_MARGIN + 18;
        int y = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int w = TL_BOX_W;
        int h = TL_BOX_H;

        float s = TEXT_SCALE;
        int fontH = tr.fontHeight;
        int scaledFontH = Math.round(fontH * s);
        int dividerH = 2;

        String leaderNameRaw = getLeaderName();

        int topLineY   = y + (h - (scaledFontH + 2 + dividerH + 2 + scaledFontH)) / 2;
        int divY       = topLineY + scaledFontH + 2;

        int leaderTextAreaW = w - (BTN_GAP + BTN_W);
        String leaderName = ellipsizeScaled(tr, leaderNameRaw, leaderTextAreaW - 4, s);
        int leaderTextX = centeredScaledTextXWithinWidth(tr, leaderName, x, leaderTextAreaW, s);
        int leaderTextY = divY + dividerH + 2 + 5;

        int leaderTextWpx = Math.round(tr.getWidth(leaderName) * s);
        int btnX = leaderTextX + leaderTextWpx + BTN_GAP;
        int btnY = leaderTextY + (scaledFontH - BTN_H) / 2;
        if (btnX + BTN_W > x + w) btnX = x + w - BTN_W;

        gearW = GEAR_W; gearH = GEAR_H;
        gearX = x + w - GEAR_PAD - gearW;
        int desiredGearY = btnY + (BTN_H - gearH) / 2;
        gearY = Math.max(y + GEAR_PAD, Math.min(y + h - GEAR_PAD - gearH, desiredGearY));

        if (mouseX >= gearX && mouseX < gearX + gearW && mouseY >= gearY && mouseY < gearY + gearH) {
            boolean opening = !showSettings;
            showSettings = opening;
            startGearSpin(opening ? +1 : -1);
            return true;
        }

        if (isMouseOver(btnX, btnY, BTN_W, BTN_H)) {
            var mc = MinecraftClient.getInstance();
            FTBTeamsCommandHelper.sendLeaveCommand();
            if (mc != null) mc.setScreen(null);
            return true;
        }
        return false;
    }

    private static void startGearSpin(int dir) {
        gearAnimDir = dir;
        gearAnimStartMs = System.currentTimeMillis();
        gearAnimAngle = 0f;
    }

    private static boolean isLocalPlayerLeader() {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;
        if (ClientPartyHudData.leaderUuid == null) return true;
        return mc.player.getUuid().equals(ClientPartyHudData.leaderUuid);
    }

    private static void ensureFilters() {
        if (filterSet) return;
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        for (Identifier id : new Identifier[]{DIVIDER, LEAVE_NORMAL, LEAVE_HOVER, SETTINGS_BUTTON}) {
            AbstractTexture tex = tm.getTexture(id);
            if (tex != null) tex.setFilter(false, false);
        }
        filterSet = true;
    }

    private static String ellipsizeScaled(TextRenderer tr, String s, int maxPx, float scale) {
        if (Math.round(tr.getWidth(s) * scale) <= maxPx) return s;
        String dots = "...";
        int dw = Math.round(tr.getWidth(dots) * scale);
        StringBuilder b = new StringBuilder(s);
        while (b.length() > 0 && Math.round(tr.getWidth(b.toString()) * scale) + dw > maxPx) {
            b.deleteCharAt(b.length() - 1);
        }
        return b + dots;
    }

    private static int centeredScaledTextX(TextRenderer tr, String text, int boxX, int boxW, float scale) {
        return Math.round(boxX + (boxW - tr.getWidth(text) * scale) / 2f);
    }
    private static int centeredScaledTextXWithinWidth(TextRenderer tr, String text, int leftX, int width, float scale) {
        return Math.round(leftX + (width - tr.getWidth(text) * scale) / 2f);
    }

    private String getLeaderName() {
        if (ClientPartyHudData.leaderUuid != null) {
            for (var m : ClientPartyHudData.members()) {
                if (m.uuid.equals(ClientPartyHudData.leaderUuid)) return m.name;
            }
        }
        var mc = MinecraftClient.getInstance();
        return (mc != null && mc.player != null) ? mc.player.getName().getString() : "";
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static boolean isMouseOver(int x, int y, int w, int h) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }
}
