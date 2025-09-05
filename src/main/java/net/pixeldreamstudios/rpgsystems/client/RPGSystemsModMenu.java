package net.pixeldreamstudios.rpgsystems.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;

@Environment(EnvType.CLIENT)
public final class RPGSystemsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ReadOnlyConfigScreen(parent);
    }
    private static final class ReadOnlyConfigScreen extends Screen {
        private final Screen parent;
        private CheckboxWidget partyEnabled;
        private CheckboxWidget petEnabled;
        private CheckboxWidget titleEnabled;
        private CheckboxWidget partyLogToConsole;

        protected ReadOnlyConfigScreen(Screen parent) {
            super(Text.literal("RPG Systems Config (read-only)"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int y = 60;
            int w = 260;
            int h = 20;

            RPGSystemsConfig cfg = RPGSystemsConfig.get();

            this.partyEnabled = CheckboxWidget.builder(Text.literal("Enable Party System"), this.textRenderer)
                    .pos(centerX - w/2, y).checked(cfg.systems.party).build();
            this.partyEnabled.active = false;
            y += 24;

            this.petEnabled = CheckboxWidget.builder(Text.literal("Enable Pet System"), this.textRenderer)
                    .pos(centerX - w/2, y).checked(cfg.systems.pet).build();
            this.petEnabled.active = false;
            y += 24;
            this.titleEnabled = CheckboxWidget.builder(Text.literal("Enable Title System"), this.textRenderer)
                    .pos(centerX - w/2, y).checked(cfg.systems.title).build();
            this.titleEnabled.active = false;
            y += 24;

            this.partyLogToConsole = CheckboxWidget.builder(Text.literal("Log Party Chat To Server Console"), this.textRenderer)
                    .pos(centerX - w/2, y).checked(cfg.party.logChatToConsole).build();
            this.partyLogToConsole.active = false;
            y += 28;

            this.addDrawableChild(this.partyEnabled);
            this.addDrawableChild(this.petEnabled);
            this.addDrawableChild(this.titleEnabled);
            this.addDrawableChild(this.partyLogToConsole);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Client Options…"),
                            b -> this.client.setScreen(new ClientConfigsScreen(this)))
                    .size(180, h).position(centerX - 90, y).build());
            y += 24;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                    .size(100, h).position(centerX - 50, y).build());
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            super.render(ctx, mouseX, mouseY, delta);
            ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Edit file: config/rpgsystems/rpgsystems.json"), this.width / 2, 36, 0xAAAAAA);
        }

        @Override
        public void close() {
            this.client.setScreen(this.parent);
        }
    }
}
