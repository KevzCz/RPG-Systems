package net.pixeldreamstudios.rpgsystems.client.title.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil;
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitleBox;
import net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitleDescriptionBox;
import net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitlesListBox;
import net.pixeldreamstudios.rpgsystems.client.title.widget.TitleButtonWidget;
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;
import net.pixeldreamstudios.rpgsystems.title.PermaGroupKey;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.util.SpellRender;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public final class TitleScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.of("rpg-systems", "textures/gui/title/title_screen.png");
    private static final Identifier BACK_NORMAL = Identifier.of("rpg-systems", "textures/gui/title/back_normal.png");
    private static final Identifier BACK_HOVER  = Identifier.of("rpg-systems", "textures/gui/title/back_hover.png");
    private static final Identifier STATS_ICON  = Identifier.of("rpg-systems", "textures/gui/title/stats.png");
    private static final Identifier SPELLS_ICON = Identifier.of("rpg-systems", "textures/gui/title/spells.png");
    private static final Identifier POWERS_ICON = Identifier.of("rpg-systems", "textures/gui/title/powers.png");
    private static final Identifier TOGGLE_ON  = Identifier.of("rpg-systems", "textures/gui/title/on.png");
    private static final Identifier TOGGLE_OFF = Identifier.of("rpg-systems", "textures/gui/title/off.png");
    private static final int TOGGLE_W = 16;
    private static final int TOGGLE_H = 7;
    private static final int TOGGLE_PAD  = 2;
    private final MobSpotP hoveredMob = null;
    private final TagSpotP hoveredTag = null;
    private record RowHit(int y, int h, String key) {}

    private final List<RowHit> currentRowHits = new ArrayList<>();
    private int x;
    private int y;
    private final int backgroundWidth = 176;
    private final int backgroundHeight = 166;
    private static final int CONTENT_TEXT_PAD_X = 3;
    private TitleButtonWidget backButton;

    private TitleButtonWidget statsButton;
    private TitleButtonWidget spellsButton;
    private TitleButtonWidget powersButton;

    private TitlesListBox titlesListBox;
    private TitleDescriptionBox descriptionBox;
    TitleBox titleBox;

    private enum PanelType { NONE, STATS, SPELLS, POWERS }
    private enum Section { EQUIPPED, PERMANENT }

    private PanelType openPanel = PanelType.NONE;
    private Section section = Section.EQUIPPED;
    private static final class MobSpotP {
        final int x,y,w,h; final Identifier id; final Text name;
        MobSpotP(int x,int y,int w,int h,Identifier id,Text name){this.x=x;this.y=y;this.w=w;this.h=h;this.id=id;this.name=name;}
        boolean hit(int mx,int my){return mx>=x && mx<=x+w && my>=y && my<=y+h;}
    }
    private static final class TagSpotP {
        final int x,y,w,h; final Identifier tagId;
        TagSpotP(int x,int y,int w,int h,Identifier tagId){this.x=x;this.y=y;this.w=w;this.h=h;this.tagId=tagId;}
        boolean hit(int mx,int my){return mx>=x && mx<=x+w && my>=y && my<=y+h;}
    }
    private final List<MobSpotP> panelMobSpots = new ArrayList<>();
    private final List<TagSpotP> panelTagSpots = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int tabEquippedX, tabEquippedY, tabEquippedW, tabEquippedH;
    private int tabPermanentX, tabPermanentY, tabPermanentW, tabPermanentH;
    private int contentLeft, contentTop, contentRight, contentBottom;
    private int contentScroll = 0;
    private static final float PANEL_TEXT_SCALE = 0.65f;

    public TitleScreen() {
        super(Text.translatable("screen.rpgsystems.titles"));
    }

    @Override
    protected void init() {
        this.x = (this.width - backgroundWidth) / 2;
        this.y = (this.height - backgroundHeight) / 2;

        int backSize = 8;
        int backX = x + backgroundWidth - 5 - backSize;
        int backY = y + 5;

        this.backButton = new TitleButtonWidget(
                backX, backY, backSize, backSize,
                BACK_NORMAL, BACK_HOVER,
                () -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.setScreen(new InventoryScreen(client.player));
                    } else {
                        this.close();
                    }
                }
        );
        this.addDrawableChild(this.backButton);

        int iconSize = 8;
        int gap = 3;
        int tlX = x + 5 + 2;
        int tlY = y + 5;

        this.statsButton = new TitleButtonWidget(
                tlX, tlY, iconSize, iconSize,
                STATS_ICON, STATS_ICON,
                () -> togglePanel(PanelType.STATS)
        );
        this.addDrawableChild(this.statsButton);

        this.spellsButton = new TitleButtonWidget(
                tlX + iconSize + gap, tlY, iconSize, iconSize,
                SPELLS_ICON, SPELLS_ICON,
                () -> togglePanel(PanelType.SPELLS)
        );
        this.addDrawableChild(this.spellsButton);

        this.powersButton = new TitleButtonWidget(
                tlX + (iconSize + gap) * 2, tlY, iconSize, iconSize,
                POWERS_ICON, POWERS_ICON,
                () -> togglePanel(PanelType.POWERS)
        );
        this.addDrawableChild(this.powersButton);

        this.titlesListBox = new TitlesListBox(x, y, backgroundWidth, backgroundHeight);
        this.descriptionBox = new TitleDescriptionBox(x, y, backgroundWidth, backgroundHeight);
        this.titleBox = new TitleBox(x, y, backgroundWidth, backgroundHeight);
        this.titleBox.attachToScreen(this);

        this.titlesListBox.setSelectionListener(title -> this.descriptionBox.setTitle(title));
        this.titlesListBox.setOnSelectionChanged(title -> this.titleBox.setSelectedTitle(title));

        Identifier equippedId = TitleClientData.getSelfActive();
        if (equippedId != null && TitleRegistry.get(equippedId) != null) {
            this.titlesListBox.selectById(equippedId);
        } else {
            this.titlesListBox.selectFirstVisible();
        }

        layoutPanel();
    }

    private int toggleX() { return contentRight - TOGGLE_PAD - TOGGLE_W; }

    private Set<String> disabledSet() {
        return TitleClientData.getPermaDisabled();
    }
    private boolean isDisabled(String key) {
        return disabledSet().contains(key);
    }
    private void toggleKey(String key) {
        LinkedHashSet<String> newSet = new LinkedHashSet<>(disabledSet());
        if (!newSet.add(key)) newSet.remove(key);

        TitleClientData.setPermaDisabled(newSet);

        sendPermaDisabledToServer(newSet);
    }

    private int drawToggle(DrawContext ctx, int rowY, int rowH, boolean enabled) {
        final float scale = 0.75f;
        final int x = toggleX();
        final int y = rowY + Math.max(0, (rowH - TOGGLE_H) / 2);

        final int boxCx = x + TOGGLE_W / 2;
        final int boxCy = y + TOGGLE_H / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(boxCx, boxCy, 0);
        ctx.getMatrices().scale(scale, scale, 1.0f);
        ctx.getMatrices().translate(-TOGGLE_W / 2.0f, -TOGGLE_H / 2.0f, 0);

        ctx.drawTexture(enabled ? TOGGLE_ON : TOGGLE_OFF, 0, 0, 0, 0, TOGGLE_W, TOGGLE_H, TOGGLE_W, TOGGLE_H);

        ctx.getMatrices().pop();
        return x;
    }

    private void togglePanel(PanelType type) {
        if (this.openPanel == type) {
            this.openPanel = PanelType.NONE;
            this.contentScroll = 0;
        } else {
            this.openPanel = type;
            this.section = Section.EQUIPPED;
            this.contentScroll = 0;
        }
        layoutPanel();
    }
    private void layoutPanel() {
        this.panelW = 150;
        this.panelH = 96;
        this.panelX = x + 5;
        this.panelY = y + 24 - 3;

        int tabsTop = panelY + 16;
        int tabH = 12;
        int tabGap = 6;

        TextRenderer tr = this.textRenderer;
        int eqWText = tr.getWidth(Text.literal("Equipped"));
        int pmWText = tr.getWidth(Text.literal("Permanent"));

        int padX = 6;
        int eqW = Math.max(40, eqWText + padX * 2);
        int pmW = Math.max(56, pmWText + padX * 2);

        int tabW = Math.max(eqW, pmW);
        int total = tabW + tabGap + tabW;

        int startX = panelX + (panelW - total) / 2;

        this.tabEquippedX = startX;
        this.tabEquippedY = tabsTop;
        this.tabEquippedW = tabW;
        this.tabEquippedH = tabH;

        this.tabPermanentX = startX + tabW + tabGap;
        this.tabPermanentY = tabsTop;
        this.tabPermanentW = tabW;
        this.tabPermanentH = tabH;

        int left  = panelX;
        int top   = panelY;
        int right = panelX + panelW;
        int bottom= panelY + panelH;

        this.contentLeft   = left + 6;
        this.contentTop    = top  + 16 + 12 + 6;
        this.contentRight  = right - 6;
        this.contentBottom = bottom - 6;
    }
    private int measureLinesHeight(List<Text> lines, int l, int r, float scale) {
        TextRenderer font = this.textRenderer;
        int wrapWidth = Math.max(1, Math.round((r - l) / scale));
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));
        int h = 0;
        for (Text line : lines) {
            List<OrderedText> wrapped = font.wrapLines(line, wrapWidth);
            h += wrapped.size() * lineH;
        }
        return h + Math.max(1, Math.round(2 * scale));
    }

    private int measureSpellsHeight(int count, float scale) {
        TextRenderer font = this.textRenderer;
        int icon = Math.max(1, Math.round(12 * scale));
        int lineH = Math.max(icon, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));
        return count * lineH + Math.max(1, Math.round(2 * scale));
    }

    private int measurePowersHeight(int count, float scale) {
        TextRenderer font = this.textRenderer;
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(6 * scale));
        return count * lineH + Math.max(1, Math.round(2 * scale));
    }

    private int getCurrentContentHeight() {
        if (openPanel == PanelType.NONE) return 0;

        if (openPanel == PanelType.STATS) {
            List<Text> lines;
            if (section == Section.EQUIPPED) {
                Title active = getEquippedTitle();
                lines = new ArrayList<>();
                if (active != null) {
                    lines.addAll(formatBonusesAttributes(active.bonuses));
                    lines.addAll(formatDamageLines(active.bonuses));
                }
                if (lines.isEmpty()) lines = List.of(Text.literal("None"));
            } else {
                Aggregated agg = aggregatePermanent();
                lines = new ArrayList<>();
                lines.addAll(formatAggregatedAttributes(agg.attrSums));
                lines.addAll(formatAggregatedDamage(agg.dmgSums));
                if (lines.isEmpty()) lines = List.of(Text.literal("None"));
            }
            return measureLinesHeight(lines, contentLeft + CONTENT_TEXT_PAD_X, contentRight, PANEL_TEXT_SCALE);
        }

        if (openPanel == PanelType.SPELLS) {
            List<Identifier> spells;
            if (section == Section.EQUIPPED) {
                Title active = getEquippedTitle();
                spells = new ArrayList<>();
                if (active != null) {
                    for (Title.Bonus bns : active.bonuses) {
                        if (bns != null && bns.spellId != null && bns.spellId.isPresent()) spells.add(bns.spellId.get());
                    }
                }
            } else {
                spells = new ArrayList<>(aggregatePermanent().spells);
            }
            if (spells.isEmpty()) {
                return measureLinesHeight(List.of(Text.literal("None")), contentLeft + CONTENT_TEXT_PAD_X, contentRight, PANEL_TEXT_SCALE);
            }
            return measureSpellsHeight(spells.size(), PANEL_TEXT_SCALE);
        }

        List<Identifier> powers;
        if (section == Section.EQUIPPED) {
            Title active = getEquippedTitle();
            powers = new ArrayList<>();
            if (active != null) {
                for (Title.Bonus bns : active.bonuses) {
                    if (bns != null && bns.powerId != null && bns.powerId.isPresent()) powers.add(bns.powerId.get());
                }
            }
        } else {
            powers = new ArrayList<>(aggregatePermanent().powers);
        }
        if (powers.isEmpty()) {
            return measureLinesHeight(List.of(Text.literal("None")), contentLeft + CONTENT_TEXT_PAD_X, contentRight, PANEL_TEXT_SCALE);
        }
        return measurePowersHeight(powers.size(), PANEL_TEXT_SCALE);
    }

    private void clampScroll() {
        int viewH = contentBottom - contentTop;
        int contentH = getCurrentContentHeight();
        int maxScroll = Math.max(0, contentH - viewH);
        if (contentScroll < 0) contentScroll = 0;
        if (contentScroll > maxScroll) contentScroll = maxScroll;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        this.titlesListBox.setScreenOrigin(x, y);
        this.titlesListBox.setBackgroundSize(backgroundWidth, backgroundHeight);
        this.titlesListBox.render(context, mouseX, mouseY, delta);

        this.descriptionBox.setScreenOrigin(x, y);
        this.descriptionBox.setBackgroundSize(backgroundWidth, backgroundHeight);
        this.descriptionBox.render(context, mouseX, mouseY);

        this.titleBox.setScreenOrigin(x, y);
        this.titleBox.setBackgroundSize(backgroundWidth, backgroundHeight);
        this.titleBox.render(context);

        int titleX = x + backgroundWidth / 2;
        int titleY = y + 7;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFF);

        if (this.statsButton != null && this.statsButton.isHovered()) {
            context.drawTooltip(this.textRenderer, Text.literal("Stats"), mouseX, mouseY);
        } else if (this.spellsButton != null && this.spellsButton.isHovered()) {
            context.drawTooltip(this.textRenderer, Text.literal("Spells"), mouseX, mouseY);
        } else if (this.powersButton != null && this.powersButton.isHovered()) {
            context.drawTooltip(this.textRenderer, Text.literal("Powers"), mouseX, mouseY);
        }

        if (openPanel != PanelType.NONE) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 500);
            renderPanel(context, mouseX, mouseY, delta);
            context.getMatrices().pop();
        } else {
            this.descriptionBox.renderHints(context, mouseX, mouseY);
        }
    }

    private void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int left = panelX;
        int top = panelY;
        int right = panelX + panelW;
        int bottom = panelY + panelH;
        panelMobSpots.clear();
        panelTagSpots.clear();
        ctx.fill(left, top, right, bottom, 0xC0101010);
        ctx.drawBorder(left, top, panelW, panelH, 0xFFFFFFFF);

        String title = switch (openPanel) {
            case STATS -> "Stats";
            case SPELLS -> "Spells";
            case POWERS -> "Powers";
            default -> "";
        };
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(title), left + panelW / 2, top + 4, 0xFFFFFF);

        renderTabs(ctx, mouseX, mouseY);

        int contentLeft = left + 6;
        int contentTop = top + 16 + 12 + 6;
        int contentRight = right - 6;
        int contentBottom = bottom - 6;

        ctx.fill(contentLeft - 1, contentTop - 1, contentRight + 1, contentBottom + 1, 0xFF000000);
        ctx.fill(contentLeft, contentTop, contentRight, contentBottom, 0xB0151515);

        clampScroll();
        ctx.enableScissor(contentLeft, contentTop, contentRight, contentBottom);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, -contentScroll, 0);
        currentRowHits.clear();
        switch (openPanel) {
            case STATS -> renderStatsContent(ctx, contentLeft, contentTop, contentRight, contentBottom);
            case SPELLS -> renderSpellsContent(ctx, contentLeft, contentTop, contentRight, contentBottom);
            case POWERS -> renderPowersContent(ctx, contentLeft, contentTop, contentRight, contentBottom);
            default -> {}
        }

        ctx.getMatrices().pop();
        ctx.disableScissor();
        if (isWithin(mouseX, mouseY, contentLeft, contentTop, contentRight - contentLeft, contentBottom - contentTop)) {
            for (MobSpotP s : panelMobSpots) {
                if (s.hit(mouseX, mouseY)) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(0, 0, 2000);
                    TitleDescriptionBox.renderMobTooltip(ctx, mouseX, mouseY, s.id, s.name);
                    ctx.getMatrices().pop();
                    return;
                }
            }
            for (TagSpotP s : panelTagSpots) {
                if (s.hit(mouseX, mouseY)) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(0, 0, 2000);
                    TitleDescriptionBox.renderTagTooltip(ctx, mouseX, mouseY, s.tagId);
                    ctx.getMatrices().pop();
                    return;
                }
            }
        }
    }

    private void renderTabs(DrawContext ctx, int mouseX, int mouseY) {
        boolean eqSelected = (section == Section.EQUIPPED);
        int eqBg = eqSelected ? 0xFF2A2A2A : 0xFF1A1A1A;
        int pmBg = !eqSelected ? 0xFF2A2A2A : 0xFF1A1A1A;

        ctx.fill(tabEquippedX, tabEquippedY, tabEquippedX + tabEquippedW, tabEquippedY + tabEquippedH, eqBg);
        ctx.drawBorder(tabEquippedX, tabEquippedY, tabEquippedW, tabEquippedH, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Equipped"),
                tabEquippedX + tabEquippedW / 2, tabEquippedY + 2, 0xFFFFFF);

        ctx.fill(tabPermanentX, tabPermanentY, tabPermanentX + tabPermanentW, tabPermanentY + tabPermanentH, pmBg);
        ctx.drawBorder(tabPermanentX, tabPermanentY, tabPermanentW, tabPermanentH, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Permanent"),
                tabPermanentX + tabPermanentW / 2, tabPermanentY + 2, 0xFFFFFF);
    }

    private void renderStatsContent(DrawContext ctx, int l, int t, int r, int b) {
        if (section == Section.EQUIPPED) {
            List<Text> lines = new ArrayList<>();
            Title active = getEquippedTitle();
            if (active != null) {
                lines.addAll(formatBonusesAttributes(active.bonuses));
                lines.addAll(formatDamageLines(active.bonuses));
            }
            if (lines.isEmpty()) lines.add(Text.literal("None"));
            drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, lines, PANEL_TEXT_SCALE);
            return;
        }

        Aggregated agg = aggregatePermanent();
        drawAggregatedAttributesWithToggles(ctx, l, t, agg.attrSums, PANEL_TEXT_SCALE);
        int yAfterAttrs = toggledLastY;
        drawAggregatedDamageWithTogglesAndSpots(ctx, l, yAfterAttrs, agg.dmgSums, PANEL_TEXT_SCALE);
        if (agg.attrSums.isEmpty() && agg.dmgSums.isEmpty()) {
            drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, List.of(Text.literal("None")), PANEL_TEXT_SCALE);
        }
    }

    private void renderSpellsContent(DrawContext ctx, int l, int t, int r, int b) {
        if (section == Section.EQUIPPED) {
            List<Text> lines = new ArrayList<>();
            Title active = getEquippedTitle();
            if (active != null) {
                lines.addAll(formatBonusesAttributes(active.bonuses));
            }
            if (!lines.isEmpty()) {
                drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, lines, PANEL_TEXT_SCALE);
                int usedH = measureLinesHeight(lines, l + CONTENT_TEXT_PAD_X, contentRight, PANEL_TEXT_SCALE);
                drawDamageRowsScrollable(ctx, l, t + usedH, active != null ? active.bonuses : List.of(), PANEL_TEXT_SCALE);
            } else if (active != null) {

                drawDamageRowsScrollable(ctx, l, t, active.bonuses, PANEL_TEXT_SCALE);
            } else {
                drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, List.of(Text.literal("None")), PANEL_TEXT_SCALE);
            }
            return;
        }


        Aggregated agg = aggregatePermanent();
        List<Identifier> spells = new ArrayList<>(agg.spells);

        if (spells.isEmpty()) {
            drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, List.of(Text.literal("None")), PANEL_TEXT_SCALE);
            return;
        }

        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * PANEL_TEXT_SCALE));
        int icon = Math.max(1, Math.round(12 * PANEL_TEXT_SCALE));
        int gap  = Math.max(1, Math.round(4 * PANEL_TEXT_SCALE));
        int lineH = Math.max(icon, Math.round(font.fontHeight * PANEL_TEXT_SCALE)) + Math.max(1, Math.round(2 * PANEL_TEXT_SCALE));

        for (Identifier sid : spells) {
            Identifier tex = SpellRender.iconTexture(sid);
            Text name = resolveSpellName(sid);

            int iconY = y + (lineH - icon) / 2;
            ctx.drawTexture(tex, l, iconY, 0, 0, icon, icon, icon, icon);
            drawScaled(ctx, name.asOrderedText(), l + icon + gap + CONTENT_TEXT_PAD_X,
                    iconY + (icon - Math.round(font.fontHeight * PANEL_TEXT_SCALE)) / 2, 0xFFFFFF, PANEL_TEXT_SCALE);

            String key = PermaGroupKey.spell(sid);
            drawToggle(ctx, y, lineH, !isDisabled(key));
            currentRowHits.add(new RowHit(y - contentScroll, lineH, key));

            y += lineH;
        }
    }


    private void renderPowersContent(DrawContext ctx, int l, int t, int r, int b) {
        if (section == Section.EQUIPPED) {

            List<Identifier> powers = new ArrayList<>();
            Title active = getEquippedTitle();
            if (active != null) {
                for (Title.Bonus bns : active.bonuses) {
                    if (bns != null && bns.powerId != null && bns.powerId.isPresent()) powers.add(bns.powerId.get());
                }
            }
            if (powers.isEmpty()) {
                drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, List.of(Text.literal("None")), PANEL_TEXT_SCALE);
            } else {
                drawPowerRowsScrollable(ctx, l, t, powers, PANEL_TEXT_SCALE);
            }
            return;
        }

        Aggregated agg = aggregatePermanent();
        List<Identifier> powers = new ArrayList<>(agg.powers);
        if (powers.isEmpty()) {
            drawLinesScrollable(ctx, l + CONTENT_TEXT_PAD_X, t, List.of(Text.literal("None")), PANEL_TEXT_SCALE);
            return;
        }

        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * PANEL_TEXT_SCALE));
        int lineH = Math.max(1, Math.round(font.fontHeight * PANEL_TEXT_SCALE)) + Math.max(1, Math.round(6 * PANEL_TEXT_SCALE));

        for (Identifier pid : powers) {
            Text line = Text.literal("• " + toTitleCase(pid.getPath()));
            drawScaled(ctx, line.asOrderedText(), l + CONTENT_TEXT_PAD_X, y, 0xFFFFFF, PANEL_TEXT_SCALE);

            String key = PermaGroupKey.power(pid);
            drawToggle(ctx, y, lineH, !isDisabled(key));
            currentRowHits.add(new RowHit(y - contentScroll, lineH, key));

            y += lineH;
        }
    }

    private int toggledLastY = 0;

    private void drawAggregatedAttributesWithToggles(
            DrawContext ctx, int l, int t, Map<AttrKey, Double> sums, float scale) {

        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * scale));
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));

        List<Map.Entry<AttrKey, Double>> entries = new ArrayList<>(sums.entrySet());
        entries.sort(Comparator.comparing(e -> e.getKey().attrId.toString()));

        boolean ilt = iltPresent();

        for (Map.Entry<AttrKey, Double> e : entries) {
            AttrKey k = e.getKey();
            double amt = e.getValue();

            var attr = Registries.ATTRIBUTE.get(k.attrId);
            Text attrName = Text.translatable(attr.getTranslationKey());
            String sign = amt >= 0 ? "+" : "";

            Text rowText;

            if (ilt) {
                String raw = attrName.getString();
                int[] span = IconLeadingUtil.firstIconSpan(raw);
                if (span[0] >= 0) {
                    String icon = raw.substring(span[0], span[1]);
                    String restRaw = raw.substring(0, span[0]) + raw.substring(span[1]);
                    String rest = IconLeadingUtil.stripSectionCodes(restRaw).replaceFirst("^\\s+", "");
                    Text iconText = Text.literal(icon + " ");
                    Text restText = Text.literal(rest);

                    if (k.op == EntityAttributeModifier.Operation.ADD_VALUE) {
                        rowText = Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(amt) + " "))
                                .append(restText);
                    } else if (k.op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                        rowText = Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(amt * 100.0) + "% "))
                                .append(restText)
                                .append(Text.literal(" (base)"));
                    } else {
                        rowText = Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(amt * 100.0) + "% "))
                                .append(restText)
                                .append(Text.literal(" (total)"));
                    }

                    String key = PermaGroupKey.attr(k.attrId, k.op);
                    drawScaled(ctx, rowText.asOrderedText(), l + CONTENT_TEXT_PAD_X, y, 0xFFFFFF, scale);
                    drawToggle(ctx, y, lineH, !isDisabled(key));
                    currentRowHits.add(new RowHit(y - contentScroll, lineH, key));

                    y += lineH;
                    continue;
                }
            }

            if (k.op == EntityAttributeModifier.Operation.ADD_VALUE) {
                rowText = Text.literal("• ").append(Text.literal(sign + trim(amt) + " ")).append(attrName);
            } else if (k.op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                rowText = Text.literal("• ").append(Text.literal(sign + trim(amt * 100.0) + "% ")).append(attrName).append(Text.literal(" (base)"));
            } else {
                rowText = Text.literal("• ").append(Text.literal(sign + trim(amt * 100.0) + "% ")).append(attrName).append(Text.literal(" (total)"));
            }

            String key = PermaGroupKey.attr(k.attrId, k.op);
            drawScaled(ctx, rowText.asOrderedText(), l + CONTENT_TEXT_PAD_X, y, 0xFFFFFF, scale);
            drawToggle(ctx, y, lineH, !isDisabled(key));
            currentRowHits.add(new RowHit(y - contentScroll, lineH, key));

            y += lineH;
        }
        toggledLastY = y;
    }


    private void drawDamageRowsScrollable(DrawContext ctx, int l, int t, List<Title.Bonus> bonuses, float scale) {
        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * scale));
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));

        for (Title.Bonus b : bonuses) {
            if (b == null) continue;
            if ((b.damageTarget == null || b.damageTarget.isEmpty()) &&
                    (b.damageTag == null || b.damageTag.isEmpty())) continue;

            String value = (b.damageOp == Title.DamageOp.ADDED)
                    ? ((b.damageAmount >= 0 ? "+" : "") + trim(b.damageAmount))
                    : ((b.damageAmount * 100.0 >= 0 ? "+" : "") + trim(b.damageAmount * 100.0) + "%");

            // prefix "• +X DMG vs "
            Text pre = Text.literal("• " + value + " DMG vs ");
            int x = l + CONTENT_TEXT_PAD_X;
            OrderedText preOT = pre.asOrderedText();
            drawScaled(ctx, preOT, x, y, 0xFFFFFF, scale);
            x += Math.round(font.getWidth(preOT) * scale);

            // clickable/hoverable target part
            if (b.damageTarget != null && b.damageTarget.isPresent()) {
                var type = Registries.ENTITY_TYPE.get(b.damageTarget.get());
                Text mobName = Text.translatable(type.getTranslationKey());
                OrderedText mobOT = mobName.asOrderedText();
                int w = Math.round(font.getWidth(mobOT) * scale);
                drawScaled(ctx, mobOT, x, y, 0xFFFFFF, scale);
                panelMobSpots.add(new MobSpotP(x, y - contentScroll, w, Math.round(font.fontHeight * scale), b.damageTarget.get(), mobName));
                x += w;
            } else if (b.damageTag != null && b.damageTag.isPresent()) {
                Identifier tagId = b.damageTag.get();
                String pretty = toTitleCase(tagId.getPath());
                Text tagText = Text.literal(pretty).styled(s -> s.withUnderline(true));
                OrderedText tagOT = tagText.asOrderedText();
                int w = Math.round(font.getWidth(tagOT) * scale);
                drawScaled(ctx, tagOT, x, y, 0xFFFFFF, scale);
                panelTagSpots.add(new TagSpotP(x, y - contentScroll, w, Math.round(font.fontHeight * scale), tagId));
                x += w;
            }

            y += lineH;
        }
    }

    // Draw aggregated damage (Permanent) with toggles and hover rects
    private void drawAggregatedDamageWithTogglesAndSpots(
            DrawContext ctx, int l, int t, Map<DmgKey, Double> sums, float scale) {

        TextRenderer font = this.textRenderer;
        int y = Math.max(t, toggledLastY == 0 ? t : toggledLastY);
        y += Math.max(0, Math.round(1 * scale));
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));

        List<Map.Entry<DmgKey, Double>> entries = new ArrayList<>(sums.entrySet());
        entries.sort(Comparator.comparing(e -> (e.getKey().isTag ? "#" : "") + e.getKey().idOrTag.toString()));

        for (Map.Entry<DmgKey, Double> e : entries) {
            DmgKey k = e.getKey();
            double amt = e.getValue();

            String value = (k.op == Title.DamageOp.ADDED)
                    ? ((amt >= 0 ? "+" : "") + trim(amt))
                    : ((amt * 100.0 >= 0 ? "+" : "") + trim(amt * 100.0) + "%");

            Text pre = Text.literal("• " + value + " DMG vs ");
            int x = l + CONTENT_TEXT_PAD_X;
            OrderedText preOT = pre.asOrderedText();
            drawScaled(ctx, preOT, x, y, 0xFFFFFF, scale);
            x += Math.round(font.getWidth(preOT) * scale);

            if (!k.isTag) {
                var type = Registries.ENTITY_TYPE.get(k.idOrTag);
                Text mobName = Text.translatable(type.getTranslationKey());
                OrderedText mobOT = mobName.asOrderedText();
                int w = Math.round(font.getWidth(mobOT) * scale);
                drawScaled(ctx, mobOT, x, y, 0xFFFFFF, scale);
                panelMobSpots.add(new MobSpotP(x, y - contentScroll, w, Math.round(font.fontHeight * scale), k.idOrTag, mobName));
            } else {
                String pretty = toTitleCase(k.idOrTag.getPath());
                Text tagText = Text.literal(pretty).styled(s -> s.withUnderline(true));
                OrderedText tagOT = tagText.asOrderedText();
                int w = Math.round(font.getWidth(tagOT) * scale);
                drawScaled(ctx, tagOT, x, y, 0xFFFFFF, scale);
                panelTagSpots.add(new TagSpotP(x, y - contentScroll, w, Math.round(font.fontHeight * scale), k.idOrTag));
            }

            // existing toggle logic
            String key = k.isTag
                    ? PermaGroupKey.dmgTag(k.idOrTag, k.op)
                    : PermaGroupKey.dmgTarget(k.idOrTag, k.op);
            drawToggle(ctx, y, lineH, !isDisabled(key));
            currentRowHits.add(new RowHit(y - contentScroll, lineH, key));

            y += lineH;
        }
        toggledLastY = y;
    }

    private Title getEquippedTitle() {
        Identifier activeId = TitleClientData.getSelfActive();
        return activeId != null ? TitleRegistry.get(activeId) : null;
    }
    private static final class AttrKey {
        final Identifier attrId;
        final EntityAttributeModifier.Operation op;
        AttrKey(Identifier id, EntityAttributeModifier.Operation op) { this.attrId=id; this.op=op; }
        @Override public boolean equals(Object o){ if (!(o instanceof AttrKey a)) return false; return Objects.equals(attrId,a.attrId)&&op==a.op; }
        @Override public int hashCode(){ return Objects.hash(attrId, op); }
    }

    private static final class DmgKey {
        final Identifier idOrTag;
        final boolean isTag;
        final Title.DamageOp op;
        DmgKey(Identifier idOrTag, boolean isTag, Title.DamageOp op){ this.idOrTag=idOrTag; this.isTag=isTag; this.op=op; }
        @Override public boolean equals(Object o){ if (!(o instanceof DmgKey d)) return false; return isTag==d.isTag && op==d.op && Objects.equals(idOrTag,d.idOrTag); }
        @Override public int hashCode(){ return Objects.hash(idOrTag, isTag, op); }
    }

    private static final class Aggregated {
        final Map<AttrKey, Double> attrSums = new LinkedHashMap<>();
        final Map<DmgKey, Double>  dmgSums  = new LinkedHashMap<>();
        final Set<Identifier> spells = new LinkedHashSet<>();
        final Set<Identifier> powers = new LinkedHashSet<>();
    }

    private Aggregated aggregatePermanent() {
        Aggregated out = new Aggregated();
        for (Identifier id : TitleClientData.getSelfUnlocked()) {
            Title tTitle = TitleRegistry.get(id);
            if (tTitle == null) continue;
            for (Title.Bonus b : tTitle.permaBonuses) {
                if (b == null) continue;

                if (b.attribute != null) {
                    Identifier attrId = Registries.ATTRIBUTE.getId(b.attribute.value());
                    if (attrId != null) {
                        AttrKey key = new AttrKey(attrId, b.operation);
                        out.attrSums.merge(key, b.amount, Double::sum);
                    }
                } else if (b.damageTarget != null && b.damageTarget.isPresent()) {
                    DmgKey key = new DmgKey(b.damageTarget.get(), false, b.damageOp);
                    out.dmgSums.merge(key, b.damageAmount, Double::sum);
                } else if (b.damageTag != null && b.damageTag.isPresent()) {
                    DmgKey key = new DmgKey(b.damageTag.get(), true, b.damageOp);
                    out.dmgSums.merge(key, b.damageAmount, Double::sum);
                } else if (b.spellId != null && b.spellId.isPresent()) {
                    out.spells.add(b.spellId.get());
                } else if (b.powerId != null && b.powerId.isPresent()) {
                    out.powers.add(b.powerId.get());
                }
            }
        }
        return out;
    }

    private boolean iltPresent() {
        FabricLoader fl = FabricLoader.getInstance();
        return fl.isModLoaded("iconleadingtooltip") || fl.isModLoaded("icon-leading-tooltip");
    }

    private List<Text> formatBonusesAttributes(List<Title.Bonus> bonuses) {
        List<Text> out = new ArrayList<>();
        boolean ilt = iltPresent();

        for (Title.Bonus b : bonuses) {
            if (b == null || b.attribute == null) continue;

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

    private List<Text> formatAggregatedAttributes(Map<AttrKey, Double> sums) {
        List<Text> out = new ArrayList<>();
        boolean ilt = iltPresent();

        List<Map.Entry<AttrKey, Double>> entries = new ArrayList<>(sums.entrySet());
        entries.sort(Comparator.comparing(e -> e.getKey().attrId.toString()));

        for (Map.Entry<AttrKey, Double> e : entries) {
            AttrKey k = e.getKey();
            double amt = e.getValue();
            var attr = Registries.ATTRIBUTE.get(k.attrId);
            Text attrName = Text.translatable(attr.getTranslationKey());
            String sign = amt >= 0 ? "+" : "";

            if (ilt) {
                String raw = attrName.getString();
                int[] span = IconLeadingUtil.firstIconSpan(raw);
                if (span[0] >= 0) {
                    String icon = raw.substring(span[0], span[1]);
                    String restRaw = raw.substring(0, span[0]) + raw.substring(span[1]);
                    String rest = IconLeadingUtil.stripSectionCodes(restRaw).replaceFirst("^\\s+", "");
                    Text iconText = Text.literal(icon + " ");
                    Text restText = Text.literal(rest);

                    if (k.op == EntityAttributeModifier.Operation.ADD_VALUE) {
                        out.add(Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(amt) + " "))
                                .append(restText));
                    } else if (k.op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                        out.add(Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(amt * 100.0) + "% "))
                                .append(restText)
                                .append(Text.literal(" (base)")));
                    } else {
                        out.add(Text.empty()
                                .append(iconText)
                                .append(Text.literal(sign + trim(amt * 100.0) + "% "))
                                .append(restText)
                                .append(Text.literal(" (total)")));
                    }
                    continue;
                }
            }

            if (k.op == EntityAttributeModifier.Operation.ADD_VALUE) {
                out.add(Text.literal("• ").append(Text.literal(sign + trim(amt) + " ")).append(attrName));
            } else if (k.op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                out.add(Text.literal("• ").append(Text.literal(sign + trim(amt * 100.0) + "% ")).append(attrName).append(Text.literal(" (base)")));
            } else {
                out.add(Text.literal("• ").append(Text.literal(sign + trim(amt * 100.0) + "% ")).append(attrName).append(Text.literal(" (total)")));
            }
        }
        return out;
    }


    private List<Text> formatDamageLines(List<Title.Bonus> bonuses) {
        List<Text> out = new ArrayList<>();
        for (Title.Bonus b : bonuses) {
            if (b == null) continue;
            if ((b.damageTarget != null && b.damageTarget.isPresent())
                    || (b.damageTag != null && b.damageTag.isPresent())) {

                String value;
                if (b.damageOp == Title.DamageOp.ADDED) {
                    value = (b.damageAmount >= 0 ? "+" : "") + trim(b.damageAmount);
                } else {
                    double v = b.damageAmount * 100.0;
                    value = (v >= 0 ? "+" : "") + trim(v) + "%";
                }

                if (b.damageTarget != null && b.damageTarget.isPresent()) {
                    var type = Registries.ENTITY_TYPE.get(b.damageTarget.get());
                    Text mobName = Text.translatable(type.getTranslationKey());
                    out.add(Text.literal(value + " DMG vs ").append(mobName));
                } else {
                    Identifier tagId = b.damageTag.get();
                    String pretty = toTitleCase(tagId.getPath());
                    out.add(Text.literal(value + " DMG vs any " + pretty));
                }
            }
        }
        return out;
    }

    private List<Text> formatAggregatedDamage(Map<DmgKey, Double> sums) {
        List<Text> out = new ArrayList<>();

        List<Map.Entry<DmgKey, Double>> entries = new ArrayList<>(sums.entrySet());
        entries.sort(Comparator.comparing(e -> (e.getKey().isTag ? "#" : "") + e.getKey().idOrTag.toString()));

        for (Map.Entry<DmgKey, Double> e : entries) {
            DmgKey k = e.getKey();
            double amt = e.getValue();

            String value;
            if (k.op == Title.DamageOp.ADDED) {
                value = (amt >= 0 ? "+" : "") + trim(amt);
            } else {
                double v = amt * 100.0;
                value = (v >= 0 ? "+" : "") + trim(v) + "%";
            }

            if (!k.isTag) {
                var type = Registries.ENTITY_TYPE.get(k.idOrTag);
                Text mobName = Text.translatable(type.getTranslationKey());
                out.add(Text.literal(  value + " DMG vs ").append(mobName));
            } else {
                String pretty = toTitleCase(k.idOrTag.getPath());
                out.add(Text.literal(  value + " DMG vs any " + pretty));
            }
        }
        return out;
    }

    private void drawScaled(DrawContext ctx, OrderedText text, int x, int y, int color, float scale) {
        if (scale == 1f) { ctx.drawTextWithShadow(this.textRenderer, text, x, y, color); return; }
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0f);
        int sx = Math.round(x / scale);
        int sy = Math.round(y / scale);
        ctx.drawTextWithShadow(this.textRenderer, text, sx, sy, color);
        ctx.getMatrices().pop();
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.indexOf('.') >= 0) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private Text resolveSpellName(Identifier id) {
        var client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            var reg = SpellRegistry.from(client.world);
            var entry = reg.getEntry(id).orElse(null);
            if (entry != null) {
                Text t = Text.translatable("spell." + id.getNamespace() + "." + id.getPath());
                String raw = t.getString();
                if (!raw.equals("spell." + id.getNamespace() + "." + id.getPath())) return t;
            }
        }
        return Text.literal(toTitleCase(id.getPath()));
    }

    private static String toTitleCase(String path) {
        String nice = path.replace('_', ' ');
        return Arrays.stream(nice.split(" "))
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (openPanel != PanelType.NONE) {
            if (isWithin(mouseX, mouseY, tabEquippedX, tabEquippedY, tabEquippedW, tabEquippedH)) {
                this.section = Section.EQUIPPED;
                this.contentScroll = 0;
                return true;
            }
            if (isWithin(mouseX, mouseY, tabPermanentX, tabPermanentY, tabPermanentW, tabPermanentH)) {
                this.section = Section.PERMANENT;
                this.contentScroll = 0;
                return true;
            }

        }
        if (openPanel != PanelType.NONE && section == Section.PERMANENT) {

            if (isWithin(mouseX, mouseY, contentLeft, contentTop, contentRight - contentLeft, contentBottom - contentTop)) {
                int tx = toggleX();
                for (RowHit rh : currentRowHits) {
                    int ry = rh.y();
                    int rhH = rh.h();

                    if (mouseX >= tx && mouseX < tx + TOGGLE_W &&
                            mouseY >= ry && mouseY < ry + rhH) {
                        toggleKey(rh.key());
                        return true;
                    }
                }
            }
        }
        boolean handledByChildren = false;
        if (this.titlesListBox != null && this.titlesListBox.mouseClicked(mouseX, mouseY, button)) {
            handledByChildren = true;
        } else if (this.descriptionBox != null && this.descriptionBox.mouseClicked(mouseX, mouseY, button)) {
            handledByChildren = true;
        } else if (super.mouseClicked(mouseX, mouseY, button)) {
            handledByChildren = true;
        }

        if (handledByChildren) {
            return true;
        }

        if (openPanel != PanelType.NONE) {
            if (!isWithin(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
                this.openPanel = PanelType.NONE;
                this.contentScroll = 0;
                return true;
            }
        }

        return false;
    }

    private void sendPermaDisabledToServer(Set<String> disabled) {
        ClientPlayNetworking.send(
                new TitlePayloads.RequestSetPermaToggles(
                        new ArrayList<>(disabled)
                )
        );
    }
    private boolean isWithin(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < (rx + rw) && my >= ry && my < (ry + rh);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (openPanel != PanelType.NONE) {
            if (isWithin(mouseX, mouseY, contentLeft, contentTop, contentRight - contentLeft, contentBottom - contentTop)) {
                int step = 12;
                contentScroll -= (int)Math.round(verticalAmount * step);
                clampScroll();
                return true;
            }
        }

        if (this.titlesListBox != null && this.titlesListBox.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if (this.descriptionBox != null && this.descriptionBox.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.titlesListBox != null && this.titlesListBox.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        if (this.descriptionBox != null && this.descriptionBox.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.titlesListBox != null && this.titlesListBox.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (this.descriptionBox != null && this.descriptionBox.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawTexture(BACKGROUND, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
    }
    private void drawLinesScrollable(DrawContext ctx, int l, int t, List<Text> lines, float scale) {
        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * scale));
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));
        int wrapWidth = Math.max(1, Math.round((contentRight - l) / scale));

        for (Text line : lines) {
            List<OrderedText> wrapped = font.wrapLines(line, wrapWidth);
            for (OrderedText ot : wrapped) {
                drawScaled(ctx, ot, l, y, 0xFFFFFF, scale);
                y += lineH;
            }
        }
    }

    private void drawSpellRowsScrollable(DrawContext ctx, int l, int t, List<Identifier> spells, float scale) {
        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * scale));
        int icon = Math.max(1, Math.round(12 * scale));
        int gap  = Math.max(1, Math.round(4 * scale));
        int lineH = Math.max(icon, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(2 * scale));

        for (Identifier sid : spells) {
            Identifier tex = SpellRender.iconTexture(sid);
            Text name = resolveSpellName(sid);

            int iconY = y + (lineH - icon) / 2;
            ctx.drawTexture(tex, l, iconY, 0, 0, icon, icon, icon, icon);
            drawScaled(ctx, name.asOrderedText(), l + icon + gap + CONTENT_TEXT_PAD_X,
                    iconY + (icon - Math.round(font.fontHeight * scale)) / 2, 0xFFFFFF, scale);
            y += lineH;
        }
    }

    private void drawPowerRowsScrollable(DrawContext ctx, int l, int t, List<Identifier> powers, float scale) {
        TextRenderer font = this.textRenderer;
        int y = t + Math.max(1, Math.round(2 * scale));
        int lineH = Math.max(1, Math.round(font.fontHeight * scale)) + Math.max(1, Math.round(6 * scale));

        for (Identifier pid : powers) {
            Text line = Text.literal("• " + toTitleCase(pid.getPath()));
            drawScaled(ctx, line.asOrderedText(), l + CONTENT_TEXT_PAD_X, y, 0xFFFFFF, scale);
            y += lineH;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
