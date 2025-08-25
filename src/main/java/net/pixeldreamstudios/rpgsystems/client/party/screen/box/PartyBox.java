package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
@Environment(EnvType.CLIENT)
public interface PartyBox {
    void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH);

    default boolean mouseClicked(double mouseX, double mouseY, int button,
                                 int canvasX, int canvasY, int canvasW, int canvasH,
                                 TextRenderer tr) { return false; }

    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                  int canvasX, int canvasY, int canvasW, int canvasH,
                                  TextRenderer tr) { return false; }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
                                 int canvasX, int canvasY, int canvasW, int canvasH,
                                 TextRenderer tr) { return false; }

    default boolean mouseReleased(double mouseX, double mouseY, int button,
                                  int canvasX, int canvasY, int canvasW, int canvasH,
                                  TextRenderer tr) { return false; }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char chr, int modifiers) { return false; }
}
