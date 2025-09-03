package net.pixeldreamstudios.rpgsystems.client.title.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
@Environment(EnvType.CLIENT)
public final class TitleButtonWidget extends PressableWidget {
    private final Identifier normalTexture;
    private final Identifier hoverTexture;
    private final Runnable onPressAction;

    public TitleButtonWidget(int x, int y, int width, int height, Identifier normalTexture, Identifier hoverTexture, Runnable onPressAction) {
        super(x, y, width, height, Text.empty());
        this.normalTexture = normalTexture;
        this.hoverTexture = hoverTexture;
        this.onPressAction = onPressAction;
    }

    public void setShown(boolean shown) {
        this.visible = shown;
    }

    public void setEnabled(boolean enabled) {
        this.active = enabled;
    }

    @Override
    public void onPress() {
        if (!this.active) return;
        if (onPressAction != null) onPressAction.run();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        Identifier tex = this.isHovered() ? hoverTexture : normalTexture;
        context.drawTexture(tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
