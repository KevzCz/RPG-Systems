package net.pixeldreamstudios.rpgsystems.client.title.screen.box;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.pixeldreamstudios.rpgsystems.client.title.TitleClientData;
import net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil;
import net.pixeldreamstudios.rpgsystems.client.title.TitleTextureResolver;
import net.pixeldreamstudios.rpgsystems.client.title.widget.TitleButtonWidget;
import net.pixeldreamstudios.rpgsystems.mixin.client.ScreenAccessor;
import net.pixeldreamstudios.rpgsystems.network.title.TitlePayloads;
import net.pixeldreamstudios.rpgsystems.title.Title;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class TitleBox {
    private static final int LEFT_PADDING = 7;
    private static final int RIGHT_PADDING = 6;
    private static final int TOP_PADDING = 20;
    private static final int BOX_HEIGHT = 61;

    private static final int BUTTON_PADDING = 5;
    private static final int BUTTON_SIZE = 16;

    private static final Identifier APPLY_NORMAL = Identifier.of("rpg-systems", "textures/gui/title/apply_title_normal.png");
    private static final Identifier APPLY_HOVER  = Identifier.of("rpg-systems", "textures/gui/title/apply_title_hover.png");
    private static final Identifier DISABLE_NORMAL = Identifier.of("rpg-systems", "textures/gui/title/disable_title_normal.png");
    private static final Identifier DISABLE_HOVER  = Identifier.of("rpg-systems", "textures/gui/title/disable_title_hover.png");

    private static final Identifier LOCKED_ICON = Identifier.of("rpg-systems", "textures/gui/title/locked.png");
    private static final int LOCK_SIZE = 8;

    private int screenX;
    private int screenY;
    private int bgWidth;
    private int bgHeight;

    private final TextRenderer font;
    private Title selected;

    private TitleButtonWidget applyButton;
    private TitleButtonWidget disableButton;

    public TitleBox(int screenX, int screenY, int bgWidth, int bgHeight) {
        this.font = MinecraftClient.getInstance().textRenderer;
        this.screenX = screenX;
        this.screenY = screenY;
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
        createButtons();
        updateButtonLayout();
        updateButtonVisibility();
    }

    public void setScreenOrigin(int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;
        updateButtonLayout();
    }

    public void setBackgroundSize(int bgWidth, int bgHeight) {
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
        updateButtonLayout();
    }

    public void setSelectedTitle(Title title) {
        this.selected = title;
        updateButtonVisibility();
    }

    public Title getSelectedTitle() {
        return selected;
    }

    public void attachToScreen(net.minecraft.client.gui.screen.Screen screen) {
        ScreenAccessor acc = (ScreenAccessor) (Object) screen;
        if (applyButton != null) acc.rpgsystems$addDrawableChild(applyButton);
        if (disableButton != null) acc.rpgsystems$addDrawableChild(disableButton);
        updateButtonLayout();
        updateButtonVisibility();
    }

    public void render(DrawContext ctx) {
        int[] r = boxRect();
        int x = r[0], y = r[1], w = r[2], h = r[3];

        if (selected != null) {
            TitleTextureResolver.FrameInfo fi = TitleTextureResolver.currentFrame(selected.id);
            if (fi != null) {
                int maxW = Math.max(8, w - 16);
                int maxH = Math.max(8, h - 8);

                float scale = Math.min(maxW / (float) fi.frameWidth, maxH / (float) fi.frameHeight);
                scale = Math.max(0.01f, scale);

                int drawW = Math.round(fi.frameWidth * scale);
                int drawH = Math.round(fi.frameHeight * scale);

                int drawX = Math.round(x + (w / 2f) - (drawW / 2f));
                int drawY = Math.round(y + (h / 2f) - (drawH / 2f));

                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                RenderSystem.setShaderTexture(0, fi.textureId);

                MatrixStack matrices = ctx.getMatrices();
                matrices.push();
                matrices.translate(drawX + drawW / 2f, drawY + drawH / 2f, 0);
                matrices.scale(drawW, drawH, 1.0f);
                drawQuadGuiUV(matrices.peek().getPositionMatrix(), fi.u0, fi.v0, fi.u1, fi.v1);
                matrices.pop();
            } else {
                String raw = selected.displayName != null
                        ? selected.displayName.getString()
                        : selected.id.getPath();
                TitleStyleUtil.Parsed parsed = TitleStyleUtil.parse(raw);

                int textWidth = font.getWidth(parsed.text());
                int baseX = Math.round(x + (w / 2f) - (textWidth / 2f));
                int baseY = Math.round(y + (h / 2f) - (font.fontHeight / 2f));

                long nowMs = Util.getMeasuringTimeMs();
                float advanceX = 0f;
                int index = 0;

                for (int i = 0; i < parsed.text().length(); ) {
                    int cp = parsed.text().codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    int cw = font.getWidth(ch);

                    int rgb =
                            (parsed.gradient() != null)
                                    ? TitleStyleUtil.gradientRgb(index, parsed.text().length(), parsed.gradient())
                                    : (parsed.rainbow()
                                    ? TitleStyleUtil.rainbowRgb(nowMs, index, parsed.rainbowSpeed() != null ? parsed.rainbowSpeed() : 0.18f)
                                    : TitleStyleUtil.resolveOrWhite(parsed.baseRgb()));

                    if (parsed.pulseSpeed() != null)
                        rgb = TitleStyleUtil.pulseRgb(nowMs, rgb, parsed.pulseSpeed());

                    float yOff = parsed.wiggle()
                            ? TitleStyleUtil.wiggleYOffsetPx(nowMs, index, parsed.wiggleAmp() != null ? parsed.wiggleAmp() : 2.0f)
                            : 0f;

                    float xOff = (parsed.shakeAmp() != null)
                            ? TitleStyleUtil.shakeXOffsetPx(nowMs, index, parsed.shakeAmp())
                            : 0f;

                    if (parsed.outlineRgb() != null) {
                        int px = Math.max(1, parsed.outlinePx() != null ? parsed.outlinePx() : 1);
                        int oc = 0xFF000000 | (parsed.outlineRgb() & 0xFFFFFF);
                        for (int ox = -px; ox <= px; ox++) for (int oy = -px; oy <= px; oy++) {
                            if (ox == 0 && oy == 0) continue;
                            ctx.drawText(font, ch,
                                    Math.round(baseX + advanceX + xOff) + ox,
                                    Math.round(baseY + yOff) + oy, oc, false);
                        }
                    }

                    ctx.drawTextWithShadow(font, ch,
                            Math.round(baseX + advanceX + xOff),
                            Math.round(baseY + yOff),
                            0xFF000000 | (rgb & 0xFFFFFF));


                    advanceX += cw;
                    index++;
                    i += Character.charCount(cp);
                }
            }

            boolean unlocked = TitleClientData.getSelfUnlocked().contains(selected.id);
            if (!unlocked) {
                int lockX = x + w - BUTTON_PADDING - LOCK_SIZE;
                int lockY = y + BUTTON_PADDING;
                ctx.drawTexture(LOCKED_ICON, lockX, lockY, 0, 0, LOCK_SIZE, LOCK_SIZE, LOCK_SIZE, LOCK_SIZE);
            }
        }

        updateButtonLayout();
        updateButtonVisibility();
    }

    private void createButtons() {
        this.applyButton = new TitleButtonWidget(0, 0, BUTTON_SIZE, BUTTON_SIZE, APPLY_NORMAL, APPLY_HOVER, () -> {
            if (selected == null) return;
            ClientPlayNetworking.send(new TitlePayloads.RequestSetActive(Optional.of(selected.id)));
        });

        this.disableButton = new TitleButtonWidget(0, 0, BUTTON_SIZE, BUTTON_SIZE, DISABLE_NORMAL, DISABLE_HOVER, () -> {
            ClientPlayNetworking.send(new TitlePayloads.RequestSetActive(Optional.empty()));
        });
    }

    private void updateButtonLayout() {
        int[] r = boxRect();
        int x = r[0], y = r[1], w = r[2], h = r[3];

        int leftBtnX = x + BUTTON_PADDING;
        int leftBtnY = y + h - BUTTON_PADDING - BUTTON_SIZE;
        int rightBtnX = x + w - BUTTON_PADDING - BUTTON_SIZE;
        int rightBtnY = leftBtnY;

        if (applyButton != null) {
            applyButton.setX(leftBtnX);
            applyButton.setY(leftBtnY);
        }
        if (disableButton != null) {
            disableButton.setX(rightBtnX);
            disableButton.setY(rightBtnY);
        }
    }

    private void updateButtonVisibility() {
        Identifier active = TitleClientData.getSelfActive();
        boolean hasSelection = selected != null;
        boolean unlocked = hasSelection && TitleClientData.getSelfUnlocked().contains(selected.id);
        boolean isApplied = hasSelection && active != null && active.equals(selected.id);

        if (applyButton != null) {
            applyButton.setEnabled(hasSelection && unlocked && !isApplied);
            applyButton.setShown(hasSelection && unlocked && !isApplied);
        }
        if (disableButton != null) {
            disableButton.setEnabled(isApplied);
            disableButton.setShown(isApplied);
        }
    }

    private static void drawQuadGuiUV(org.joml.Matrix4f mat, float u0, float v0, float u1, float v1) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buf.vertex(mat, -0.5f,  0.5f, 0f).texture(u0, v1);
        buf.vertex(mat,  0.5f,  0.5f, 0f).texture(u1, v1);
        buf.vertex(mat,  0.5f, -0.5f, 0f).texture(u1, v0);
        buf.vertex(mat, -0.5f, -0.5f, 0f).texture(u0, v0);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private int[] boxRect() {
        int boxX = screenX + LEFT_PADDING;
        int boxY = screenY + TOP_PADDING;
        int boxW = bgWidth - LEFT_PADDING - RIGHT_PADDING;
        int boxH = BOX_HEIGHT;
        return new int[] { boxX, boxY, boxW, boxH };
    }
}
