package net.pixeldreamstudios.rpgsystems.client.party.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyStatusEffects;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

import static net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud.*;

final class OriginalHud implements PartyHudRenderer {
    private static final boolean DEBUG_BOXES = false;
    private static final int PANEL_W = 128;
    private static final int PANEL_H = 32;
    private static final int ROW_GAP  = 1;
    private static final float BG_SCALE = 0.75f;

    private static final int PLATE_W = 20;
    private static final int PLATE_INSET = 5;
    private static final int NAME_CHAR_MAX = 20;
    private static final int NAME_PAD_X = 2;
    private static final int NAME_PAD_Y = 1;
    private static final float TEXT_SCALE = 0.25f;

    private static final float PLATE_SHIFT_X = -13.5f;
    private static final int PLATE_SHIFT_Y = -2;

    private static final float LVL_INSET_X = -0.3f;
    private static final int   LVL_INSET_Y = 3;
    private static final int   LVL_SLOT_W  = 14;
    private static final int   LVL_PAD_Y   = 1;

    private static final int MODEL_LEFT_MARGIN = 15;
    private static final int MODEL_SIZE = 4;

    private static final int BAR_GAP_FROM_MODEL = 3;
    private static final int BAR_RIGHT_PAD = 6;
    private static final int BAR_END_SHORTEN = 20;
    private static final int BAR_H_BASE = 3;

    private static final int BAR_HP_FILL    = 0xFF2ECC71;
    private static final int BAR_HUNGER_FILL= 0xFFE6C200;
    private static final int BAR_STAM_FILL  = 0xFFE6C200;
    private static final int BAR_MANA_FILL  = 0xFF3498DB;
    private static final int BAR_RPGMANA_FILL = 0xFF3498DB;

    @Override
    public void render(DrawContext ctx, RenderTickCounter tickCounter, PartyHudClientConfig cfg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden || mc.player == null) return;

        ensureBgNearest();
        ensureHudBarTexturesNearest();

        int xAnchor = cfg.partyHudX;
        int yAnchorStart = cfg.partyHudY;

        float hudScale = cfg.hudScale;
        if (hudScale <= 0.1f) hudScale = 0.1f;

        int unscaledRowStep = Math.round(PANEL_H * BG_SCALE) + ROW_GAP;
        int rowStep = Math.round(unscaledRowStep * hudScale);


        List<ClientPartyHudData.Member> list = partyMembers();
        if (list.isEmpty()) return;

        int yScreen = yAnchorStart;
        for (var m : list) {
            var mtx = ctx.getMatrices();
            mtx.push();
            mtx.translate(xAnchor, yScreen, 0);
            mtx.scale(hudScale, hudScale, 1f);
            drawRow(ctx, 0, 0, m.uuid, m.name, m.totalLevel, cfg);
            mtx.pop();
            yScreen += rowStep;
        }
    }

    private void drawRow(DrawContext ctx, int x, int y, UUID uuid, String name, int level, PartyHudClientConfig cfg) {
        Identifier bg = isLeader(uuid) ? PARTY_BG_LEADER : PARTY_BG;
        drawTextureScaled(ctx, bg, x, y, PANEL_W, PANEL_H, 128, 32, BG_SCALE);
        final int scaledW = Math.round(PANEL_W * BG_SCALE);
        final int scaledH = Math.round(PANEL_H * BG_SCALE);
        var tr = MinecraftClient.getInstance().textRenderer;

        final int modelCenterX = x + MODEL_LEFT_MARGIN;
        final int modelBottomY = y + (scaledH / 2) + (MODEL_SIZE / 2) + 2;
        drawMemberModel(ctx, modelCenterX, modelBottomY, MODEL_SIZE, uuid, name);

        final float lvlSlotXf = x + LVL_INSET_X;
        final float lvlSlotYf = y + LVL_INSET_Y;
        final int   lvlSlotW  = LVL_SLOT_W;
        final int   lvlSlotH  = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE)) + LVL_PAD_Y * 2;

        String lvlText = (level >= 0) ? ("" + level) : "?";
        int lvlTextW = Math.round(tr.getWidth(lvlText) * TEXT_SCALE);
        int lvlTextH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
        float lvlTextX = lvlSlotXf + (lvlSlotW - lvlTextW) / 2f;
        float lvlTextY = lvlSlotYf + (lvlSlotH - lvlTextH) / 2f;
        drawScaledTextF(ctx, tr, lvlText, lvlTextX, lvlTextY, 0xFFFFFFFF, TEXT_SCALE);

        final float slotXF = x + Math.round(PANEL_W * BG_SCALE) - PLATE_INSET - PLATE_W + PLATE_SHIFT_X;
        final float slotYF = y + PLATE_INSET + PLATE_SHIFT_Y;

        String display = (name == null ? "Unknown" : name);
        if (display.length() > NAME_CHAR_MAX) display = display.substring(0, NAME_CHAR_MAX);
        int innerMaxPx = Math.max(0, PLATE_W - NAME_PAD_X * 2);
        display = ellipsizeScaled(tr, display, innerMaxPx, TEXT_SCALE);

        int textW = Math.round(tr.getWidth(display) * TEXT_SCALE);
        int textH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
        float textXF = slotXF + (PLATE_W - textW) / 2f;
        float textYF = slotYF + ((Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE)) + NAME_PAD_Y * 2) - textH) / 2f;
        drawScaledTextF(ctx, tr, display, textXF, textYF, 0xFFFFFFFF, TEXT_SCALE);

        final int modelRight = modelCenterX + MODEL_SIZE / 2;
        final int barX = modelRight + BAR_GAP_FROM_MODEL;
        final int barRight = x + scaledW - BAR_RIGHT_PAD - BAR_END_SHORTEN;
        final int barW = Math.max(0, barRight - barX);
        final int barH = Math.max(1, Math.round(BAR_H_BASE * BG_SCALE));

        List<RenderBar> bars = collectBars(uuid, cfg,
                BAR_HP_FILL, BAR_HUNGER_FILL, BAR_STAM_FILL, BAR_MANA_FILL, BAR_RPGMANA_FILL);
        if (bars.size() > 3) bars = bars.subList(0, 3);

        if (barW > 0 && !bars.isEmpty()) {
            int totalH = bars.size() * (barH + 1) - 1;
            int topY = y + (scaledH - totalH) / 2 + 1;

            for (int i = 0; i < bars.size(); i++) {
                RenderBar b = bars.get(i);
                int by = topY + i * (barH + 1);
                drawHudBarSmart(ctx, barX, by, barW, barH, b);
                String txt = b.text;
                int tW = Math.round(tr.getWidth(txt) * TEXT_SCALE);
                int tH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
                int tX = barX + barW - 1 - tW;
                int tY = by + (barH - tH) / 2;
                drawScaledText(ctx, tr, txt, tX, tY, 0xFFFFFFFF, TEXT_SCALE);
                if (DEBUG_BOXES) drawBorder(ctx, barX, by, barW, barH, 0x80FF00FF);
            }
        }

        if (cfg.showArrows) {
            Identifier arrow = pickArrow(uuid);
            if (arrow != null) {
                int aw = Math.round(ARROW_W * BG_SCALE);
                int ah = Math.round(ARROW_H * BG_SCALE);
                int ax = x + scaledW - 3 - aw - 15;
                int ay = y + scaledH - 3 - ah;
                drawTextureScaled(ctx, arrow, ax - 1, ay + 3, ARROW_W, ARROW_H, ARROW_W, ARROW_H, BG_SCALE);
            }
        }

        List<Identifier> theirEffects = ClientPartyStatusEffects.get(uuid);
        if (theirEffects != null && !theirEffects.isEmpty()) {
            float anchorX = x + scaledW - 18f - 3f;
            float boxH = scaledH;
            drawMemberEffectIcons(ctx, anchorX, y, boxH, theirEffects);
        }
    }

    private void drawMemberModel(DrawContext ctx, int centerX, int bottomY, int size, UUID uuid, String fallbackName) {
        var pe = getRenderablePlayer(uuid, fallbackName);
        if (pe == null) return;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0.0F, 0.0F, 400.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        Vector3f translation = new Vector3f(0f, 0f, 0f);
        Quaternionf rotation  = new Quaternionf().rotationZ((float) Math.PI);
        Quaternionf bodyRot   = new Quaternionf();
        InventoryScreen.drawEntity(ctx, centerX, bottomY, size, translation, rotation, bodyRot, pe);

        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        ctx.getMatrices().pop();
    }

    private void drawMemberEffectIcons(DrawContext ctx, float anchorX, float topY, float boxHeight, List<Identifier> effectIds) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        int total = effectIds.size();
        if (total <= 0) return;

        int z = 400;
        int index = 0;
        int col = 0;
        float EFFECT_ICON_SIZE = 3f;
        float EFFECT_ICON_GAP  = 2f;
        int   EFFECTS_PER_COLUMN = 2;
        int   EFFECT_BASE_PX = 18;

        while (index < total) {
            int remaining = total - index;
            int thisColCount = Math.min(EFFECTS_PER_COLUMN, remaining);

            float colHeight = thisColCount * EFFECT_ICON_SIZE + (thisColCount - 1) * EFFECT_ICON_GAP;
            float startY = topY + (boxHeight - colHeight) / 2f;
            float startX = anchorX + col * (EFFECT_ICON_SIZE + EFFECT_ICON_GAP);

            for (int r = 0; r < thisColCount; r++) {
                Identifier id = effectIds.get(index++);
                if (id == null) continue;

                StatusEffect effect = Registries.STATUS_EFFECT.get(id);
                if (effect == null) continue;

                RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
                if (entry == null) continue;

                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(entry);
                if (sprite == null) continue;

                float x = startX;
                float y = startY + r * (EFFECT_ICON_SIZE + EFFECT_ICON_GAP);

                var m = ctx.getMatrices();
                m.push();
                m.translate(x, y, 0f);
                float s = EFFECT_ICON_SIZE / EFFECT_BASE_PX;
                m.scale(s, s, 1f);
                ctx.drawSprite(0, 0, z, EFFECT_BASE_PX, EFFECT_BASE_PX, sprite);
                m.pop();
            }
            col++;
        }
    }
}
