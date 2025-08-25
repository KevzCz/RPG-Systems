package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
@Environment(EnvType.CLIENT)
public final class PartyMembersBox implements PartyBox {

    private static final boolean DEBUG_BOXES = false;
    private static final int TL_BOX_W = 66;
    private static final int TL_BOX_H = 40;
    private static final int TL_BOX_MARGIN = 8;
    private static final int TL_BOX_OFFSET_Y = 15;
    private static final int BELOW_GAP = 4;
    private static final float TEXT_SCALE = 0.5f;
    private static final int ROW_H = 12;
    private static final int CONTENT_PAD_X = 6;
    private static final int CONTENT_PAD_Y = 6;
    private static final int MAX_BOX_H = 210;
    private static final Identifier PARTY_LEADER    = Identifier.of("rpg-systems", "textures/gui/party_leader.png");
    private static final Identifier KICK_NORMAL     = Identifier.of("rpg-systems", "textures/gui/button/kick_normal.png");
    private static final Identifier KICK_HOVER      = Identifier.of("rpg-systems", "textures/gui/button/kick_hover.png");
    private static final Identifier OFFLINE_ICON  = Identifier.of("rpg-systems", "textures/gui/offline.png");
    private static final int ICON_W = 8;
    private static final int ICON_H = 8;
    private static final int ICON_PAD_RIGHT = 3;
    private static final int KICK_W = 8;
    private static final int KICK_H = 8;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_PAD_RIGHT = 2;
    private static final int SCROLLBAR_PAD_Y = 2;
    private static final int SCROLLBAR_TRACK_HEIGHT = -1;
    private static final int SCROLLBAR_THUMB_MIN = 10;
    private static final int SCROLLBAR_COLOR_TRACK = 0x40101010;
    private static final int SCROLLBAR_COLOR_THUMB = 0x90FFFFFF;
    private static final int CONTROL_H = 0;
    private static boolean texturesFiltered = false;
    private UUID selected = null;
    private int scrollPx = 0;
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0;

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH) {
        ensureNearestFilters();

        int infoX = canvasX + TL_BOX_MARGIN + 18;
        int infoY = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int infoW = TL_BOX_W;
        int infoH = TL_BOX_H;

        int x = infoX;
        int y = infoY + infoH + BELOW_GAP;
        int w = infoW;
        int bottomMargin = 8;
        int h = Math.min(MAX_BOX_H, Math.max(0, (canvasY + canvasH - bottomMargin) - y));

        if (DEBUG_BOXES) {
            ctx.fill(x, y, x + w, y + h, 0x404CAF50);
            drawBorder(ctx, x, y, w, h, 0xFF4CAF50);
        }

        List<ClientPartyHudData.Member> base = new ArrayList<>(ClientPartyHudData.members());
        base.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT)));

        int listTop = y + CONTENT_PAD_Y;
        int naturalBottom = y + h - CONTENT_PAD_Y - CONTROL_H;
        int minVisibleH = 16 * ROW_H;
        int listBottom = naturalBottom;
        int availableH = naturalBottom - listTop;
        if (availableH < minVisibleH) listBottom = Math.min(naturalBottom, listTop + minVisibleH);
        int visibleH = Math.max(0, listBottom - listTop);

        int totalRows = base.size();
        int totalH = totalRows * ROW_H;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollPx = clampToRow(snapToRow(scrollPx), 0, maxScroll);

        boolean showScrollbar = totalH > visibleH;

        int rightPadForScrollbar = showScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_PAD_RIGHT) : 0;

        int startRow = scrollPx / ROW_H;
        int drawY = listTop;
        UUID leaderUuid = effectiveLeaderUuid(base);
        UUID viewerUuid = null;
        var mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) viewerUuid = mc.player.getUuid();
        boolean viewerIsLeader = (leaderUuid != null && leaderUuid.equals(viewerUuid));

        for (int i = startRow; i < totalRows; i++) {
            if (drawY + ROW_H > listBottom) break;

            ClientPartyHudData.Member m = base.get(i);
            boolean isSel = selected != null && selected.equals(m.uuid);
            if (isSel) {
                ctx.fill(x + 2, drawY - 1, x + w - 2, drawY + ROW_H - 1, 0x803B89C9);
            }

            boolean isLeaderRow = leaderUuid != null && leaderUuid.equals(m.uuid);

            int rowContentRight = x + w - CONTENT_PAD_X - rightPadForScrollbar;
            int rightElemLeft = rowContentRight;
            int textTopY = drawY + 2;
            int nameStartX = x + CONTENT_PAD_X;
            if (isLeaderRow) {
                int iconX = rowContentRight - ICON_W;
                int iconY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - ICON_H) / 2f);
                ctx.drawTexture(PARTY_LEADER, iconX, iconY, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
                rightElemLeft = iconX;
            } else if (viewerIsLeader) {
                int btnX = rowContentRight - KICK_W;
                int btnY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - KICK_H) / 2f);
                boolean overKick = isMouseOver(btnX, btnY, KICK_W, KICK_H);
                ctx.drawTexture(overKick ? KICK_HOVER : KICK_NORMAL, btnX, btnY, 0, 0, KICK_W, KICK_H, KICK_W, KICK_H);
                rightElemLeft = btnX;
            }

            String levelLabel = (m.totalLevel >= 0) ? ("Lv. " + m.totalLevel) : null;
            int levelPad = 4;
            int nameRightLimit;

            if (!m.online) {
                int icoX = rightElemLeft - levelPad - ICON_W;
                int icoY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - ICON_H) / 2f);
                ctx.drawTexture(OFFLINE_ICON, icoX, icoY, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
                nameRightLimit = icoX - ICON_PAD_RIGHT;
            } else if (levelLabel != null) {
                int lvlWpx = Math.round(tr.getWidth(levelLabel) * TEXT_SCALE);
                int lvlX = rightElemLeft - levelPad - lvlWpx;
                int lvlY = textTopY;
                drawScaledText(ctx, tr, levelLabel, lvlX, lvlY, 0xFFEFEFEF, TEXT_SCALE);
                nameRightLimit = lvlX - ICON_PAD_RIGHT;
            } else {
                nameRightLimit = rightElemLeft - ICON_PAD_RIGHT;
            }

            int textMaxPixels = Math.max(0, nameRightLimit - nameStartX);
            String name = m.name == null ? "Unknown" : m.name;
            String clipped = ellipsize(tr, name, textMaxPixels / TEXT_SCALE);
            drawScaledText(ctx, tr, clipped, nameStartX, textTopY, 0xFFFFFFFF, TEXT_SCALE);

            drawY += ROW_H;
        }


        if (base.isEmpty()) {
            String none = "No members";
            int tw = Math.round(tr.getWidth(none) * TEXT_SCALE);
            int tx = x + (w - tw) / 2;
            int ty = y + (h - Math.round(tr.fontHeight * TEXT_SCALE)) / 2;
            drawScaledText(ctx, tr, none, tx, ty, 0xFFBEBEBE, TEXT_SCALE);
        }

        if (showScrollbar) {
            int trackX = x + w - SCROLLBAR_PAD_RIGHT - SCROLLBAR_WIDTH;

            int trackH = (SCROLLBAR_TRACK_HEIGHT > 0)
                    ? Math.min(SCROLLBAR_TRACK_HEIGHT, visibleH)
                    : visibleH;
            int trackTop = listTop + SCROLLBAR_PAD_Y;
            trackH = Math.max(0, trackH - 2 * SCROLLBAR_PAD_Y);

            ctx.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackTop + trackH, SCROLLBAR_COLOR_TRACK);

            int thumbH = Math.max(SCROLLBAR_THUMB_MIN, (int) Math.ceil(trackH * (visibleH / (double) totalH)));
            thumbH = Math.min(thumbH, trackH);
            int thumbY = trackTop;
            if (maxScroll > 0 && trackH > thumbH) {
                double ratio = scrollPx / (double) maxScroll;
                thumbY = trackTop + (int) Math.round((trackH - thumbH) * ratio);
            }
            ctx.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, SCROLLBAR_COLOR_THUMB);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        int infoX = canvasX + TL_BOX_MARGIN + 18;
        int infoY = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;

        int y = infoY + TL_BOX_H + BELOW_GAP;
        int w = TL_BOX_W;
        int bottomMargin = 8;
        int h = Math.min(MAX_BOX_H, Math.max(0, (canvasY + canvasH - bottomMargin) - y));

        int listTop = y + CONTENT_PAD_Y;
        int listBottom = y + h - CONTENT_PAD_Y - CONTROL_H;
        int minVisibleH = 16 * ROW_H;
        if ((listBottom - listTop) < minVisibleH) listBottom = Math.min(listBottom, listTop + minVisibleH);

        if (mouseX < infoX || mouseX >= infoX + w || mouseY < listTop || mouseY >= listBottom) return false;

        List<ClientPartyHudData.Member> base = new ArrayList<>(ClientPartyHudData.members());
        base.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT)));

        int visibleH = Math.max(0, listBottom - listTop);
        int totalH = base.size() * ROW_H;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollPx = clampToRow(snapToRow(scrollPx), 0, maxScroll);

        boolean showScrollbar = totalH > visibleH;
        if (showScrollbar) {
            int trackX = infoX + w - SCROLLBAR_PAD_RIGHT - SCROLLBAR_WIDTH;
            int trackTop = listTop + SCROLLBAR_PAD_Y;
            int trackH = (SCROLLBAR_TRACK_HEIGHT > 0)
                    ? Math.min(SCROLLBAR_TRACK_HEIGHT, visibleH)
                    : visibleH;
            trackH = Math.max(0, trackH - 2 * SCROLLBAR_PAD_Y);

            int thumbH = Math.max(SCROLLBAR_THUMB_MIN, (int) Math.ceil(trackH * (visibleH / (double) totalH)));
            thumbH = Math.min(thumbH, trackH);
            int thumbY = trackTop;
            if (maxScroll > 0 && trackH > thumbH) {
                thumbY = trackTop + (int) Math.round((trackH - thumbH) * (scrollPx / (double) maxScroll));
            }

            if (mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH && mouseY >= thumbY && mouseY < thumbY + thumbH) {
                draggingScrollbar = true;
                dragGrabOffsetY = (int) (mouseY - thumbY);
                return true;
            }

            if (mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH && mouseY >= trackTop && mouseY < trackTop + trackH) {
                int newThumbY = (int) mouseY - thumbH / 2;
                newThumbY = Math.max(trackTop, Math.min(trackTop + trackH - thumbH, newThumbY));
                double denom = Math.max(1.0, (trackH - thumbH));
                double ratio = (newThumbY - trackTop) / denom;
                scrollPx = clampToRow(snapToRow((int) Math.round(ratio * maxScroll)), 0, maxScroll);
                return true;
            }
        }
        int startRowForHit = scrollPx / ROW_H;
        int drawY = listTop;
        UUID leaderUuid = ClientPartyHudData.leaderUuid;

        for (int i = startRowForHit; i < base.size(); i++) {
            if (drawY + ROW_H > listBottom) break;
            ClientPartyHudData.Member m = base.get(i);

            boolean isLeader = (leaderUuid != null && leaderUuid.equals(m.uuid));
            if (!isLeader) {
                int rowContentRight = infoX + w - CONTENT_PAD_X - (totalH > visibleH ? (SCROLLBAR_WIDTH + SCROLLBAR_PAD_RIGHT) : 0);
                int btnX = rowContentRight - KICK_W;
                int btnY = drawY + 2 + Math.round((tr.fontHeight * TEXT_SCALE - KICK_H) / 2f);

                if (mouseX >= btnX && mouseX < btnX + KICK_W && mouseY >= btnY && mouseY < btnY + KICK_H) {
                    var mc = MinecraftClient.getInstance();
                    if (mc != null && mc.getNetworkHandler() != null && m.name != null && !m.name.isBlank()) {
                        mc.getNetworkHandler().sendChatCommand("party kick " + m.name);
                    }
                    return true;
                }
            }
            drawY += ROW_H;
        }
        UUID viewerUuid = null;
        var mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) viewerUuid = mc.player.getUuid();
        boolean viewerIsLeader = (ClientPartyHudData.leaderUuid != null && ClientPartyHudData.leaderUuid.equals(viewerUuid));

        if (viewerIsLeader) {

        }

        int startRow = scrollPx / ROW_H;
        int index = startRow + (int) ((mouseY - listTop) / ROW_H);
        if (index < 0 || index >= base.size()) return false;
        ClientPartyHudData.Member m = base.get(index);
        selected = m.uuid;
        ClientPartyHudData.setSelectedMember(m.uuid);
        return true;
    }
    private static boolean isMouseOver(int x, int y, int w, int h) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                 int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        int infoX = canvasX + TL_BOX_MARGIN + 18;
        int infoY = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int infoW = TL_BOX_W;
        int infoH = TL_BOX_H;

        int x = infoX;
        int y = infoY + infoH + BELOW_GAP;
        int w = infoW;
        int bottomMargin = 8;
        int h = Math.min(MAX_BOX_H, Math.max(0, (canvasY + canvasH - bottomMargin) - y));

        int listTop = y + CONTENT_PAD_Y;
        int listBottom = y + h - CONTENT_PAD_Y - CONTROL_H;
        int minVisibleH = 16 * ROW_H;
        if ((listBottom - listTop) < minVisibleH) listBottom = Math.min(listBottom, listTop + minVisibleH);

        if (mouseX < x || mouseX >= x + w || mouseY < listTop || mouseY >= listBottom) return false;

        List<ClientPartyHudData.Member> base = new ArrayList<>(ClientPartyHudData.members());

        int visibleH = Math.max(0, listBottom - listTop);
        int totalH = base.size() * ROW_H;
        int maxScroll = Math.max(0, totalH - visibleH);

        int delta = (int) Math.round(-verticalAmount * ROW_H);
        scrollPx = clampToRow(snapToRow(scrollPx + delta), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        if (!draggingScrollbar) return false;

        int infoX = canvasX + TL_BOX_MARGIN + 18;
        int infoY = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int infoW = TL_BOX_W;
        int infoH = TL_BOX_H;

        int x = infoX;
        int y = infoY + infoH + BELOW_GAP;
        int w = infoW;
        int bottomMargin = 8;
        int h = Math.min(MAX_BOX_H, Math.max(0, (canvasY + canvasH - bottomMargin) - y));

        int listTop = y + CONTENT_PAD_Y;
        int listBottom = y + h - CONTENT_PAD_Y - CONTROL_H;
        int minVisibleH = 16 * ROW_H;
        if ((listBottom - listTop) < minVisibleH) listBottom = Math.min(listBottom, listTop + minVisibleH);

        List<ClientPartyHudData.Member> base = new ArrayList<>(ClientPartyHudData.members());
        int visibleH = Math.max(0, listBottom - listTop);
        int totalH = base.size() * ROW_H;
        int maxScroll = Math.max(0, totalH - visibleH);

        int trackTop = listTop + SCROLLBAR_PAD_Y;
        int trackH = (SCROLLBAR_TRACK_HEIGHT > 0)
                ? Math.min(SCROLLBAR_TRACK_HEIGHT, visibleH)
                : visibleH;
        trackH = Math.max(0, trackH - 2 * SCROLLBAR_PAD_Y);

        int thumbH = Math.max(SCROLLBAR_THUMB_MIN, (int) Math.ceil(trackH * (visibleH / (double) totalH)));
        thumbH = Math.min(thumbH, trackH);

        int newThumbY = (int) Math.round(mouseY) - dragGrabOffsetY;
        newThumbY = Math.max(trackTop, Math.min(trackTop + trackH - thumbH, newThumbY));

        double denom = Math.max(1.0, (trackH - thumbH));
        double ratio = (newThumbY - trackTop) / denom;
        scrollPx = clampToRow(snapToRow((int) Math.round(ratio * maxScroll)), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    private static int snapToRow(int px) {
        if (px <= 0) return 0;
        return (px / ROW_H) * ROW_H;
    }

    private static int clampToRow(int px, int min, int max) {
        if (px < min) return min;
        return Math.min(px, max - (max % ROW_H));
    }

    private static void ensureNearestFilters() {
        if (texturesFiltered) return;
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        for (Identifier id : new Identifier[]{PARTY_LEADER, KICK_NORMAL, KICK_HOVER, OFFLINE_ICON}) {
            AbstractTexture t = tm.getTexture(id);
            if (t != null) t.setFilter(false, false);
        }
        texturesFiltered = true;
    }


    private static UUID effectiveLeaderUuid(List<ClientPartyHudData.Member> list) {
        UUID leader = ClientPartyHudData.leaderUuid;
        if (leader != null) {
            for (ClientPartyHudData.Member m : list) {
                if (m.uuid.equals(leader)) return leader;
            }
        }
        return list.isEmpty() ? null : list.get(0).uuid;
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static String ellipsize(TextRenderer tr, String s, double maxLogicalWidth) {
        if (tr.getWidth(s) <= maxLogicalWidth) return s;
        String dots = "...";
        int dw = tr.getWidth(dots);
        StringBuilder b = new StringBuilder(s);
        while (b.length() > 0 && tr.getWidth(b.toString()) + dw > maxLogicalWidth) b.deleteCharAt(b.length() - 1);
        return b + dots;
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }
}
