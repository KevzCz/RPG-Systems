package net.pixeldreamstudios.rpgsystems.client.party.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyStatusEffects;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;
import net.pixeldreamstudios.rpgsystems.compat.RpgManaCompat;
import net.pixeldreamstudios.rpgsystems.compat.TrbAttributesCompat;
import net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Environment(EnvType.CLIENT)
public final class PartyHud implements HudRenderCallback {
    private static final boolean DEBUG_BOXES = false;
    private static final boolean DEBUG_EFFECTS = false;

    private static final boolean DEBUG_FORCE_DUMMY = false;
    private static final int DEBUG_DUMMY_COUNT = 10;

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
    private static final int   LVL_COLOR   = 0xFFFFFFFF;

    private static final int MODEL_LEFT_MARGIN = 15;
    private static final int MODEL_SIZE = 4;

    private static final int BAR_GAP_FROM_MODEL = 3;
    private static final int BAR_RIGHT_PAD = 6;
    private static final int BAR_END_SHORTEN = 20;
    private static final int BAR_H_BASE = 3;
    private static final int BAR_BG         = 0xFF3E3E3E;
    private static final int BAR_BORDER     = 0x00000000;
    private static final int BAR_HP_FILL    = 0xFF2ECC71;
    private static final int BAR_HUNGER_FILL= 0xFFE6C200;
    private static final int BAR_STAM_FILL  = 0xFFE6C200;
    private static final int BAR_MANA_FILL  = 0xFF3498DB;
    private static final int BAR_TEXT_COLOR = 0xFFFFFFFF;
    private static final int BAR_RPGMANA_FILL = 0xFF3498DB;
    private static final Identifier ARROW_UP    = Identifier.of("rpg-systems","textures/gui/party_hud/up.png");
    private static final Identifier ARROW_DOWN  = Identifier.of("rpg-systems","textures/gui/party_hud/down.png");
    private static final Identifier ARROW_LEFT  = Identifier.of("rpg-systems","textures/gui/party_hud/left.png");
    private static final Identifier ARROW_RIGHT = Identifier.of("rpg-systems","textures/gui/party_hud/right.png");
    private static final int ARROW_W = 11;
    private static final int ARROW_H = 11;
    private static final int ARROW_INSET = 3;

    private static final Identifier PARTY_BG = Identifier.of("rpg-systems", "textures/gui/party_hud.png");
    private static boolean bgFiltered = false;

    private static final boolean staminaAvailableCached = FabricLoader.getInstance().isModLoaded("staminaattributes");
    private static final boolean manaAvailableCached = FabricLoader.getInstance().isModLoaded("manaattributes");
    private static final boolean rpgManaAvailableCached = FabricLoader.getInstance().isModLoaded("rpgmana");

    private static final float EFFECT_ICON_SIZE = 3f;
    private static final float EFFECT_ICON_GAP  = 2f;
    private static final int   EFFECTS_PER_COLUMN = 2;
    private static final float POTION_RIGHT_OFFSET = -18f;
    private static final int   EFFECT_BASE_PX = 18;
    private static final Identifier TEX_BAR_OUTLINE_HUD = Identifier.of("rpg-systems","textures/gui/party_hud/bar_outline_hud.png");
    private static final Identifier TEX_HP_FILL_HUD     = Identifier.of("rpg-systems","textures/gui/party_hud/health_bar_hud.png");
    private static final Identifier TEX_ABSORB_FILL_HUD = Identifier.of("rpg-systems","textures/gui/party_hud/absorption_bar_hud.png");
    private static final Identifier TEX_MANA_FILL_HUD   = Identifier.of("rpg-systems","textures/gui/party_hud/mana_bar_hud.png");
    private static final Identifier TEX_HUNGER_FILL_HUD = Identifier.of("rpg-systems","textures/gui/party_hud/hunger_bar_hud.png");
    private static final int HUD_TEX_SRC_W = 108;
    private static final int HUD_TEX_SRC_H = 8;

    private static boolean hudBarTexturesFiltered = false;
    public static void init() {
        HudRenderCallback.EVENT.register(new PartyHud());
        ClientPlayNetworking.registerGlobalReceiver(
                PartyStatusEffectsPayloads.MemberEffects.ID,
                (payload, context) -> {
                    List<Identifier> ids = new ArrayList<>();
                    for (String s : payload.effectIds()) {
                        try { ids.add(Identifier.of(s)); } catch (Throwable ignored) {}
                    }
                    if (DEBUG_EFFECTS) {
                        log("recv effects for " + payload.memberId() + " -> size=" + ids.size() + " " + ids);
                    }
                    context.client().execute(() -> ClientPartyStatusEffects.update(payload.memberId(), ids));
                }
        );

        if (DEBUG_EFFECTS) log("PartyHud init: payload + receiver registered");
    }
    private static void drawHudTexturedWhole(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) HUD_TEX_SRC_W, h / (float) HUD_TEX_SRC_H, 1f);
        ctx.drawTexture(tex, 0, 0, 0, 0, HUD_TEX_SRC_W, HUD_TEX_SRC_H, HUD_TEX_SRC_W, HUD_TEX_SRC_H);
        m.pop();
    }
    private static void drawHudBarSmart(DrawContext ctx, int x, int y, int w, int h, RenderBar b) {
        switch (b.kind) {
            case HP -> {
                float hp  = clamp01(b.pct);
                float abs = clamp01(b.extraPct);

                drawHudTexturedSegment(ctx, TEX_HP_FILL_HUD, x, y, w, h, 0f, hp);

                if (abs > 0f) {
                    drawHudTexturedSegmentWithAlpha(ctx, TEX_ABSORB_FILL_HUD, x, y, w, h, 0f, abs, 0.75f);
                }

                drawHudTexturedWhole(ctx, TEX_BAR_OUTLINE_HUD, x, y, w, h);
            }
            case HUNGER -> {
                drawHudTexturedSegment(ctx, TEX_HUNGER_FILL_HUD, x, y, w, h, 0f, clamp01(b.pct));
                drawHudTexturedWhole(ctx, TEX_BAR_OUTLINE_HUD, x, y, w, h);
            }
            case MANA, RPGMANA -> {
                drawHudTexturedSegment(ctx, TEX_MANA_FILL_HUD, x, y, w, h, 0f, clamp01(b.pct));
                drawHudTexturedWhole(ctx, TEX_BAR_OUTLINE_HUD, x, y, w, h);
            }
            case STAMINA -> {
                drawBarPretty(ctx, x, y, w, h, clamp01(b.pct), BAR_BG, BAR_BORDER, b.color);
            }
        }
    }
    private static void drawHudTexturedSegment(DrawContext ctx, Identifier tex, int x, int y, int w, int h, float startPct, float endPct) {
        float s = Math.max(0f, Math.min(1f, startPct));
        float e = Math.max(0f, Math.min(1f, endPct));
        if (e <= s) return;

        int srcU = Math.round(HUD_TEX_SRC_W * s);
        int srcW = Math.max(0, Math.round(HUD_TEX_SRC_W * (e - s)));

        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) HUD_TEX_SRC_W, h / (float) HUD_TEX_SRC_H, 1f);
        ctx.drawTexture(tex, srcU, 0, srcU, 0, srcW, HUD_TEX_SRC_H, HUD_TEX_SRC_W, HUD_TEX_SRC_H);
        m.pop();
    }

    private static void drawHudTexturedSegmentWithAlpha(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                                                        float startPct, float endPct, float alpha) {
        float s = Math.max(0f, Math.min(1f, startPct));
        float e = Math.max(0f, Math.min(1f, endPct));
        if (e <= s) return;

        int srcU = Math.round(HUD_TEX_SRC_W * s);
        int srcW = Math.max(0, Math.round(HUD_TEX_SRC_W * (e - s)));

        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) HUD_TEX_SRC_W, h / (float) HUD_TEX_SRC_H, 1f);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
        ctx.drawTexture(tex, srcU, 0, srcU, 0, srcW, HUD_TEX_SRC_H, HUD_TEX_SRC_W, HUD_TEX_SRC_H);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        m.pop();
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        var cfg = PartyHudClientConfig.get();
        if (!cfg.hudEnabled) return;

        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden || mc.player == null) return;

        ensureBgNearest();
        ensureHudBarTexturesNearest();

        final int xAnchor = cfg.partyHudX;
        final int yAnchorStart = cfg.partyHudY;

        float hudScale = cfg.hudScale;
        if (hudScale <= 0f) hudScale = 1f;
        if (hudScale < 0.5f) hudScale = 0.5f;
        if (hudScale > 3.0f) hudScale = 3.0f;

        final int unscaledRowStep = Math.round(PANEL_H * BG_SCALE) + ROW_GAP;
        final int rowStep = Math.round(unscaledRowStep * hudScale);

        if (DEBUG_FORCE_DUMMY) {
            int yScreen = yAnchorStart;
            for (int i = 0; i < DEBUG_DUMMY_COUNT; i++) {
                String name = "Dummy_" + (i + 1);
                int level = (i + 1) * 3;
                UUID uuid = UUID.nameUUIDFromBytes(("rpgsystems:dummy:" + i).getBytes(StandardCharsets.UTF_8));
                var mtx = ctx.getMatrices();
                mtx.push();
                mtx.translate(xAnchor, yScreen, 0);
                mtx.scale(hudScale, hudScale, 1f);
                drawRow(ctx, 0, 0, uuid, name, level, cfg);
                mtx.pop();
                yScreen += rowStep;
            }
            return;
        }

        List<ClientPartyHudData.Member> list = ClientPartyHudData.membersSortedExcludingSelf();
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
        drawTextureScaled(ctx, PARTY_BG, x, y, PANEL_W, PANEL_H, 128, 32, BG_SCALE);

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
        drawScaledTextF(ctx, tr, lvlText, lvlTextX, lvlTextY, LVL_COLOR, TEXT_SCALE);

        final int slotW = PLATE_W;
        final int slotH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE)) + NAME_PAD_Y * 2;
        final float slotXF = x + scaledW - PLATE_INSET - PLATE_W + PLATE_SHIFT_X;
        final float slotYF = y + PLATE_INSET + PLATE_SHIFT_Y;

        String display = (name == null ? "Unknown" : name);
        if (display.length() > NAME_CHAR_MAX) display = display.substring(0, NAME_CHAR_MAX);
        int innerMaxPx = Math.max(0, slotW - NAME_PAD_X * 2);
        display = ellipsizeScaled(tr, display, innerMaxPx, TEXT_SCALE);

        int textW = Math.round(tr.getWidth(display) * TEXT_SCALE);
        int textH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
        float textXF = slotXF + (PLATE_W - textW) / 2f;
        float textYF = slotYF + ( (Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE)) + NAME_PAD_Y * 2) - textH ) / 2f;
        drawScaledTextF(ctx, tr, display, textXF, textYF, 0xFFFFFFFF, TEXT_SCALE);

        final int modelRight = modelCenterX + MODEL_SIZE / 2;
        final int barX = modelRight + BAR_GAP_FROM_MODEL;
        final int barRight = x + scaledW - BAR_RIGHT_PAD - BAR_END_SHORTEN;
        final int barW = Math.max(0, barRight - barX);
        final int barH = Math.max(1, Math.round(BAR_H_BASE * BG_SCALE));

        List<RenderBar> bars = collectBars(uuid, cfg);
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
                drawScaledText(ctx, tr, txt, tX, tY, BAR_TEXT_COLOR, TEXT_SCALE);
                if (DEBUG_BOXES) drawBorder(ctx, barX, by, barW, barH, 0x80FF00FF);
            }
        }

        if (cfg.showArrows) {
            Identifier arrow = pickArrow(uuid);
            if (arrow != null) {
                int aw = Math.round(ARROW_W * BG_SCALE);
                int ah = Math.round(ARROW_H * BG_SCALE);
                int ax = x + scaledW - ARROW_INSET - aw - 15;
                int ay = y + scaledH - ARROW_INSET - ah;
                drawTextureScaled(ctx, arrow, ax - 1, ay + 3, ARROW_W, ARROW_H, ARROW_W, ARROW_H, BG_SCALE);
                if (DEBUG_BOXES) drawBorder(ctx, ax, ay, aw, ah, 0x80FFFF00);
            }
        }

        List<Identifier> theirEffects = ClientPartyStatusEffects.get(uuid);
        if (DEBUG_EFFECTS) {
            log("effects for " + name + " [" + uuid + "]: count=" + (theirEffects == null ? 0 : theirEffects.size()) + " " + theirEffects);
        }
        if (theirEffects != null && !theirEffects.isEmpty()) {
            float anchorX = x + scaledW + POTION_RIGHT_OFFSET - EFFECT_ICON_SIZE;
            float boxH = scaledH;
            if (DEBUG_EFFECTS) log("effect anchor for " + name + ": anchorX=" + anchorX + " topY=" + y + " boxH=" + boxH);
            drawMemberEffectIcons(ctx, anchorX, y, boxH, theirEffects);
        }

        if (DEBUG_BOXES) {
            drawBorder(ctx, x, y, scaledW, Math.round(PANEL_H * BG_SCALE), 0x8040FFFF);
            drawBorder(ctx, modelCenterX - MODEL_SIZE / 2, modelBottomY - MODEL_SIZE, MODEL_SIZE, MODEL_SIZE, 0x80FF4040);
        }
    }

    private Identifier pickArrow(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return null;

        PlayerEntity self = mc.player;
        PlayerEntity target = mc.world.getPlayerByUuid(uuid);
        if (target == null) return null;

        double dx = target.getX() - self.getX();
        double dz = target.getZ() - self.getZ();

        double yawRad = Math.toRadians(self.getYaw());
        double forwardDot = (-Math.sin(yawRad)) * dx + (Math.cos(yawRad)) * dz;
        double rightDot   = ( Math.cos(yawRad)) * dx + (Math.sin(yawRad)) * dz;

        double af = Math.abs(forwardDot);
        double ar = Math.abs(rightDot);

        if (af >= ar) {
            return forwardDot >= 0 ? ARROW_UP : ARROW_DOWN;
        } else {
            return rightDot >= 0 ? ARROW_LEFT : ARROW_RIGHT;
        }
    }

    private void drawMemberModel(DrawContext ctx, int centerX, int bottomY, int size, UUID uuid, String fallbackName) {
        PlayerEntity pe = getRenderablePlayer(uuid, fallbackName);
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

    private static final Map<UUID, OtherClientPlayerEntity> DUMMIES = new HashMap<>();

    private static PlayerEntity getRenderablePlayer(UUID uuid, String fallbackName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return null;
        ClientWorld world = mc.world;
        if (world == null) return null;

        PlayerEntity live = world.getPlayerByUuid(uuid);
        if (live != null) return live;

        OtherClientPlayerEntity cached = DUMMIES.get(uuid);
        if (cached != null && cached.clientWorld == world) return cached;

        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        GameProfile profile = null;
        if (nh != null && nh.getPlayerListEntry(uuid) != null) {
            profile = nh.getPlayerListEntry(uuid).getProfile();
        }
        if (profile == null) profile = new GameProfile(uuid, fallbackName != null ? fallbackName : uuid.toString());

        OtherClientPlayerEntity dummy = new OtherClientPlayerEntity(world, profile);
        dummy.bodyYaw = 180f;
        DUMMIES.put(uuid, dummy);
        return dummy;
    }

    private static void drawTextureScaled(DrawContext ctx, Identifier tex, int x, int y,
                                          int w, int h, int texW, int texH, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawTexture(tex, 0, 0, 0, 0, w, h, texW, texH);
        m.pop();
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static void drawScaledTextF(DrawContext ctx, TextRenderer tr, String text, float x, float y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private static String ellipsizeScaled(TextRenderer tr, String s, int maxPx, float scale) {
        if (Math.round(tr.getWidth(s) * scale) <= maxPx) return s;
        String dots = "...";
        int dw = Math.round(tr.getWidth(dots) * scale);
        StringBuilder b = new StringBuilder(s);
        while (b.length() > 0 && Math.round(tr.getWidth(b.toString()) * scale) + dw > maxPx) b.deleteCharAt(b.length() - 1);
        return b + dots;
    }
    private static void ensureHudBarTexturesNearest() {
        if (hudBarTexturesFiltered) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();
        AbstractTexture o = tm.getTexture(TEX_BAR_OUTLINE_HUD); if (o != null) o.setFilter(false, false);
        AbstractTexture h = tm.getTexture(TEX_HP_FILL_HUD);     if (h != null) h.setFilter(false, false);
        AbstractTexture a = tm.getTexture(TEX_ABSORB_FILL_HUD); if (a != null) a.setFilter(false, false);
        AbstractTexture m = tm.getTexture(TEX_MANA_FILL_HUD);   if (m != null) m.setFilter(false, false);
        AbstractTexture g = tm.getTexture(TEX_HUNGER_FILL_HUD); if (g != null) g.setFilter(false, false);
        hudBarTexturesFiltered = true;
    }
    private static void ensureBgNearest() {
        if (bgFiltered) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();

        AbstractTexture tex = tm.getTexture(PARTY_BG);
        if (tex != null) tex.setFilter(false, false);

        AbstractTexture a1 = tm.getTexture(ARROW_UP);    if (a1 != null) a1.setFilter(false, false);
        AbstractTexture a2 = tm.getTexture(ARROW_DOWN);  if (a2 != null) a2.setFilter(false, false);
        AbstractTexture a3 = tm.getTexture(ARROW_LEFT);  if (a3 != null) a3.setFilter(false, false);
        AbstractTexture a4 = tm.getTexture(ARROW_RIGHT); if (a4 != null) a4.setFilter(false, false);

        bgFiltered = true;
    }

    private enum Kind { HP, HUNGER, STAMINA, MANA, RPGMANA }

    private static final class RenderBar {
        final Kind kind;
        final float pct;
        final float extraPct;
        final String text;
        final int color;
        RenderBar(Kind kind, float pct, float extraPct, String text, int color) {
            this.kind = kind;
            this.pct = clamp01(pct);
            this.extraPct = clamp01(extraPct);
            this.text = text;
            this.color = color;
        }
    }


    private static List<RenderBar> collectBars(UUID uuid, PartyHudClientConfig cfg) {
        List<RenderBar> out = new ArrayList<>();
        ClientPartyHudData.Member mem = ClientPartyHudData.getByUuid(uuid);
        if (cfg.showHpBar) {
            float pct = 0f, absPct = 0f;
            int now = -1, max = -1;

            if (mem != null) {
                max = Math.round(mem.maxHealth);
                pct    = mem.maxHealth > 0 ? mem.health     / mem.maxHealth : 0f;
                absPct = mem.maxHealth > 0 ? mem.absorption / mem.maxHealth : 0f;
                now = Math.round(mem.health + mem.absorption);
            }

            String txt = (now >= 0 && max > 0) ? (now + " / " + max) : "?? / ??";
            out.add(new RenderBar(Kind.HP, pct, absPct, txt, BAR_HP_FILL));
        }

        if (cfg.showHungerBar) {
            float pct = (mem != null && mem.hunger >= 0) ? Math.max(0f, Math.min(1f, mem.hunger / 20f)) : 0f;
            String txt = (mem != null && mem.hunger >= 0) ? (mem.hunger + " / 20") : "?? / 20";
            out.add(new RenderBar(Kind.HUNGER, pct, 0f, txt, BAR_HUNGER_FILL));
        }
        if (cfg.showStaminaBar && staminaAvailableCached) {
            float now = -1f, max = -1f;
            if (mem != null && mem.staminaMax > 0f) {
                now = mem.staminaNow; max = mem.staminaMax;
            } else {
                StackedValue v = queryTracked(uuid, true);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? now / max : 0f;
            String txt = (now >= 0 && max > 0) ? ((int)now + " / " + (int)max) : "?? / ??";
            out.add(new RenderBar(Kind.STAMINA, pct, 0f, txt, BAR_STAM_FILL));
        }

        if (cfg.showManaBar && manaAvailableCached) {
            float now = -1f, max = -1f;
            if (mem != null && mem.manaMax > 0f) {
                now = mem.manaNow; max = mem.manaMax;
            } else {
                StackedValue v = queryTracked(uuid, false);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? now / max : 0f;
            String txt = (max > 0) ? ((int)now + " / " + (int)max) : "?? / ??";
            out.add(new RenderBar(Kind.MANA, pct, 0f, txt, BAR_MANA_FILL));
        }

        if (cfg.showRpgManaBar && rpgManaAvailableCached) {
            float now = -1f, max = -1f;
            if (mem != null && mem.rpgManaMax > 0f) {
                now = mem.rpgManaNow; max = mem.rpgManaMax;
            } else {
                StackedValue v = queryRpgMana(uuid);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? now / max : 0f;
            String txt = (max > 0) ? ((int)now + " / " + (int)max) : "?? / ??";
            out.add(new RenderBar(Kind.RPGMANA, pct, 0f, txt, BAR_RPGMANA_FILL));
        }


        return out;
    }

    private static final class StackedValue {
        final float now;
        final float max;
        StackedValue(float now, float max) { this.now = now; this.max = max; }
    }

    private static void drawBarPretty(DrawContext ctx, int x, int y, int w, int h, float pct,
                                      int bgARGB, int borderARGB, int fillARGB) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, bgARGB);
        int iw = Math.max(0, Math.round(w * Math.max(0f, Math.min(1f, pct))));
        if (iw > 0) ctx.fill(x, y, x + iw, y + h, fillARGB);
    }

    private void drawMemberEffectIcons(DrawContext ctx, float anchorX, float topY, float boxHeight, List<Identifier> effectIds) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        int total = effectIds.size();
        if (total <= 0) return;

        int z = 400;
        int index = 0;
        int col = 0;

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

                drawSpriteScaledF(ctx, x, y, z, EFFECT_ICON_SIZE, sprite);
            }
            col++;
        }
    }


    private static void drawSpriteScaledF(DrawContext ctx, float x, float y, int z, float size, Sprite sprite) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        float s = size / EFFECT_BASE_PX;
        m.scale(s, s, 1f);
        ctx.drawSprite(0, 0, z, EFFECT_BASE_PX, EFFECT_BASE_PX, sprite);
        m.pop();
    }

    private static StackedValue queryTracked(UUID uuid, boolean stamina) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new StackedValue(-1, -1);

        if (stamina && !staminaAvailableCached) return new StackedValue(-1, -1);
        if (!stamina && !manaAvailableCached)   return new StackedValue(-1, -1);

        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new StackedValue(-1, -1);

        if (stamina) {
            float[] pair = TrbAttributesCompat.readStamina(e);
            return new StackedValue(pair[0], pair[1]);
        } else {
            float[] pair = TrbAttributesCompat.readMana(e);
            return new StackedValue(pair[0], pair[1]);
        }
    }

    private static StackedValue queryRpgMana(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new StackedValue(-1, -1);
        if (!rpgManaAvailableCached) return new StackedValue(-1, -1);

        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new StackedValue(-1, -1);

        float[] pair = RpgManaCompat.readMana(e);
        return new StackedValue(pair[0], pair[1]);
    }

    private static void log(String msg) {
        try {
            RPGSystems.LOGGER.info("[PartyHud] " + msg);
        } catch (Throwable t) {
            System.out.println("[PartyHud] " + msg);
        }
    }
}
