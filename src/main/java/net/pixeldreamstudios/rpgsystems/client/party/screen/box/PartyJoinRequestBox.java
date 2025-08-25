package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyJoinRequests;
import net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
@Environment(EnvType.CLIENT)
public final class PartyJoinRequestBox implements PartyBox {
    private static final Identifier TEX_JR_ACC_NORMAL = Identifier.of("rpg-systems","textures/gui/button/party_joinreq_accept_normal.png");
    private static final Identifier TEX_JR_ACC_HOVER  = Identifier.of("rpg-systems","textures/gui/button/party_joinreq_accept_hover.png");
    private static final Identifier TEX_JR_DEC_NORMAL = Identifier.of("rpg-systems","textures/gui/button/party_joinreq_decline_normal.png");
    private static final Identifier TEX_JR_DEC_HOVER  = Identifier.of("rpg-systems","textures/gui/button/party_joinreq_decline_hover.png");
    private static final int TL_BOX_W = 120;
    private static final int TL_BOX_MARGIN = 8;
    private static final int TL_BOX_OFFSET_Y = 15;
    private static final int GAP_FROM_LEFT_COLUMN = 12;
    private static final int SHIFT_LEFT = 60;
    private static final int LEFT_OF_INVITES_W = 255;
    private static final int GAP_FROM_CHAT = 6;
    private static final int BOX_W = 66;
    private static final int BOX_H = 120;
    private static final int INVITE_BOX_Y_OFFSET = -46;
    private static final int BELOW_INVITES_GAP   = 8;
    private static final int   PAD          = 3;
    private static final float TITLE_SCALE  = 0.50f;
    private static final float ROW_SCALE    = 0.50f;
    private static final boolean DEBUG_BOX = false;
    private static final int BG = 0x402196F3;
    private static final int BORDER = 0xFF2196F3;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int ROW_COLOR = 0xFFEFEFEF;
    private static final int EMPTY_COLOR = 0xFFBEBEBE;
    private static final int BUBBLE_PAD_H = 3;
    private static final int BUBBLE_PAD_V = 2;
    private static final int BUBBLE_GAP   = 3;
    private static final int BUBBLE_RADIUS = 3;
    private static final int BUBBLE_BG = 0x90333333;
    private static final int BTN_W = 14;
    private static final int BTN_H = 14;
    private static final int BTN_GAP = 2;
    private int boxX, boxY, boxW, boxH;
    private int listLeft, listTop, listRight, listBottom;
    private int rowH, textHScaled, bubbleTextMax;
    private int scrollPx = 0;

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH) {
        int contentLeft = canvasX + TL_BOX_MARGIN + 18;
        int infoTop = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int xLeftCol = contentLeft + TL_BOX_W;

        boxX = xLeftCol + GAP_FROM_LEFT_COLUMN - SHIFT_LEFT + LEFT_OF_INVITES_W + GAP_FROM_CHAT - 1;
        int invitesBoxY = infoTop + PartyMemberInfoBox.getPreferredHeight() + 4 + INVITE_BOX_Y_OFFSET;
        boxY = invitesBoxY + BOX_H + BELOW_INVITES_GAP;
        boxW = BOX_W;
        boxH = BOX_H;

        if (DEBUG_BOX) {
            ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, BG);
            drawBorder(ctx, boxX, boxY, boxW, boxH, BORDER);
        }

        String title = "Join Requests";
        int titleY = boxY + PAD;
        int titleX = centeredScaledTextX(tr, title, boxX, boxW, TITLE_SCALE);
        drawScaledText(ctx, tr, title, titleX, titleY, TITLE_COLOR, TITLE_SCALE);

        textHScaled = Math.round(tr.fontHeight * ROW_SCALE);
        rowH = Math.max(6, textHScaled + 2 * BUBBLE_PAD_V);

        listLeft   = boxX + PAD;
        listRight  = boxX + boxW - PAD;
        listTop    = titleY + textHScaled + 4;
        listBottom = boxY + boxH - PAD;

        int buttonsTotalW = BTN_W * 2 + BTN_GAP + BUBBLE_PAD_H;
        bubbleTextMax = Math.max(0, (listRight - listLeft) - BUBBLE_PAD_H - buttonsTotalW);

        List<ClientPartyJoinRequests.Req> reqs = visibleRequests();

        int totalH = Math.max(0, reqs.size() * (rowH + BUBBLE_GAP) - (reqs.isEmpty() ? 0 : BUBBLE_GAP));
        int visibleH = Math.max(0, listBottom - listTop);
        int maxScroll = Math.max(0, totalH - visibleH);
        if (scrollPx > maxScroll) scrollPx = maxScroll;
        if (scrollPx < 0) scrollPx = 0;

        ctx.enableScissor(listLeft, listTop, listRight, listBottom);

        if (reqs.isEmpty()) {
            String empty = "No requests";
            int tw = Math.round(tr.getWidth(empty) * ROW_SCALE);
            drawScaledText(ctx, tr, empty,
                    boxX + (boxW - tw) / 2,
                    listTop + Math.max(0, (visibleH - textHScaled) / 2),
                    EMPTY_COLOR, ROW_SCALE);
        } else {
            int y = listTop - scrollPx;
            int bubbleW = listRight - listLeft;

            for (ClientPartyJoinRequests.Req r : reqs) {
                int bx = listLeft;
                int by = y;

                if (by + rowH >= listTop && by <= listBottom - 1) {
                    drawSoftRounded(ctx, bx, by, bubbleW, rowH, BUBBLE_RADIUS, BUBBLE_BG);

                    String name = r.requesterName == null ? "Unknown" : r.requesterName;
                    String shown = ellipsize(tr, name, bubbleTextMax, ROW_SCALE);

                    int ty = by + (rowH - textHScaled) / 2;
                    drawScaledText(ctx, tr, shown, bx + BUBBLE_PAD_H, ty + 1, ROW_COLOR, ROW_SCALE);

                    int btnH = BTN_H;
                    int accX = bx + bubbleW - BUBBLE_PAD_H - BTN_W;
                    int decX = accX - (BTN_W + BTN_GAP);
                    int bY   = by + (rowH - btnH) / 2 - 1;

                    boolean overDec = isMouseOver(decX, bY, BTN_W, btnH);
                    boolean overAcc = isMouseOver(accX, bY, BTN_W, btnH);

                    final float s = 0.5f;

                    {
                        int ox = decX + Math.round((BTN_W - BTN_W * s) / 2f);
                        int oy = bY   + Math.round((BTN_H - BTN_H * s) / 2f);
                        var m2 = ctx.getMatrices();
                        m2.push();
                        m2.translate(ox, oy, 0);
                        m2.scale(s, s, 1f);
                        ctx.drawTexture(overDec ? TEX_JR_DEC_HOVER : TEX_JR_DEC_NORMAL, 10, 0, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
                        m2.pop();
                    }

                    {
                        int ox = accX + Math.round((BTN_W - BTN_W * s) / 2f);
                        int oy = bY   + Math.round((BTN_H - BTN_H * s) / 2f);
                        var m2 = ctx.getMatrices();
                        m2.push();
                        m2.translate(ox, oy, 0);
                        m2.scale(s, s, 1f);
                        ctx.drawTexture(overAcc ? TEX_JR_ACC_HOVER : TEX_JR_ACC_NORMAL, 0, 0, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
                        m2.pop();
                    }


                }

                y += rowH + BUBBLE_GAP;
                if (y > listBottom) break;
            }
        }

        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        if (!isMouseOver(boxX, boxY, boxW, boxH)) return false;

        List<ClientPartyJoinRequests.Req> reqs = visibleRequests();
        int y = listTop - scrollPx;
        int bubbleW = listRight - listLeft;

        for (ClientPartyJoinRequests.Req r : reqs) {
            int bx = listLeft;
            int by = y;

            if (by + rowH >= listTop && by <= listBottom - 1) {
                int btnH = Math.max(6, textHScaled + 2);
                int accX = bx + bubbleW - BUBBLE_PAD_H - BTN_W;
                int decX = accX - BTN_GAP - BTN_W;
                int bY   = by + (rowH - btnH) / 2;

                if (isMouseOver(decX, bY, BTN_W, btnH)) {
                    respond(r, false);
                    ClientPartyJoinRequests.removeLocal(r.requesterUuid);
                    return true;
                }
                if (isMouseOver(accX, bY, BTN_W, btnH)) {
                    respond(r, true);
                    ClientPartyJoinRequests.removeLocal(r.requesterUuid);
                    return true;
                }

            }

            y += rowH + BUBBLE_GAP;
            if (y > listBottom) break;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double ha, double va,
                                 int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        if (!isMouseOver(listLeft, listTop, listRight - listLeft, listBottom - listTop)) return false;

        int totalH = Math.max(0, visibleRequests().size() * (rowH + BUBBLE_GAP) - BUBBLE_GAP);
        int visibleH = Math.max(0, listBottom - listTop);
        int maxScroll = Math.max(0, totalH - visibleH);

        int step = rowH + BUBBLE_GAP;
        scrollPx = Math.max(0, Math.min(maxScroll, scrollPx + (int)Math.round(va * step)));
        return true;
    }

    private static void respond(ClientPartyJoinRequests.Req r, boolean accept) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null || r == null) return;

        ClientPlayNetworking.send(new PartyJoinRequestPayloads.JoinReqRespond(r.partyId, r.requesterUuid, accept));

    }

    private static String ellipsize(TextRenderer tr, String s, int maxPixels, float scale) {
        if (Math.round(tr.getWidth(s) * scale) <= maxPixels) return s;
        String dots = "...";
        int dw = Math.round(tr.getWidth(dots) * scale);
        StringBuilder b = new StringBuilder(s);
        while (b.length() > 0 && Math.round(tr.getWidth(b.toString()) * scale) + dw > maxPixels) {
            b.deleteCharAt(b.length() - 1);
        }
        return b + dots;
    }

    private static int centeredScaledTextX(TextRenderer tr, String text, int boxX, int boxW, float scale) {
        return Math.round(boxX + (boxW - tr.getWidth(text) * scale) / 2f);
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0);
    }

    private static void drawSoftRounded(DrawContext ctx, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(2, Math.min(r, Math.min(w / 2, h / 2)));
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, argb);
        ctx.fill(x + r - 1, y, x + w - (r - 1), y + 1, argb);
        ctx.fill(x + r - 1, y + h - 1, x + w - (r - 1), y + h, argb);
        ctx.fill(x, y + r - 1, x + 1, y + h - (r - 1), argb);
        ctx.fill(x + w - 1, y + r - 1, x + w, y + h - (r - 1), argb);
    }

    private static boolean isMouseOver(int x, int y, int w, int h) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private static List<ClientPartyJoinRequests.Req> visibleRequests() {
        Set<UUID> memberIds = ClientPartyHudData.members().stream()
                .map(m -> m.uuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!memberIds.isEmpty()) {
            ClientPartyJoinRequests.pruneByPartyMembers(memberIds);
        }
        return ClientPartyJoinRequests.pending();
    }

}
