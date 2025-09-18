package net.pixeldreamstudios.rpgsystems.client.enemy.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.DamageNumbersClientConfig;

@Environment(EnvType.CLIENT)
public final class DamageNumbersScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget enabled;
    private ButtonWidget modeButton;
    private CheckboxWidget showPet;
    private CheckboxWidget onlyParty;
    private DistanceSlider distance;
    private ButtonWidget damageTypesButton;

    private DamageNumbersClientConfig.ShowMode mode;

    public DamageNumbersScreen(Screen parent) {
        super(Text.literal("Damage Numbers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var cfg = DamageNumbersClientConfig.get();
        int cx = this.width / 2;
        int y = 56;
        int w = 280;

        mode = cfg.showMode;

        enabled = CheckboxWidget.builder(Text.literal("Enable damage numbers"), this.textRenderer)
                .pos(cx - w/2, y).checked(cfg.enabled).build();
        this.addDrawableChild(enabled);
        y += 24;

        modeButton = ButtonWidget.builder(Text.literal(modeLabel(mode)), b -> {
            mode = nextMode(mode);
            b.setMessage(Text.literal(modeLabel(mode)));
        }).dimensions(cx - w/2, y, w, 20).build();
        this.addDrawableChild(modeButton);
        y += 24;

        showPet = CheckboxWidget.builder(Text.literal("Show pet damage numbers"), this.textRenderer)
                .pos(cx - w/2, y).checked(cfg.showPetDamage).build();
        this.addDrawableChild(showPet);
        y += 24;

        onlyParty = CheckboxWidget.builder(Text.literal("Only show damage from my party"), this.textRenderer)
                .pos(cx - w/2, y).checked(cfg.onlyShowPartyDamage).build();
        this.addDrawableChild(onlyParty);
        y += 24;

        distance = new DistanceSlider(cx - w/2, y, w, 20, cfg.viewDistance);
        this.addDrawableChild(distance);
        y += 36;

        damageTypesButton = ButtonWidget.builder(Text.literal("Damage Types"),
                        b -> this.client.setScreen(new DamageTypeConfigScreen(this)))
                .dimensions(cx - w/2, y, w, 20).build();
        this.addDrawableChild(damageTypesButton);
        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            cfg.enabled = enabled.isChecked();
            cfg.showMode = mode;
            cfg.showPetDamage = showPet.isChecked();
            cfg.onlyShowPartyDamage = onlyParty.isChecked();
            cfg.viewDistance = distance.getBlocks();
            DamageNumbersClientConfig.save();
            this.client.setScreen(parent);
        }).dimensions(cx - 60, y, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Damage Numbers", this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Display and filtering", this.width / 2, 36, 0xAAAAAA);
    }

    private static String modeLabel(DamageNumbersClientConfig.ShowMode m) {
        return switch (m) {
            case ALL -> "Show: All sources";
            case PLAYERS_ONLY -> "Show: Players only";
            case NONE -> "Show: None";
        };
    }
    private static DamageNumbersClientConfig.ShowMode nextMode(DamageNumbersClientConfig.ShowMode m) {
        return switch (m) {
            case ALL -> DamageNumbersClientConfig.ShowMode.PLAYERS_ONLY;
            case PLAYERS_ONLY -> DamageNumbersClientConfig.ShowMode.NONE;
            case NONE -> DamageNumbersClientConfig.ShowMode.ALL;
        };
    }

    private static final class DistanceSlider extends SliderWidget {
        DistanceSlider(int x, int y, int w, int h, double blocks) {
            super(x, y, w, h,
                    Text.literal("View Distance: " + (int) blocks + " blocks"),
                    clamp01(blocks / 256.0));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("View Distance: " + getBlocksInt() + " blocks"));
        }

        @Override
        protected void applyValue() {
        }

        double getBlocks() {
            return this.value * 256.0;
        }

        int getBlocksInt() {
            return (int) Math.round(getBlocks());
        }

        private static double clamp01(double v) {
            return v < 0 ? 0 : (v > 1 ? 1 : v);
        }
    }
}
