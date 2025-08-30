package net.pixeldreamstudios.rpgsystems.client.title.screen.box;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.title.Title;

import java.util.ArrayList;
import java.util.List;

public final class TitleDescriptionBox {
    private static final int RIGHT_PADDING = 6;
    private static final int BOTTOM_PADDING = 12;
    private static final int LEFT_BOX_WIDTH = 62;
    private static final int LEFT_BOX_LEFT_PADDING = 6;
    private static final int GUTTER = 8;
    private static final int BOX_HEIGHT = 64;

    private int screenX;
    private int screenY;
    private int bgWidth;
    private int bgHeight;

    private final TextRenderer font;
    private Title current;

    private float textScale = 0.5f;
    private int scrollY = 0;
    private int maxScrollCached = 0;

    public TitleDescriptionBox(int screenX, int screenY, int bgWidth, int bgHeight) {
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

    public void setTextScale(float scale) {
        this.textScale = Math.max(0.5f, Math.min(2.0f, scale));
    }

    public void setTitle(Title title) {
        this.current = title;
        this.scrollY = 0;
    }

    public void render(DrawContext ctx) {
        int boxW = bgWidth - LEFT_BOX_LEFT_PADDING - LEFT_BOX_WIDTH - GUTTER - RIGHT_PADDING;
        int boxX = screenX + bgWidth - RIGHT_PADDING - boxW;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        if (current == null) {
            maxScrollCached = 0;
            scrollY = 0;
            return;
        }

        int innerX = boxX + 4;
        int innerW = boxW - 8;
        int viewportTop = boxY + 2;
        int viewportBottom = boxY + BOX_HEIGHT - 2;

        ctx.enableScissor(innerX, viewportTop, innerX + innerW, viewportBottom);

        int y = boxY + 3 - scrollY;

        Text name = current.displayName != null ? current.displayName : Text.literal(current.id.toString());
        drawScaled(ctx, name.asOrderedText(), innerX, y, 0xFFFFFF, textScale);
        y += scaled(10);

        if (current.description != null && !current.description.getString().isEmpty()) {
            int wrapWidth = Math.max(1, Math.round(innerW / textScale));
            List<OrderedText> lines = font.wrapLines(current.description, wrapWidth);
            for (OrderedText ot : lines) {
                drawScaled(ctx, ot, innerX, y, 0xDDDDDD, textScale);
                y += scaled(9);
            }
        }

        if (!current.bonuses.isEmpty()) {
            drawScaled(ctx, Text.translatable("title.rpgsystems.bonuses").asOrderedText(), innerX, y, 0xFFFFFF, textScale);
            y += scaled(10);

            List<Text> bonusTexts = formatBonuses();
            int wrapWidth = Math.max(1, Math.round(innerW / textScale));
            for (Text bt : bonusTexts) {
                List<OrderedText> wrapped = font.wrapLines(bt, wrapWidth);
                for (OrderedText ot : wrapped) {
                    drawScaled(ctx, ot, innerX, y, 0xC0FFFFFF, textScale);
                    y += scaled(9);
                }
            }
        }

        ctx.disableScissor();

        int contentHeight = y - (boxY + 3);
        int viewportH = viewportBottom - viewportTop;
        int maxScroll = Math.max(0, contentHeight - viewportH);
        maxScrollCached = maxScroll;
        if (scrollY > maxScrollCached) scrollY = maxScrollCached;
        if (scrollY < 0) scrollY = 0;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int boxW = bgWidth - LEFT_BOX_LEFT_PADDING - LEFT_BOX_WIDTH - GUTTER - RIGHT_PADDING;
        int boxX = screenX + bgWidth - RIGHT_PADDING - boxW;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        int innerX = boxX + 4;
        int innerW = boxW - 8;
        int viewportTop = boxY + 2;
        int viewportBottom = boxY + BOX_HEIGHT - 2;

        if (mouseX < innerX || mouseX > innerX + innerW || mouseY < viewportTop || mouseY > viewportBottom) {
            return false;
        }
        if (maxScrollCached <= 0) {
            return false;
        }

        int before = scrollY;
        int step = scaled(12);
        if (amount > 0) scrollY = Math.max(0, scrollY - step);
        else if (amount < 0) scrollY = Math.min(maxScrollCached, scrollY + step);
        return scrollY != before;
    }

    private List<Text> formatBonuses() {
        List<Text> out = new ArrayList<>();
        for (Title.Bonus b : current.bonuses) {
            String attrKey = b.attribute.value().getTranslationKey();
            Text attrName = Text.translatable(attrKey);
            String sign = b.amount >= 0 ? "+" : "";
            if (b.operation == EntityAttributeModifier.Operation.ADD_VALUE) {
                out.add(Text.literal(sign + trim(b.amount) + " ").append(attrName));
            } else if (b.operation == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                out.add(Text.literal(sign + trim(b.amount * 100.0) + "% ").append(attrName).append(Text.literal(" (base)")));
            } else {
                out.add(Text.literal(sign + trim(b.amount * 100.0) + "% ").append(attrName).append(Text.literal(" (total)")));
            }
        }
        return out;
    }

    private static String trim(double v) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    private int scaled(int px) {
        return Math.max(1, Math.round(px * textScale));
    }

    private void drawScaled(DrawContext ctx, OrderedText text, int x, int y, int color, float scale) {
        if (scale == 1.0f) {
            ctx.drawTextWithShadow(font, text, x, y, color);
            return;
        }
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0f);
        int sx = Math.round(x / scale);
        int sy = Math.round(y / scale);
        ctx.drawTextWithShadow(font, text, sx, sy, color);
        ctx.getMatrices().pop();
    }
}
