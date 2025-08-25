package net.pixeldreamstudios.rpgsystems.client.party.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyMemberInfoClientConfig;

@Environment(EnvType.CLIENT)
public final class PartyMemberInfoOptionsScreen extends Screen {
    private final Screen parent;

    private CheckboxWidget showHp;
    private CheckboxWidget showHunger;
    private CheckboxWidget showStamina;
    private CheckboxWidget showMana;
    private CheckboxWidget showRpgMana;
    private boolean rpgManaAvailable;
    private boolean staminaAvailable;
    private boolean manaAvailable;
    private String warning = "";

    public PartyMemberInfoOptionsScreen(Screen parent) {
        super(Text.literal("Party Member Info Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 60;
        int w = 280;
        int h = 20;

        this.staminaAvailable = FabricLoader.getInstance().isModLoaded("staminaattributes");
        this.manaAvailable = FabricLoader.getInstance().isModLoaded("manaattributes");
        this.rpgManaAvailable = FabricLoader.getInstance().isModLoaded("rpgmana");
        var cfg = PartyMemberInfoClientConfig.get();

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
        y += 36;

        this.addDrawableChild(showRpgMana);
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
        var cfg = PartyMemberInfoClientConfig.get();
        cfg.showHpBar = this.showHp.isChecked();
        cfg.showHungerBar = this.showHunger.isChecked();
        cfg.showStaminaBar = this.showStamina.isChecked() && staminaAvailable;
        cfg.showManaBar = this.showMana.isChecked() && manaAvailable;
        cfg.showRpgManaBar = this.showRpgMana.isChecked() && rpgManaAvailable;

        cfg.enforceMaxThreeBars();
        PartyMemberInfoClientConfig.save();
    }

    private int currentBarCount() {
        int c = 0;
        if (showHp.isChecked()) c++;
        if (showHunger.isChecked()) c++;
        if (showStamina.isChecked() && staminaAvailable) c++;
        if (showMana.isChecked() && manaAvailable) c++;
        if (showRpgMana.isChecked() && rpgManaAvailable) c++;
        return c;
    }

    private boolean wouldExceedIfToggled(CheckboxWidget cb) {
        boolean turningOn = !cb.isChecked();
        if (!turningOn) return false;
        int projected = currentBarCount() + 1;
        return projected > 3;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (showHp != null && showHp.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showHp)) return true;
            if (showHunger != null && showHunger.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showHunger)) return true;
            if (showStamina != null && showStamina.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showStamina)) return true;
            if (showMana != null && showMana.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showMana)) return true;
            if (showRpgMana != null && showRpgMana.isMouseOver(mouseX, mouseY) && wouldExceedIfToggled(showRpgMana)) return true;
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
