package net.pixeldreamstudios.rpgsystems.client.party.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
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

    private boolean staminaAvailable;
    private boolean manaAvailable;
    private String warning = "";

    public PartyHudOptionsScreen(Screen parent) {
        super(Text.literal("Party HUD Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 56;
        int w = 280;
        int h = 20;

        this.staminaAvailable = FabricLoader.getInstance().isModLoaded("staminaattributes");
        this.manaAvailable = FabricLoader.getInstance().isModLoaded("manaattributes");
        this.rpgManaAvailable = FabricLoader.getInstance().isModLoaded("rpgmana");
        var cfg = PartyHudClientConfig.get();

        this.hudEnabled = CheckboxWidget.builder(Text.literal("Enable Party HUD"), this.textRenderer)
                .pos(centerX - w/2, y).checked(cfg.hudEnabled).build();
        y += 24;

        this.showArrows = CheckboxWidget.builder(Text.literal("Directional Arrows"), this.textRenderer)
                .pos(centerX - w/2, y).checked(cfg.showArrows).build();
        y += 24;

        this.showHp = CheckboxWidget.builder(Text.literal("HP Bar"), this.textRenderer)
                .pos(centerX - w/2, y).checked(cfg.showHpBar).build();
        y += 24;

        this.showHunger = CheckboxWidget.builder(Text.literal("Hunger Bar"), this.textRenderer)
                .pos(centerX - w/2, y).checked(cfg.showHungerBar).build();
        y += 24;

        this.showStamina = CheckboxWidget.builder(Text.literal("Stamina Bar (TheRedBrain)"), this.textRenderer)
                .pos(centerX - w/2, y).checked(cfg.showStaminaBar && staminaAvailable).build();
        this.showStamina.active = staminaAvailable;
        y += 24;

        this.showMana = CheckboxWidget.builder(Text.literal("Mana Bar (TheRedBrain)"), this.textRenderer)
                .pos(centerX - w/2, y).checked(cfg.showManaBar && manaAvailable).build();
        this.showMana.active = manaAvailable;
        y += 24;
        this.showRpgMana = CheckboxWidget.builder(Text.literal("Mana (RPGMana)"), this.textRenderer)
                .pos(centerX - w/2, y)
                .checked(cfg.showRpgManaBar && rpgManaAvailable)
                .build();
        this.showRpgMana.active = rpgManaAvailable;
        y += 24;

        this.addDrawableChild(showRpgMana);
        this.addDrawableChild(hudEnabled);
        this.addDrawableChild(showArrows);
        this.addDrawableChild(showHp);
        this.addDrawableChild(showHunger);
        this.addDrawableChild(showStamina);
        this.addDrawableChild(showMana);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            applyAndSave();
            this.client.setScreen(this.parent);
        }).size(100, h).position(centerX - 110, y).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> this.client.setScreen(this.parent))
                .size(100, h).position(centerX + 10, y).build());
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

        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("At most 3 bars can be active"), this.width / 2, 36, 0xAAAAAA);
        if (!warning.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(warning), this.width / 2, this.height - 40, 0xFF5555);
        }
    }
}
