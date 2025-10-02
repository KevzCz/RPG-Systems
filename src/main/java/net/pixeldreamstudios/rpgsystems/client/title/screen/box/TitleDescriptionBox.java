package net.pixeldreamstudios.rpgsystems.client.title.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil;
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.client.title.TitleIconRenderer;
import net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.spell_engine.client.util.SpellRender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    private static final int COLOR_DIVIDER    = 0x40FFFFFF;
    private static final int UI_MARGIN_Y = 2;
    private static final int BULLET_PAD  = 4;
    private static final Identifier CHECK_OFF = Identifier.of("rpg-systems", "textures/gui/title/checkbox_todo.png");
    private static final Identifier CHECK_ON  = Identifier.of("rpg-systems", "textures/gui/title/checkbox_done.png");
    private static final Identifier INFO_ICON = Identifier.of("rpg-systems", "textures/gui/title/info.png");
    private static final int ICON_SIZE = 9;
    private static final int INFO_GAP  = 2;

    private static final int SCROLLBAR_BORDER_COLOR = 0x66000000;
    private static final int SCROLLBAR_THUMB_COLOR = 0x99FFFFFF;
    private static final int SCROLLBAR_THUMB_HOVER_COLOR = 0xBBFFFFFF;
    private static final int PLATE_TEXT_COLOR      = 0xFFFFFFFF;
    private static final int PLATE_MAIN_BG         = 0xFF3A4C6A;
    private static final int PLATE_MAIN_BR         = 0xFF1F2A3D;
    private static final int PLATE_SUB_BG          = 0xFF2E3748;
    private static final int PLATE_SUB_BR          = 0xFF1A2230;
    private static final int PLATE_TAG_BG          = 0x33222A36;
    private static final int PLATE_TAG_BR          = 0x66A0B0C0;
    private static final int PLATE_GAP_BELOW       = 4;
    private static final int SECTION_GAP_TO_CHILD  = 3;
    private static final int CATEGORY_GAP          = 3;

    private int screenX;
    private int screenY;
    private int bgWidth;
    private int bgHeight;
    private final TextRenderer font;
    private Title current;
    private float textScale = 0.5f;
    private int scrollY = 0;
    private int maxScrollCached = 0;

    private int scrollbarOffsetX = 4;
    private int scrollbarOffsetY = 0;
    private int scrollbarWidth = 1;
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0;

    private void drawDivider(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, COLOR_DIVIDER);
    }

    private static final class HintSpot {
        final int x, y, w, h;
        final List<Text> hint;
        HintSpot(int x, int y, int w, int h, List<Text> hint) { this.x=x; this.y=y; this.w=w; this.h=h; this.hint=hint; }
        boolean contains(double mx, double my) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }
    }

    private static final class MobSpot {
        final int x, y, w, h;
        final Identifier mobId;
        final Text mobName;
        MobSpot(int x, int y, int w, int h, Identifier mobId, Text mobName) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.mobId=mobId; this.mobName=mobName;
        }
        boolean contains(double mx, double my) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }
    }

    private static final class TagSpot {
        final int x, y, w, h;
        final Identifier tagId;
        final List<Identifier> entityIds;
        final List<Text> entityNames;
        TagSpot(int x, int y, int w, int h, Identifier tagId,
                List<Identifier> entityIds, List<Text> entityNames) {
            this.x=x; this.y=y; this.w=w; this.h=h;
            this.tagId = tagId; this.entityIds = entityIds; this.entityNames = entityNames;
        }
        boolean contains(double mx, double my) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }
    }

    private final List<HintSpot> hintSpots = new ArrayList<>();
    private final List<MobSpot> mobSpots = new ArrayList<>();
    private final List<TagSpot> tagSpots = new ArrayList<>();

    public TitleDescriptionBox(int screenX, int screenY, int bgWidth, int bgHeight) {
        this.font = MinecraftClient.getInstance().textRenderer;
        this.screenX = screenX;
        this.screenY = screenY;
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
    }

    public void setScreenOrigin(int screenX, int screenY) { this.screenX = screenX; this.screenY = screenY; }
    public void setBackgroundSize(int bgWidth, int bgHeight) { this.bgWidth = bgWidth; this.bgHeight = bgHeight; }
    public void setTitle(Title title) { this.current = title; this.scrollY = 0; }


    public void render(DrawContext ctx, int mouseX, int mouseY) {
        int boxW = bgWidth - LEFT_BOX_LEFT_PADDING - LEFT_BOX_WIDTH - GUTTER - RIGHT_PADDING;
        int boxX = screenX + bgWidth - RIGHT_PADDING - boxW;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        hintSpots.clear();
        mobSpots.clear();
        tagSpots.clear();

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
            drawDivider(ctx, innerX, y - scrollY, innerW);
            y += scaled(6);
        }

        if (current.conditions != null && !current.conditions.isEmpty()) {
            drawScaled(ctx, Text.translatable("title.rpgsystems.conditions").asOrderedText(), innerX, y - scrollY, COLOR_HEADER, textScale);
            y += scaled(10);

            int checkSizePx = scaled(ICON_SIZE);

            List<CondLine> lines = buildConditionLines();
            for (CondLine cl : lines) {
                boolean showInfo = (cl.hint != null);
                int infoSize = scaled(ICON_SIZE);
                int infoReserve = showInfo ? (infoSize + scaled(INFO_GAP)) : 0;

                int textX = innerX + checkSizePx + BULLET_PAD;
                int availableTextW = innerW - (textX - innerX) - infoReserve;

                int startY = y;
                int lineH = scaled(9);

                int blockH;
                boolean inline = cl.inlineIcon && TitleIconRenderer.hasIcon(cl.source) && cl.pre != null && cl.post != null;

                int iconSizePx = 0;
                List<OrderedText> restLines = List.of();
                OrderedText firstPostLine = null;
                OrderedText preOT = null;

                if (inline) {
                    preOT = cl.pre.asOrderedText();
                    int preWpx = Math.round(font.getWidth(preOT) * textScale);

                    int iconGap = scaled(2);
                    iconSizePx = scaled(ICON_SIZE);
                    int firstRemainPx = Math.max(0, availableTextW - preWpx - iconSizePx - iconGap);

                    String postRaw = cl.post.getString();
                    int firstCut = cutIndexByPixelWidth(postRaw, Math.max(1, Math.round(firstRemainPx / textScale)));
                    String firstRaw = postRaw.substring(0, Math.min(firstCut, postRaw.length()));
                    String restRaw = postRaw.substring(Math.min(firstCut, postRaw.length()));

                    firstPostLine = Text.literal(firstRaw).asOrderedText();

                    int normalWrap = Math.max(1, Math.round(availableTextW / textScale));
                    restLines = restRaw.isEmpty() ? List.of() : font.wrapLines(Text.literal(restRaw), normalWrap);

                    int linesDrawn = 1 + restLines.size();
                    blockH = Math.max(checkSizePx, Math.max(iconSizePx, lineH * linesDrawn));
                } else if (cl.mobName != null && cl.mobId != null && cl.pre != null && cl.post != null) {
                    preOT = cl.pre.asOrderedText();
                    int preWpx = Math.round(font.getWidth(preOT) * textScale);

                    OrderedText mobOT = cl.mobName.asOrderedText();
                    int mobWpx = Math.round(font.getWidth(mobOT) * textScale);

                    int firstRemainPx = Math.max(0, availableTextW - preWpx - mobWpx);

                    String postRaw = cl.post.getString();
                    int firstCut = cutIndexByPixelWidth(postRaw, Math.max(1, Math.round(firstRemainPx / textScale)));
                    String firstRaw = postRaw.substring(0, Math.min(firstCut, postRaw.length()));
                    String restRaw = postRaw.substring(Math.min(firstCut, postRaw.length()));

                    firstPostLine = Text.literal(firstRaw).asOrderedText();
                    int normalWrap = Math.max(1, Math.round(availableTextW / textScale));
                    restLines = restRaw.isEmpty() ? List.of() : font.wrapLines(Text.literal(restRaw), normalWrap);

                    int linesDrawn = 1 + restLines.size();
                    blockH = Math.max(checkSizePx, lineH * linesDrawn);
                } else if (cl.tagName != null && cl.tagId != null && cl.pre != null && cl.post != null) {
                    preOT = cl.pre.asOrderedText();
                    int preWpx = Math.round(font.getWidth(preOT) * textScale);
                    OrderedText tagOT = cl.tagName.asOrderedText();
                    int tagWpx = Math.round(font.getWidth(tagOT) * textScale);

                    int firstRemainPx = Math.max(0, availableTextW - preWpx - tagWpx);
                    String postRaw = cl.post.getString();
                    int firstCut = cutIndexByPixelWidth(postRaw, Math.max(1, Math.round(firstRemainPx / textScale)));
                    String firstRaw = postRaw.substring(0, Math.min(firstCut, postRaw.length()));
                    String restRaw = postRaw.substring(Math.min(firstCut, postRaw.length()));
                    firstPostLine = Text.literal(firstRaw).asOrderedText();

                    int normalWrap = Math.max(1, Math.round(availableTextW / textScale));
                    restLines = restRaw.isEmpty() ? List.of() : font.wrapLines(Text.literal(restRaw), normalWrap);

                    int linesDrawn = 1 + restLines.size();
                    blockH = Math.max(checkSizePx, lineH * linesDrawn);
                } else {
                    int wrap = Math.max(1, Math.round(availableTextW / textScale));
                    restLines = font.wrapLines(cl.text, wrap);
                    int linesDrawn = Math.max(1, restLines.size());
                    blockH = Math.max(checkSizePx, lineH * linesDrawn);
                }

                int checkY = centerY(startY, scrollY, blockH, checkSizePx);
                Identifier checkSprite = cl.done ? CHECK_ON : CHECK_OFF;
                drawSprite(ctx, checkSprite, innerX, checkY, checkSizePx, checkSizePx);

                int ty = startY + 1;

                if (inline) {
                    int iconGap = scaled(2);

                    drawScaled(ctx, preOT, textX, ty - scrollY, cl.color, textScale);

                    int preWpx = Math.round(font.getWidth(preOT) * textScale);
                    int iconX = textX + preWpx;
                    int baselineY = (startY + 1) - scrollY;
                    int iconSize = scaled(ICON_SIZE);
                    int iconY = baselineY + (lineH - iconSize) / 2 - Math.max(1, Math.round(textScale));

                    TitleIconRenderer.renderForCondition(ctx, cl.source, iconX, iconY, iconSize);

                    List<Text> iconTip = iconTooltip(cl.source);
                    if (iconTip != null && !iconTip.isEmpty()) {
                        hintSpots.add(new HintSpot(iconX, iconY, iconSize, iconSize, iconTip));
                    }

                    int postX = iconX + iconSize + iconGap;
                    drawScaled(ctx, firstPostLine, postX, ty - scrollY, cl.color, textScale);

                    for (int i = 0; i < (restLines != null ? restLines.size() : 0); i++) {
                        drawScaled(ctx, restLines.get(i), textX, (ty + (i + 1) * lineH) - scrollY, cl.color, textScale);
                    }
                } else if (cl.mobName != null && cl.mobId != null && cl.pre != null && cl.post != null) {
                    drawScaled(ctx, preOT, textX, ty - scrollY, cl.color, textScale);
                    int preWpx = Math.round(font.getWidth(preOT) * textScale);

                    Text mobStyled = cl.mobName.copy().setStyle(Style.EMPTY.withUnderline(true));
                    OrderedText mobOT = mobStyled.asOrderedText();
                    int mobWpx = Math.round(font.getWidth(mobOT) * textScale);

                    int mobX = textX + preWpx;
                    int mobY = ty - scrollY;
                    int lineH2 = Math.max(1, Math.round(9 * textScale));
                    boolean hoveringMob = mouseX >= mobX && mouseX <= mobX + mobWpx && mouseY >= mobY && mouseY <= mobY + lineH2;
                    int mobColor = hoveringMob ? (cl.color & 0x00FFFFFF) | 0xCC000000 : cl.color;

                    drawScaled(ctx, mobOT, mobX, mobY, mobColor, textScale);

                    mobSpots.add(new MobSpot(mobX, mobY, mobWpx, lineH2, cl.mobId, cl.mobName));

                    int postX = mobX + mobWpx;
                    drawScaled(ctx, firstPostLine, postX, ty - scrollY, cl.color, textScale);

                    for (int i = 0; i < (restLines != null ? restLines.size() : 0); i++) {
                        drawScaled(ctx, restLines.get(i), textX, (ty + (i + 1) * lineH) - scrollY, cl.color, textScale);
                    }
                } else if (cl.tagName != null && cl.tagId != null && cl.pre != null && cl.post != null) {
                    drawScaled(ctx, preOT, textX, ty - scrollY, cl.color, textScale);
                    int preWpx = Math.round(font.getWidth(preOT) * textScale);

                    OrderedText tagOT = cl.tagName.asOrderedText();
                    int tagWpx = Math.round(font.getWidth(tagOT) * textScale);
                    int tagX = textX + preWpx;
                    int tagY = ty - scrollY;
                    drawScaled(ctx, tagOT, tagX, tagY, cl.color, textScale);

                    int tagH = Math.max(1, Math.round(9 * textScale));
                    var entries = listEntriesForTag(cl.tagId);
                    var ids = idsOfEntries(entries);
                    var names = namesOfEntries(entries);
                    if (!ids.isEmpty()) {
                        tagSpots.add(new TagSpot(tagX, tagY, tagWpx, tagH, cl.tagId, ids, names));
                    }

                    int postX = tagX + tagWpx;
                    drawScaled(ctx, firstPostLine, postX, ty - scrollY, cl.color, textScale);

                    for (int i = 0; i < (restLines != null ? restLines.size() : 0); i++) {
                        drawScaled(ctx, restLines.get(i), textX, (ty + (i + 1) * lineH) - scrollY, cl.color, textScale);
                    }
                } else {
                    for (int i = 0; i < (restLines != null ? restLines.size() : 0); i++) {
                        drawScaled(ctx, restLines.get(i), textX, (ty + i * lineH) - scrollY, cl.color, textScale);
                    }
                }

                if (showInfo) {
                    infoSize = scaled(ICON_SIZE);
                    int badgeX = innerX + innerW - infoSize;
                    int badgeY = centerY(startY, scrollY, blockH, infoSize);
                    drawSprite(ctx, INFO_ICON, badgeX, badgeY, infoSize, infoSize);
                    hintSpots.add(new HintSpot(badgeX, badgeY, infoSize, infoSize, List.of(cl.hint)));
                }

                y += blockH + UI_MARGIN_Y;
            }

            y += scaled(4);
            drawDivider(ctx, innerX, y - scrollY, innerW);
            y += scaled(6);
        }

        boolean hasEquipped = hasAnyEquipped();
        boolean hasPerma = hasAnyPerma();

        if (hasEquipped || hasPerma) {
            y += drawFullWidthPill(ctx, innerX, y - scrollY, innerW, Text.literal("Bonuses"),
                    PLATE_MAIN_BG, PLATE_MAIN_BR, PLATE_TEXT_COLOR, textScale) + PLATE_GAP_BELOW;

            if (hasEquipped) {
                y += drawCenteredPill(ctx, innerX, innerW, y - scrollY,
                        Text.literal("Equipped"), PLATE_SUB_BG, PLATE_SUB_BR, 0xFFE6ECF8, textScale * 0.95f)
                        + SECTION_GAP_TO_CHILD;
                y = drawEquippedSection(ctx, innerX, innerW, y);
            }

            if (hasPerma) {
                y += CATEGORY_GAP;
                y += drawCenteredPill(ctx, innerX, innerW, y - scrollY,
                        Text.literal("Permanent"), PLATE_SUB_BG, PLATE_SUB_BR, 0xFFE6ECF8, textScale * 0.95f)
                        + SECTION_GAP_TO_CHILD;
                y = drawPermaSection(ctx, innerX, innerW, y);
            }
        }

        ctx.disableScissor();

        int contentHeight = y - (boxY + 3);
        int viewportH = viewportBottom - viewportTop;
        int maxScroll = Math.max(0, contentHeight - viewportH);
        maxScrollCached = maxScroll;
        if (scrollY > maxScrollCached) scrollY = maxScrollCached;
        if (scrollY < 0) scrollY = 0;

        renderScrollbar(ctx, mouseX, mouseY, innerX, viewportTop, innerW, viewportH, contentHeight);
    }

    private boolean hasAnyEquipped() {
        if (current == null) return false;
        for (Title.Bonus b : current.bonuses) {
            if (b.attribute != null
                    || (b.spellId != null && b.spellId.isPresent())
                    || (b.powerId != null && b.powerId.isPresent())
                    || (b.damageTarget != null && b.damageTarget.isPresent())
                    || (b.damageTag != null && b.damageTag.isPresent())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyPerma() {
        if (current == null) return false;
        for (Title.Bonus b : current.permaBonuses) {
            if (b.attribute != null
                    || (b.spellId != null && b.spellId.isPresent())
                    || (b.powerId != null && b.powerId.isPresent())
                    || (b.damageTarget != null && b.damageTarget.isPresent())
                    || (b.damageTag != null && b.damageTag.isPresent())) {
                return true;
            }
        }
        return false;
    }

    private int drawEquippedSection(DrawContext ctx, int innerX, int innerW, int y) {
        List<Text> attrLines = formatBonusesAttributes(current.bonuses);
        List<Identifier> spells = collectIds(current.bonuses, true, false);
        List<Identifier> powers = collectIds(current.bonuses, false, true);

        int yy = y;
        if (!attrLines.isEmpty()) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Attributes")) + CATEGORY_GAP;
            yy = drawWrappedList(ctx, innerX, innerW, yy, attrLines, COLOR_BONUS_TEXT, textScale);
        }
        if (!spells.isEmpty()) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Spells")) + CATEGORY_GAP;
            yy = drawSpellList(ctx, innerX, innerW, yy, spells, textScale * 0.90f);
        }
        if (!powers.isEmpty()) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Powers")) + CATEGORY_GAP;
            yy = drawPowerList(ctx, innerX, innerW, yy, powers, textScale * 0.90f);
        }

        if (hasDamage(current.bonuses)) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Damage against")) + CATEGORY_GAP;
            yy = drawDamageList(ctx, innerX, innerW, yy, current.bonuses, textScale);
        }
        return yy;
    }

    private int drawPermaSection(DrawContext ctx, int innerX, int innerW, int y) {
        List<Text> attrLines = formatBonusesAttributes(current.permaBonuses);
        List<Identifier> spells = collectIds(current.permaBonuses, true, false);
        List<Identifier> powers = collectIds(current.permaBonuses, false, true);

        int yy = y;
        if (!attrLines.isEmpty()) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Attributes")) + CATEGORY_GAP;
            yy = drawWrappedList(ctx, innerX, innerW, yy, attrLines, COLOR_BONUS_TEXT, textScale);
        }
        if (!spells.isEmpty()) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Spells")) + CATEGORY_GAP;
            yy = drawSpellList(ctx, innerX, innerW, yy, spells, textScale * 0.90f);
        }
        if (!powers.isEmpty()) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Powers")) + CATEGORY_GAP;
            yy = drawPowerList(ctx, innerX, innerW, yy, powers, textScale * 0.90f);
        }
        if (hasDamage(current.permaBonuses)) {
            yy += drawLeftTagPill(ctx, innerX, yy - scrollY, Text.literal("Damage against")) + CATEGORY_GAP;
            yy = drawDamageList(ctx, innerX, innerW, yy, current.permaBonuses, textScale);
        }
        return yy;
    }

    private boolean hasDamage(List<Title.Bonus> list) {
        for (Title.Bonus b : list) {
            if ((b.damageTarget != null && b.damageTarget.isPresent())
                    || (b.damageTag != null && b.damageTag.isPresent())) return true;
        }
        return false;
    }

    private int drawWrappedList(DrawContext ctx, int innerX, int innerW, int y, List<Text> lines, int color, float scale) {
        int wrapWidth = Math.max(1, Math.round(innerW / scale));
        for (Text t : lines) {
            List<OrderedText> wrapped = font.wrapLines(t, wrapWidth);
            for (OrderedText ot : wrapped) {
                drawScaled(ctx, ot, innerX, y - scrollY, color, scale);
                y += Math.max(1, Math.round(font.fontHeight * scale));
            }
            y += UI_MARGIN_Y;
        }
        return y;
    }

    private int drawDamageList(DrawContext ctx, int innerX, int innerW, int y, List<Title.Bonus> bonuses, float scale) {
        final int color = COLOR_BONUS_TEXT;
        final int lineH = Math.max(1, Math.round(9 * scale));
        for (Title.Bonus b : bonuses) {
            Text targetText;
            Identifier mobId = null;
            Identifier tagId = null;

            if (b.damageTarget != null && b.damageTarget.isPresent()) {
                mobId = b.damageTarget.get();
                targetText = plainName(Text.translatable(Registries.ENTITY_TYPE.get(mobId).getTranslationKey()))
                        .copy().setStyle(Style.EMPTY.withUnderline(true));
            } else if (b.damageTag != null && b.damageTag.isPresent()) {
                tagId = b.damageTag.get();
                targetText = Text.literal(toTitleCase(tagId.getPath())).setStyle(Style.EMPTY.withUnderline(true));
            } else {
                continue;
            }

            String sgn = (b.damageOp == Title.DamageOp.ADDED)
                    ? ((b.damageAmount >= 0 ? "+" : "") + trim(b.damageAmount))
                    : ((b.damageAmount * 100.0 >= 0 ? "+" : "") + trim(b.damageAmount * 100.0) + "%");

            Text pre = Text.literal("• ");
            Text mid = Text.literal(" ");
            Text post = Text.literal(" " + sgn);

            int x = innerX;
            OrderedText preOT = pre.asOrderedText();
            drawScaled(ctx, preOT, x, y - scrollY, color, scale);
            x += Math.round(font.getWidth(preOT) * scale);

            OrderedText mainOT = targetText.asOrderedText();
            int w = Math.round(font.getWidth(mainOT) * scale);
            drawScaled(ctx, mainOT, x, y - scrollY, color, scale);
            int h = Math.max(1, Math.round(font.fontHeight * scale));

            if (mobId != null) {
                mobSpots.add(new MobSpot(x, y - scrollY, w, h, mobId, targetText));
            } else if (tagId != null) {
                var entries = listEntriesForTag(tagId);
                var ids = idsOfEntries(entries);
                var names = namesOfEntries(entries);
                if (!ids.isEmpty()) tagSpots.add(new TagSpot(x, y - scrollY, w, h, tagId, ids, names));
            }

            x += w;

            drawScaled(ctx, mid.asOrderedText(), x, y - scrollY, color, scale);
            x += Math.round(font.getWidth(mid) * scale);

            drawScaled(ctx, post.asOrderedText(), x, y - scrollY, color, scale);
            y += lineH + UI_MARGIN_Y;
        }
        return y;
    }

    private int drawSpellList(DrawContext ctx, int innerX, int innerW, int y, List<Identifier> spells, float scale) {
        final int iconSize = Math.max(1, Math.round(12 * scale));
        final int gap      = Math.max(1, Math.round(BULLET_PAD * scale));
        final int textH    = Math.max(1, Math.round(font.fontHeight * scale));

        for (Identifier sid : spells) {
            Identifier icon = SpellRender.iconTexture(sid);
            Text name = resolveSpellName(sid);

            int blockH = Math.max(iconSize, textH);
            int iconY  = (y - scrollY) + (blockH - iconSize) / 2;
            int nameY  = (y - scrollY) + (blockH - textH) / 2;

            ctx.drawTexture(icon, innerX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            drawScaled(ctx, name.asOrderedText(), innerX + iconSize + gap, nameY + 1, COLOR_BONUS_TEXT, scale);

            y += blockH + UI_MARGIN_Y;
        }
        return y;
    }

    private int drawPowerList(DrawContext ctx, int innerX, int innerW, int y, List<Identifier> powers, float scale) {
        final int lineH = Math.max(1, Math.round(9 * scale));
        for (Identifier pid : powers) {
            Text line = Text.literal("• ").append(resolvePowerName(pid));
            drawScaled(ctx, line.asOrderedText(), innerX, y - scrollY, COLOR_BONUS_TEXT, scale);
            y += lineH + UI_MARGIN_Y;
        }
        return y;
    }
    private int drawLeftTagPill(DrawContext ctx, int x, int y, Text label) {
        float scale = textScale * 0.90f;
        OrderedText ot = label.asOrderedText();
        int padX = Math.max(6, scaled(8));
        int padY = Math.max(2, scaled(3));
        int textW = Math.round(font.getWidth(ot) * scale);
        int textH = Math.max(1, Math.round(font.fontHeight * scale));
        int w = textW + padX * 2;
        int h = textH + padY * 2;

        int radius = Math.max(2, Math.round(4 * scale));
        drawRoundedRect(ctx, x, y, w, h, radius, PLATE_TAG_BG, PLATE_TAG_BR);

        int textX = x + (w - textW) / 2;
        int textY = y + (h - textH) / 2;
        drawScaled(ctx, ot, textX, textY, 0xFFD6E0EA, scale);
        return h;
    }

    private int drawFullWidthPill(DrawContext ctx, int x, int y, int width, Text label, int bg, int br, int textColor, float scale) {
        int padY = Math.max(2, scaled(3));
        int textH = Math.max(1, Math.round(font.fontHeight * scale));
        int h = textH + padY * 2;

        int radius = Math.max(2, Math.round(4 * scale));
        drawRoundedRect(ctx, x, y, width, h, radius, bg, br);

        OrderedText ot = label.asOrderedText();
        int textW = Math.round(font.getWidth(ot) * scale);
        int textX = x + (width - textW) / 2;
        int textY = y + (h - textH) / 2;
        drawScaled(ctx, ot, textX, textY, textColor, scale);
        return h;
    }

    private int drawCenteredPill(DrawContext ctx, int innerX, int innerW, int y, Text label, int bg, int br, int textColor, float scale) {
        OrderedText ot = label.asOrderedText();
        int padX = Math.max(6, scaled(8));
        int padY = Math.max(2, scaled(3));
        int textW = Math.round(font.getWidth(ot) * scale);
        int textH = Math.max(1, Math.round(font.fontHeight * scale));
        int w = textW + padX * 2;
        int h = textH + padY * 2;

        int x = innerX + (innerW - w) / 2;

        int radius = Math.max(2, Math.round(4 * scale));
        drawRoundedRect(ctx, x, y, w, h, radius, bg, br);

        int textX = x + (w - textW) / 2;
        int textY = y + (h - textH) / 2;
        drawScaled(ctx, ot, textX, textY, textColor, scale);
        return h;
    }

    private void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int bg, int br) {
        int right = x + w;
        int bottom = y + h;

        ctx.fill(x + r, y, right - r, bottom, bg);
        ctx.fill(x, y + r, right, bottom - r, bg);

        drawCorner(ctx, x + r, y + r, r, true, true, bg);
        drawCorner(ctx, right - r - 1, y + r, r, false, true, bg);
        drawCorner(ctx, x + r, bottom - r - 1, r, true, false, bg);
        drawCorner(ctx, right - r - 1, bottom - r - 1, r, false, false, bg);

        ctx.drawBorder(x, y, w, h, br);
    }

    private void drawCorner(DrawContext ctx, int cx, int cy, int r, boolean left, boolean top, int color) {
        int signX = left ? -1 : 1;
        int signY = top ? -1 : 1;
        for (int i = 0; i < r; i++) {
            int dx = r - i;
            int dy = i;
            int px = cx + signX * dx;
            int py = cy + signY * dy;
            ctx.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static int centerY(int startY, int scrollY, int blockH, int h) {
        return startY - scrollY + (blockH - h) / 2;
    }

    public void renderHints(DrawContext ctx, int mouseX, int mouseY) {

        for (MobSpot s : mobSpots) {
            if (s.contains(mouseX, mouseY)) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(0, 0, 400);
                drawMobTooltip(ctx, mouseX, mouseY, s);
                ctx.getMatrices().pop();
                return;
            }
        }

        for (TagSpot s : tagSpots) {
            if (s.contains(mouseX, mouseY)) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(0, 0, 400);
                drawTagTooltip(ctx, mouseX, mouseY, s);
                ctx.getMatrices().pop();
                return;
            }
        }

        for (HintSpot s : hintSpots) {
            if (s.contains(mouseX, mouseY)) {
                ctx.drawTooltip(font, s.hint, mouseX, mouseY);
                return;
            }
        }
    }

    private void drawMobTooltip(DrawContext ctx, int mouseX, int mouseY, MobSpot s) {
        String modName = modNameOf(s.mobId.getNamespace());
        Text line1 = s.mobName;
        Text line2 = Text.literal(modName);

        int pad = 6;
        int gapHeaderBody = 6;
        int headerTextGap = 2;
        int textH = font.fontHeight;
        int w1 = font.getWidth(line1);
        int w2 = font.getWidth(line2);
        int headerW = Math.max(w1, w2) + pad * 2;
        int headerH = pad + (textH * 2 + headerTextGap) + pad;

        final int BASE = 40;
        var entity = TitleIconRenderer.getPreviewEntity(s.mobId);
        float eW = entity != null ? Math.max(0.1f, entity.getWidth())  : 1.0f;
        float eH = entity != null ? Math.max(0.1f, entity.getHeight()) : 1.0f;

        float excessW = Math.max(0f, eW - 1f);
        float excessH = Math.max(0f, eH - 1f);

        int previewAreaW = BASE + Math.round(BASE * excessW);
        int previewAreaH = BASE + Math.round(BASE * excessH);

        int bodyPad = 6;
        int bodyW = previewAreaW + bodyPad * 2;
        int bodyH = previewAreaH + bodyPad * 2;

        int tipW = Math.max(headerW, bodyW);
        int tipH = headerH + gapHeaderBody + 1 + bodyH;

        int cursorPad = 12, edgePad = 4;
        var win = MinecraftClient.getInstance().getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();

        if (tipW > sw - edgePad * 2) {
            tipW = sw - edgePad * 2;
            bodyW = tipW;
            if (bodyW < previewAreaW + bodyPad * 2) {
                float squeeze = (float)(previewAreaW + bodyPad * 2) / Math.max(1, bodyW);
                previewAreaH = Math.round(previewAreaH * squeeze);
                bodyH = previewAreaH + bodyPad * 2;
            }
        }
        if (tipH > sh - edgePad * 2) {
            tipH = sh - edgePad * 2;
            bodyH = Math.max(16, tipH - headerH - gapHeaderBody - 1);
        }

        int x = mouseX + cursorPad;
        int y = mouseY + cursorPad;
        if (x + tipW + edgePad > sw) x = mouseX - cursorPad - tipW;
        if (y + tipH + edgePad > sh) y = mouseY - cursorPad - tipH;

        x = Math.max(edgePad, Math.min(sw - tipW - edgePad, x));
        y = Math.max(edgePad, Math.min(sh - tipH - edgePad, y));

        int bg = 0xF0101010;
        int border1 = 0x50505050;
        int border2 = 0xA0A0A0A0;

        ctx.fill(x - 1, y - 1, x + tipW + 1, y + tipH + 1, border1);
        ctx.fill(x, y, x + tipW, y + tipH, bg);
        ctx.drawBorder(x, y, tipW, tipH, border2);

        int headerCenterX = x + tipW / 2;
        int nameY = y + pad;
        int modY  = nameY + textH + headerTextGap;

        ctx.drawCenteredTextWithShadow(font, line1, headerCenterX, nameY, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(font, line2, headerCenterX, modY, 0xFFB0B0B0);

        int dividerY = y + headerH + (gapHeaderBody / 2);
        ctx.fill(x + 2, dividerY, x + tipW - 2, dividerY + 1, 0x40FFFFFF);

        int bodyX = x;
        int bodyY = y + headerH + gapHeaderBody + 1;

        ctx.fill(bodyX + 1, bodyY + 1, bodyX + tipW - 1, bodyY + bodyH - 1, 0x08080808);

        int areaLeft = bodyX + (tipW - previewAreaW) / 2;
        int areaTop  = bodyY + (bodyH - previewAreaH) / 2;

        ctx.drawBorder(areaLeft, areaTop, previewAreaW, previewAreaH, 0x20202020);

        int renderLeft = areaLeft + (previewAreaW - BASE) / 2;
        int renderTop  = areaTop  + (previewAreaH - BASE) / 2;

        ctx.enableScissor(areaLeft, areaTop, areaLeft + previewAreaW, areaTop + previewAreaH);
        TitleIconRenderer.renderEntityPreview(ctx, s.mobId, renderLeft, renderTop, BASE);
        ctx.disableScissor();
    }

    private void drawTagTooltip(DrawContext ctx, int mouseX, int mouseY, TagSpot s) {
        long time = (MinecraftClient.getInstance().world != null)
                ? MinecraftClient.getInstance().world.getTime()
                : System.currentTimeMillis() / 50L;
        int n = Math.max(1, s.entityIds.size());
        int idx = (int) (Math.floorDiv(time, 20) % n);

        Identifier mobId = s.entityIds.get(idx);
        Text mobName = s.entityNames.get(idx);
        drawMobTooltip(ctx, mouseX, mouseY, new MobSpot(s.x, s.y, s.w, s.h, mobId, mobName));
    }

    private static String modNameOf(String namespace) {
        if ("minecraft".equals(namespace)) return "Minecraft";
        Optional<? extends net.fabricmc.loader.api.ModContainer> c =
                FabricLoader.getInstance().getModContainer(namespace);
        return c.map(mc -> mc.getMetadata().getName()).orElse(namespace);
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int[] track = scrollbarTrackRect();
        int trackX = track[0], trackY = track[1], trackW = track[2], trackH = track[3];

        if (mouseX < trackX || mouseX > trackX + trackW || mouseY < trackY || mouseY > trackY + trackH) {
            return false;
        }
        if (maxScrollCached <= 0) return true;

        int thumbH = scrollbarThumbHeight(trackH, maxScrollCached + trackH);
        int usable = Math.max(0, trackH - thumbH);
        float ratio = (maxScrollCached == 0) ? 0f : (scrollY / (float) maxScrollCached);
        int thumbTop = trackY + Math.round(usable * ratio);
        int thumbBottom = thumbTop + thumbH;

        if (mouseY >= thumbTop && mouseY <= thumbBottom) {
            draggingScrollbar = true;
            dragGrabOffsetY = (int) Math.round(mouseY) - thumbTop;
            return true;
        }

        int page = trackH;
        if (mouseY < thumbTop) {
            scrollY = Math.max(0, scrollY - page);
        } else {
            scrollY = Math.min(maxScrollCached, scrollY + page);
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!draggingScrollbar) return false;
        if (maxScrollCached <= 0) return true;

        int[] track = scrollbarTrackRect();
        int trackX = track[0], trackY = track[1], trackW = track[2], trackH = track[3];

        int thumbH = scrollbarThumbHeight(trackH, maxScrollCached + trackH);
        int usable = Math.max(0, trackH - thumbH);

        int newTop = (int) Math.round(mouseY) - dragGrabOffsetY;
        newTop = Math.max(trackY, Math.min(trackY + usable, newTop));

        float ratio = usable <= 0 ? 0f : (newTop - trackY) / (float) usable;
        scrollY = Math.max(0, Math.min(maxScrollCached, Math.round(ratio * maxScrollCached)));
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    private void drawSprite(DrawContext ctx, Identifier id, int x, int y, int w, int h) {
        ctx.drawTexture(id, x, y, 0, 0, w, h, w, h);
    }

    private void renderScrollbar(DrawContext ctx, int mouseX, int mouseY, int innerX, int viewportTop, int innerW, int viewportH, int contentHeight) {
        if (maxScrollCached <= 0) return;

        int trackW = scrollbarWidth;
        int trackX = innerX + innerW - trackW + scrollbarOffsetX;
        int trackY = viewportTop + scrollbarOffsetY;
        int trackH = viewportH;

        int thumbH = scrollbarThumbHeight(trackH, contentHeight);
        int usable = Math.max(0, trackH - thumbH);
        float ratio = (maxScrollCached == 0) ? 0f : (scrollY / (float) maxScrollCached);
        int thumbTop = trackY + Math.round(usable * ratio);

        boolean hoveringThumb = mouseX >= trackX && mouseX <= trackX + trackW && mouseY >= thumbTop && mouseY <= thumbTop + thumbH;
        int thumbColor = hoveringThumb || draggingScrollbar ? SCROLLBAR_THUMB_HOVER_COLOR : SCROLLBAR_THUMB_COLOR;

        ctx.fill(trackX + 1, thumbTop, trackX + trackW - 1, thumbTop + thumbH, thumbColor);
        ctx.drawBorder(trackX + 1, thumbTop, trackW - 2, thumbH, SCROLLBAR_BORDER_COLOR);
    }

    private int[] scrollbarTrackRect() {
        int boxW = bgWidth - LEFT_BOX_LEFT_PADDING - LEFT_BOX_WIDTH - GUTTER - RIGHT_PADDING;
        int boxX = screenX + bgWidth - RIGHT_PADDING - boxW;
        int boxY = screenY + bgHeight - BOTTOM_PADDING - BOX_HEIGHT;

        int innerX = boxX + 4;
        int innerW = boxW - 8;
        int viewportTop = boxY + 2;
        int viewportBottom = boxY + BOX_HEIGHT - 2;
        int viewportH = viewportBottom - viewportTop;

        int trackW = scrollbarWidth;
        int trackX = innerX + innerW - trackW + scrollbarOffsetX;
        int trackY = viewportTop + scrollbarOffsetY;

        return new int[] { trackX, trackY, trackW, viewportH };
    }

    private int scrollbarThumbHeight(int trackHeight, int contentHeight) {
        if (contentHeight <= 0) return Math.max(8, trackHeight);
        float visibleRatio = Math.min(1f, trackHeight / (float) contentHeight);
        int h = Math.round(trackHeight * visibleRatio);
        return Math.max(8, Math.min(trackHeight, h));
    }

    private List<Text> formatBonusesAttributes(List<Title.Bonus> bonuses) {
        List<Text> out = new ArrayList<>();
        boolean ilt = isIconLeadingTooltipPresent();

        for (Title.Bonus b : bonuses) {
            if ((b.spellId != null && b.spellId.isPresent())
                    || (b.powerId != null && b.powerId.isPresent())
                    || b.attribute == null) {
                continue;
            }

            String attrKey = b.attribute.value().getTranslationKey();
            Text attrName = Text.translatable(attrKey);
            String sign = b.amount >= 0 ? "+" : "";

            if (ilt) {
                String raw = attrName.getString();
                int[] span = IconLeadingUtil.firstIconSpan(raw);
                if (span[0] >= 0) {
                    String icon = raw.substring(span[0], span[1]);
                    String restRaw = raw.substring(0, span[0]) + raw.substring(span[1]);
                    String rest = IconLeadingUtil.stripSectionCodes(restRaw).replaceFirst("^\\s+", "");
                    Text iconText = Text.literal(icon + " ");
                    Text restText = Text.literal(rest);

                    if (b.operation == EntityAttributeModifier.Operation.ADD_VALUE) {
                        out.add(Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(b.amount) + " "))
                                .append(restText));
                    } else if (b.operation == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                        out.add(Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(b.amount * 100.0) + "% "))
                                .append(restText)
                                .append(Text.literal(" (base)")));
                    } else {
                        out.add(Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(b.amount * 100.0) + "% "))
                                .append(restText)
                                .append(Text.literal(" (total)")));
                    }
                    continue;
                }
            }

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

    private List<Identifier> collectIds(List<Title.Bonus> bonuses, boolean spells, boolean powers) {
        List<Identifier> list = new ArrayList<>();
        for (Title.Bonus b : bonuses) {
            if (spells && b.spellId != null && b.spellId.isPresent()) list.add(b.spellId.get());
            if (powers && b.powerId != null && b.powerId.isPresent()) list.add(b.powerId.get());
        }
        return list;
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
        return Text.literal(toTitleCase(id.getPath()));
    }

    private Text resolvePowerName(Identifier id) {
        return Text.literal(toTitleCase(id.getPath()));
    }

    private static String toTitleCase(String path) {
        String nice = path.replace('_', ' ');
        String[] parts = nice.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(p.substring(0,1).toUpperCase(Locale.ROOT)).append(p.substring(1));
                if (i + 1 < parts.length) sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static final class CondLine {
        final Text text;
        final Text pre;
        final Text post;
        final boolean inlineIcon;
        final Text hint;
        final int color;
        final boolean done;
        final Title.Condition source;

        final Text mobName;
        final Identifier mobId;

        final Text tagName;
        final Identifier tagId;

        private CondLine(Text text, Text pre, Text post, boolean inlineIcon, Text hint, int color, boolean done,
                         Title.Condition source, Text mobName, Identifier mobId, Text tagName, Identifier tagId) {
            this.text = text; this.pre = pre; this.post = post; this.inlineIcon = inlineIcon;
            this.hint = hint; this.color = color; this.done = done; this.source = source;
            this.mobName = mobName; this.mobId = mobId;
            this.tagName = tagName; this.tagId = tagId;
        }

        static CondLine text(Text text, Text hint, int color, boolean done, Title.Condition source) {
            return new CondLine(text, null, null, false, hint, color, done, source, null, null, null, null);
        }

        static CondLine inlineIcon(Text pre, Text post, Text hint, int color, boolean done, Title.Condition source) {
            return new CondLine(null, pre, post, true, hint, color, done, source, null, null, null, null);
        }

        static CondLine mob(Text pre, Text mobName, Identifier mobId, Text post, Text hint, int color, boolean done, Title.Condition source) {
            return new CondLine(null, pre, post, false, hint, color, done, source, mobName, mobId, null, null);
        }

        static CondLine tag(Text pre, Text tagName, Identifier tagId, Text post, Text hint, int color, boolean done, Title.Condition source) {
            return new CondLine(null, pre, post, false, hint, color, done, source, null, null, tagName, tagId);
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
                MutableText line = Text.literal(c.hint.get())
                        .append(Text.literal(" "))
                        .append(progressTail(c, cur, doneFlag));
                out.add(CondLine.text(line, null, color, doneFlag, c));
                continue;
            }

            switch (c.type) {
                case KILL_MOBS -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    Text tail = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");

                    if (c.entityTagId != null && c.entityTagId.isPresent()) {
                        Identifier tagId = c.entityTagId.get();
                        Text pre = Text.literal("Defeat " + target + " any ");
                        Text tagText = Text.literal(toTitleCase(tagId.getPath())).setStyle(Style.EMPTY.withUnderline(true));
                        out.add(CondLine.tag(pre, tagText, tagId, tail, c.hint.map(Text::literal).orElse(null), color, reached, c));
                        break;
                    }

                    if (c.entityType.isPresent()) {
                        var id = c.entityType.get();
                        Text mobName = plainName(Text.translatable(Registries.ENTITY_TYPE.get(id).getTranslationKey()));
                        Text pre = Text.literal("Defeat " + target + " ");
                        Text post = tail;
                        out.add(CondLine.mob(pre, mobName, id, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else if (c.entitySpec.isPresent()) {
                        String spec = c.entitySpec.get();
                        Identifier mid = Identifier.tryParse(spec);
                        if (mid != null && Registries.ENTITY_TYPE.containsId(mid)) {
                            Text mobName = plainName(Text.translatable(Registries.ENTITY_TYPE.get(mid).getTranslationKey()));
                            Text pre = Text.literal("Defeat " + target + " ");
                            Text post = tail;
                            out.add(CondLine.mob(pre, mobName, mid, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                        } else if ("any".equalsIgnoreCase(spec)) {
                            Text line = Text.translatable("title.rpgsystems.condition.kill_any", target).append(tail);
                            out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                        } else if (spec.endsWith(":*")) {
                            String ns = spec.substring(0, spec.indexOf(':'));
                            Text line = Text.literal("Defeat " + target + " mobs from " + ns).append(tail);
                            out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                        } else {
                            Text line = Text.literal("Defeat " + target + " mobs: " + spec).append(tail);
                            out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                        }
                    } else {
                        Text line = Text.translatable("title.rpgsystems.condition.kill_any", target).append(tail);
                        out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }

                case OBTAIN_ITEM -> {
                    if (c.item.isPresent()) {
                        int target = Math.max(1, c.count);
                        boolean reached = doneFlag || (cur >= target);
                        int color = reached ? COLOR_DONE : COLOR_TODO;

                        Text pre  = Text.literal("Obtain " + target + " ");
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");

                        out.add(CondLine.inlineIcon(pre, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }

                case WALK_BLOCKS -> {
                    long target = Math.max(1, c.distance);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = Text.translatable("title.rpgsystems.condition.walk", target)
                            .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case CRAFT_ITEM -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    if (c.item.isPresent()) {
                        Text pre  = Text.literal("Craft " + target + " ");
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                        out.add(CondLine.inlineIcon(pre, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }

                case MINE_BLOCKS -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    if (c.block.isPresent()) {
                        Text pre  = Text.literal("Mine " + target + " ");
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                        out.add(CondLine.inlineIcon(pre, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else {
                        MutableText line = Text.literal("Mine " + target + " blocks")
                                .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                        out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }

                case REACH_LEVEL_XP -> {
                    int target = Math.max(1, c.level);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = Text.translatable("title.rpgsystems.condition.reach_xp", target)
                            .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case REACH_LEVEL_PUFFERFISH -> {
                    int target = Math.max(1, c.level);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = Text.translatable("title.rpgsystems.condition.reach_pufferfish", target)
                            .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case ADVANCEMENT -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText base = c.advancement.isPresent()
                            ? Text.translatable("title.rpgsystems.condition.advancement", c.advancement.get().toString())
                            : Text.translatable("title.rpgsystems.condition.advancement", "");
                    MutableText line = base.append(Text.literal(" (" + (reached ? 1 : 0) + "/1)"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case VISIT_BIOME -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = c.biome.isPresent()
                            ? Text.literal("Visit ").append(Text.translatable("biome." + c.biome.get().getNamespace() + "." + c.biome.get().getPath()))
                            : Text.literal("Visit a biome");
                    line = line.append(Text.literal(" (" + (reached ? 1 : 0) + "/1)"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case ENTER_DIMENSION -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    MutableText line = c.dimension.isPresent()
                            ? Text.literal("Enter " + c.dimension.get())
                            : Text.literal("Enter a dimension");
                    line = line.append(Text.literal(" (" + (reached ? 1 : 0) + "/1)"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case INTERACT_BLOCK -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;
                    if (c.block.isPresent()) {
                        Text pre  = Text.literal("Interact with ");
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                        out.add(CondLine.inlineIcon(pre, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else {
                        MutableText line = Text.literal("Interact with a block")
                                .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                        out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }
                case INTERACT_ENTITY -> {
                    int target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    if (c.entityType.isPresent()) {
                        Text pre  = Text.literal("Interact with ");
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                        out.add(CondLine.inlineIcon(pre, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else if (c.entityTagId != null && c.entityTagId.isPresent()) {
                        Identifier tagId = c.entityTagId.get();
                        Text pre  = Text.literal("Interact with any ");
                        Text tagText = Text.literal(toTitleCase(tagId.getPath())).setStyle(Style.EMPTY.withUnderline(true));
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                        out.add(CondLine.tag(pre, tagText, tagId, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else {
                        Text who;
                        if (c.entitySpec.isPresent()) {
                            String spec = c.entitySpec.get();
                            if ("any".equalsIgnoreCase(spec)) {
                                who = Text.literal("any entity");
                            } else if (spec.endsWith(":*")) {
                                who = Text.literal(spec.substring(0, spec.indexOf(':')) + " entities");
                            } else {
                                Identifier mid = Identifier.tryParse(spec);
                                if (mid != null && Registries.ENTITY_TYPE.containsId(mid)) {
                                    who = plainName(Text.translatable(Registries.ENTITY_TYPE.get(mid).getTranslationKey()));
                                } else {
                                    who = Text.literal(spec);
                                }
                            }
                        } else {
                            who = Text.literal("an entity");
                        }
                        MutableText line = Text.literal("Interact with ").append(who)
                                .append(Text.literal(" (" + Math.min(cur, target) + "/" + target + ")"));
                        out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }
                case FIND_STRUCTURE -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    MutableText what;
                    if (c.structure.isPresent()) {
                        Identifier sid = c.structure.get();
                        String tKey = "structure." + sid.getNamespace() + "." + sid.getPath();
                        Text cand = Text.translatable(tKey);
                        if (!cand.getString().equals(tKey)) {
                            what = cand.copy();
                        } else {
                            what = Text.literal(toTitleCase(sid.getPath()));
                        }
                    } else {
                        what = Text.literal("a structure");
                    }

                    MutableText line = Text.literal("Discover ").append(what)
                            .append(Text.literal(" (" + (reached ? 1 : 0) + "/1)"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
                case DEAL_DAMAGE_TOTAL -> {
                    long target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    if (c.entityTagId != null && c.entityTagId.isPresent()) {
                        Identifier tagId = c.entityTagId.get();
                        Text pre = Text.literal("Deal " + target + " total damage to any ");
                        Text tagText = Text.literal(toTitleCase(tagId.getPath())).setStyle(Style.EMPTY.withUnderline(true));
                        Text post = Text.literal(" (" + Math.min(cur, target) + "/" + target + ")");
                        out.add(CondLine.tag(pre, tagText, tagId, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else {
                        Text who = c.entitySpec.isPresent()
                                ? Text.literal(c.entitySpec.get())
                                : c.entityType.map(t -> plainName(Text.translatable(Registries.ENTITY_TYPE.get(t).getTranslationKey())))
                                .orElse(Text.literal("any"));

                        MutableText line = Text.literal("Deal ")
                                .append(Text.literal(String.valueOf(target)))
                                .append(Text.literal(" total damage to "))
                                .append(who)
                                .append(Text.literal(" ("))
                                .append(Text.literal(String.valueOf(Math.min(cur, target))))
                                .append(Text.literal("/"))
                                .append(Text.literal(String.valueOf(target)))
                                .append(Text.literal(")"));
                        out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }
                case DEAL_DAMAGE_MAX -> {
                    long target = Math.max(1, c.count);
                    boolean reached = cur >= target || doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    if (c.entityTagId != null && c.entityTagId.isPresent()) {
                        Identifier tagId = c.entityTagId.get();
                        Text pre = Text.literal("Deal a single hit of at least " + target + " to any ");
                        Text tagText = Text.literal(toTitleCase(tagId.getPath())).setStyle(Style.EMPTY.withUnderline(true));
                        Text post = Text.literal(" (best: " + cur + ")");
                        out.add(CondLine.tag(pre, tagText, tagId, post, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    } else {
                        Text who = c.entitySpec.isPresent()
                                ? Text.literal(c.entitySpec.get())
                                : c.entityType.map(t -> plainName(Text.translatable(Registries.ENTITY_TYPE.get(t).getTranslationKey())))
                                .orElse(Text.literal("any"));

                        MutableText line = Text.literal("Deal a single hit of at least ")
                                .append(Text.literal(String.valueOf(target)))
                                .append(Text.literal(" to "))
                                .append(who)
                                .append(Text.literal(" (best: "))
                                .append(Text.literal(String.valueOf(cur)))
                                .append(Text.literal(")"));
                        out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                    }
                }
                case CHECK_ATTRIBUTE -> {
                    boolean reached = doneFlag;
                    int color = reached ? COLOR_DONE : COLOR_TODO;

                    MutableText attrName;
                    if (c.attributeId.isPresent()) {
                        var key = net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE, c.attributeId.get());
                        var entry = MinecraftClient.getInstance().world
                                .getRegistryManager().get(RegistryKeys.ATTRIBUTE).getEntry(key).orElse(null);
                        if (entry != null) {
                            attrName = Text.translatable(entry.value().getTranslationKey());
                        } else {
                            attrName = Text.literal(c.attributeId.get().toString());
                        }
                    } else {
                        attrName = Text.literal("attribute");
                    }

                    double min = c.minValue;
                    MutableText line = Text.literal("Reach ")
                            .append(attrName)
                            .append(Text.literal(" ≥ " + trim(min)))
                            .append(Text.literal(" (now: " + cur + ")"));
                    out.add(CondLine.text(line, c.hint.map(Text::literal).orElse(null), color, reached, c));
                }
            }
        }
        return out;
    }


    private static Text plainName(Text t) {
        return Text.literal(stripLeadingIconLikeChunk(t.getString()));
    }

    private static String stripLeadingIconLikeChunk(String s) {
        if (s == null || s.isEmpty()) return "";
        s = s.replaceAll("\\u00A7[0-9A-FK-ORa-fk-or]", "");
        int firstSpace = s.indexOf(' ');
        if (firstSpace > 0) {
            String head = s.substring(0, firstSpace);
            boolean headHasAlnum = head.chars().anyMatch(Character::isLetterOrDigit);
            if (!headHasAlnum) {
                s = s.substring(firstSpace + 1).stripLeading();
            }
        }
        return s;
    }

    private static Text progressTail(Title.Condition c, long cur, boolean doneFlag) {
        switch (c.type) {
            case KILL_MOBS, OBTAIN_ITEM, CRAFT_ITEM, MINE_BLOCKS, INTERACT_BLOCK, INTERACT_ENTITY -> {
                int target = Math.max(1, c.count);
                return Text.literal("(" + Math.min(cur, target) + "/" + target + ")");
            }
            case WALK_BLOCKS -> {
                long target = Math.max(1, c.distance);
                return Text.literal("(" + Math.min(cur, target) + "/" + target + ")");
            }
            case REACH_LEVEL_XP, REACH_LEVEL_PUFFERFISH -> {
                int target = Math.max(1, c.level);
                return Text.literal("(" + Math.min(cur, target) + "/" + target + ")");
            }
            case ADVANCEMENT, VISIT_BIOME, ENTER_DIMENSION, FIND_STRUCTURE -> {
                return Text.literal("(" + (doneFlag ? 1 : 0) + "/1)");
            }
            case DEAL_DAMAGE_TOTAL -> {
                long target = Math.max(1, c.count);
                return Text.literal("(" + Math.min(cur, target) + "/" + target + ")");
            }
            case DEAL_DAMAGE_MAX -> {
                return Text.literal("(best: " + cur + ")");
            }
            case CHECK_ATTRIBUTE -> {
                return Text.literal("(now: " + cur + ")");
            }
            default -> {
                return Text.literal("");
            }
        }
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

    private int cutIndexByPixelWidth(String s, int maxPxAtScale1) {
        if (maxPxAtScale1 <= 0 || s.isEmpty()) return 0;
        int i = 0, w = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int cw = font.getWidth(ch);
            if (w + cw > maxPxAtScale1) break;
            w += cw;
            i += Character.charCount(cp);
        }
        return i;
    }

    private List<Text> iconTooltip(Title.Condition c) {
        if (c.item.isPresent()) {
            Identifier id = c.item.get();
            var item = Registries.ITEM.get(id);
            Text name = plainName(Text.translatable(item.getTranslationKey()));
            String mod = modNameOf(id.getNamespace());
            return List.of(name, Text.literal(mod).formatted(Formatting.GRAY));
        }
        if (c.block.isPresent()) {
            Identifier id = c.block.get();
            var block = Registries.BLOCK.get(id);
            Text name = plainName(Text.translatable(block.getTranslationKey()));
            String mod = modNameOf(id.getNamespace());
            return List.of(name, Text.literal(mod).formatted(Formatting.GRAY));
        }
        if (c.entityType.isPresent()) {
            var typeId = c.entityType.get();
            var type = Registries.ENTITY_TYPE.get(typeId);
            Text name = plainName(Text.translatable(type.getTranslationKey()));
            String mod = modNameOf(typeId.getNamespace());
            return List.of(name, Text.literal(mod).formatted(Formatting.GRAY));
        }
        if (c.entitySpec.isPresent()) {
            Identifier mid = Identifier.tryParse(c.entitySpec.get());
            if (mid != null && Registries.ENTITY_TYPE.containsId(mid)) {
                var type = Registries.ENTITY_TYPE.get(mid);
                Text name = plainName(Text.translatable(type.getTranslationKey()));
                String mod = modNameOf(mid.getNamespace());
                return List.of(name, Text.literal(mod).formatted(Formatting.GRAY));
            }
        }
        return null;
    }

    private static boolean isIconLeadingTooltipPresent() {
        FabricLoader fl = FabricLoader.getInstance();
        return fl.isModLoaded("iconleadingtooltip") || fl.isModLoaded("icon-leading-tooltip");
    }

    private static List<RegistryEntry<EntityType<?>>> listEntriesForTag(Identifier tagId) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return List.of();
        var reg = mc.world.getRegistryManager().get(RegistryKeys.ENTITY_TYPE);
        var tk = TagKey.of(RegistryKeys.ENTITY_TYPE, tagId);
        var out = new ArrayList<RegistryEntry<EntityType<?>>>();
        reg.iterateEntries(tk).forEach(out::add);
        return out;
    }
    private static List<Identifier> idsOfEntries(List<RegistryEntry<EntityType<?>>> entries) {
        var list = new ArrayList<Identifier>(entries.size());
        for (var e : entries) {
            Identifier id = Registries.ENTITY_TYPE.getId(e.value());
            if (id != null) list.add(id);
        }
        return list;
    }
    private static List<Text> namesOfEntries(List<RegistryEntry<EntityType<?>>> entries) {
        var list = new ArrayList<Text>(entries.size());
        for (var e : entries) {
            list.add(plainName(Text.translatable(e.value().getTranslationKey())));
        }
        return list;
    }
    public static void renderMobTooltip(DrawContext ctx, int mouseX, int mouseY, Identifier mobId, Text mobName) {
        MobSpot tmp = new MobSpot(0, 0, 0, 0, mobId, mobName);
        TitleDescriptionBox dummy = new TitleDescriptionBox(0, 0, 0, 0);
        dummy.drawMobTooltip(ctx, mouseX, mouseY, tmp);
    }

    public static void renderTagTooltip(DrawContext ctx, int mouseX, int mouseY, Identifier tagId) {
        var entries = listEntriesForTag(tagId);
        if (entries.isEmpty()) return;
        long time = (MinecraftClient.getInstance().world != null)
                ? MinecraftClient.getInstance().world.getTime()
                : System.currentTimeMillis() / 50L;
        int idx = (int) (Math.floorDiv(time, 20) % entries.size());

        Identifier mobId = Registries.ENTITY_TYPE.getId(entries.get(idx).value());
        if (mobId == null) return;
        Text mobName = plainName(Text.translatable(entries.get(idx).value().getTranslationKey()));

        renderMobTooltip(ctx, mouseX, mouseY, mobId, mobName);
    }

}
