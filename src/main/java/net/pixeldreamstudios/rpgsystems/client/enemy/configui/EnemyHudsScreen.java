package net.pixeldreamstudios.rpgsystems.client.enemy.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.EnemyHudClientConfig;

@Environment(EnvType.CLIENT)
public final class EnemyHudsScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget showOnNearbyChanges;

    public EnemyHudsScreen(Screen parent) {
        super(Text.literal("Enemy HUDs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var cfg = EnemyHudClientConfig.get();
        int cx = this.width / 2;
        int y = 56;
        int w = 320;

        showOnNearbyChanges = CheckboxWidget.builder(Text.literal("Show enemy health bar when nearby HP changes"), this.textRenderer)
                .pos(cx - w / 2, y).checked(cfg.showHealthbarOnNearbyHpChanges).build();
        this.addDrawableChild(showOnNearbyChanges);
        y += 32;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            cfg.showHealthbarOnNearbyHpChanges = showOnNearbyChanges.isChecked();
            EnemyHudClientConfig.save();
            this.client.setScreen(parent);
        }).dimensions(cx - 60, y, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Enemy HUDs", this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Display options", this.width / 2, 36, 0xAAAAAA);
    }
}
