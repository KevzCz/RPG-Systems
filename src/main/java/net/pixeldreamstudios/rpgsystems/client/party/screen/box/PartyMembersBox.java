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
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyPins;
import net.pixeldreamstudios.rpgsystems.client.party.CompatCommandHelper;

import java.util.*;

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
    private static final int HINT_PAD_X = 4;
    private static final int HINT_PAD_Y = 3;

    private static final Identifier PARTY_LEADER = Identifier.of("rpg-systems", "textures/gui/party_leader.png");
    private static final Identifier KICK_NORMAL  = Identifier.of("rpg-systems", "textures/gui/button/kick_normal.png");
    private static final Identifier KICK_HOVER   = Identifier.of("rpg-systems", "textures/gui/button/kick_hover.png");
    private static final Identifier OFFLINE_ICON = Identifier.of("rpg-systems", "textures/gui/offline.png");
    private static final Identifier PIN_ICON     = Identifier.of("rpg-systems", "textures/gui/pin.png");

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

    private static final float HINT_SCALE = 0.55f;

    private static final float DIALOG_SCALE = 0.6f;
    private static final int DIALOG_PAD = 8;
    private static final int DIALOG_BUTTON_PAD_X = 8;
    private static final int DIALOG_BUTTON_PAD_Y = 4;
    private static final int DIALOG_LIST_MAX_ROWS = 8;
    private static final int DIALOG_BG = 0xD0222222;
    private static final int DIALOG_BG_SHADOW = 0x60000000;
    private static final int DIALOG_BORDER = 0x80FFFFFF;
    private static final int BTN_BG = 0xFF3B89C9;
    private static final int BTN_BG_HOVER = 0xFF4FA3E2;
    private static final int BTN_BG_DISABLED = 0xFF6B6B6B;
    private static final int BTN_TEXT = 0xFFFFFFFF;
    private static final int DIALOG_TITLE = 0xFFFFFFFF;
    private static final int DIALOG_TEXT = 0xFFEFEFEF;
    private static final int DIALOG_BACKDROP = 0x80000000;
    private static final int LIST_ROW_H = ROW_H;

    public static final int DIALOG_W_MIN = 140;
    public static final int DIALOG_W_MAX = 180;

    private static boolean texturesFiltered = false;

    private UUID selected = null;
    private int scrollPx = 0;
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0;

    private List<ClientPartyHudData.Member> lastListForPins = java.util.Collections.emptyList();

    private ClientPartyHudData.Member hoverMember = null;
    private boolean hoverOverPinIcon = false;
    private ClientPartyHudData.Member hoverKickMember = null;
    private boolean hoverOverKick = false;
    private boolean hoverOverLeaderIcon = false;

    private boolean showPromoteDialog = false;
    private List<ClientPartyHudData.Member> promoteCandidates = java.util.Collections.emptyList();
    private int promoteSelectedIndex = -1;
    private int promoteListScrollPx = 0;

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

        List<ClientPartyHudData.Member> base = net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.DEBUG_FORCE_DUMMY
                ? new ArrayList<>(net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.partyMembers())
                : new ArrayList<>(ClientPartyHudData.members());
        base.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT)));
        lastListForPins = base;

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

        boolean showScrollbarLocal = totalH > visibleH;
        int rightPadForScrollbar = showScrollbarLocal ? (SCROLLBAR_WIDTH + SCROLLBAR_PAD_RIGHT) : 0;

        int startRow = scrollPx / ROW_H;
        int drawY = listTop;
        UUID leaderUuid = effectiveLeaderUuid(base);
        UUID viewerUuid = null;
        var mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) viewerUuid = mc.player.getUuid();
        boolean viewerIsLeader = (leaderUuid != null && leaderUuid.equals(viewerUuid));

        final int mx = (int)(MinecraftClient.getInstance().mouse.getX()
                * MinecraftClient.getInstance().getWindow().getScaledWidth()
                / MinecraftClient.getInstance().getWindow().getWidth());
        final int my = (int)(MinecraftClient.getInstance().mouse.getY()
                * MinecraftClient.getInstance().getWindow().getScaledHeight()
                / MinecraftClient.getInstance().getWindow().getHeight());

        hoverMember = null;
        hoverOverPinIcon = false;
        hoverKickMember = null;
        hoverOverKick = false;
        hoverOverLeaderIcon = false;

        for (int i = startRow; i < totalRows; i++) {
            if (drawY + ROW_H > listBottom) break;

            ClientPartyHudData.Member m = base.get(i);
            boolean isSel = selected != null && selected.equals(m.uuid);
            if (isSel) ctx.fill(x + 2, drawY - 1, x + w - 2, drawY + ROW_H - 1, 0x803B89C9);

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
                if (viewerIsLeader && isMouseOver(iconX, iconY, ICON_W, ICON_H)) {
                    hoverOverLeaderIcon = true;
                }
            } else if (viewerIsLeader) {
                int btnX = rowContentRight - KICK_W;
                int btnY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - KICK_H) / 2f);
                boolean overKick = isMouseOver(btnX, btnY, KICK_W, KICK_H);
                ctx.drawTexture(overKick ? KICK_HOVER : KICK_NORMAL, btnX, btnY, 0, 0, KICK_W, KICK_H, KICK_W, KICK_H);
                rightElemLeft = btnX;
                if (overKick) {
                    hoverOverKick = true;
                    hoverKickMember = m;
                }
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

            boolean isPinned = ClientPartyPins.isPinned(m.uuid);
            int pinIconX = -1, pinIconY = -1;
            if (isPinned) {
                pinIconX = nameStartX;
                pinIconY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - ICON_H) / 2f);
                drawPinCrisp(ctx, pinIconX, pinIconY);
                nameStartX += ICON_W + 3;
            }

            int textMaxPixels = Math.max(0, nameRightLimit - nameStartX);
            String name = (m.name == null) ? "Unknown" : m.name;
            String clipped = ellipsize(tr, name, textMaxPixels / TEXT_SCALE);
            int nameColor = isPinned ? 0xFFFFE082 : 0xFFFFFFFF;
            drawScaledText(ctx, tr, clipped, nameStartX, textTopY, nameColor, TEXT_SCALE);

            int rowLeft = x;
            int rowRight = x + w;
            if (!hoverOverKick && mx >= rowLeft && mx < rowRight && my >= drawY && my < (drawY + ROW_H)) {
                hoverMember = m;
                if (isPinned && pinIconX >= 0) {
                    hoverOverPinIcon = (mx >= pinIconX && mx < pinIconX + ICON_W && my >= pinIconY && my < pinIconY + ICON_H);
                }
            }

            drawY += ROW_H;
        }

        if (base.isEmpty()) {
            String none = "No members";
            int tw = Math.round(tr.getWidth(none) * TEXT_SCALE);
            int tx = x + (w - tw) / 2;
            int ty = y + (h - Math.round(tr.fontHeight * TEXT_SCALE)) / 2;
            drawScaledText(ctx, tr, none, tx, ty, 0xFFBEBEBE, TEXT_SCALE);
        }

        if (showScrollbarLocal) {
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

        if (hoverOverKick && hoverKickMember != null) {
            String label = "Kick " + (hoverKickMember.name == null ? "Unknown" : hoverKickMember.name) + "?";
            drawHint(ctx, tr, label, mx, my, canvasX, canvasY, canvasW, canvasH);
        } else if (hoverOverLeaderIcon && viewerIsLeader) {
            String label = "Change leader?";
            drawHint(ctx, tr, label, mx, my, canvasX, canvasY, canvasW, canvasH);
        } else if (hoverMember != null) {
            boolean pinned = ClientPartyPins.isPinned(hoverMember.uuid);
            boolean overPin = hoverOverPinIcon;
            boolean isSelf = false;
            var mcSelf = MinecraftClient.getInstance();
            if (mcSelf != null && mcSelf.player != null) {
                isSelf = hoverMember.uuid != null && hoverMember.uuid.equals(mcSelf.player.getUuid());
            }
            String fullName = (hoverMember.name == null) ? "Unknown" : hoverMember.name;

            if (overPin && pinned) {
                Integer order = ClientPartyPins.getOrder(hoverMember.uuid);
                String label = "Middle click to Unpin #" + order;
                int iconW = ICON_W, iconH = ICON_H;
                int textW = Math.round(tr.getWidth(label) * HINT_SCALE);
                int textH = Math.max(1, Math.round(tr.fontHeight * HINT_SCALE));
                int contentH = Math.max(iconH, textH);
                int boxW = HINT_PAD_X + iconW + 3 + textW + HINT_PAD_X;
                int boxH = HINT_PAD_Y + contentH + HINT_PAD_Y;

                int bx = Math.min(mx + 12, canvasX + canvasW - boxW - 2);
                int by = Math.min(my + 10, canvasY + canvasH - boxH - 2);

                ctx.fill(bx + 2, by + 2, bx + boxW + 2, by + boxH + 2, 0x60000000);
                ctx.fill(bx, by, bx + boxW, by + boxH, 0xC0222222);
                drawBorder(ctx, bx, by, boxW, boxH, 0x80FFFFFF);

                int iconY = by + HINT_PAD_Y + (contentH - iconH) / 2;
                int ix = bx + HINT_PAD_X;
                drawPinCrisp(ctx, ix, iconY);

                int ty = by + HINT_PAD_Y + (contentH - textH) / 2;
                int tx = ix + iconW + 3;
                drawScaledText(ctx, tr, label, tx, ty, 0xFFC8C8C8, HINT_SCALE);
            } else {
                List<String> lines = new ArrayList<>();
                lines.add(fullName);
                if (!pinned && !isSelf) {
                    lines.add("Middle click to Pin #" + ClientPartyPins.nextOrder());
                }

                int maxW = 0;
                for (String ln : lines) maxW = Math.max(maxW, Math.round(tr.getWidth(ln) * HINT_SCALE));
                int lineH = Math.max(1, Math.round(tr.fontHeight * HINT_SCALE));
                int innerH = lines.size() * lineH + Math.max(0, lines.size() - 1);
                int boxW = HINT_PAD_X + maxW + HINT_PAD_X;
                int boxH = HINT_PAD_Y + innerH + HINT_PAD_Y;

                int bx = Math.min(mx + 12, canvasX + canvasW - boxW - 2);
                int by = Math.min(my + 10, canvasY + canvasH - boxH - 2);

                ctx.fill(bx + 2, by + 2, bx + boxW + 2, by + boxH + 2, 0x60000000);
                ctx.fill(bx, by, bx + boxW, by + boxH, 0xC0222222);
                drawBorder(ctx, bx, by, boxW, boxH, 0x80FFFFFF);

                int ty = by + HINT_PAD_Y;
                for (int i = 0; i < lines.size(); i++) {
                    String ln = lines.get(i);
                    int lw = Math.round(tr.getWidth(ln) * HINT_SCALE);
                    int tx = bx + (boxW - HINT_PAD_X * 2 - lw) / 2 + HINT_PAD_X;
                    int color = (i == 0) ? 0xFFFFFFFF : 0xFFC8C8C8;
                    drawScaledText(ctx, tr, ln, tx, ty, color, HINT_SCALE);
                    if (i + 1 < lines.size()) ty += lineH + 1;
                }
            }
        }

        if (showPromoteDialog) {
            drawPromoteDialog(ctx, tr, canvasX, canvasY, canvasW, canvasH, base, leaderUuid);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {

        if (showPromoteDialog) {
            return handlePromoteDialogClick(mouseX, mouseY, button, canvasX, canvasY, canvasW, canvasH, tr);
        }

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

        if (mouseX < infoX || mouseX >= infoX + w || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }

        List<ClientPartyHudData.Member> base =
                net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.DEBUG_FORCE_DUMMY
                        ? new ArrayList<>(net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.partyMembers())
                        : new ArrayList<>(ClientPartyHudData.members());
        base.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT)));

        int visibleH = Math.max(0, listBottom - listTop);
        int totalH = base.size() * ROW_H;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollPx = clampToRow(snapToRow(scrollPx), 0, maxScroll);

        boolean showScrollbar = totalH > visibleH;

        UUID leaderUuid = ClientPartyHudData.leaderUuid;
        UUID viewerUuid = (MinecraftClient.getInstance().player != null)
                ? MinecraftClient.getInstance().player.getUuid() : null;
        boolean viewerIsLeader = (leaderUuid != null && leaderUuid.equals(viewerUuid));

        if (viewerIsLeader) {
            int rightPadForScrollbar = showScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_PAD_RIGHT) : 0;
            int startRowForHit = scrollPx / ROW_H;
            int drawY = listTop;

            for (int i = startRowForHit; i < base.size(); i++) {
                if (drawY + ROW_H > listBottom) break;

                ClientPartyHudData.Member m = base.get(i);
                boolean isLeaderRow = leaderUuid != null && leaderUuid.equals(m.uuid);

                int textTopY = drawY + 2;
                int rowContentRight = infoX + w - CONTENT_PAD_X - rightPadForScrollbar;

                if (isLeaderRow) {
                    int iconX = rowContentRight - ICON_W;
                    int iconY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - ICON_H) / 2f);
                    if (mouseX >= iconX && mouseX < iconX + ICON_W && mouseY >= iconY && mouseY < iconY + ICON_H) {
                        openPromoteDialog(base, leaderUuid);
                        return true;
                    }
                } else if (m.uuid != null && viewerUuid != null && !viewerUuid.equals(m.uuid)) {
                    int btnX = rowContentRight - KICK_W;
                    int btnY = Math.round(textTopY + (tr.fontHeight * TEXT_SCALE - KICK_H) / 2f);
                    if (mouseX >= btnX && mouseX < btnX + KICK_W && mouseY >= btnY && mouseY < btnY + KICK_H) {
                        var mc = MinecraftClient.getInstance();
                        if (m.name != null && !m.name.isBlank()) {
                            CompatCommandHelper.sendKickCommand(m.name);
                        }
                        return true;
                    }
                }
                drawY += ROW_H;
            }
        }

        int startRow = scrollPx / ROW_H;
        int index = startRow + (int) ((mouseY - listTop) / ROW_H);
        if (index < 0 || index >= base.size()) return false;

        ClientPartyHudData.Member m = base.get(index);

        if (button == 0) {
            selected = m.uuid;
            ClientPartyHudData.setSelectedMember(m.uuid);
            return true;
        } else if (button == 2) {
            UUID self = (MinecraftClient.getInstance().player != null)
                    ? MinecraftClient.getInstance().player.getUuid() : null;
            if (self != null && self.equals(m.uuid)) {
                return true;
            }
            if (ClientPartyPins.isPinned(m.uuid)) {
                ClientPartyPins.unpin(m.uuid);
            } else {
                ClientPartyPins.pin(m.uuid, 0, 0);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                 int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {

        if (showPromoteDialog) {
            return handlePromoteDialogScroll(mouseX, mouseY, verticalAmount, canvasX, canvasY, canvasW, canvasH, tr);
        }

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

        List<ClientPartyHudData.Member> base = net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.DEBUG_FORCE_DUMMY
                ? new ArrayList<>(net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.partyMembers())
                : new ArrayList<>(ClientPartyHudData.members());

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

        if (showPromoteDialog) return false;

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

        List<ClientPartyHudData.Member> base = net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.DEBUG_FORCE_DUMMY
                ? new ArrayList<>(net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.partyMembers())
                : new ArrayList<>(ClientPartyHudData.members());
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
        for (Identifier id : new Identifier[]{PARTY_LEADER, KICK_NORMAL, KICK_HOVER, OFFLINE_ICON, PIN_ICON}) {
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

    private static boolean isMouseOver(int x, int y, int w, int h) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void drawPinCrisp(DrawContext ctx, int x, int y) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(0.25f, 0.25f, 1f);
        ctx.drawTexture(PIN_ICON, 0, 0, 0, 0, 32, 32, 32, 32);
        m.pop();
    }

    private void drawHint(DrawContext ctx, TextRenderer tr, String label, int mx, int my,
                          int canvasX, int canvasY, int canvasW, int canvasH) {
        int textW = Math.round(tr.getWidth(label) * HINT_SCALE);
        int textH = Math.max(1, Math.round(tr.fontHeight * HINT_SCALE));
        int boxW = HINT_PAD_X + textW + HINT_PAD_X;
        int boxH = HINT_PAD_Y + textH + HINT_PAD_Y;

        int bx = Math.min(mx + 12, canvasX + canvasW - boxW - 2);
        int by = Math.min(my + 10, canvasY + canvasH - boxH - 2);

        ctx.fill(bx + 2, by + 2, bx + boxW + 2, by + boxH + 2, 0x60000000);
        ctx.fill(bx, by, bx + boxW, by + boxH, 0xC0222222);
        drawBorder(ctx, bx, by, boxW, boxH, 0x80FFFFFF);

        int tx = bx + (boxW - HINT_PAD_X * 2 - textW) / 2 + HINT_PAD_X;
        int ty = by + HINT_PAD_Y + (boxH - 2 * HINT_PAD_Y - textH) / 2;
        drawScaledText(ctx, tr, label, tx, ty, 0xFFFFFFFF, HINT_SCALE);
    }

    private void openPromoteDialog(List<ClientPartyHudData.Member> base, UUID leaderUuid) {
        List<ClientPartyHudData.Member> cands = new ArrayList<>();
        for (ClientPartyHudData.Member m : base) {
            if (m.uuid != null && !m.uuid.equals(leaderUuid)) cands.add(m);
        }
        cands.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase(Locale.ROOT)));
        promoteCandidates = cands;
        promoteSelectedIndex = -1;
        promoteListScrollPx = 0;
        showPromoteDialog = true;
    }

    private void drawPromoteDialog(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH,
                                   List<ClientPartyHudData.Member> base, UUID leaderUuid) {

        int dialogW = Math.max(DIALOG_W_MIN, Math.min(DIALOG_W_MAX, canvasW - 32));
        int dialogH;
        int titleH = Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));
        int listRows = Math.min(DIALOG_LIST_MAX_ROWS, Math.max(1, promoteCandidates.size()));
        int listAreaH = listRows * LIST_ROW_H;
        int buttonsH = (DIALOG_BUTTON_PAD_Y * 2) + Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));
        dialogH = DIALOG_PAD + titleH + 6 + listAreaH + 8 + buttonsH + DIALOG_PAD;

        int dx = canvasX + (canvasW - dialogW) / 2;
        int dy = canvasY + (canvasH - dialogH) / 2;

        ctx.fill(dx + 2, dy + 2, dx + dialogW + 2, dy + dialogH + 2, DIALOG_BG_SHADOW);
        ctx.fill(dx, dy, dx + dialogW, dy + dialogH, DIALOG_BG);
        drawBorder(ctx, dx, dy, dialogW, dialogH, DIALOG_BORDER);

        String title = "Promote new leader";
        int tw = Math.round(tr.getWidth(title) * DIALOG_SCALE);
        int tx = dx + (dialogW - tw) / 2;
        int ty = dy + DIALOG_PAD;
        drawScaledText(ctx, tr, title, tx, ty, DIALOG_TITLE, DIALOG_SCALE);

        int listTop = ty + titleH + 6;
        int listLeft = dx + DIALOG_PAD;
        int listRight = dx + dialogW - DIALOG_PAD;
        int listBottom = listTop + listAreaH;

        ctx.fill(listLeft - 1, listTop - 1, listRight + 1, listBottom + 1, 0x20101010);

        int totalH = promoteCandidates.size() * LIST_ROW_H;
        int visibleH = listAreaH;
        int maxScroll = Math.max(0, totalH - visibleH);
        promoteListScrollPx = clampToRow(snapToRow(promoteListScrollPx), 0, maxScroll);

        int startRow = promoteListScrollPx / LIST_ROW_H;
        int drawY = listTop;

        for (int i = startRow; i < promoteCandidates.size(); i++) {
            if (drawY + LIST_ROW_H > listBottom) break;
            ClientPartyHudData.Member m = promoteCandidates.get(i);
            boolean sel = (i == promoteSelectedIndex);
            if (sel) ctx.fill(listLeft, drawY, listRight, drawY + LIST_ROW_H, 0x803B89C9);

            String name = (m.name == null) ? "Unknown" : m.name;
            int lw = Math.round(tr.getWidth(name) * DIALOG_SCALE);
            int centerX = listLeft + ((listRight - listLeft) - lw) / 2;
            drawScaledText(ctx, tr, name, centerX, drawY + 2, DIALOG_TEXT, DIALOG_SCALE);

            drawY += LIST_ROW_H;
        }

        int btnY = listBottom + 8;
        String cancelLabel = "Cancel";
        String promoteLabel = "Promote";
        int cancelW = Math.round(tr.getWidth(cancelLabel) * DIALOG_SCALE) + DIALOG_BUTTON_PAD_X * 2;
        int promoteW = Math.round(tr.getWidth(promoteLabel) * DIALOG_SCALE) + DIALOG_BUTTON_PAD_X * 2;
        int gap = 6;
        int totalBtnsW = cancelW + gap + promoteW;
        int cancelX = dx + (dialogW - totalBtnsW) / 2;
        int promoteX = cancelX + cancelW + gap;
        int btnH = DIALOG_BUTTON_PAD_Y * 2 + Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));

        int mx = (int)(MinecraftClient.getInstance().mouse.getX()
                * MinecraftClient.getInstance().getWindow().getScaledWidth()
                / MinecraftClient.getInstance().getWindow().getWidth());
        int my = (int)(MinecraftClient.getInstance().mouse.getY()
                * MinecraftClient.getInstance().getWindow().getScaledHeight()
                / MinecraftClient.getInstance().getWindow().getHeight());

        boolean overCancel = mx >= cancelX && mx < cancelX + cancelW && my >= btnY && my < btnY + btnH;
        boolean overPromote = mx >= promoteX && mx < promoteX + promoteW && my >= btnY && my < btnY + btnH;

        ctx.fill(cancelX, btnY, cancelX + cancelW, btnY + btnH, overCancel ? BTN_BG_HOVER : BTN_BG);
        drawBorder(ctx, cancelX, btnY, cancelW, btnH, 0x40000000);
        int cancelTextX = cancelX + (cancelW - Math.round(tr.getWidth(cancelLabel) * DIALOG_SCALE)) / 2;
        int cancelTextY = btnY + (btnH - Math.round(tr.fontHeight * DIALOG_SCALE)) / 2;
        drawScaledText(ctx, tr, cancelLabel, cancelTextX, cancelTextY, BTN_TEXT, DIALOG_SCALE);

        boolean canPromote = promoteSelectedIndex >= 0 && promoteSelectedIndex < promoteCandidates.size();
        int promoteBg = canPromote ? (overPromote ? BTN_BG_HOVER : BTN_BG) : BTN_BG_DISABLED;
        ctx.fill(promoteX, btnY, promoteX + promoteW, btnY + btnH, promoteBg);
        drawBorder(ctx, promoteX, btnY, promoteW, btnH, 0x40000000);
        int promoteTextX = promoteX + (promoteW - Math.round(tr.getWidth(promoteLabel) * DIALOG_SCALE)) / 2;
        int promoteTextY = btnY + (btnH - Math.round(tr.fontHeight * DIALOG_SCALE)) / 2;
        drawScaledText(ctx, tr, promoteLabel, promoteTextX, promoteTextY, BTN_TEXT, DIALOG_SCALE);
    }

    private boolean handlePromoteDialogClick(double mouseX, double mouseY, int button,
                                             int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        int dialogW = Math.max(DIALOG_W_MIN, Math.min(DIALOG_W_MAX, canvasW - 32));
        int titleH = Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));
        int listRows = Math.min(DIALOG_LIST_MAX_ROWS, Math.max(1, promoteCandidates.size()));
        int listAreaH = listRows * LIST_ROW_H;
        int buttonsH = (DIALOG_BUTTON_PAD_Y * 2) + Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));
        int dialogH = DIALOG_PAD + titleH + 6 + listAreaH + 8 + buttonsH + DIALOG_PAD;

        int dx = canvasX + (canvasW - dialogW) / 2;
        int dy = canvasY + (canvasH - dialogH) / 2;

        int listTop = dy + DIALOG_PAD + titleH + 6;
        int listLeft = dx + DIALOG_PAD;
        int listRight = dx + dialogW - DIALOG_PAD;
        int listBottom = listTop + listAreaH;

        int totalH = promoteCandidates.size() * LIST_ROW_H;
        int visibleH = listAreaH;
        int maxScroll = Math.max(0, totalH - visibleH);
        promoteListScrollPx = clampToRow(snapToRow(promoteListScrollPx), 0, maxScroll);

        if (button == 0) {
            if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
                int startRow = promoteListScrollPx / LIST_ROW_H;
                int index = startRow + (int) ((mouseY - listTop) / LIST_ROW_H);
                if (index >= 0 && index < promoteCandidates.size()) {
                    promoteSelectedIndex = index;
                }
                return true;
            }

            String cancelLabel = "Cancel";
            String promoteLabel = "Promote";
            int cancelW = Math.round(tr.getWidth(cancelLabel) * DIALOG_SCALE) + DIALOG_BUTTON_PAD_X * 2;
            int promoteW = Math.round(tr.getWidth(promoteLabel) * DIALOG_SCALE) + DIALOG_BUTTON_PAD_X * 2;
            int gap = 6;
            int totalBtnsW = cancelW + gap + promoteW;
            int btnY = listBottom + 8;
            int cancelX = dx + (dialogW - totalBtnsW) / 2;
            int promoteX = cancelX + cancelW + gap;
            int btnH = DIALOG_BUTTON_PAD_Y * 2 + Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));

            if (mouseX >= cancelX && mouseX < cancelX + cancelW && mouseY >= btnY && mouseY < btnY + btnH) {
                showPromoteDialog = false;
                return true;
            }

            boolean canPromote = promoteSelectedIndex >= 0 && promoteSelectedIndex < promoteCandidates.size();
            if (mouseX >= promoteX && mouseX < promoteX + promoteW && mouseY >= btnY && mouseY < btnY + btnH) {
                if (canPromote) {
                    ClientPartyHudData.Member sel = promoteCandidates.get(promoteSelectedIndex);
                    if (sel != null && sel.name != null && !sel.name.isBlank()) {
                        CompatCommandHelper.sendPromoteCommand(sel.name);
                    }
                    showPromoteDialog = false;
                }
                return true;
            }
        }

        return false;
    }

    private boolean handlePromoteDialogScroll(double mouseX, double mouseY, double verticalAmount,
                                              int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        int dialogW = Math.max(DIALOG_W_MIN, Math.min(DIALOG_W_MAX, canvasW - 32));
        int titleH = Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE));
        int listRows = Math.min(DIALOG_LIST_MAX_ROWS, Math.max(1, promoteCandidates.size()));
        int listAreaH = listRows * LIST_ROW_H;
        int dialogH = DIALOG_PAD + titleH + 6 + listAreaH + 8 + ((DIALOG_BUTTON_PAD_Y * 2) + Math.max(1, Math.round(tr.fontHeight * DIALOG_SCALE))) + DIALOG_PAD;

        int dx = canvasX + (canvasW - dialogW) / 2;
        int dy = canvasY + (canvasH - dialogH) / 2;

        int listTop = dy + DIALOG_PAD + titleH + 6;
        int listLeft = dx + DIALOG_PAD;
        int listRight = dx + dialogW - DIALOG_PAD;
        int listBottom = listTop + listAreaH;

        if (mouseX < listLeft || mouseX >= listRight || mouseY < listTop || mouseY >= listBottom) return false;

        int totalH = promoteCandidates.size() * LIST_ROW_H;
        int visibleH = listAreaH;
        int maxScroll = Math.max(0, totalH - visibleH);

        int delta = (int) Math.round(-verticalAmount * LIST_ROW_H);
        promoteListScrollPx = clampToRow(snapToRow(promoteListScrollPx + delta), 0, maxScroll);
        return true;
    }
}
