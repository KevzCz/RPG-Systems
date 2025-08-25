package net.pixeldreamstudios.rpgsystems.client.party.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.screen.box.*;
@Environment(EnvType.CLIENT)
public final class PartyScreen extends Screen {

    private static final int TEXTURE_W = 450;
    private static final int TEXTURE_H = 300;

    private static final int DEST_W = 450;
    private static final int DEST_H = 300;

    private static final Identifier BG = Identifier.of("rpg-systems", "textures/gui/party_screen.png");

    private int canvasX;
    private int canvasY;

    private PartyBox[] boxes;

    public PartyScreen() {
        super(Text.literal("Party"));
    }

    @Override
    protected void init() {
        ensureNearestFilter();
        computeCanvas();
        boxes = new PartyBox[] {
                new PartyInfoBox(),
                new PartyMembersBox(),
                new PartyMemberInfoBox(),
                new PartyChatBox(),
                new PartyInviteBox(),
                new PartyJoinRequestBox()
        };
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        computeCanvas();
    }

    private void computeCanvas() {
        canvasX = (this.width - DEST_W) / 2;
        canvasY = (this.height - DEST_H) / 2;
    }

    private void ensureNearestFilter() {
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        AbstractTexture tex = tm.getTexture(BG);
        if (tex != null) tex.setFilter(false, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        computeCanvas();

        context.drawTexture(BG, canvasX, canvasY, 0, 0, DEST_W, DEST_H, TEXTURE_W, TEXTURE_H);

        TextRenderer tr = this.textRenderer;
        if (boxes != null) {
            for (PartyBox box : boxes) {
                box.render(context, tr, canvasX, canvasY, DEST_W, DEST_H);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (boxes != null) {
            for (PartyBox box : boxes) {
                if (box.mouseClicked(mouseX, mouseY, button, canvasX, canvasY, DEST_W, DEST_H, this.textRenderer)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (boxes != null) {
            for (PartyBox box : boxes) {
                if (box.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount,
                        canvasX, canvasY, DEST_W, DEST_H, this.textRenderer)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (boxes != null) {
            for (PartyBox box : boxes) {
                if (box.mouseDragged(mouseX, mouseY, button, deltaX, deltaY,
                        canvasX, canvasY, DEST_W, DEST_H, this.textRenderer)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (boxes != null) {
            for (PartyBox box : boxes) {
                if (box.mouseReleased(mouseX, mouseY, button,
                        canvasX, canvasY, DEST_W, DEST_H, this.textRenderer)) {
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (boxes != null) {
            for (PartyBox box : boxes) {
                if (box.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (boxes != null) {
            for (PartyBox box : boxes) {
                if (box.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
