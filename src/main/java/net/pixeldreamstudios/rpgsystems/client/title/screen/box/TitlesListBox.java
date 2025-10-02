package net.pixeldreamstudios.rpgsystems.client.title.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class TitlesListBox {
    private static final int BOX_WIDTH = 59;
    private static final int BOX_HEIGHT = 64;
    private static final int LEFT_PADDING = 7;
    private static final int BOTTOM_PADDING = 12;
    private static final int VISIBLE_ROWS = 4;

    private static final Identifier LOCKED_ICON = Identifier.of("rpg-systems", "textures/gui/title/locked.png");
    private static final int LOCK_SIZE = 8;
    private static final int LOCK_RIGHT_INSET = 4;

    private static final int HIGHLIGHT_COLOR = 0x44FFFFFF;

    private static final int ROW_INNER_PAD_L = 4;
    private static final int ROW_INNER_PAD_R = 6;

    private static final float MARQUEE_SPEED_PX_PER_SEC = 42f;
    private static final float MARQUEE_HOLD_SECONDS = 1.5f;

    private int screenX;
    private int screenY;
    private int bgWidth;
    private int bgHeight;

    private final TextRenderer font;
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private final List<Consumer<Title>> selectionListeners = new ArrayList<>();
    private float textScale = 0.5f;

    private int scrollbarOffsetX = 0;
    private int scrollbarOffsetY = 0;
    private int scrollbarWidth = 1;
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0;

    private static final int SCROLLBAR_TRACK_COLOR = 0x33000000;
    private static final int SCROLLBAR_BORDER_COLOR = 0x66000000;
    private static final int SCROLLBAR_THUMB_COLOR = 0x99FFFFFF;
    private static final int SCROLLBAR_THUMB_HOVER_COLOR = 0xBBFFFFFF;

    public TitlesListBox(int screenX, int screenY, int bgWidth, int bgHeight) {
        this.font = MinecraftClient.getInstance().textRenderer;
        this.screenX = screenX;
        this.screenY = screenY;
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
    }

    public void setScreenOrigin(int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;
    }

    public void setBackgroundSize(int bgWidth, int bgHeight) {
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
    }

    public void setSelectionListener(Consumer<Title> listener) {
        this.selectionListeners.clear();
        this.selectionListeners.add(listener);
    }

    public void setOnSelectionChanged(Consumer<Title> listener) {
        this.selectionListeners.add(listener);
    }

    public void setScrollbarWidth(int width) {
        this.scrollbarWidth = Math.max(2, Math.min(12, width));
    }

    public Title getSelectedTitle() {
        List<Title> all = visibleTitles();
        if (selectedIndex < 0 || selectedIndex >= all.size()) return null;
        return all.get(selectedIndex);
    }

    public void selectById(Identifier id) {
        if (id == null) return;
        List<Title> all = visibleTitles();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(id)) {
                selectedIndex = i;
                notifySelectionChanged();
                return;
            }
        }
    }

    public void selectFirstVisible() {
        List<Title> all = visibleTitles();
        if (!all.isEmpty()) {
            selectedIndex = 0;
            notifySelectionChanged();
        }
    }

    private void notifySelectionChanged() {
        List<Title> all = visibleTitles();
        if (selectedIndex >= 0 && selectedIndex < all.size()) {
            Title selected = all.get(selectedIndex);
            for (Consumer<Title> listener : selectionListeners) {
                listener.accept(selected);
            }
        }
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int boxX = screenX + LEFT_PADDING;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        List<Title> allTitles = visibleTitles();
        int maxOffset = Math.max(0, allTitles.size() - VISIBLE_ROWS);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (selectedIndex >= allTitles.size()) selectedIndex = -1;

        int innerLeft = boxX + ROW_INNER_PAD_L;
        int innerRight = boxX + BOX_WIDTH - ROW_INNER_PAD_R;

        long now = Util.getMeasuringTimeMs();
        float rowH = BOX_HEIGHT / (float) VISIBLE_ROWS;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= allTitles.size()) break;

            Title t = allTitles.get(index);
            String raw = t.displayName != null ? t.displayName.getString() : t.id.toString();
            String clean = TitleStyleUtil.parse(raw).text();
            Text label = Text.literal(clean);
            OrderedText ordered = label.asOrderedText();

            int partTop = boxY + Math.round(rowH * row);
            int partBottom = (row == VISIBLE_ROWS - 1) ? (boxY + BOX_HEIGHT) : (boxY + Math.round(rowH * (row + 1)));
            int textHScaled = Math.max(1, Math.round(font.fontHeight * textScale));
            int textY = partTop + Math.max(0, Math.round(((partBottom - partTop) - textHScaled) / 2f));

            if (index == selectedIndex) {
                ctx.fill(boxX + 1, partTop + 1, boxX + BOX_WIDTH - 1, partBottom - 1, HIGHLIGHT_COLOR);
            }

            boolean unlocked = TitleClientData.getSelfUnlocked().contains(t.id);
            int lockReserve = unlocked ? 0 : (LOCK_RIGHT_INSET + LOCK_SIZE);
            int contentLeft = innerLeft;
            int contentRight = innerRight - lockReserve;
            int contentWidth = Math.max(1, contentRight - contentLeft);

            int rowClipTop = partTop + 1;
            int rowClipBottom = partBottom - 1;
            ctx.enableScissor(contentLeft, rowClipTop, contentRight, rowClipBottom);

            int fullWidth = font.getWidth(ordered);
            int fullWidthScaled = Math.round(fullWidth * textScale);
            boolean marquee = fullWidthScaled > contentWidth;

            if (!marquee) {
                drawScaled(ctx, ordered, contentLeft, textY, 0xFFFFFFFF, textScale);
            } else {
                float overflow = Math.max(1f, fullWidthScaled - contentWidth);
                float travelTime = overflow / MARQUEE_SPEED_PX_PER_SEC;
                float cycle = MARQUEE_HOLD_SECONDS + travelTime + MARQUEE_HOLD_SECONDS + travelTime;

                float seconds = (now % 1_000_000L) / 1000f;
                float phaseOffset = row * 0.27f;
                float ts = (seconds + phaseOffset) % cycle;

                float offsetPx;
                if (ts < MARQUEE_HOLD_SECONDS) {
                    offsetPx = 0f;
                } else if (ts < MARQUEE_HOLD_SECONDS + travelTime) {
                    offsetPx = (ts - MARQUEE_HOLD_SECONDS) * MARQUEE_SPEED_PX_PER_SEC;
                } else if (ts < MARQUEE_HOLD_SECONDS + travelTime + MARQUEE_HOLD_SECONDS) {
                    offsetPx = overflow;
                } else {
                    float backT = ts - (MARQUEE_HOLD_SECONDS + travelTime + MARQUEE_HOLD_SECONDS);
                    offsetPx = overflow - backT * MARQUEE_SPEED_PX_PER_SEC;
                }

                int drawX = contentLeft - Math.round(offsetPx);
                drawScaled(ctx, ordered, drawX, textY, 0xFFFFFFFF, textScale);
            }

            ctx.disableScissor();

            if (!unlocked) {
                int iconX = boxX + BOX_WIDTH - LOCK_RIGHT_INSET - LOCK_SIZE;
                int iconY = partTop + Math.max(0, Math.round((rowH - LOCK_SIZE) / 2f));
                ctx.drawTexture(LOCKED_ICON, iconX, iconY, 0, 0, LOCK_SIZE, LOCK_SIZE, LOCK_SIZE, LOCK_SIZE);
            }
        }

        renderScrollbar(ctx, mouseX, mouseY, allTitles);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int[] box = boxRect();
        int boxX = box[0], boxY = box[1], boxW = box[2], boxH = box[3];
        if (mouseX < boxX || mouseX > boxX + boxW || mouseY < boxY || mouseY > boxY + boxH) {
            return false;
        }

        List<Title> all = visibleTitles();
        int maxOffset = Math.max(0, all.size() - VISIBLE_ROWS);
        if (amount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (amount < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] r = boxRect();
        int boxX = r[0], boxY = r[1], boxW = r[2], boxH = r[3];
        if (mouseX < boxX || mouseX > boxX + boxW || mouseY < boxY || mouseY > boxY + boxH) {
            return false;
        }
        if (button != 0) return true;

        if (handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }

        float rowH = BOX_HEIGHT / (float) VISIBLE_ROWS;
        int relY = (int) (mouseY - boxY);
        int row = Math.min(VISIBLE_ROWS - 1, Math.max(0, (int) Math.floor(relY / rowH)));
        int index = scrollOffset + row;

        List<Title> all = visibleTitles();
        if (index < all.size()) {
            selectedIndex = index;
            Title selected = all.get(index);
            for (Consumer<Title> listener : selectionListeners) {
                listener.accept(selected);
            }
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!draggingScrollbar) return false;

        List<Title> all = visibleTitles();
        int maxOffset = Math.max(0, all.size() - VISIBLE_ROWS);
        if (maxOffset <= 0) return true;

        int[] track = scrollbarTrackRect();
        int trackX = track[0], trackY = track[1], trackW = track[2], trackH = track[3];
        int thumbH = scrollbarThumbHeight(all.size(), trackH);
        int usable = trackH - thumbH;

        int newTop = (int) Math.round(mouseY) - dragGrabOffsetY;
        newTop = Math.max(trackY, Math.min(trackY + usable, newTop));

        float ratio = usable <= 0 ? 0f : (newTop - trackY) / (float) usable;
        scrollOffset = Math.max(0, Math.min(maxOffset, Math.round(ratio * maxOffset)));
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    private int[] boxRect() {
        int boxX = screenX + LEFT_PADDING;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;
        return new int[] { boxX, boxY, BOX_WIDTH, BOX_HEIGHT };
    }

    private void drawScaled(DrawContext ctx, OrderedText text, int x, int y, int color, float scale) {
        if (scale == 1.0f) {
            ctx.drawTextWithShadow(font, text, x, y, color);
            return;
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 0);
        ctx.getMatrices().scale(scale, scale, 1.0f);
        int sx = Math.round(x / scale);
        int sy = Math.round(y / scale);
        ctx.drawTextWithShadow(font, text, sx, sy, color);
        ctx.getMatrices().pop();
    }

    private static String sortKeyFor(Title t) {
        String raw = t.displayName != null ? t.displayName.getString() : t.id.toString();
        String clean = TitleStyleUtil.parse(raw).text().trim();
        if (clean.isEmpty()) clean = t.id.toString();
        return clean;
    }

    private List<Title> visibleTitles() {
        Set<Identifier> unlockedSet = TitleClientData.getSelfUnlocked();
        List<Title> list = new ArrayList<>();
        for (Title t : TitleRegistry.all().values()) {
            if (!t.hidden || unlockedSet.contains(t.id)) {
                list.add(t);
            }
        }
        list.sort((a, b) -> {
            String ak = sortKeyFor(a);
            String bk = sortKeyFor(b);
            int c = String.CASE_INSENSITIVE_ORDER.compare(ak, bk);
            if (c != 0) return c;
            return a.id.toString().compareTo(b.id.toString());
        });
        return list;
    }

    private void renderScrollbar(DrawContext ctx, int mouseX, int mouseY, List<Title> allTitles) {
        int total = allTitles.size();
        int maxOffset = Math.max(0, total - VISIBLE_ROWS);
        if (maxOffset <= 0) return;

        int[] track = scrollbarTrackRect();
        int trackX = track[0], trackY = track[1], trackW = track[2], trackH = track[3];

        ctx.fill(trackX, trackY, trackX + trackW, trackY + trackH, SCROLLBAR_TRACK_COLOR);
        ctx.drawBorder(trackX, trackY, trackW, trackH, SCROLLBAR_BORDER_COLOR);

        int thumbH = scrollbarThumbHeight(total, trackH);
        int usable = Math.max(0, trackH - thumbH);
        float ratio = (maxOffset == 0) ? 0f : (scrollOffset / (float) maxOffset);
        int thumbTop = trackY + Math.round(usable * ratio);

        boolean hoveringThumb = mouseX >= trackX && mouseX <= trackX + trackW && mouseY >= thumbTop && mouseY <= thumbTop + thumbH;
        int thumbColor = hoveringThumb || draggingScrollbar ? SCROLLBAR_THUMB_HOVER_COLOR : SCROLLBAR_THUMB_COLOR;

        ctx.fill(trackX + 1, thumbTop, trackX + trackW - 1, thumbTop + thumbH, thumbColor);
        ctx.drawBorder(trackX + 1, thumbTop, trackW - 2, thumbH, SCROLLBAR_BORDER_COLOR);
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        List<Title> all = visibleTitles();
        int maxOffset = Math.max(0, all.size() - VISIBLE_ROWS);
        if (maxOffset <= 0) return false;

        int[] track = scrollbarTrackRect();
        int trackX = track[0], trackY = track[1], trackW = track[2], trackH = track[3];
        if (mouseX < trackX || mouseX > trackX + trackW || mouseY < trackY || mouseY > trackY + trackH) {
            return false;
        }

        int thumbH = scrollbarThumbHeight(all.size(), trackH);
        int usable = Math.max(0, trackH - thumbH);
        float ratio = (maxOffset == 0) ? 0f : (scrollOffset / (float) maxOffset);
        int thumbTop = trackY + Math.round(usable * ratio);
        int thumbBottom = thumbTop + thumbH;

        if (mouseY >= thumbTop && mouseY <= thumbBottom) {
            draggingScrollbar = true;
            dragGrabOffsetY = (int) Math.round(mouseY) - thumbTop;
            return true;
        }

        if (mouseY < thumbTop) {
            scrollOffset = Math.max(0, scrollOffset - VISIBLE_ROWS);
        } else {
            scrollOffset = Math.min(maxOffset, scrollOffset + VISIBLE_ROWS);
        }
        return true;
    }

    private int[] scrollbarTrackRect() {
        int[] r = boxRect();
        int boxX = r[0], boxY = r[1], boxW = r[2], boxH = r[3];

        int trackW = scrollbarWidth;
        int baseX = boxX + boxW - trackW;
        int baseY = boxY;

        int trackX = baseX + scrollbarOffsetX;
        int trackY = baseY + scrollbarOffsetY;

        return new int[] { trackX, trackY, trackW, boxH };
    }

    private int scrollbarThumbHeight(int totalItems, int trackHeight) {
        if (totalItems <= 0) return Math.max(8, trackHeight);
        float visibleRatio = Math.min(1f, VISIBLE_ROWS / (float) totalItems);
        int h = Math.round(trackHeight * visibleRatio);
        return Math.max(8, Math.min(trackHeight, h));
    }
}
