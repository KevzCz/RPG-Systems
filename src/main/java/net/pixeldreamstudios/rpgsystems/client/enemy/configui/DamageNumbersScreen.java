// net/pixeldreamstudios/rpgsystems/client/enemy/configui/DamageNumbersScreen.java
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
    private CheckboxWidget showPet;
    private DistanceSlider  distance;

    public DamageNumbersScreen(Screen parent) {
        super(Text.literal("Damage Numbers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var cfg = DamageNumbersClientConfig.get();
        int cx = this.width / 2;
        int y = 60;
        int w = 280;
        int h = 20;

        enabled = CheckboxWidget.builder(Text.literal("Enable damage numbers"), this.textRenderer)
                .pos(cx - w/2, y).checked(cfg.enabled).build();
        this.addDrawableChild(enabled);
        y += 24;

        showPet = CheckboxWidget.builder(Text.literal("Show pet damage numbers"), this.textRenderer)
                .pos(cx - w/2, y).checked(cfg.showPetDamage).build();
        this.addDrawableChild(showPet);
        y += 24;

        distance = new DistanceSlider(cx - w/2, y, w, 20, cfg.viewDistance);
        this.addDrawableChild(distance);
        y += 36;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            cfg.enabled = enabled.isChecked();
            cfg.showPetDamage = showPet.isChecked();
            cfg.viewDistance = distance.getBlocks();
            DamageNumbersClientConfig.save();
            this.client.setScreen(parent);
        }).size(100, h).position(cx - 110, y).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> this.client.setScreen(parent))
                .size(100, h).position(cx + 10, y).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width/2, 20, 0xFFFFFF);
    }

    private static final class DistanceSlider extends SliderWidget {
        DistanceSlider(int x, int y, int w, int h, double currentBlocks) {
            super(x, y, w, h, Text.empty(), norm(currentBlocks));
            updateMessage();
        }

        double getBlocks() { return denorm(this.value); }

        @Override protected void updateMessage() {
            setMessage(Text.literal("View distance: " + (int)Math.round(denorm(this.value)) + " blocks"));
        }
        @Override protected void applyValue() { updateMessage(); }

        private static double norm(double d) { d = Math.max(2, Math.min(256, d)); return (d - 2.0) / (256.0 - 2.0); }
        private static double denorm(double v) { v = Math.max(0, Math.min(1, v)); return 2.0 + v * (256.0 - 2.0); }
    }
}
