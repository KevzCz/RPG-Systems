package net.pixeldreamstudios.rpgsystems.client.title.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.title.config.TitlesClientConfig;

@Environment(EnvType.CLIENT)
public final class TitlesOptionsScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget showOwn;
    private CheckboxWidget showOthers;
    private ButtonWidget saveBtn;
    private ButtonWidget doneBtn;

    public TitlesOptionsScreen(Screen parent) {
        super(Text.literal("RPG Systems: Titles Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 60;
        int w = 280;

        TitlesClientConfig cfg = TitlesClientConfig.get();

        this.showOwn = CheckboxWidget.builder(Text.literal("Show My Title"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showOwnTitle).build();
        y += 24;

        this.showOthers = CheckboxWidget.builder(Text.literal("Show Others' Titles"), this.textRenderer)
                .pos(centerX - w / 2, y).checked(cfg.showOthersTitles).build();
        y += 36;

        this.saveBtn = ButtonWidget.builder(Text.literal("Save"), b -> {
            TitlesClientConfig c = TitlesClientConfig.get();
            c.showOwnTitle = this.showOwn.isChecked();
            c.showOthersTitles = this.showOthers.isChecked();
            c.save();
        }).dimensions(centerX - 142, y, 130, 20).build();

        this.doneBtn = ButtonWidget.builder(Text.literal("Done"), b -> {
            TitlesClientConfig c = TitlesClientConfig.get();
            c.showOwnTitle = this.showOwn.isChecked();
            c.showOthersTitles = this.showOthers.isChecked();
            c.save();
            this.client.setScreen(this.parent);
        }).dimensions(centerX + 12, y, 130, 20).build();

        addDrawableChild(this.showOwn);
        addDrawableChild(this.showOthers);
        addDrawableChild(this.saveBtn);
        addDrawableChild(this.doneBtn);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Enable or disable title rendering"), this.width / 2, 36, 0xAAAAAA);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
