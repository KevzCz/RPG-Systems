package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyChat;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Environment(EnvType.CLIENT)
public final class PartyChatBox implements PartyBox {

    private static final boolean DEBUG_BOX = false;
    private static final int TL_BOX_W = 120;
    private static final int TL_BOX_MARGIN = 8;
    private static final int TL_BOX_OFFSET_Y = 15;
    private static final int GAP_FROM_LEFT_COLUMN = 12;
    private static final int SHIFT_LEFT = 60;
    private static final int BOX_W = 255;
    private static final int BOX_H = 201;
    private static final int PAD = 6;
    private static final float LOG_TEXT_SCALE = 0.65f;
    private static final float INPUT_TEXT_SCALE = 0.7f;
    private static final int BG_COLOR = 0x402196F3;
    private static final int BORDER_COLOR = 0xFF2196F3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PLACEHOLDER_COLOR = 0xFFBEBEBE;
    private static final int INPUT_PAD_V = 4;
    private static final int INPUT_PAD_H = 4;
    private static final int INPUT_BG = 0x40101010;
    private static final int INPUT_BORDER = 0x80FFFFFF;
    private static final int SELECTION_BG = 0x8033AAFF;
    private static final String PLACEHOLDER = "Type a message…";
    private static final int BUBBLE_PAD_H = 4;
    private static final int BUBBLE_PAD_V = 2;
    private static final int BUBBLE_GAP   = 4;
    private static final int NAME_GAP     = 2;
    private static final int LINE_GAP     = 1;
    private static final int BUBBLE_RADIUS = 3;
    private static final int BUBBLE_SELF  = 0x903498DB;
    private static final int BUBBLE_OTHER = 0x90333333;
    private static final double WRAP_FRACTION = 2.0 / 3.0;
    private boolean focused = false;
    private final StringBuilder input = new StringBuilder();
    private int caret = 0;
    private int selAnchor = -1;
    private long lastBlinkMs = 0;
    private boolean blinkOn = true;
    private int scrollPx = 0;
    private int inputScrollPx = 0;

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH) {
        int contentLeft  = canvasX + TL_BOX_MARGIN + 18;
        int infoTop      = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int xLeftCol     = contentLeft + TL_BOX_W;

        int x = xLeftCol + GAP_FROM_LEFT_COLUMN - SHIFT_LEFT;
        int y = infoTop + PartyMemberInfoBox.getPreferredHeight() + 4;

        int w = BOX_W;
        int h = BOX_H;

        if (DEBUG_BOX) {
            ctx.fill(x, y, x + w, y + h, BG_COLOR);
            drawBorder(ctx, x, y, w, h, BORDER_COLOR);
        }

        int inputTextH = Math.round(tr.fontHeight * INPUT_TEXT_SCALE);
        int inputH = inputTextH + INPUT_PAD_V * 2;
        int logTop = y + PAD;
        int logBottom = y + h - PAD - inputH - 4;
        if (logBottom < logTop) logBottom = logTop;

        int inputTop = y + h - PAD - inputH;
        int inputLeft = x + PAD;
        int inputRight = x + w - PAD;

        int innerLeft  = x + PAD;
        int innerRight = x + w - PAD;
        int innerW = innerRight - innerLeft;

        int visibleH = Math.max(0, logBottom - logTop);

        List<ClientPartyChat.Msg> msgs = ClientPartyChat.all();
        UUID self = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getUuid() : null;

        int lineH  = Math.round(tr.fontHeight * LOG_TEXT_SCALE);
        int nameH  = lineH;
        int wrapTextPx = Math.max(20, (int)Math.floor(innerW * WRAP_FRACTION) - 2 * BUBBLE_PAD_H);
        int bubbleMaxW = Math.max(40, (int)Math.floor(innerW * 0.82));

        int totalH = measureTotalHeightWrapped(tr, msgs, wrapTextPx, bubbleMaxW, lineH, nameH);

        int maxScroll = Math.max(0, totalH - visibleH);
        if (scrollPx > maxScroll) scrollPx = maxScroll;
        if (scrollPx < 0) scrollPx = 0;

        ctx.enableScissor(innerLeft, logTop, innerRight, logBottom);

        int cursorY = logBottom + scrollPx;


        for (int i = msgs.size() - 1; i >= 0; i--) {
            ClientPartyChat.Msg m = msgs.get(i);
            String msgText = (m.text == null ? "" : m.text);
            String who     = (m.name == null ? "Unknown" : m.name);
            boolean isSelf = (self != null && m.sender != null && self.equals(m.sender));


            List<String> lines = wrapLines(tr, msgText, wrapTextPx);
            int widestScaled = 0;
            for (String ln : lines) widestScaled = Math.max(widestScaled, Math.round(tr.getWidth(ln) * LOG_TEXT_SCALE));

            int bubbleW = Math.min(bubbleMaxW, widestScaled + 2 * BUBBLE_PAD_H);
            int bubbleH = lines.size() * lineH + (lines.size() - 1) * LINE_GAP + 2 * BUBBLE_PAD_V;

            int nameW = Math.round(tr.getWidth(who) * LOG_TEXT_SCALE);

            int blockH = nameH + NAME_GAP + bubbleH + BUBBLE_GAP;

            int blockTop  = cursorY - blockH;
            int bubbleTop = blockTop + nameH + NAME_GAP;
            int bubbleLeft = isSelf ? (innerRight - bubbleW) : innerLeft;
            int nameY = blockTop + (nameH - lineH);
            int nameX = isSelf ? (bubbleLeft + bubbleW - nameW) : bubbleLeft;


            if (bubbleTop < logBottom && (bubbleTop + bubbleH) > logTop) {
                drawSoftRounded(ctx, bubbleLeft, bubbleTop, bubbleW, bubbleH, BUBBLE_RADIUS, isSelf ? BUBBLE_SELF : BUBBLE_OTHER);
                drawScaledText(ctx, tr, who, nameX, nameY, 0xFFFFFFFF, LOG_TEXT_SCALE);
                int textX = bubbleLeft + BUBBLE_PAD_H;
                int textY = bubbleTop + BUBBLE_PAD_V;
                for (int li = 0; li < lines.size(); li++) {
                    drawScaledText(ctx, tr, lines.get(li), textX, textY, TEXT_COLOR, LOG_TEXT_SCALE);
                    textY += (li == lines.size() - 1 ? 0 : (lineH + LINE_GAP));
                }
            }

            cursorY = blockTop;
            if (cursorY <= logTop) break;
        }

        ctx.disableScissor();

        ctx.fill(inputLeft, inputTop, inputRight, inputTop + inputH, INPUT_BG);
        drawBorder(ctx, inputLeft, inputTop, inputRight - inputLeft, inputH, INPUT_BORDER);

        int inputInnerLeft  = inputLeft  + INPUT_PAD_H;
        int inputInnerRight = inputRight - INPUT_PAD_H;
        int inputClipTop    = inputTop + 1;
        int inputClipBot    = inputTop + inputH - 1;

        int inputTextX = inputInnerLeft;
        int inputTextY = inputTop + INPUT_PAD_V;
        int visibleInputW = Math.max(0, inputInnerRight - inputInnerLeft);

        int caretXScaled = scaledWidth(tr, input.substring(0, Math.min(caret, input.length())), INPUT_TEXT_SCALE);
        int totalTextScaled = scaledWidth(tr, input.toString(), INPUT_TEXT_SCALE);

        ensureCaretVisible(caretXScaled, totalTextScaled, visibleInputW);

        ctx.enableScissor(inputInnerLeft, inputClipTop, inputInnerRight, inputClipBot);

        if (hasSelection()) {
            int a = selMin(), b = selMax();
            int selStartScaled = scaledWidth(tr, input.substring(0, a), INPUT_TEXT_SCALE);
            int selEndScaled   = scaledWidth(tr, input.substring(0, b), INPUT_TEXT_SCALE);
            int sx = inputTextX - inputScrollPx + selStartScaled;
            int ex = inputTextX - inputScrollPx + selEndScaled;
            int ch = Math.round(tr.fontHeight * INPUT_TEXT_SCALE);
            if (ex > sx) {
                ctx.fill(sx, inputTextY, ex, inputTextY + ch, SELECTION_BG);
            }
        }

        if (input.length() == 0 && !focused) {
            drawScaledText(ctx, tr, PLACEHOLDER, inputTextX, inputTextY, PLACEHOLDER_COLOR, INPUT_TEXT_SCALE);
        } else {
            drawScaledText(ctx, tr, input.toString(), inputTextX - inputScrollPx, inputTextY, TEXT_COLOR, INPUT_TEXT_SCALE);

            long now = System.currentTimeMillis();
            if (now - lastBlinkMs > 500) { lastBlinkMs = now; blinkOn = !blinkOn; }

            if (focused && blinkOn && !hasSelection()) {
                int ch = Math.round(tr.fontHeight * INPUT_TEXT_SCALE);
                int caretDrawX = inputTextX - inputScrollPx + caretXScaled;
                ctx.fill(caretDrawX, inputTextY, caretDrawX + 1, inputTextY + ch, 0xFFFFFFFF);
            }
        }

        ctx.disableScissor();

        if (msgs.isEmpty()) {
            String none = "Party chat empty";
            int tw = Math.round(tr.getWidth(none) * LOG_TEXT_SCALE);
            int tx = x + (w - tw) / 2;
            int ty = logTop + Math.max(0, (visibleH - Math.round(tr.fontHeight * LOG_TEXT_SCALE)) / 2);
            drawScaledText(ctx, tr, none, tx, ty, PLACEHOLDER_COLOR, LOG_TEXT_SCALE);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double ha, double va,
                                 int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        int contentLeft  = canvasX + TL_BOX_MARGIN + 18;
        int infoTop      = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int xLeftCol     = contentLeft + TL_BOX_W;

        int x = xLeftCol + GAP_FROM_LEFT_COLUMN - SHIFT_LEFT;
        int y = infoTop + PartyMemberInfoBox.getPreferredHeight() + 4;

        int w = BOX_W;
        int h = BOX_H;

        int logTop = y + PAD;
        int inputTextH = Math.round(tr.fontHeight * INPUT_TEXT_SCALE);
        int inputH = inputTextH + INPUT_PAD_V * 2;
        int logBottom = y + h - PAD - inputH - 4;

        if (!(mouseX >= x && mouseX < x + w && mouseY >= logTop && mouseY < logBottom)) return false;

        int innerLeft  = x + PAD;
        int innerRight = x + w - PAD;
        int innerW = innerRight - innerLeft;

        int visibleH = Math.max(0, logBottom - logTop);
        List<ClientPartyChat.Msg> msgs = ClientPartyChat.all();

        int lineH = Math.round(tr.fontHeight * LOG_TEXT_SCALE);
        int nameH = lineH;
        int wrapTextPx = Math.max(20, (int)Math.floor(innerW * WRAP_FRACTION) - 2 * BUBBLE_PAD_H);
        int bubbleMaxW = Math.max(40, (int)Math.floor(innerW * 0.82));
        int totalH = measureTotalHeightWrapped(tr, msgs, wrapTextPx, bubbleMaxW, lineH, nameH);

        int maxScroll = Math.max(0, totalH - visibleH);
        if (maxScroll == 0) return true;

        int nominalBlock = lineH + NAME_GAP + (lineH + 2 * BUBBLE_PAD_V) + BUBBLE_GAP;
        int step = Math.max(12, nominalBlock);
        int delta = (int)Math.round(va * step);

        scrollPx = Math.max(0, Math.min(maxScroll, scrollPx + delta));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        int contentLeft  = canvasX + TL_BOX_MARGIN + 18;
        int infoTop      = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int xLeftCol     = contentLeft + TL_BOX_W;

        int x = xLeftCol + GAP_FROM_LEFT_COLUMN - SHIFT_LEFT;
        int y = infoTop + PartyMemberInfoBox.getPreferredHeight() + 4;
        int w = BOX_W;
        int h = BOX_H;

        int inputTextH = Math.round(tr.fontHeight * INPUT_TEXT_SCALE);
        int inputH = inputTextH + INPUT_PAD_V * 2;
        int inputTop = y + h - PAD - inputH;
        int inputLeft = x + PAD;
        int inputRight = x + w - PAD;

        boolean inside = (mouseX >= inputLeft && mouseX < inputRight && mouseY >= inputTop && mouseY < inputTop + inputH);
        focused = inside;
        if (focused) {
            caret = input.length();
            clearSelection();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        boolean shift = (modifiers & 0x0001) != 0;
        boolean ctrl  = (modifiers & 0x0002) != 0 || (modifiers & 0x0008) != 0;

        if (ctrl && keyCode == 65 /* A */) {
            selAnchor = 0;
            caret = input.length();
            return true;
        }

        if (ctrl && keyCode == 67 /* C */) {
            if (hasSelection()) MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
            return true;
        }
        if (ctrl && keyCode == 88 /* X */) {
            if (hasSelection()) {
                MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
                deleteSelection();
            }
            return true;
        }
        if (ctrl && keyCode == 86 /* V */) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) replaceSelection(clip);
            return true;
        }

        if (keyCode == 257 || keyCode == 335) {
            sendIfAny();
            return true;
        }

        if (keyCode == 259) {
            if (hasSelection()) {
                deleteSelection();
            } else if (ctrl) {
                int start = prevWordIndex(caret);
                if (start < caret) {
                    input.delete(start, caret);
                    caret = start;
                }
            } else if (caret > 0) {
                input.deleteCharAt(caret - 1);
                caret--;
            }
            return true;
        }
        if (keyCode == 261) {
            if (hasSelection()) {
                deleteSelection();
            } else if (ctrl) {
                int end = nextWordIndex(caret);
                if (end > caret) {
                    input.delete(caret, end);
                }
            } else if (caret < input.length()) {
                input.deleteCharAt(caret);
            }
            return true;
        }

        if (keyCode == 263 ) {
            int dest = ctrl ? prevWordIndex(caret) : Math.max(0, caret - 1);
            moveCaret(dest, shift);
            return true;
        }
        if (keyCode == 262 ) {
            int dest = ctrl ? nextWordIndex(caret) : Math.min(input.length(), caret + 1);
            moveCaret(dest, shift);
            return true;
        }
        if (keyCode == 268 ) {
            int dest = 0;
            if (ctrl) dest = 0;
            moveCaret(dest, shift);
            return true;
        }
        if (keyCode == 269 /* END */) {
            int dest = input.length();
            moveCaret(dest, shift);
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (chr == '\r' || chr == '\n') return true;
        if (Character.isISOControl(chr)) return true;

        replaceSelection(Character.toString(chr));
        return true;
    }


    private static int measureTotalHeightWrapped(TextRenderer tr, List<ClientPartyChat.Msg> msgs,
                                                 int wrapTextPx, int bubbleMaxW,
                                                 int lineH, int nameH) {
        int total = 0;
        for (int i = msgs.size() - 1; i >= 0; i--) {
            String text = msgs.get(i).text == null ? "" : msgs.get(i).text;
            List<String> lines = wrapLines(tr, text, wrapTextPx);
            int widestScaled = 0;
            for (String ln : lines) widestScaled = Math.max(widestScaled, Math.round(tr.getWidth(ln) * LOG_TEXT_SCALE));
            int bubbleW = Math.min(bubbleMaxW, widestScaled + 2 * BUBBLE_PAD_H);
            int bubbleH = lines.size() * lineH + (lines.size() - 1) * LINE_GAP + 2 * BUBBLE_PAD_V;
            total += nameH + NAME_GAP + bubbleH + BUBBLE_GAP;
        }
        return total;
    }

    /** Word-wrapping into lines, measuring at LOG_TEXT_SCALE in pixels. */
    private static List<String> wrapLines(TextRenderer tr, String text, int maxPixels) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty() || maxPixels <= 4) {
            out.add(text == null ? "" : text);
            return out;
        }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int linePx = 0;

        for (String w : words) {
            String toAdd = (line.isEmpty() ? w : " " + w);
            int addPx = scaledWidth(tr, toAdd, LOG_TEXT_SCALE);
            if (linePx + addPx <= maxPixels) {
                line.append(toAdd);
                linePx += addPx;
            } else {
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                    linePx = 0;
                }
                int wPx = scaledWidth(tr, w, LOG_TEXT_SCALE);
                if (wPx <= maxPixels) {
                    line.append(w);
                    linePx = scaledWidth(tr, line.toString(), LOG_TEXT_SCALE);
                } else {

                    int idx = 0;
                    while (idx < w.length()) {
                        int take = 0, accPx = 0;
                        while (idx + take < w.length()) {
                            String sub = w.substring(idx, idx + take + 1);
                            int subPx = scaledWidth(tr, sub, LOG_TEXT_SCALE);
                            if (accPx + subPx > maxPixels) break;
                            take++; accPx += subPx;
                        }
                        if (take == 0) take = 1;
                        out.add(w.substring(idx, idx + take));
                        idx += take;
                    }
                }
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }


    private boolean hasSelection() { return selAnchor != -1 && selAnchor != caret; }
    private int selMin() { return Math.min(selAnchor, caret); }
    private int selMax() { return Math.max(selAnchor, caret); }
    private void clearSelection() { selAnchor = -1; }
    private String getSelectedText() {
        if (!hasSelection()) return "";
        return input.substring(selMin(), selMax());
    }
    private void deleteSelection() {
        if (!hasSelection()) return;
        int a = selMin(), b = selMax();
        input.delete(a, b);
        caret = a;
        clearSelection();
    }
    private void replaceSelection(String s) {
        if (hasSelection()) deleteSelection();
        input.insert(caret, s);
        caret += s.length();
        clearSelection();
    }
    private void moveCaret(int dest, boolean extendSelection) {
        dest = Math.max(0, Math.min(input.length(), dest));
        if (extendSelection) {
            if (selAnchor == -1) selAnchor = caret;
        } else {
            clearSelection();
        }
        caret = dest;
    }

    private int prevWordIndex(int from) {
        int i = Math.max(0, Math.min(input.length(), from));

        while (i > 0 && Character.isWhitespace(input.charAt(i - 1))) i--;

        while (i > 0 && !Character.isWhitespace(input.charAt(i - 1))) i--;
        return i;
    }

    private int nextWordIndex(int from) {
        int i = Math.max(0, Math.min(input.length(), from));

        while (i < input.length() && Character.isWhitespace(input.charAt(i))) i++;

        while (i < input.length() && !Character.isWhitespace(input.charAt(i))) i++;
        return i;
    }

    private void insertText(String s) { replaceSelection(s); }

    private void sendIfAny() {
        String msg = input.toString().trim();
        if (msg.isEmpty()) return;

        UUID pid = ClientPartyHudData.partyId;
        if (pid != null) ClientPartyChat.send(pid, msg);

        input.setLength(0);
        caret = 0;
        clearSelection();

        inputScrollPx = 0;
        scrollPx = 0;
    }

    private static int scaledWidth(TextRenderer tr, String s, float scale) {
        return Math.round(tr.getWidth(s) * scale);
    }

    private void ensureCaretVisible(int caretScaledX, int totalScaledW, int visibleW) {
        int margin = 2;

        int left = inputScrollPx;
        int right = inputScrollPx + visibleW;

        if (caretScaledX + margin > right) {
            inputScrollPx = caretScaledX + margin - visibleW;
        } else if (caretScaledX - margin < left) {
            inputScrollPx = Math.max(0, caretScaledX - margin);
        }

        int maxScroll = Math.max(0, totalScaledW - visibleW);
        if (inputScrollPx > maxScroll) inputScrollPx = maxScroll;
        if (inputScrollPx < 0) inputScrollPx = 0;
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
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    /** Pixel-art soft rounded rectangle */
    private static void drawSoftRounded(DrawContext ctx, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(2, Math.min(r, Math.min(w / 2, h / 2)));
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, argb);
        ctx.fill(x + r - 1, y, x + w - (r - 1), y + 1, argb);
        ctx.fill(x + r - 1, y + h - 1, x + w - (r - 1), y + h, argb);
        ctx.fill(x, y + r - 1, x + 1, y + h - (r - 1), argb);
        ctx.fill(x + w - 1, y + r - 1, x + w, y + h - (r - 1), argb);
    }
}
