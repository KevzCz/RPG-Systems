package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyInvites;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
@Environment(EnvType.CLIENT)
public final class PartyInviteBox implements PartyBox {
    private static final int TL_BOX_W = 120;
    private static final int TL_BOX_MARGIN = 8;
    private static final int TL_BOX_OFFSET_Y = 15;
    private static final int GAP_FROM_LEFT_COLUMN = 12;
    private static final int SHIFT_LEFT = 60;
    private static final int LEFT_OF_INVITES_W = 255;
    private static final int GAP_FROM_CHAT = 6;
    private static final int BOX_W = 66;
    private static final int BOX_H = 120;
    private static final int   PAD          = 3;
    private static final float TITLE_SCALE  = 0.50f;
    private static final float ROW_SCALE    = 0.50f;
    private static final int BG = 0x402196F3;
    private static final int BORDER = 0xFF2196F3;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int ROW_COLOR = 0xFFEFEFEF;
    private static final int EMPTY_COLOR = 0xFFBEBEBE;
    private static final int BTN_W = 14;
    private static final int BTN_H = 14;
    private static final int INPUT_BG = 0x40101010;
    private static final int INPUT_BORDER = 0x80FFFFFF;
    private static final int LIST_BG = 0xE0101010;
    private static final int LIST_BORDER = 0x90FFFFFF;
    private static final int HOVER_BG = 0x8033AAFF;
    private static final int BUTTON_BG = 0x803498DB;
    private static final int BUTTON_BG_HOVER = 0xA03498DB;
    private static final String PLACEHOLDER = "Select…";
    private static final int BUBBLE_PAD_H = 3;
    private static final int BUBBLE_PAD_V = 2;
    private static final int BUBBLE_GAP   = 3;
    private static final int BUBBLE_RADIUS = 3;
    private static final int BUBBLE_BG = 0x90333333;
    private static final long INVITE_TTL_MS = 60_000L;
    private boolean dropdownOpen = false;
    private String selectedName = null;
    private List<String> candidates = List.of();
    private int listScroll = 0;
    private int invitesScrollPx = 0;
    private int boxX, boxY, boxW, boxH;
    private int ddX, ddY, ddW, ddH;
    private int btnX, btnY, btnW, btnH;
    private int listX, listY, listW, listH;
    private int listRowH;
    private static final Identifier TEX_SEND_NORMAL = Identifier.of("rpg-systems","textures/gui/button/party_invite_send_normal.png");
    private static final Identifier TEX_SEND_HOVER  = Identifier.of("rpg-systems","textures/gui/button/party_invite_send_hover.png");
    private int invitesLeft, invitesTop, invitesRight, invitesBottom;
    private int bubbleH, bubbleW, bubbleTextMax, textHScaled;

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH) {
        int contentLeft = canvasX + TL_BOX_MARGIN + 18;
        int infoTop = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int xLeftCol = contentLeft + TL_BOX_W;

        boxX = xLeftCol + GAP_FROM_LEFT_COLUMN - SHIFT_LEFT + LEFT_OF_INVITES_W + GAP_FROM_CHAT - 1;
        boxY = infoTop + PartyMemberInfoBox.getPreferredHeight() + 4 - 46;
        boxW = BOX_W;
        boxH = BOX_H;

        String title = "Invites Sent";
        int titleY = boxY + PAD;
        int titleX = centeredScaledTextX(tr, title, boxX, boxW, TITLE_SCALE);
        drawScaledText(ctx, tr, title, titleX, titleY, TITLE_COLOR, TITLE_SCALE);

        int titleHpx = Math.round(tr.fontHeight * TITLE_SCALE);
        int topAfterTitle = titleY + titleHpx + 4;

        textHScaled = Math.round(tr.fontHeight * ROW_SCALE);

        final float BTN_VIS_SCALE = 0.5f;
        int scaledBtnH = Math.max(6, Math.round(BTN_H * BTN_VIS_SCALE));

        ddH = Math.max(scaledBtnH, textHScaled + 2);
        ddX = boxX + PAD;
        ddY = topAfterTitle;
        ddW = Math.max(10, (boxX + boxW - PAD - BTN_W - 2) - ddX);

        btnW = BTN_W;
        btnH = BTN_H;
        btnX = boxX + boxW - PAD - btnW;
        btnY = ddY + (ddH - BTN_H) / 2 - 1;

        ctx.fill(ddX, ddY, ddX + ddW, ddY + ddH, INPUT_BG);
        drawBorder(ctx, ddX, ddY, ddW, ddH, INPUT_BORDER);

        String shown = (selectedName == null || selectedName.isBlank()) ? PLACEHOLDER : selectedName;
        int shownColor = (selectedName == null) ? EMPTY_COLOR : ROW_COLOR;
        drawScaledText(ctx, tr, ellipsize(tr, shown, ddW - 4, ROW_SCALE),
                ddX + 2, ddY + (ddH - textHScaled) / 2 + 1, shownColor, ROW_SCALE);

        boolean hoverBtn = isMouseOver(btnX, btnY, btnW, btnH);
        int ox = btnX + Math.round((btnW - btnW * BTN_VIS_SCALE) / 2f);
        int oy = btnY + Math.round((btnH - btnH * BTN_VIS_SCALE) / 2f);
        var m = ctx.getMatrices();
        m.push();
        m.translate(ox, oy, 0);
        m.scale(BTN_VIS_SCALE, BTN_VIS_SCALE, 1f);
        ctx.drawTexture(hoverBtn ? TEX_SEND_HOVER : TEX_SEND_NORMAL, 0, 0, 0, 0, btnW, btnH, btnW, btnH);
        m.pop();

        int listTopFixed = ddY + ddH + 3;
        invitesLeft   = boxX + PAD;
        invitesRight  = boxX + boxW - PAD;
        invitesTop    = listTopFixed;
        invitesBottom = boxY + boxH - PAD;

        bubbleW = Math.max(0, invitesRight - invitesLeft);
        bubbleTextMax = Math.max(0, bubbleW - 2 * BUBBLE_PAD_H);
        bubbleH = Math.max(6, textHScaled + 2 * BUBBLE_PAD_V);

        List<ClientPartyInvites.Sent> sent = visibleInvites();
        long now = System.currentTimeMillis();

        int visibleH = Math.max(0, invitesBottom - invitesTop);
        int count = sent.size();
        int totalH = count == 0 ? 0 : (count * (bubbleH + BUBBLE_GAP) - BUBBLE_GAP);
        int maxScrollPx = Math.max(0, totalH - visibleH);

        if (invitesScrollPx > maxScrollPx) invitesScrollPx = maxScrollPx;
        if (invitesScrollPx < 0) invitesScrollPx = 0;

        ctx.enableScissor(invitesLeft, invitesTop, invitesRight, invitesBottom);

        if (sent.isEmpty()) {
            String empty = "No invites sent";
            int tw = Math.round(tr.getWidth(empty) * ROW_SCALE);
            drawScaledText(ctx, tr, empty,
                    boxX + (boxW - tw) / 2,
                    invitesTop + Math.max(0, (visibleH - textHScaled) / 2),
                    EMPTY_COLOR, ROW_SCALE);
        } else {
            int yCursor = invitesTop - invitesScrollPx;
            for (ClientPartyInvites.Sent s : sent) {
                int bx = invitesLeft;
                int by = yCursor;

                if (by + bubbleH >= invitesTop && by <= invitesBottom - 1) {
                    drawSoftRounded(ctx, bx, by, bubbleW, bubbleH, BUBBLE_RADIUS, BUBBLE_BG);

                    long rem = Math.max(0L, INVITE_TTL_MS - (now - s.createdAt));
                    String timeTxt = formatMmSs(rem);
                    int timeW = Math.round(tr.getWidth(timeTxt) * ROW_SCALE);

                    int nameMaxPx = Math.max(0, bubbleTextMax - timeW - 4);
                    String nameTxt = ellipsize(tr, (s.targetName == null ? "Unknown" : s.targetName), nameMaxPx, ROW_SCALE);

                    int txName = bx + BUBBLE_PAD_H;
                    int ty     = by + (bubbleH - textHScaled) / 2;
                    drawScaledText(ctx, tr, nameTxt, txName, ty, ROW_COLOR, ROW_SCALE);

                    int txTime = bx + bubbleW - BUBBLE_PAD_H - Math.round(timeW);
                    drawScaledText(ctx, tr, timeTxt, txTime, ty, ROW_COLOR, ROW_SCALE);
                }

                yCursor += bubbleH + BUBBLE_GAP;
                if (yCursor > invitesBottom) break;
            }
        }

        ctx.disableScissor();

        candidates = getInvitableNames();
        if (selectedName != null && candidates.stream().noneMatch(n -> n.equalsIgnoreCase(selectedName))) {
            selectedName = null;
        }

        if (dropdownOpen) {
            int textH = Math.round(tr.fontHeight * ROW_SCALE);
            listRowH = Math.max(6, textH + 2);
            int maxRows = Math.max(3, Math.min(8, (boxY + boxH - PAD) - (ddY + ddH + 2)) / listRowH);
            listX = ddX;
            listY = ddY + ddH + 2;
            listW = ddW;
            listH = Math.min(candidates.size(), maxRows) * listRowH + 2;

            ctx.fill(listX, listY, listX + listW, listY + listH, LIST_BG);
            drawBorder(ctx, listX, listY, listW, listH, LIST_BORDER);

            int maxScroll = Math.max(0, candidates.size() - maxRows);
            if (listScroll > maxScroll) listScroll = maxScroll;
            if (listScroll < 0) listScroll = 0;

            ctx.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);

            int drawY = listY + 1;
            for (int i = 0; i < Math.min(maxRows, candidates.size() - listScroll); i++) {
                int idx = listScroll + i;
                String name = candidates.get(idx);
                int rowTop = drawY + i * listRowH;
                boolean hover = isMouseOver(listX + 1, rowTop, listW - 2, listRowH);
                if (hover) ctx.fill(listX + 1, rowTop, listX + listW - 1, rowTop + listRowH, HOVER_BG);
                drawScaledText(ctx, tr, ellipsize(tr, name, listW - 6, ROW_SCALE),
                        listX + 3, rowTop + (listRowH - textHScaled) / 2, 0xFFFFFFFF, ROW_SCALE);
            }

            ctx.disableScissor();
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        if (!isMouseOver(boxX, boxY, boxW, boxH)) return false;

        if (isMouseOver(btnX, btnY, btnW, btnH)) {
            if (selectedName != null && !selectedName.isBlank()) {
                sendInvite(selectedName);
                selectedName = null;
                dropdownOpen = false;
            }
            return true;
        }

        if (isMouseOver(ddX, ddY, ddW, ddH)) {
            dropdownOpen = !dropdownOpen;
            if (dropdownOpen) requestEligibleFromServer();
            return true;
        }

        if (dropdownOpen && isMouseOver(listX, listY, listW, listH)) {
            int relY = (int) mouseY - listY - 1;
            int idx = listScroll + (relY / listRowH);
            if (idx >= 0 && idx < candidates.size()) {
                selectedName = candidates.get(idx);
                dropdownOpen = false;
            }
            return true;
        }

        if (dropdownOpen) {
            dropdownOpen = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double ha, double va,
                                 int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {

        if (dropdownOpen && isMouseOver(listX, listY, listW, listH)) {
            int maxRows = Math.max(3, listH > 0 ? (listH - 2) / Math.max(1, listRowH) : 0);
            int maxScroll = Math.max(0, candidates.size() - maxRows);
            if (maxScroll > 0) listScroll = Math.max(0, Math.min(maxScroll, listScroll - (int) Math.signum(va)));
            return true;
        }

        if (isMouseOver(invitesLeft, invitesTop, invitesRight - invitesLeft, invitesBottom - invitesTop)) {
            int step = bubbleH + BUBBLE_GAP;
            int count = visibleInvites().size();
            int totalH = Math.max(0, (count * (bubbleH + BUBBLE_GAP)) - BUBBLE_GAP);
            int visibleH = Math.max(0, invitesBottom - invitesTop);
            int maxScrollPx = Math.max(0, totalH - visibleH);
            invitesScrollPx = Math.max(0, Math.min(maxScrollPx, invitesScrollPx + (int) Math.round(va * step)));
            return true;
        }

        return false;
    }


    private static String formatMmSs(long ms) {
        long sec = Math.max(0, (ms + 999) / 1000);
        long m = sec / 60;
        long s = sec % 60;
        return m + ":" + (s < 10 ? "0" + s : String.valueOf(s));
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static int centeredScaledTextX(TextRenderer tr, String text, int boxX, int boxW, float scale) {
        return Math.round(boxX + (boxW - tr.getWidth(text) * scale) / 2f);
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

    private static List<String> getInvitableNames() {
        Set<String> eligible = ClientPartyInvites.eligibleNames();

        Set<String> alreadyInvited = ClientPartyInvites.sent().stream()
                .map(s -> s.targetName == null ? "" : s.targetName.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> myPartyLower = ClientPartyHudData.members().stream()
                .map(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> out = eligible.stream()
                .filter(n -> !alreadyInvited.contains(n.toLowerCase(Locale.ROOT)))
                .filter(n -> !myPartyLower.contains(n.toLowerCase(Locale.ROOT)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return out;
    }

    private static void sendInvite(String targetName) {
        if (targetName == null || targetName.isBlank()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendChatCommand("party invite " + targetName);

    }
    private static List<ClientPartyInvites.Sent> visibleInvites() {

        List<ClientPartyInvites.Sent> sent = new ArrayList<>(ClientPartyInvites.sent());
        Set<String> partyNamesLower = ClientPartyHudData.members().stream()
                .map(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!partyNamesLower.isEmpty()) {
            sent.removeIf(s -> s.targetName != null
                    && partyNamesLower.contains(s.targetName.toLowerCase(Locale.ROOT)));
        }
        return sent;
    }
    private static void requestEligibleFromServer() {
        var mc = MinecraftClient.getInstance();
        if (mc != null && mc.getNetworkHandler() != null) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads.EligibleInviteesRequest());
        }
    }


}
