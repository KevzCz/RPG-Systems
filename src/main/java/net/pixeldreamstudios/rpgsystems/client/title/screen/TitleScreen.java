package net.pixeldreamstudios.rpgsystems.client.title.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitleBox;
import net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitleDescriptionBox;
import net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitlesListBox;
import net.pixeldreamstudios.rpgsystems.client.title.widget.TitleButtonWidget;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;
@Environment(EnvType.CLIENT)
public final class TitleScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.of("rpg-systems", "textures/gui/title/title_screen.png");
    private static final Identifier BACK_NORMAL = Identifier.of("rpg-systems", "textures/gui/title/back_normal.png");
    private static final Identifier BACK_HOVER  = Identifier.of("rpg-systems", "textures/gui/title/back_hover.png");
    private int x;
    private int y;
    private final int backgroundWidth = 176;
    private final int backgroundHeight = 166;
    private TitleButtonWidget backButton;
    private TitlesListBox titlesListBox;
    private TitleDescriptionBox descriptionBox;
    TitleBox titleBox;
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

        this.titlesListBox = new TitlesListBox(x, y, backgroundWidth, backgroundHeight);
        this.descriptionBox = new TitleDescriptionBox(x, y, backgroundWidth, backgroundHeight);
        this.titleBox = new net.pixeldreamstudios.rpgsystems.client.title.screen.box.TitleBox(x, y, backgroundWidth, backgroundHeight);
        this.titleBox.attachToScreen(this);
        this.titlesListBox.setSelectionListener(title -> this.descriptionBox.setTitle(title));
        this.titlesListBox.setOnSelectionChanged(title -> this.titleBox.setSelectedTitle(title));
        if (!TitleRegistry.all().isEmpty()) {
            var first = TitleRegistry.all().values().iterator().next();
            this.descriptionBox.setTitle(first);
            this.titleBox.setSelectedTitle(first);
        }

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int titleX = x + backgroundWidth / 2;
        int titleY = y + 7;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        this.titlesListBox.setScreenOrigin(x, y);
        this.titlesListBox.setBackgroundSize(backgroundWidth, backgroundHeight);
        this.titlesListBox.render(context, mouseX, mouseY, delta);

        this.descriptionBox.setScreenOrigin(x, y);
        this.descriptionBox.setBackgroundSize(backgroundWidth, backgroundHeight);
        this.descriptionBox.render(context);

        this.titleBox.setScreenOrigin(x, y);
        this.titleBox.setBackgroundSize(backgroundWidth, backgroundHeight);
        this.titleBox.render(context);
        
        this.descriptionBox.renderHints(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.titlesListBox != null && this.titlesListBox.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if (this.descriptionBox != null && this.descriptionBox.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount,verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.titlesListBox != null && this.titlesListBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(BACKGROUND, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
