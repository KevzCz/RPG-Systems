package net.pixeldreamstudios.rpgsystems.client.party.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.party.CompatCommandHelper;

@Environment(EnvType.CLIENT)
public class PartyCreateSelectionScreen extends Screen {

    private final Screen parent;

    public PartyCreateSelectionScreen(Screen parent) {
        super(Text.translatable("screen.rpgsystems.party.create.select"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int buttonSpacing = 5;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("FTB Teams Party"),
                button -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendChatCommand("ftbteams party create");
                    }
                    this.close();
                }
        ).dimensions(centerX - buttonWidth / 2, centerY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("PartyAddon Group"),
                button -> {
                    CompatCommandHelper.openPartyScreen();
                    this.close();
                }
        ).dimensions(centerX - buttonWidth / 2, centerY + buttonSpacing, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> this.close()
        ).dimensions(centerX - buttonWidth / 2, centerY + buttonHeight + buttonSpacing * 3, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Choose which party system to use:"), 
                this.width / 2, this.height / 2 - 35, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
