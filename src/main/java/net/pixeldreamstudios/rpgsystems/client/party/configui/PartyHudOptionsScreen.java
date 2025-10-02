package net.pixeldreamstudios.rpgsystems.client.party.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;

@Environment(EnvType.CLIENT)
public final class PartyHudOptionsScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget showRpgMana;
    private boolean rpgManaAvailable;
    private CheckboxWidget hudEnabled;
    private CheckboxWidget showArrows;
    private CheckboxWidget showHp;
    private CheckboxWidget showHunger;
    private CheckboxWidget showStamina;
    private CheckboxWidget showMana;

    private ButtonWidget styleButton;
    private PartyHudClientConfig.HudStyle localStyle;

    private TextFieldWidget hudXField;
    private TextFieldWidget hudYField;
    private TextFieldWidget hudScaleField;
    private TextFieldWidget maxHudCountField;
    private int yMaxHuds;
    private boolean staminaAvailable;
    private boolean manaAvailable;
    private String warning = "";

    private int yStart;
    private int formHeight;
    private double scroll;
    private int contentHeight;

    private int yHudEnabled;
    private int yShowArrows;
    private int yHudStyle;
    private int yHudX;
    private int yHudY;
    private int yHudScale;
    private int yShowHp;
    private int yShowHunger;
    private int yShowStamina;
    private int yShowMana;
    private int yShowRpgMana;

    public PartyHudOptionsScreen(Screen parent) {
        super(Text.literal("Party HUD Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearChildren();

        int centerX = this.width / 2;
        int w = 280;
        int h = 20;

        yStart = 56;
        int buttonsPad = 70;
        formHeight = Math.max(60, this.height - yStart - buttonsPad);

        this.staminaAvailable = FabricLoader.getInstance().isModLoaded("staminaattributes");
        this.manaAvailable    = FabricLoader.getInstance().isModLoaded("manaattributes");
        this.rpgManaAvailable = FabricLoader.getInstance().isModLoaded("rpgmana");
        var cfg = PartyHudClientConfig.get();
        this.localStyle = cfg.hudStyle;

        int y = yStart;

        this.hudEnabled = CheckboxWidget.builder(Text.literal("Enable Party HUD"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.hudEnabled).build();
        yHudEnabled = y;
        y += 24;

        this.showArrows = CheckboxWidget.builder(Text.literal("Directional Arrows"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showArrows).build();
        yShowArrows = y;
        y += 24;

        this.styleButton = ButtonWidget.builder(Text.literal("HUD Style: " + localStyle.name()), b -> {
            this.localStyle = (this.localStyle == PartyHudClientConfig.HudStyle.ORIGINAL)
                    ? PartyHudClientConfig.HudStyle.SIMPLE : PartyHudClientConfig.HudStyle.ORIGINAL;
            this.styleButton.setMessage(Text.literal("HUD Style: " + localStyle.name()));
        }).size(180, h).position(centerX - w / 2, y).build();
        yHudStyle = y;
        y += 28;

        int fieldW = 100;

        hudXField = new TextFieldWidget(this.textRenderer, centerX - w / 2 + w - fieldW, y, fieldW, h, Text.literal(""));
        hudXField.setText(String.valueOf(cfg.partyHudX));
        yHudX = y;
        addDrawableChild(hudXField);
        y += 24;

        hudYField = new TextFieldWidget(this.textRenderer, centerX - w / 2 + w - fieldW, y, fieldW, h, Text.literal(""));
        hudYField.setText(String.valueOf(cfg.partyHudY));
        yHudY = y;
        addDrawableChild(hudYField);
        y += 24;

        hudScaleField = new TextFieldWidget(this.textRenderer, centerX - w / 2 + w - fieldW, y, fieldW, h, Text.literal(""));
        hudScaleField.setText(trimFloat(cfg.hudScale));
        yHudScale = y;
        addDrawableChild(hudScaleField);
        y += 28;

        maxHudCountField = new TextFieldWidget(this.textRenderer, centerX - w / 2 + w - fieldW, y, fieldW, h, Text.literal(""));
        maxHudCountField.setText(String.valueOf(cfg.maxVisiblePartyHuds));
        yMaxHuds = y;
        addDrawableChild(maxHudCountField);
        y += 24;

        this.showHp = CheckboxWidget.builder(Text.literal("HP Bar"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showHpBar).build();
        yShowHp = y;
        y += 24;

        this.showHunger = CheckboxWidget.builder(Text.literal("Hunger Bar"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showHungerBar).build();
        yShowHunger = y;
        y += 24;

        this.showStamina = CheckboxWidget.builder(Text.literal("Stamina Bar (TheRedBrain)"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showStaminaBar && staminaAvailable).build();
        this.showStamina.active = staminaAvailable;
        yShowStamina = y;
        y += 24;

        this.showMana = CheckboxWidget.builder(Text.literal("Mana Bar (TheRedBrain)"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showManaBar && manaAvailable).build();
        this.showMana.active = manaAvailable;
        yShowMana = y;
        y += 24;

        this.showRpgMana = CheckboxWidget.builder(Text.literal("Mana (RPGMana)"), this.textRenderer)
                .pos(centerX - w / 2, y)
                .checked(cfg.showRpgManaBar && rpgManaAvailable)
                .build();
        this.showRpgMana.active = rpgManaAvailable;
        yShowRpgMana = y;
        y += 24;

        this.addDrawableChild(styleButton);
        this.addDrawableChild(hudEnabled);
        this.addDrawableChild(showArrows);
        this.addDrawableChild(showHp);
        this.addDrawableChild(showHunger);
        this.addDrawableChild(showStamina);
        this.addDrawableChild(showMana);
        this.addDrawableChild(showRpgMana);

        contentHeight = (y - yStart);

        int btnY = yStart + formHeight + 16;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            applyAndSave();
            this.client.setScreen(this.parent);
        }).size(100, h).position(centerX - 110, btnY).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> this.client.setScreen(this.parent))
                .size(100, h).position(centerX + 10, btnY).build());

        applyScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (mouseY >= yStart && mouseY <= yStart + formHeight) {
            this.scroll = clampScroll(this.scroll - vert * 18.0);
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horiz, vert);
    }

    private double clampScroll(double s) {
        int max = Math.max(0, contentHeight - formHeight);
        if (s < 0) return 0;
        if (s > max) return max;
        return s;
    }

    private boolean inViewportY(int widgetY, int height) {
        int top = yStart;
        int bottom = yStart + formHeight;
        int y1 = widgetY + height;
        return y1 > top && widgetY < bottom;
    }

    private void setVis(net.minecraft.client.gui.widget.ClickableWidget w, boolean v) {
        w.visible = v;
    }

    private void applyScroll() {
        int off = (int) Math.round(scroll);

        hudEnabled.setY(yHudEnabled - off);
        showArrows.setY(yShowArrows - off);
        styleButton.setY(yHudStyle - off);
        hudXField.setY(yHudX - off);
        hudYField.setY(yHudY - off);
        hudScaleField.setY(yHudScale - off);
        maxHudCountField.setY(yMaxHuds - off);
        showHp.setY(yShowHp - off);
        showHunger.setY(yShowHunger - off);
        showStamina.setY(yShowStamina - off);
        showMana.setY(yShowMana - off);
        showRpgMana.setY(yShowRpgMana - off);

        int rowH = 20;
        setVis(hudEnabled,        inViewportY(hudEnabled.getY(), rowH));
        setVis(showArrows,        inViewportY(showArrows.getY(), rowH));
        setVis(styleButton,       inViewportY(styleButton.getY(), rowH));
        setVis(hudXField,         inViewportY(hudXField.getY(), rowH));
        setVis(hudYField,         inViewportY(hudYField.getY(), rowH));
        setVis(hudScaleField,     inViewportY(hudScaleField.getY(), rowH));
        setVis(maxHudCountField,  inViewportY(maxHudCountField.getY(), rowH));
        setVis(showHp,            inViewportY(showHp.getY(), rowH));
        setVis(showHunger,        inViewportY(showHunger.getY(), rowH));
        setVis(showStamina,       inViewportY(showStamina.getY(), rowH));
        setVis(showMana,          inViewportY(showMana.getY(), rowH));
        setVis(showRpgMana,       inViewportY(showRpgMana.getY(), rowH));

        showStamina.active = staminaAvailable && showStamina.visible;
        showMana.active    = manaAvailable    && showMana.visible;
        showRpgMana.active = rpgManaAvailable && showRpgMana.visible;
    }


    private static int parseIntSafe(TextFieldWidget f, int fallback) {
        try { return Integer.parseInt(f.getText().trim()); } catch (Throwable t) { return fallback; }
    }

    private static float parseFloatSafe(TextFieldWidget f, float fallback) {
        try { return Float.parseFloat(f.getText().trim()); } catch (Throwable t) { return fallback; }
    }

    private static String trimFloat(float f) {
        String s = Float.toString(f);
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private void applyAndSave() {
        var cfg = PartyHudClientConfig.get();
        cfg.hudEnabled = this.hudEnabled.isChecked();
        cfg.showArrows = this.showArrows.isChecked();
        cfg.showHpBar = this.showHp.isChecked();
        cfg.showHungerBar = this.showHunger.isChecked();
        cfg.showStaminaBar = this.showStamina.isChecked() && staminaAvailable;
        cfg.showManaBar = this.showMana.isChecked() && manaAvailable;
        cfg.showRpgManaBar = this.showRpgMana.isChecked() && rpgManaAvailable;
        cfg.hudStyle = this.localStyle;

        cfg.partyHudX = parseIntSafe(hudXField, cfg.partyHudX);
        cfg.partyHudY = parseIntSafe(hudYField, cfg.partyHudY);

        float parsedScale = parseFloatSafe(hudScaleField, cfg.hudScale);
        if (Float.isNaN(parsedScale) || Float.isInfinite(parsedScale)) {
            parsedScale = cfg.hudScale;
        }
        cfg.hudScale = Math.max(parsedScale, 0.1f);
        int parsedLimit = parseIntSafe(maxHudCountField, cfg.maxVisiblePartyHuds);
        if (parsedLimit < -1) parsedLimit = -1;
        cfg.maxVisiblePartyHuds = parsedLimit;
        cfg.normalizeVisibilityLimit();

        cfg.enforceMaxThreeBars();
        PartyHudClientConfig.save();
    }

    private int currentBarCount() {
        int c = 0;
        if (showHp.isChecked()) c++;
        if (showHunger.isChecked()) c++;
        if (showStamina.isChecked()) c++;
        if (showMana.isChecked()) c++;
        if (showRpgMana.isChecked() && rpgManaAvailable) c++;
        return c;
    }

    private boolean wouldExceedIfToggled(CheckboxWidget cb) {
        boolean turningOn = !cb.isChecked();
        if (!turningOn) return false;
        int projected = currentBarCount() + 1;
        return projected > 3;
    }

    private boolean interceptIfLimitHit(double mouseX, double mouseY) {
        if (showHp.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showHp)) {
            warning = "You can enable at most 3 bars";
            return true;
        }
        if (showHunger.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showHunger)) {
            warning = "You can enable at most 3 bars";
            return true;
        }
        if (showStamina.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showStamina)) {
            warning = "You can enable at most 3 bars";
            return true;
        }
        if (showMana.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showMana)) {
            warning = "You can enable at most 3 bars";
            return true;
        }
        if (showRpgMana.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showRpgMana)) {
            warning = "You can enable at most 3 bars";
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && interceptIfLimitHit(mouseX, mouseY)) {
            return true;
        }
        warning = "";
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int w = 280;
        int labelX = centerX - w / 2;
        int labelColor = 0xAAAAAA;

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("At most 3 bars can be active"), this.width / 2, 36, 0xAAAAAA);

        int off = (int) Math.round(scroll);
        int xLabelY = yHudX - off;
        int yLabelY = yHudY - off;
        int sLabelY = yHudScale - off;

        ctx.enableScissor(0, yStart, this.width, yStart + formHeight);
        ctx.drawText(this.textRenderer, Text.literal("HUD X (from left)"), labelX, xLabelY + 4, labelColor, false);
        ctx.drawText(this.textRenderer, Text.literal("HUD Y (from top)"),  labelX, yLabelY + 4, labelColor, false);
        ctx.drawText(this.textRenderer, Text.literal("HUD Scale (> 0.1)"), labelX, sLabelY + 4, labelColor, false);
        int limitLabelY = yMaxHuds - off;
        ctx.drawText(this.textRenderer, Text.literal("Max visible HUDs (-1 = infinite)"), labelX, limitLabelY + 4, labelColor, false);
        ctx.disableScissor();

        if (!warning.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(warning), this.width / 2, this.height - 40, 0xFF5555);
        }
    }
}
