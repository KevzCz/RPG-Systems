package net.pixeldreamstudios.rpgsystems.client.title.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil;
import net.pixeldreamstudios.rpgsystems.title.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class TitleDescriptionBox {
    private static final int RIGHT_PADDING = 6;
    private static final int BOTTOM_PADDING = 12;
    private static final int LEFT_BOX_WIDTH = 62;
    private static final int LEFT_BOX_LEFT_PADDING = 6;
    private static final int GUTTER = 8;
    private static final int BOX_HEIGHT = 64;
    private static final int COLOR_HEADER     = 0xFFF0F0F0;
    private static final int COLOR_DESC       = 0xFFE0E0E0;
    private static final int COLOR_BONUS_TEXT = 0xCCFFFFFF;
    private static final int COLOR_DONE       = 0xFF7BFF7B;
    private static final int COLOR_TODO       = 0xFFFFFFFF;
    private static final int COLOR_HINT_FG    = 0xFFBFE5FF;

    private static final int UI_MARGIN_Y = 2;
    private static final int BULLET_PAD  = 4;
    private static final Identifier CHECK_OFF = Identifier.of("rpg-systems", "textures/gui/title/checkbox_todo.png");
    private static final Identifier CHECK_ON  = Identifier.of("rpg-systems", "textures/gui/title/checkbox_done.png");
    private static final Identifier INFO_ICON = Identifier.of("rpg-systems", "textures/gui/title/info.png");
    private static final int ICON_SIZE = 9;
    private static final int INFO_GAP  = 2;

    private int screenX;
    private int screenY;
    private int bgWidth;
    private int bgHeight;

    private final TextRenderer font;
    private Title current;

    private float textScale = 0.5f;
    private int scrollY = 0;
    private int maxScrollCached = 0;

    private static final class HintSpot {
        final int x, y, w, h;
        final Text hint;
        HintSpot(int x, int y, int w, int h, Text hint) { this.x=x; this.y=y; this.w=w; this.h=h; this.hint=hint; }
        boolean contains(double mx, double my) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }
    }
    private final List<HintSpot> hintSpots = new ArrayList<>();

    public TitleDescriptionBox(int screenX, int screenY, int bgWidth, int bgHeight) {
        this.font = MinecraftClient.getInstance().textRenderer;
        this.screenX = screenX;
        this.screenY = screenY;
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
    }

    public void setScreenOrigin(int screenX, int screenY) { this.screenX = screenX; this.screenY = screenY; }
    public void setBackgroundSize(int bgWidth, int bgHeight) { this.bgWidth = bgWidth; this.bgHeight = bgHeight; }
    public void setTextScale(float scale) { this.textScale = Math.max(0.5f, Math.min(2.0f, scale)); }
    public void setTitle(Title title) { this.current = title; this.scrollY = 0; }

    public void render(DrawContext ctx) {
        int boxW = bgWidth - LEFT_BOX_LEFT_PADDING - LEFT_BOX_WIDTH - GUTTER - RIGHT_PADDING;
        int boxX = screenX + bgWidth - RIGHT_PADDING - boxW;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        hintSpots.clear();

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

        int y = boxY + 3;

        String rawTitle = current.displayName != null ? current.displayName.getString() : current.id.toString();
        TitleStyleUtil.Parsed parsed = TitleStyleUtil.parse(rawTitle);
        int headerRgb = 0xFF000000 | TitleStyleUtil.resolveOrWhite(parsed.baseRgb());
        drawScaled(ctx, Text.literal(parsed.text()).asOrderedText(), innerX, y - scrollY, headerRgb, textScale);
        y += scaled(12) + UI_MARGIN_Y;

        if (current.description != null && !current.description.getString().isEmpty()) {
            int wrapWidth = Math.max(1, Math.round(innerW / textScale));
            List<OrderedText> lines = font.wrapLines(current.description, wrapWidth);
            for (OrderedText ot : lines) {
                drawScaled(ctx, ot, innerX, y - scrollY, COLOR_DESC, textScale);
                y += scaled(9);
            }
            y += scaled(4);
        }

        if (current.conditions != null && !current.conditions.isEmpty()) {
            drawScaled(ctx, Text.translatable("title.rpgsystems.conditions").asOrderedText(), innerX, y - scrollY, COLOR_HEADER, textScale);
            y += scaled(10);

            int checkSize = scaled(ICON_SIZE);
            int infoSize  = scaled(ICON_SIZE);

            List<CondLine> lines = buildConditionLines();
            for (CondLine cl : lines) {
                int infoReserve = (cl.hint != null ? (infoSize + scaled(INFO_GAP)) : 0);

                int textX = innerX + checkSize + BULLET_PAD;
                int availableTextW = innerW - (textX - innerX) - infoReserve;
                int wrapW = Math.max(1, Math.round(availableTextW / textScale));

                int startY = y;
                List<OrderedText> wrapped = font.wrapLines(cl.text, wrapW);
                int lineH = scaled(9);
                int blockH = Math.max(checkSize, Math.max(lineH, wrapped.size() * lineH));

                int checkY = startY - scrollY + (blockH - checkSize) / 2;
                Identifier checkSprite = cl.done ? CHECK_ON : CHECK_OFF;
                drawSprite(ctx, checkSprite, innerX, checkY, checkSize, checkSize);

                int ty = startY;
                for (OrderedText ot : wrapped) {
                    drawScaled(ctx, ot, textX, ty - scrollY, cl.color, textScale);
                    ty += lineH;
                }

                if (cl.hint != null) {
                    int badgeX = innerX + innerW - infoSize;
                    int badgeY = startY - scrollY + (blockH - infoSize) / 2;
                    drawSprite(ctx, INFO_ICON, badgeX, badgeY, infoSize, infoSize);
                    hintSpots.add(new HintSpot(badgeX, badgeY, infoSize, infoSize, cl.hint));
                }

                y += blockH + UI_MARGIN_Y;
            }

            y += scaled(4);
        }

        if (!current.bonuses.isEmpty()) {
            drawScaled(ctx, Text.translatable("title.rpgsystems.bonuses").asOrderedText(), innerX, y - scrollY, COLOR_HEADER, textScale);
            y += scaled(10);

            List<Text> bonusTexts = formatBonuses();
            int wrapWidth = Math.max(1, Math.round(innerW / textScale));
            for (Text bt : bonusTexts) {
                List<OrderedText> wrapped = font.wrapLines(bt, wrapWidth);
                for (OrderedText ot : wrapped) {
                    drawScaled(ctx, ot, innerX, y - scrollY, COLOR_BONUS_TEXT, textScale);
                    y += scaled(9);
                }
                y += UI_MARGIN_Y;
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

    public void renderHints(DrawContext ctx, int mouseX, int mouseY) {
        for (HintSpot s : hintSpots) {
            if (s.contains(mouseX, mouseY)) {
                ctx.drawTooltip(font, s.hint, mouseX, mouseY);
                break;
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int boxW = bgWidth - LEFT_BOX_LEFT_PADDING - LEFT_BOX_WIDTH - GUTTER - RIGHT_PADDING;
        int boxX = screenX + bgWidth - RIGHT_PADDING - boxW;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        int innerX = boxX + 4;
        int innerW = boxW - 8;
        int viewportTop = boxY + 2;
        int viewportBottom = boxY + BOX_HEIGHT - 2;

        if (mouseX < innerX || mouseX > innerX + innerW || mouseY < viewportTop || mouseY > viewportBottom) return false;
        if (maxScrollCached <= 0) return false;

        int before = scrollY;
        int step = scaled(12);
        if (amount > 0) scrollY = Math.max(0, scrollY - step);
        else if (amount < 0) scrollY = Math.min(maxScrollCached, scrollY + step);
        return scrollY != before;
    }


    private void drawSprite(DrawContext ctx, Identifier id, int x, int y, int w, int h) {
        ctx.drawTexture(id, x, y, 0, 0, w, h, w, h);
    }

    private List<Text> formatBonuses() {
        List<Text> out = new ArrayList<>();
        for (Title.Bonus b : current.bonuses) {
            if (b.spellId != null && b.spellId.isPresent()) {
                Identifier id = b.spellId.get();
                Text nice = resolveSpellName(id);
                out.add(Text.literal("★ ")
                        .append(Text.translatable("title.rpgsystems.bonus.spell"))
                        .append(Text.literal(" "))
                        .append(nice));
                continue;
            }
            String attrKey = b.attribute.value().getTranslationKey();
            Text attrName = Text.translatable(attrKey);
            String sign = b.amount >= 0 ? "+" : "";
            if (b.operation == EntityAttributeModifier.Operation.ADD_VALUE) {
                out.add(Text.literal("• ").append(Text.literal(sign + trim(b.amount) + " ")).append(attrName));
            } else if (b.operation == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                out.add(Text.literal("• ").append(Text.literal(sign + trim(b.amount * 100.0) + "% ")).append(attrName).append(Text.literal(" (base)")));
            } else {
                out.add(Text.literal("• ").append(Text.literal(sign + trim(b.amount * 100.0) + "% ")).append(attrName).append(Text.literal(" (total)")));
            }
        }
        return out;
    }

    private Text resolveSpellName(Identifier id) {
        var client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            var reg = net.spell_engine.api.spell.registry.SpellRegistry.from(client.world);
            var entry = reg.getEntry(id).orElse(null);
            if (entry != null) {
                Text t = Text.translatable("spell." + id.getNamespace() + "." + id.getPath());
                String raw = t.getString();
                if (!raw.equals("spell." + id.getNamespace() + "." + id.getPath())) return t;
            }
        }
        String nice = id.getPath().replace('_', ' ');
        String[] parts = nice.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(p.substring(0,1).toUpperCase(Locale.ROOT)).append(p.substring(1));
                if (i+1 < parts.length) sb.append(' ');
            }
        }
        return Text.literal(sb.toString());
    }

    private static final class CondLine {
        final Text text;
        final Text hint;
        final int color;
        final boolean done;
        CondLine(Text text, Text hint, int color, boolean done) {
            this.text = text; this.hint = hint; this.color = color; this.done = done;
        }
    }

    private List<CondLine> buildConditionLines() {
        List<CondLine> out = new ArrayList<>();
        List<TitleClientData.CondProg> progs = TitleClientData.getProgress(current.id);

        for (int i = 0; i < current.conditions.size(); i++) {
            Title.Condition c = current.conditions.get(i);

            long cur = 0;
            boolean doneFlag = false;
            if (i < progs.size()) {
                cur = progs.get(i).current();
                doneFlag = progs.get(i).done();
            }

            if (c.hidden && c.hint.isPresent()) {
                int color = doneFlag ? COLOR_DONE : COLOR_TODO;
                out.add(new CondLine(Text.literal(c.hint.get()), null, color, doneFlag));
                continue;
            }

            switch (c.type) {
                case KILL_MOBS -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    Text tail = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                    MutableText line;
                    if (c.entityType.isPresent()) {
                        Text et = Text.translatable(Registries.ENTITY_TYPE.get(c.entityType.get()).getTranslationKey());
                        line = Text.translatable("title.rpgsystems.condition.kill_specific", target, et).append(tail);
                    } else if (c.entitySpec.isPresent()) {
                        String spec = c.entitySpec.get();
                        if ("any".equalsIgnoreCase(spec)) {
                            line = Text.translatable("title.rpgsystems.condition.kill_any", target).append(tail);
                        } else if (spec.endsWith(":*")) {
                            String ns = spec.substring(0, spec.indexOf(':'));
                            line = Text.literal("Defeat " + target + " mobs from " + ns).append(tail);
                        } else {
                            Identifier mid = Identifier.tryParse(spec);
                            if (mid != null && Registries.ENTITY_TYPE.containsId(mid)) {
                                Text et = Text.translatable(Registries.ENTITY_TYPE.get(mid).getTranslationKey());
                                line = Text.translatable("title.rpgsystems.condition.kill_specific", target, et).append(tail);
                            } else {
                                line = Text.literal("Defeat " + target + " mobs: " + spec).append(tail);
                            }
                        }
                    } else {
                        line = Text.translatable("title.rpgsystems.condition.kill_any", target).append(tail);
                    }
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
                case OBTAIN_ITEM -> {
                    if (c.item.isPresent()) {
                        int target = Math.max(1, c.count);
                        Item item = Registries.ITEM.get(c.item.get());
                        Text it = Text.translatable(item.getTranslationKey());
                        boolean reached = doneFlag || (cur >= target);
                        int color = reached ? COLOR_DONE : COLOR_TODO;
                        MutableText line = Text.translatable("title.rpgsystems.condition.obtain_x", it, target)
                                .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                        out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                    }
                }
                case WALK_BLOCKS -> {
                    long target = Math.max(1, c.distance);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = Text.translatable("title.rpgsystems.condition.walk", target)
                            .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
                case CRAFT_ITEM -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    if (c.item.isPresent()) {
                        Item item = Registries.ITEM.get(c.item.get());
                        Text it = Text.translatable(item.getTranslationKey());
                        MutableText line = Text.literal("Craft " + target + " ").append(it)
                                .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                        out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                    }
                }
                case MINE_BLOCKS -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = c.block.isPresent()
                            ? Text.literal("Mine " + target + " ").append(Text.translatable(Registries.BLOCK.get(c.block.get()).getTranslationKey()))
                            : Text.literal("Mine " + target + " blocks");
                    line = line.append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
                case REACH_LEVEL -> {
                    int target = Math.max(1, c.level);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = Text.literal("Reach total level " + target)
                            .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
                case ADVANCEMENT -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    int target = 1;
                    MutableText base = c.advancement.isPresent()
                            ? Text.translatable("title.rpgsystems.condition.advancement", c.advancement.get().toString())
                            : Text.translatable("title.rpgsystems.condition.advancement", "");
                    MutableText line = base.append(Text.literal(" (" + (reached ? 1 : Math.min(cur, target)) + "/" + target + ")"));
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
                case VISIT_BIOME -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = c.biome.isPresent()
                            ? Text.literal("Visit ").append(Text.translatable("biome." + c.biome.get().getNamespace() + "." + c.biome.get().getPath()))
                            : Text.literal("Visit a biome");
                    line = line.append(Text.literal(" (" + (reached ? 1 : 0) + "/1)"));
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
                case ENTER_DIMENSION -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = c.dimension.isPresent()
                            ? Text.literal("Enter " + c.dimension.get())
                            : Text.literal("Enter a dimension");
                    line = line.append(Text.literal(" (" + (reached ? 1 : 0) + "/1)"));
                    out.add(new CondLine(line, c.hint.orElse(null) == null ? null : Text.literal(c.hint.get()), color, reached));
                }
            }
        }
        return out;
    }

    private static String trim(double v) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        if (s.indexOf('.') >= 0) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
    private int scaled(int px) { return Math.max(1, Math.round(px * textScale)); }

    private void drawScaled(DrawContext ctx, OrderedText text, int x, int y, int color, float scale) {
        if (scale == 1.0f) { ctx.drawTextWithShadow(font, text, x, y, color); return; }
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0f);
        int sx = Math.round(x / scale);
        int sy = Math.round(y / scale);
        ctx.drawTextWithShadow(font, text, sx, sy, color);
        ctx.getMatrices().pop();
    }
}
